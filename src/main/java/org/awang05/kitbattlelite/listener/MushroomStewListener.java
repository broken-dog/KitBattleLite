package org.awang05.kitbattlelite.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.awang05.kitbattlelite.arena.ArenaService;
import org.awang05.kitbattlelite.item.MushroomStewService;
import org.bukkit.Material;

/**
 * 竞技场内右键蘑菇煲：即时回血并把蘑菇煲换成碗。
 *
 * <p>主手不是本插件发出去的蘑菇煲时完全不干预，原版进食流程照旧。
 */
public final class MushroomStewListener implements Listener {
    private final ArenaService arenaService;
    private final MushroomStewService stews;

    public MushroomStewListener(ArenaService arenaService, MushroomStewService stews) {
        this.arenaService = arenaService;
        this.stews = stews;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            if (event.getHand() == EquipmentSlot.OFF_HAND
            && (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.MUSHROOM_STEW
            || event.getPlayer().getInventory().getItemInMainHand().getType() == Material.BOWL)
            && arenaService.isInArena(event.getPlayer())) {
                Material type =event.getPlayer().getInventory().getItemInOffHand().getType();
                if (type != Material.AIR) {
                    event.getPlayer().setCooldown(type, 0);
                    event.setCancelled(true);
                }
            }
            return;
        }
        if (!event.getAction().isRightClick()) {
            return;
        }
        if (!arenaService.isInArena(event.getPlayer())) {
            return;
        }
        if (stews.use(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        // 忽略子类
        if (event instanceof PlayerInteractAtEntityEvent) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            if (event.getHand() == EquipmentSlot.OFF_HAND
                    && (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.MUSHROOM_STEW
                    || event.getPlayer().getInventory().getItemInMainHand().getType() == Material.BOWL)
                    && arenaService.isInArena(event.getPlayer())) {
                Material type =event.getPlayer().getInventory().getItemInOffHand().getType();
                if (type != Material.AIR) {
                    event.getPlayer().setCooldown(type, 0);
                    event.setCancelled(true);
                }
            }
            return;
        }
        if (!arenaService.isInArena(event.getPlayer())) {
            return;
        }
        if (stews.use(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
