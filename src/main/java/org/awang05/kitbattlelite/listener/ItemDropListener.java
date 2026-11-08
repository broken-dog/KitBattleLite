package org.awang05.kitbattlelite.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.awang05.kitbattlelite.arena.ArenaService;
import org.awang05.kitbattlelite.item.MushroomStewService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 阻止竞技场内丢弃物品
 *
 * <p>避免把职业装备/按钮丢到地图上，同时让吃完蘑菇煲留下的碗能扔掉并自动清理。
 *
 * <p>创造模式放行，方便管理员在预览里摆东西。
 */
public final class ItemDropListener implements Listener {
    private final JavaPlugin plugin;
    private final ArenaService arenaService;
    private final MushroomStewService stews;

    public ItemDropListener(JavaPlugin plugin, ArenaService arenaService, MushroomStewService stews) {
        this.plugin = plugin;
        this.arenaService = arenaService;
        this.stews = stews;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (!arenaService.isInArena(player)) {
            return;
        }
        Item drop = event.getItemDrop();
        if (stews.enabled() && drop.getItemStack().getType() == Material.BOWL) {
            drop.setCanPlayerPickup(false);
            drop.setCanMobPickup(false);
            Bukkit.getScheduler().runTaskLater(plugin, drop::remove, 20L);
            return;
        }
        event.setCancelled(true);
    }
}
