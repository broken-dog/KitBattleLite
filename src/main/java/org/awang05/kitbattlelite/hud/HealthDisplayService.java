package org.awang05.kitbattlelite.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.awang05.kitbattlelite.config.SettingsService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家名下的血量显示（{@code BELOW_NAME}）。
 *
 * <p>FastBoard 是纯发包实现、不碰 Bukkit 计分板，所以这里单独用一块共享计分板：所有参与者看同一块板，
 * 才能互相看到名下的血量。代价是参与者会失去主计分板上的其他目标，因此由
 * {@code health-below-name.enabled} 控制是否启用。
 *
 * <p>计分板与目标延迟到第一次 {@link #join(Player)} 时创建（构造器不访问 Bukkit）；
 * 离开时必须 {@code resetScore} 回收条目并恢复主计分板，否则条目只增不减。
 *
 * <p>生命周期：组合根创建；进图 {@link #join(Player)}，受伤/回血 {@link #update(Player)}，
 * 离场/退服 {@link #leave(Player)}，reload 后 {@link #applySettings()}，onDisable {@link #shutdown()}。
 */
public final class HealthDisplayService {
    private static final String OBJECTIVE_NAME = "kbl_health";
    private static final Component OBJECTIVE_TITLE = Component.text("❤", NamedTextColor.RED);

    private final SettingsService settings;
    private final Set<UUID> viewers = new HashSet<>();
    private Scoreboard scoreboard;
    private Objective objective;

    public HealthDisplayService(SettingsService settings) {
        this.settings = settings;
    }

    /** 进图时调用：切到共享计分板并写入当前血量。 */
    public void join(Player player) {
        if (!settings.current().healthBelowNameEnabled()) {
            return;
        }
        ensureObjective();
        viewers.add(player.getUniqueId());
        player.setScoreboard(scoreboard);
        update(player);
    }

    /** 刷新名下血量；没在显示或已离场时什么都不做。 */
    public void update(Player player) {
        if (objective == null || !viewers.contains(player.getUniqueId())) {
            return;
        }
        objective.getScore(player.getName()).setScore((int) Math.ceil(player.getHealth()));
    }

    /** 离场/退服时调用：回收条目并恢复主计分板。 */
    public void leave(Player player) {
        if (!viewers.remove(player.getUniqueId())) {
            return;
        }
        if (objective != null) {
            objective.getScore(player.getName()).resetScore();
        }
        restoreMainBoard(player);
    }

    /** 当前有多少玩家在看这块板（自检/调试用）。 */
    public int size() {
        return viewers.size();
    }

    /** 设置重读后调用：血量显示被关掉时恢复所有参与者的主板并注销目标；重新打开要等下次进图。 */
    public void applySettings() {
        if (!settings.current().healthBelowNameEnabled()) {
            shutdown();
        }
    }

    /** onDisable 调用：恢复所有在线参与者的主板并注销目标。 */
    public void shutdown() {
        for (UUID playerId : new ArrayList<>(viewers)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                restoreMainBoard(player);
            }
        }
        viewers.clear();
        if (objective != null) {
            objective.unregister();
            objective = null;
            scoreboard = null;
        }
    }

    private void ensureObjective() {
        if (objective != null) {
            return;
        }
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        scoreboard = manager.getNewScoreboard();
        objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, OBJECTIVE_TITLE);
        objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
    }

    private static void restoreMainBoard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (player.isOnline()) {
            player.setScoreboard(manager.getMainScoreboard());
        }
    }
}
