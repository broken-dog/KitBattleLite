package org.awang05.kitbattlelite.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/**
 * 把库存事件分派给对应的 {@link Menu}，按 holder 判断。
 *
 * <p>生命周期：组合根创建并注册一次；无状态，不持有玩家或菜单。
 */
public final class MenuListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Menu menu)) {
            return;
        }
        int rawSlot = event.getRawSlot();
        boolean topClick = rawSlot >= 0 && rawSlot < top.getSize();
        menu.onClick(player, event, topClick);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) {
            return;
        }
        menu.onDrag(player, event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory closed = event.getInventory();
        if (!(closed.getHolder() instanceof Menu menu)) {
            return;
        }
        menu.onClose(player, closed);
    }
}
