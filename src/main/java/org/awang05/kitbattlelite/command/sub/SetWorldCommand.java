package org.awang05.kitbattlelite.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.awang05.kitbattlelite.arena.Arena;
import org.awang05.kitbattlelite.arena.ArenaRegistry;
import org.awang05.kitbattlelite.arena.ArenaRepository;
import org.awang05.kitbattlelite.command.SubCommand;
import org.awang05.kitbattlelite.config.Messages;
import org.awang05.kitbattlelite.gui.MenuFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * {@code /kbl setworld <name|spawn|teleport|enabled> [值]}：设置玩家当前所在地图（管理员）。
 *
 * <p>地图按玩家所在世界反查；每次修改都立即写回 {@code arenas.yml}，并刷新地图选择页的图标。
 *
 * <p>生命周期：组合根创建一次，随根命令存活。
 */
public final class SetWorldCommand implements SubCommand {
    private static final List<String> PARTS = List.of("name", "spawn", "teleport", "enabled");

    private final ArenaRegistry arenas;
    private final ArenaRepository arenaRepository;
    private final MenuFactory menus;
    private final Messages messages;

    public SetWorldCommand(ArenaRegistry arenas, ArenaRepository arenaRepository,
                           MenuFactory menus, Messages messages) {
        this.arenas = arenas;
        this.arenaRepository = arenaRepository;
        this.menus = menus;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "setworld";
    }

    @Override
    public String permission() {
        return MenuFactory.ADMIN_PERMISSION;
    }

    @Override
    public boolean playerOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        Arena arena = arenas.byWorld(player.getWorld()).orElse(null);
        if (arena == null) {
            messages.send(player, "command.setworld-not-in-arena");
            return;
        }
        if (args.length == 0) {
            messages.send(player, "command.setworld-usage");
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "name" -> setName(player, arena, args);
            case "spawn" -> {
                arena.setSpawn(player.getLocation());
                persist();
                messages.send(player, "command.setworld-spawn", "arena", arena.id());
            }
            case "teleport" -> {
                arena.setTeleport(player.getLocation());
                persist();
                messages.send(player, "command.setworld-teleport", "arena", arena.id());
            }
            case "enabled" -> setEnabled(player, arena, args);
            default -> messages.send(player, "command.setworld-usage");
        }
    }

    private void setName(Player player, Arena arena, String[] args) {
        if (args.length < 2) {
            messages.send(player, "command.setworld-usage");
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        if (name.isEmpty()) {
            messages.send(player, "command.setworld-usage");
            return;
        }
        arena.setDisplayName(name);
        persist();
        // 显示名会改变选择页图标文字
        menus.updateWorldSelectIcon(arena);
        messages.send(player, "command.setworld-name", "name", name);
    }

    private void setEnabled(Player player, Arena arena, String[] args) {
        if (args.length < 2) {
            messages.send(player, "command.setworld-usage");
            return;
        }
        Boolean enabled = switch (args[1].toLowerCase(Locale.ROOT)) {
            case "true", "on", "yes", "1" -> Boolean.TRUE;
            case "false", "off", "no", "0" -> Boolean.FALSE;
            default -> null;
        };
        if (enabled == null) {
            messages.send(player, "command.setworld-usage");
            return;
        }
        arena.setEnabled(enabled);
        persist();
        menus.updateWorldSelectIcon(arena);
        messages.send(player, "command.setworld-enabled",
                "arena", arena.id(),
                "state", messages.component(enabled ? "command.state-open" : "command.state-closed"));
    }

    private void persist() {
        arenaRepository.save(arenas);
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], PARTS, new ArrayList<>());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("enabled")) {
            return StringUtil.copyPartialMatches(args[1], List.of("true", "false"), new ArrayList<>());
        }
        return List.of();
    }
}
