package org.awang05.kitbattlelite.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.entity.Player;
import org.awang05.kitbattlelite.arena.ArenaService;
import org.awang05.kitbattlelite.config.SettingsService;

import java.util.Objects;

/**
 * 竞技场内保持饱食度：取消所有饱食度下降事件。
 */
public final class FoodRetentionListener implements Listener {
    private final ArenaService arenaService;
    private final SettingsService settings;

    public FoodRetentionListener(ArenaService arenaService, SettingsService settings) {
        this.arenaService = Objects.requireNonNull(arenaService, "arenaService");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!settings.current().foodRetentionEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getFoodLevel() >= player.getFoodLevel()) {
            return;
        }
        if (arenaService.isInArena(player)) {
            event.setCancelled(true);
        }
    }
}
