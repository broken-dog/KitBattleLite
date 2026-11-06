package org.awang05.kitbattlelite.listener;

import org.awang05.kitbattlelite.arena.ArenaRegistry;
import org.awang05.kitbattlelite.gui.Menu;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.InventoryHolder;

/**
 * 锁定竞技场世界里的方块容器与末影箱
 *
 * <p>只针对 {@link BlockInventoryHolder}（箱子、木桶、发射器、潜影盒等）和末影箱
 * 玩家自己的背包与合成不锁，插件菜单（top holder 是 {@link Menu}）交给菜单自己处理。
 * 只拦交互不拦打开，玩家仍能看到容器界面但动不了内容。
 *
 * <p>创造模式放行。
 *
 * <p>生命周期：组合根创建并注册一次；无状态，不持有玩家引用。
 */
public final class InventoryListener implements Listener {
    private final ArenaRegistry arenas;

    public InventoryListener(ArenaRegistry arenas) {
        this.arenas = arenas;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        onInventoryInteract(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        onInventoryInteract(event);
    }

    public void onInventoryInteract(InventoryInteractEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof Menu) {
            return;     // 菜单交给菜单自己处理
        }
        if (!(holder instanceof BlockInventoryHolder)
                && event.getView().getType() != InventoryType.ENDER_CHEST) {
            return;     // 只锁世界里的方块容器和末影箱，背包/合成/实体库存不管
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;     // 创造模式放行，可能是管理员在操作
        }
        if (arenas.byWorld(player.getWorld()).isEmpty()) {
            return;     // 不在竞技场世界
        }
        event.setCancelled(true);
    }
}
