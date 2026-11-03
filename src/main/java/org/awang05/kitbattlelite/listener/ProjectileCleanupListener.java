package org.awang05.kitbattlelite.listener;

import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.awang05.kitbattlelite.arena.ArenaRegistry;

/**
 * 竞技场里的箭命中后立即移除：避免地上堆满可回收的箭。
 *
 * <p>只处理玩家射出的箭，且只在竞技场世界生效；其它实体/世界不生效。
 *
 * <p>生命周期：组合根创建并注册；无实体引用。
 */
public final class ProjectileCleanupListener implements Listener {
    private final ArenaRegistry arenas;

    public ProjectileCleanupListener(ArenaRegistry arenas) {
        this.arenas = arenas;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!(arrow.getShooter() instanceof Player)) {
            return;
        }
        if (arenas.byWorld(arrow.getWorld()).isEmpty()) {
            return;
        }
        arrow.remove();
    }
}
