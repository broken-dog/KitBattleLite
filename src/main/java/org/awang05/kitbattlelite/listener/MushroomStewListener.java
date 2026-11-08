package org.awang05.kitbattlelite.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.awang05.kitbattlelite.arena.ArenaService;
import org.awang05.kitbattlelite.item.MushroomStewService;

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
