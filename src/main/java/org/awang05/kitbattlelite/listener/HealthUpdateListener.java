package org.awang05.kitbattlelite.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.awang05.kitbattlelite.hud.HealthDisplayService;

import java.util.UUID;

/**
 * 受伤/回血后刷新名下血量。
 *
 * <p>在 {@code MONITOR} 观察、不改游戏状态：伤害结算过程中读到的血量还是旧值，所以延迟一 tick 再刷新。
 *
 * <p>生命周期：组合根创建并注册；不持有任何 Bukkit 实体引用。
 */
public final class HealthUpdateListener implements Listener {
    private final JavaPlugin plugin;
    private final HealthDisplayService health;

    public HealthUpdateListener(JavaPlugin plugin, HealthDisplayService health) {
        this.plugin = plugin;
        this.health = health;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        scheduleUpdate(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        scheduleUpdate(event.getEntity());
    }

    private void scheduleUpdate(Entity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player online = Bukkit.getPlayer(playerId);
            if (online != null) {
                health.update(online);
            }
        });
    }
}
