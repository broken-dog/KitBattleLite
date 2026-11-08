package org.awang05.kitbattlelite.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.awang05.kitbattlelite.arena.ArenaCombatService;
import org.awang05.kitbattlelite.arena.ArenaService;

import java.util.UUID;

/**
 * 死亡与重生：竞技场内死亡交给 {@link ArenaCombatService#handleDeath(Player)}，重生点摆回出生点。
 *
 * <p>死亡不取消，只清掉原版死亡播报（插件自己广播）；重生后下一 tick 再摆位置/给按钮。
 */
public final class PlayerDeathListener implements Listener {
    private final JavaPlugin plugin;
    private final ArenaService arenaService;
    private final ArenaCombatService combatService;

    public PlayerDeathListener(JavaPlugin plugin, ArenaService arenaService,
                               ArenaCombatService combatService) {
        this.plugin = plugin;
        this.arenaService = arenaService;
        this.combatService = combatService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!arenaService.isInArena(player)) {
            return;
        }
        combatService.handleDeath(player);
        event.deathMessage(null);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!arenaService.isInArena(player)) {
            return;
        }
        combatService.respawnLocation(player).ifPresent(event::setRespawnLocation);
        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player online = Bukkit.getPlayer(playerId);
            if (online != null) {
                combatService.afterRespawn(online);
            }
        });
    }
}
