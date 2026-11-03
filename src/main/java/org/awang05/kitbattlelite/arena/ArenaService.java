package org.awang05.kitbattlelite.arena;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.awang05.kitbattlelite.config.Messages;
import org.awang05.kitbattlelite.config.PluginSettings;
import org.awang05.kitbattlelite.hud.HealthDisplayService;
import org.awang05.kitbattlelite.hud.SidebarService;
import org.awang05.kitbattlelite.item.ItemService;
import org.awang05.kitbattlelite.item.PluginItem;
import org.awang05.kitbattlelite.kit.Kit;
import org.awang05.kitbattlelite.kit.KitRepository;
import org.awang05.kitbattlelite.player.PlayerSession;
import org.awang05.kitbattlelite.player.PlayerSnapshot;
import org.awang05.kitbattlelite.player.SessionRegistry;
import org.awang05.kitbattlelite.player.StatsRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 竞技场流程：进图、离场、选职业、死亡/重生、重载。
 *
 * <p>所有玩家状态都通过 {@link SessionRegistry} 的 UUID 会话读写；
 */
public final class ArenaService {
    private final Logger logger;
    private final ArenaRegistry arenas;
    private final SessionRegistry sessions;
    private final StatsRepository stats;
    private final KitRepository kits;
    private final ItemService items;
    private final PluginSettings settings;
    private final Messages messages;
    private final WorldLoader worldLoader;
    private final SidebarService sidebar;
    private final HealthDisplayService health;

    public ArenaService(JavaPlugin plugin, ArenaRegistry arenas, SessionRegistry sessions,
                        StatsRepository stats, KitRepository kits, ItemService items,
                        PluginSettings settings, Messages messages, WorldLoader worldLoader,
                        SidebarService sidebar, HealthDisplayService health) {
        this.logger = plugin.getLogger();
        this.arenas = arenas;
        this.sessions = sessions;
        this.stats = stats;
        this.kits = kits;
        this.items = items;
        this.settings = settings;
        this.messages = messages;
        this.worldLoader = worldLoader;
        this.sidebar = sidebar;
        this.health = health;
    }

    public PlayerSession session(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public boolean isInArena(Player player) {
        PlayerSession session = session(player);
        return session != null && session.arenaId() != null;
    }

    public Optional<Arena> arenaOf(Player player) {
        PlayerSession session = session(player);
        if (session == null || session.arenaId() == null) {
            return Optional.empty();
        }
        return arenas.find(session.arenaId());
    }

    /** 参与人数（不含管理员预览）。 */
    public int participantCount(Arena arena) {
        int count = 0;
        for (PlayerSession session : sessions.all()) {
            if (!session.isPreview() && arena.id().equals(session.arenaId())) {
                count++;
            }
        }
        return count;
    }

    /** 是否在免伤期内 */
    public boolean isProtected(Player player) {
        PlayerSession session = session(player);
        return session != null && session.isPlayerProtected();
    }

    /**
     * 进入已开放的地图：抓快照 → 传出生点 → 清背包/效果 → 冒险 → 给按钮。
     *
     * <p>重复点击同一张地图直接返回。已在别的图里会先安全离场。
     */
    public void join(Player player, Arena arena) {
        PlayerSession session = sessions.getOrCreate(player.getUniqueId());
        if (arena.id().equals(session.arenaId()) && !session.isPreview()) {
            return;
        }
        if (session.arenaId() != null) {
            leave(player, true);
        }

        session.setSnapshot(PlayerSnapshot.capture(player));
        session.setReturnLocation(player.getLocation());
        session.setArenaId(arena.id());
        session.setPreview(false);
        session.setKillStreak(0);
        session.setProtectionUntil(-1);
        session.setEdit(null);

        resetForArena(player, arena.spawn());
        giveButtons(player);
        sidebar.show(player);
        health.join(player);
    }

    /**
     * 管理员预览未开放的地图：同样抓快照/记返回点，但保持创造飞行、不给按钮、不计人数。
     */
    public void preview(Player player, Arena arena) {
        PlayerSession session = sessions.getOrCreate(player.getUniqueId());
        if (arena.id().equals(session.arenaId())) {
            return;
        }
        if (session.arenaId() != null) {
            leave(player, true);
        }

        session.setSnapshot(PlayerSnapshot.capture(player));
        session.setReturnLocation(player.getLocation());
        session.setArenaId(arena.id());
        session.setPreview(true);
        session.setKillStreak(0);
        session.setProtectionUntil(0);
        session.setEdit(null);

        resetForPreview(player, arena);
        sidebar.show(player);
        health.join(player);
    }

    /**
     * 离场：摘掉 HUD → 恢复快照 → 传回返回点 → 清竞技场字段。
     *
     * <p> 不负责更新竞技场图标
     *
     * @param teleport 退服时为 false（离线传送无意义且可能报错）
     */
    public void leave(Player player, boolean teleport) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session == null || session.arenaId() == null) {
            return;
        }
        sidebar.hide(player.getUniqueId());
        health.leave(player);

        Location back = session.returnLocation();
        PlayerSnapshot snapshot = session.snapshot();
        if (snapshot != null) {
            snapshot.restore(player);
            session.setSnapshot(null);
        }
        session.setReturnLocation(null);
        session.clearArenaState();

