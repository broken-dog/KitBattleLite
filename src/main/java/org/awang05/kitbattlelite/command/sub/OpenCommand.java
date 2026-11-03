package org.awang05.kitbattlelite.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.awang05.kitbattlelite.arena.ArenaService;
import org.awang05.kitbattlelite.command.SubCommand;
import org.awang05.kitbattlelite.config.Messages;
import org.awang05.kitbattlelite.gui.MenuFactory;

import java.util.List;

/**
 * {@code /kbl open}：打开地图选择；已经在竞技场里时只提示，不改状态。
 *
 * <p>生命周期：组合根创建一次，随根命令存活；不持有玩家。
 */
public final class OpenCommand implements SubCommand {
    private final ArenaService arenaService;
    private final MenuFactory menus;
    private final Messages messages;

    public OpenCommand(ArenaService arenaService, MenuFactory menus, Messages messages) {
        this.arenaService = arenaService;
        this.menus = menus;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "open";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        if (arenaService.isInArena(player)) {
            messages.send(player, "command.open-in-arena");
            return;
        }
        menus.openWorldSelect(player);
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return List.of();
    }
}
