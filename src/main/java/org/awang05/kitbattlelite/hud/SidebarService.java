package org.awang05.kitbattlelite.hud;

import fr.mrmicky.fastboard.adventure.FastBoard;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.awang05.kitbattlelite.config.Messages;
import org.awang05.kitbattlelite.config.PluginSettings;
import org.awang05.kitbattlelite.player.PlayerProfile;
import org.awang05.kitbattlelite.player.PlayerSession;
import org.awang05.kitbattlelite.player.SessionRegistry;
import org.awang05.kitbattlelite.player.StatsRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 侧边栏（FastBoard）的创建、刷新与销毁。
 *
 * <p>每个在线参与者最多一块板，键是 {@link UUID}；{@link FastBoard} 内部强引用 {@link Player}，
 * 所以退服/离场/禁用三处都必须 {@link #hide(UUID)}（内部会 {@code delete()} 并从表里移除）。
 *
 * <p>只读 {@link SessionRegistry} 与 {@link StatsRepository}，不创建战绩条目：渲染过程中不做写操作。
 * 文案全部来自 {@link Messages}，这里只做取值与拼接。
 *
 * <p>生命周期：组合根创建；进图 {@link #show(Player)}，状态变化 {@link #refresh(UUID)}，
 * 离场/退服 {@link #hide(UUID)}，onDisable {@link #shutdown()}。
 */
public final class SidebarService {
    private final PluginSettings settings;
    private final Messages messages;
    private final SessionRegistry sessions;
    private final StatsRepository stats;
    private final Map<UUID, FastBoard> boards = new HashMap<>();

    public SidebarService(PluginSettings settings, Messages messages, SessionRegistry sessions,
                          StatsRepository stats) {
        this.settings = settings;
        this.messages = messages;
        this.sessions = sessions;
        this.stats = stats;
    }

    /** 进图时调用；同一玩家重复调用只会刷新内容。 */
    public void show(Player player) {
        if (!settings.sidebarEnabled()) {
            return;
        }
        FastBoard board = boards.computeIfAbsent(player.getUniqueId(), id -> new FastBoard(player));
        board.updateTitle(messages.component("sidebar.title"));
        board.updateLines(lines(player));
    }

    /** 重新计算指定玩家的行；没有板或玩家已离线时什么都不做。 */
    public void refresh(UUID playerId) {
        FastBoard board = boards.get(playerId);
        if (board == null || board.isDeleted()) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        board.updateLines(lines(player));
    }

    /** 移除并删除侧边栏；退服时发包可能失败，但表一定清空。 */
    public void hide(UUID playerId) {
        FastBoard board = boards.remove(playerId);
        if (board == null) {
            return;
        }
        try {
            board.delete();
        } catch (RuntimeException e) {
            // 玩家已离线等情况下发包会失败，此时板本身也没有保留价值，忽略即可
        }
    }

    /** 当前有几块板（自检/调试用）。 */
    public int size() {
        return boards.size();
    }

    /** onDisable 调用：删掉所有板。 */
    public void shutdown() {
        for (UUID playerId : new ArrayList<>(boards.keySet())) {
            hide(playerId);
        }
    }

    private List<Component> lines(Player player) {
        PlayerSession session = sessions.get(player.getUniqueId());
        PlayerProfile profile = stats.find(player.getUniqueId()).orElse(null);
        int kills = profile == null ? 0 : profile.kills();
        int deaths = profile == null ? 0 : profile.deaths();
        double kdr = profile == null ? 0.0 : profile.kdr();
        int streak = session == null ? 0 : session.killStreak();

        List<Component> lines = new ArrayList<>(6);
        lines.add(messages.component("sidebar.separator"));
        lines.add(messages.component("sidebar.streak", "value", Integer.toString(streak)));
        lines.add(messages.component("sidebar.kills", "value", Integer.toString(kills)));
        lines.add(messages.component("sidebar.deaths", "value", Integer.toString(deaths)));
        lines.add(messages.component("sidebar.kdr", "value", String.format(Locale.ROOT, "%.2f", kdr)));
        lines.add(messages.component("sidebar.separator"));
        return lines;
    }
}
