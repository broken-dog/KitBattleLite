package org.awang05.kitbattlelite.arena;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.awang05.kitbattlelite.config.Messages;
import org.awang05.kitbattlelite.config.SettingsService;
import org.awang05.kitbattlelite.hud.HealthDisplayService;
import org.awang05.kitbattlelite.hud.SidebarService;
import org.awang05.kitbattlelite.item.MushroomStewService;
import org.awang05.kitbattlelite.kit.Kit;
import org.awang05.kitbattlelite.kit.KitRepository;
import org.awang05.kitbattlelite.player.PlayerSession;
import org.awang05.kitbattlelite.player.StatsRepository;

import java.util.Optional;

/**
 * 战斗结算流程：死亡、击杀、重生、选职业。
 *
 * <p>只负责“对局中发生了什么”，进出场与出生点摆放复用 {@link ArenaService}，查询也走它，
 * 形成单向依赖（战斗 → 会话），反向不依赖。
 *
 * <p>生命周期：组合根在 {@link ArenaService} 之后创建；无状态，状态都在会话表与战绩里。
 */
public final class ArenaCombatService {
    private final ArenaService arenaService;
    private final StatsRepository stats;
    private final KitRepository kits;
    private final SettingsService settings;
    private final Messages messages;
    private final SidebarService sidebar;
    private final HealthDisplayService health;
    private final MushroomStewService stews;

    public ArenaCombatService(ArenaService arenaService, StatsRepository stats, KitRepository kits,
                              SettingsService settings, Messages messages, SidebarService sidebar,
                              HealthDisplayService health, MushroomStewService stews) {
        this.arenaService = arenaService;
        this.stats = stats;
        this.kits = kits;
        this.settings = settings;
        this.messages = messages;
        this.sidebar = sidebar;
        this.health = health;
        this.stews = stews;
    }

    /**
     * 死亡处理：统计并落盘、清背包（避免职业物品掉落）、清药水效果、清自己射出的末影珍珠、播报。
     *
     * <p>不在这里传送/给失明——那些放在重生后（{@link #afterRespawn(Player)}），否则会被
     * 死亡流程清掉。
     */
    public void handleDeath(Player victim) {
        PlayerSession session = arenaService.session(victim);
        if (session == null || session.arenaId() == null || session.isPreview()) {
            return;
        }
        Optional<Arena> arenaOptional = arenaService.arenaOf(victim);
        if (arenaOptional.isEmpty()) {
            return;
        }
        Arena arena = arenaOptional.get();

        stats.getOrCreate(victim.getUniqueId(), victim.getName()).addDeath();
        session.setKillStreak(0);
        session.setProtectionUntil(PlayerSession.PROTECTION_NONE);
        session.setEdit(null);
        clearInventory(victim);
        victim.clearActivePotionEffects();
        removeEnderPearls(victim);
        victim.setFireTicks(0);
        sidebar.refresh(victim.getUniqueId());
        health.update(victim);

        Player killer = victim.getKiller();
        if (killer == null) {
            broadcast(arena, messages.component("arena.death-unknown", "victim", victim.getName()));
            return;
        }
        if (killer.equals(victim)) {
            broadcast(arena, messages.component("arena.death-self", "victim", victim.getName()));
            return;
        }
        PlayerSession killerSession = arenaService.session(killer);
        if (killerSession == null || killerSession.isPreview()
                || !arena.id().equals(killerSession.arenaId())) {   // 异常边界
            broadcast(arena, messages.component("arena.death-normal", "victim", victim.getName()));
            return;
        }

        killerSession.setKillStreak(killerSession.killStreak() + 1);
        stats.getOrCreate(killer.getUniqueId(), killer.getName()).addKill();
        sidebar.refresh(killer.getUniqueId());
        broadcastKill(arena, victim, killer);
        stats.save();
    }

    /** 重生点：在竞技场里返回出生点，否则空 */
    public Optional<Location> respawnLocation(Player player) {
        return arenaService.arenaOf(player).map(Arena::spawn);
    }

    /** 重生之后调用：摆回出生点、发按钮、给免伤与失明。 */
    public void afterRespawn(Player player) {
        PlayerSession session = arenaService.session(player);
        if (session == null || session.arenaId() == null) {
            return;
        }
        Optional<Arena> arenaOptional = arenaService.arenaOf(player);
        if (arenaOptional.isEmpty()) {
            return;
        }
        arenaService.placeAtSpawn(player, arenaOptional.get());
        session.setProtectionUntil(PlayerSession.PROTECTION_PERMANENT);
        int blindnessTicks = settings.current().deathBlindnessTicks();
        if (blindnessTicks > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessTicks, 1));
        }
        sidebar.refresh(player.getUniqueId());
        health.update(player);
    }

    /**
     * 选择职业：应用套装 → 打防堆叠标记 → 传送到战斗点 → 重新计时免伤。
     *
     * @return false 表示不在竞技场
     * @throws IllegalArgumentException 职业不存在
     */
    public boolean selectKit(Player player, String kitId) throws IllegalArgumentException {
        Optional<Kit> kit = kits.findKit(kitId);
        if (kit.isEmpty()) {
            throw new IllegalArgumentException("职业 " + kitId + " 不存在");
        }
        PlayerSession session = arenaService.session(player);
        if (session == null || session.arenaId() == null || session.isPreview()) {
            return false;
        }
        Optional<Arena> arena = arenaService.arenaOf(player);
        if (arena.isEmpty()) {
            return false;
        }
        player.teleport(arena.get().teleport());
        kit.get().applyTo(player);
        stews.markApplied(player);
        session.setProtectionUntil(protectionDeadline());
        sidebar.refresh(player.getUniqueId());
        return true;
    }

    private void clearInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(null);
        inventory.setItemInOffHand(null);
    }

    private void removeEnderPearls(Player player) {
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity instanceof EnderPearl pearl && player.equals(pearl.getShooter())) {
                entity.remove();
            }
        }
    }

    private void broadcastKill(Arena arena, Player victim, Player killer) {
        broadcast(arena, messages.component("arena.death-killed",
                "victim", victim.getName(), "killer", killer.getName()));
        victim.showTitle(Title.title(
                messages.component("arena.kill-victim-title", "killer", killer.getName()),
                Component.empty()));
        killer.showTitle(Title.title(
                Component.empty(),
                messages.component("arena.kill-killer-title")));
        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    /** 只发给同一张图上、非预览的玩家。 */
    private void broadcast(Arena arena, Component message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            PlayerSession session = arenaService.session(online);
            if (session != null && !session.isPreview() && arena.id().equals(session.arenaId())) {
                online.sendMessage(message);
            }
        }
    }

    private long protectionDeadline() {
        return System.currentTimeMillis() + settings.current().protectionSeconds() * 1000L;
    }
}
