package org.awang05.kitbattlelite.arena;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.awang05.kitbattlelite.hud.HealthDisplayService;
import org.awang05.kitbattlelite.hud.SidebarService;
import org.awang05.kitbattlelite.item.ItemService;
import org.awang05.kitbattlelite.item.PluginItem;
import org.awang05.kitbattlelite.player.PlayerSession;
import org.awang05.kitbattlelite.player.PlayerSnapshot;
import org.awang05.kitbattlelite.player.SessionRegistry;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 竞技场会话流程：进图、离场、管理员预览、出生点摆放、会话查询与重载。
 *
 * <p>对局里的死亡/击杀/重生/选职业见 {@link ArenaCombatService}；两者单向依赖（战斗 → 会话）。
 *
 * <p>所有玩家状态都通过 {@link SessionRegistry} 的 UUID 会话读写。
 */
public final class ArenaService {
    private final Logger logger;
    private final ArenaRegistry arenas;
    private final SessionRegistry sessions;
    private final ItemService items;
    private final SidebarService sidebar;
    private final HealthDisplayService health;

    public ArenaService(JavaPlugin plugin, ArenaRegistry arenas, SessionRegistry sessions,
                        ItemService items, SidebarService sidebar, HealthDisplayService health) {
        this.logger = plugin.getLogger();
        this.arenas = arenas;
        this.sessions = sessions;
        this.items = items;
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
        enter(player, arena, false);
    }

    /**
     * 把玩家摆到地图出生点：重置状态（传送、清背包/效果、冒险、满血满饥饿）并给底部按钮。
     *
     * <p>进图、重生共用；重生侧由 {@link ArenaCombatService} 调用。
     */
    public void placeAtSpawn(Player player, Arena arena) {
        resetForArena(player, arena.spawn());
        giveButtons(player);
    }

    /**
     * 管理员预览未开放的地图：同样抓快照/记返回点，但保持创造飞行、不给按钮、不计人数。
     */
    public void preview(Player player, Arena arena) {
        enter(player, arena, true);
    }

    /**
     * 进图与预览的共同流程，二者只差三处：会话的 {@code preview} 标记、免伤初值、摆位置的方式
     * （冒险重置 + 按钮 vs 创造飞行）。
     *
     * <p>同图同模式重复点击直接返回；换图或换模式先安全离场，避免快照被竞技场状态覆盖。
     */
    private void enter(Player player, Arena arena, boolean preview) {
        PlayerSession session = sessions.getOrCreate(player.getUniqueId());
        if (arena.id().equals(session.arenaId()) && preview == session.isPreview()) {
            return;
        }
        if (session.arenaId() != null) {
            leave(player, true);
        }

        session.setSnapshot(PlayerSnapshot.capture(player));
        session.setReturnLocation(player.getLocation());
        session.setArenaId(arena.id());
        session.setPreview(preview);
        session.setKillStreak(0);
        session.setProtectionUntil(preview
                ? PlayerSession.PROTECTION_NONE
                : PlayerSession.PROTECTION_PERMANENT);
        session.setEdit(null);

        if (preview) {
            resetForPreview(player, arena);
        } else {
            placeAtSpawn(player, arena);
        }
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
        player.setSaturation(0);
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

    private static double maxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attribute == null ? 20.0 : attribute.getValue();
    }
}
