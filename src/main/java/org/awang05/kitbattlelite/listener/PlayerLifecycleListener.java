package org.awang05.kitbattlelite.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.awang05.kitbattlelite.arena.Arena;
import org.awang05.kitbattlelite.arena.ArenaService;
import org.awang05.kitbattlelite.gui.MenuFactory;
import org.awang05.kitbattlelite.player.SessionRegistry;

import java.util.Optional;
import java.util.UUID;

/**
 * 退服与换世界：把玩家从竞技场安全请出并清掉会话条目。
 *
 * <p>换世界只在“离开竞技场所在世界”时离场；离场后刷新地图选择页人数。
 */
public final class PlayerLifecycleListener implements Listener {
    private final SessionRegistry sessions;
    private final ArenaService arenaService;
    private final MenuFactory menus;

    public PlayerLifecycleListener(SessionRegistry sessions, ArenaService arenaService, MenuFactory menus) {
        this.sessions = sessions;
        this.arenaService = arenaService;
        this.menus = menus;
    }

    /** 退服：恢复背包但不传送，随后移除会话。 */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Optional<Arena> arena = arenaService.arenaOf(player);
        arenaService.leave(player, false);
        sessions.remove(playerId);
        arena.ifPresent(menus::updateWorldSelectIcon);
    }

    /** 换世界：离开竞技场世界就离场 */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Optional<Arena> arena = arenaService.arenaOf(player);
        if (arena.isEmpty()) {
            return;
        }
        if (player.getWorld().getName().equals(arena.get().worldName())) {
            return;
        }
        arenaService.leave(player, true);
        menus.updateWorldSelectIcon(arena.get());
    }
}