        if (teleport && player.isOnline()) {
            player.closeInventory();
            player.teleport(resolveReturnLocation(player, back));
        }
    }

    /**
     * 死亡处理：统计并落盘、清背包（避免职业物品掉落）、清自己射出的末影珍珠、播报。
     *
     * <p>不在这里传送/给失明——那些放在重生后（{@link #afterRespawn(Player)}），否则会被
     * 死亡流程清掉。
     */
    public void handleDeath(Player victim) {
        PlayerSession session = session(victim);
        if (session == null || session.arenaId() == null || session.isPreview()) {
            return;
        }
        Optional<Arena> arenaOptional = arenas.find(session.arenaId());
        if (arenaOptional.isEmpty()) {
            return;
        }
        Arena arena = arenaOptional.get();

        stats.getOrCreate(victim.getUniqueId(), victim.getName()).addDeath();
        session.setKillStreak(0);
        session.setProtectionUntil(0);
        session.setEdit(null);
        clearInventory(victim);
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
        PlayerSession killerSession = session(killer);
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
        PlayerSession session = session(player);
        if (session == null || session.arenaId() == null) {
            return Optional.empty();
        }
        return arenas.find(session.arenaId()).map(Arena::spawn);
    }

    /** 重生之后调用：摆回出生点、发按钮、给免伤与失明。 */
    public void afterRespawn(Player player) {
        PlayerSession session = session(player);
        if (session == null || session.arenaId() == null) {
            return;
        }
        Optional<Arena> arenaOptional = arenas.find(session.arenaId());
        if (arenaOptional.isEmpty()) {
            return;
        }
        resetForArena(player, arenaOptional.get().spawn());
        giveButtons(player);
        session.setProtectionUntil(-1);
        if (settings.deathBlindnessTicks() > 0) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.BLINDNESS, settings.deathBlindnessTicks(), 1));
        }
        sidebar.refresh(player.getUniqueId());
        health.update(player);
    }

    /**
     * 选择职业：应用套装 → 传送到战斗点 → 重新计时免伤。
     *
     * @return false 表示不在竞技场
     * @throws IllegalArgumentException 职业不存在
     */
    public boolean selectKit(Player player, String kitId) throws IllegalArgumentException {
        Optional<Kit> kit = kits.findKit(kitId);
        if (kit.isEmpty()) {
            throw new IllegalArgumentException("职业 " + kitId + " 不存在");
        }
        PlayerSession session = session(player);
        if (session == null || session.arenaId() == null || session.isPreview()) {
            return false;
        }
        Optional<Arena> arena = arenas.find(session.arenaId());
        if (arena.isEmpty()) {
            return false;
        }
        player.teleport(arena.get().teleport());
        kit.get().applyTo(player);
        session.setProtectionUntil(protectionDeadline());
        sidebar.refresh(player.getUniqueId());
        return true;
    }

    /**
     * 导入一个世界作为未开放地图：世界已加载就直接用，未加载则尝试从磁盘加载；重复或世界不存在抛
     * {@link IllegalArgumentException}。
     */
    public Arena importWorld(String arenaId, String worldName) {
        if (worldName == null || worldName.isBlank()) {
            throw new IllegalArgumentException("世界名不能为空");
        }
        if (arenaId == null || arenaId.isBlank()) {
            throw new IllegalArgumentException("地图 ID 不能为空");
        }
        String name = worldName.trim();
        World world = worldLoader.load(name);
        if (world == null) {
            throw new IllegalArgumentException("世界 " + name + " 不存在或加载失败");
        }
        if (arenas.byWorldName(name).isPresent()) {
            throw new IllegalArgumentException("世界 " + name + " 已经是竞技场");
        }
        if (arenas.find(arenaId).isPresent()) {
            throw new IllegalArgumentException("地图 id " + arenaId + " 已被占用");
        }
        Arena arena = new Arena(arenaId, name, name, false, world.getSpawnLocation(), world.getSpawnLocation());
        arenas.register(arena);
        logger.info("已导入地图 " + name);
        return arena;
    }

    /**
     * 重载/关服前把在线玩家安全请出：摘 HUD、恢复快照、传回返回点、关闭插件菜单，并清空会话表。
     */
    public void kickAll() {
        for (UUID playerId : sessions.ids()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                leave(player, true);
                player.closeInventory();
            }
            // 会话字段缺失时 leave 会提前返回，这里再兜一次，保证没有残留的侧边栏/血量条目
            sidebar.hide(playerId);
            sessions.remove(playerId);
        }
    }

    /** onDisable 调用：请出所有在线参与者。 */
    public void shutdown() {
        kickAll();
    }

    private void resetForArena(Player player, Location target) {
        player.teleport(target);
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(null);
        inventory.setItemInOffHand(null);
        player.clearActivePotionEffects();
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setHealth(maxHealth(player));
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setFallDistance(0);
    }

    private void resetForPreview(Player player, Arena arena) {
        player.teleport(arena.spawn());
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setHealth(maxHealth(player));
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setFallDistance(0);
    }

    private void giveButtons(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.setItem(0, items.create(PluginItem.SELECT));
        inventory.setItem(8, items.create(PluginItem.LEAVE));
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

    private void broadcast(Arena arena, Component message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            PlayerSession session = sessions.get(online.getUniqueId());
            if (session != null && !session.isPreview() && arena.id().equals(session.arenaId())) {
                online.sendMessage(message);
            }
        }
    }

    private Location resolveReturnLocation(Player player, Location back) {
        if (back != null && back.getWorld() != null) {
            return back;
        }
        logger.warning("玩家 " + player.getName() + " 的返回点为空或所在世界已卸载，回退到主世界出生点");
        if (!Bukkit.getWorlds().isEmpty()) {
            return Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        return player.getWorld().getSpawnLocation();
    }

    private long protectionDeadline() {
        return System.currentTimeMillis() + settings.protectionSeconds() * 1000L;
    }

    private static double maxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attribute == null ? 20.0 : attribute.getValue();
    }
}
