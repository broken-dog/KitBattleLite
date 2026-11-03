package org.awang05.kitbattlelite.listener;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.awang05.kitbattlelite.arena.ArenaService;

/**
 * 竞技场内禁止丢弃物品：避免把职业装备/按钮丢到地图上。
 *
 * <p>创造模式放行，方便管理员在预览里摆东西。
 */
public final class ItemDropListener implements Listener {
    private final ArenaService arenaService;

    public ItemDropListener(ArenaService arenaService) {
        this.arenaService = arenaService;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (arenaService.isInArena(player)) {
            event.setCancelled(true);
        }
    }
}
