package org.awang05.kitbattlelite.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.awang05.kitbattlelite.config.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 根命令 {@code /kitbattlelite}：分发、权限与补全集中在这里。
 *
 * <p>缺参、未知子命令、无权限、非玩家统一发 {@code messages.yml} 里的用法/提示，并始终返回 true。
 *
 * <p>生命周期：组合根在 onEnable 创建并 {@code setExecutor}/{@code setTabCompleter}；无状态。
 */
public final class KitBattleCommand implements TabExecutor {
    private final Messages messages;
    private final Map<String, SubCommand> subCommands;

    /**
     * @param subCommands 按注册顺序排列；同名子命令直接抛异常，避免静默覆盖
     */
    public KitBattleCommand(Messages messages, List<SubCommand> subCommands) {
        this.messages = Objects.requireNonNull(messages, "messages");
        Map<String, SubCommand> byName = new LinkedHashMap<>();
        for (SubCommand subCommand : subCommands) {
            String key = subCommand.name().toLowerCase(Locale.ROOT);
            if (byName.put(key, subCommand) != null) {
                throw new IllegalStateException("子命令重复: " + key);
            }
        }
        this.subCommands = Collections.unmodifiableMap(byName);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "command.usage");
            return true;
        }
        SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            messages.send(sender, "command.unknown");
            return true;
        }
        if (!allowed(sender, subCommand)) {
            messages.send(sender, "command.no-permission");
            return true;
        }
        if (subCommand.playerOnly() && !(sender instanceof Player)) {
            messages.send(sender, "command.player-only");
            return true;
        }
        subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0];
            List<String> names = new ArrayList<>(subCommands.size());
            for (SubCommand subCommand : subCommands.values()) {
                if (allowed(sender, subCommand)) {
                    names.add(subCommand.name());
                }
            }
            return StringUtil.copyPartialMatches(prefix, names, new ArrayList<>());
        }
        SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null || !allowed(sender, subCommand)) {
            return List.of();
        }
        return subCommand.complete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    /** 权限为 null 表示公开；否则检查节点。 */
    private static boolean allowed(CommandSender sender, SubCommand subCommand) {
        String permission = subCommand.permission();
        return permission == null || sender.hasPermission(permission);
    }
}
