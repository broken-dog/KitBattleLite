package org.awang05.kitbattlelite.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.awang05.kitbattlelite.arena.Arena;
import org.awang05.kitbattlelite.arena.ArenaService;
import org.awang05.kitbattlelite.config.Messages;
import org.awang05.kitbattlelite.gui.MenuFactory;
import org.awang05.kitbattlelite.item.ItemService;
import org.awang05.kitbattlelite.item.PluginItem;
import org.awang05.kitbattlelite.kit.KitRepository;

import java.util.Optional;

/**
 * 快捷栏按钮监听
 *
 * <p>按钮被带出竞技场时直接没收并提示。选择职业打开第一个存在的职业页。
 */
public final class HotbarButtonListener implements Listener {
    private final ItemService items;
    private final KitRepository kits;
    private final ArenaService arenaService;
    private final MenuFactory menus;
    private final Messages messages;

    public HotbarButtonListener(ItemService items, KitRepository kits, ArenaService arenaService,
                                MenuFactory menus, Messages messages) {
        this.items = items;
        this.kits = kits;
        this.arenaService = arenaService;
        this.menus = menus;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!event.getAction().isRightClick()) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir() || !items.isPluginItem(item)) {
            return;
        }
        Player player = event.getPlayer();
        if (!arenaService.isInArena(player)) {
            confiscate(player);
            return;
        }
        if (items.is(item, PluginItem.SELECT)) {
            event.setCancelled(true);
            Integer firstPage = kits.pageIndexes().stream().findFirst().orElse(null);
            menus.openKitPage(player, firstPage == null ? 1 : firstPage);
        } else if (items.is(item, PluginItem.LEAVE)) {
            event.setCancelled(true);
            Optional<Arena> arena = arenaService.arenaOf(player);
            arenaService.leave(player, true);
            arena.ifPresent(menus::updateWorldSelectIcon);
        }
    }

    /** 竞技场外还拿着按钮：直接没收，避免带出插件物品。 */
    private void confiscate(Player player) {
        messages.send(player, "command.button-outside-arena");
        player.getInventory().setItemInMainHand(null);
    }
}
