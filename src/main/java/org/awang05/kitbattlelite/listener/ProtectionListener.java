package org.awang05.kitbattlelite.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.awang05.kitbattlelite.arena.ArenaService;

/**
 * 免伤期：在竞技场内且保护未到期时取消伤害。
 *
 * <p>保护是时间戳判断（{@code PlayerSession.protectionUntil}），没有延迟任务；判断里已经包含
 * “在竞技场”这一前提，所以不在图里的玩家不会被误保护。
 *
 * <p>生命周期：组合根创建并注册；无玩家引用。
 */
public final class ProtectionListener implements Listener {
    private final ArenaService arenaService;

    public ProtectionListener(ArenaService arenaService) {
        this.arenaService = arenaService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (arenaService.isProtected(player)) {
            event.setCancelled(true);
        }
    }
}
