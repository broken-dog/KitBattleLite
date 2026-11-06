package org.awang05.kitbattlelite.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;
import org.awang05.kitbattlelite.arena.Arena;
import org.awang05.kitbattlelite.arena.ArenaRegistry;
import org.awang05.kitbattlelite.arena.ArenaRepository;
import org.awang05.kitbattlelite.arena.ArenaService;
import org.awang05.kitbattlelite.command.SubCommand;
import org.awang05.kitbattlelite.config.Messages;
import org.awang05.kitbattlelite.gui.MenuFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /kbl import <地图ID> <世界名>}：把一个世界导入成未开放地图，并立即落盘、刷新地图选择页。
 *
 * <p>世界未加载时由 {@link ArenaService#importWorld(String, String)} 先确认文件夹再加载；
 *
 * <p>生命周期：组合根创建一次，随根命令存活。
 */
public final class ImportCommand implements SubCommand {
    private final ArenaService arenaService;
    private final ArenaRegistry arenas;
    private final ArenaRepository arenaRepository;
    private final MenuFactory menus;
    private final Messages messages;

    public ImportCommand(ArenaService arenaService, ArenaRegistry arenas,
                         ArenaRepository arenaRepository, MenuFactory menus, Messages messages) {
        this.arenaService = arenaService;
        this.arenas = arenas;
        this.arenaRepository = arenaRepository;
        this.menus = menus;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "import";
    }

    @Override
    public String permission() {
        return MenuFactory.ADMIN_PERMISSION;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            messages.send(sender, "command.import-usage");
            return;
        }
        try {
            Arena arena = arenaService.importWorld(args[0], args[1]);
            arenaRepository.save(arenas);
            menus.refreshWorldSelect();
            messages.send(sender, "command.import-success", "arena", arena.id());
        } catch (IllegalArgumentException e) {
            messages.send(sender, "command.import-failed", "reason", e.getMessage());
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        List<String> worldNames = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (arenas.byWorldName(world.getName()).isEmpty()) {
                worldNames.add(world.getName());
            }
        }
        return StringUtil.copyPartialMatches(args[1], worldNames, new ArrayList<>());
    }
}
