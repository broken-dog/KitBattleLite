package org.awang05.kitbattlelite.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.awang05.kitbattlelite.arena.Arena;
import org.awang05.kitbattlelite.arena.ArenaService;
import org.awang05.kitbattlelite.command.SubCommand;
import org.awang05.kitbattlelite.config.Messages;
import org.awang05.kitbattlelite.gui.MenuFactory;

import java.util.Optional;

/**
 * {@code /kbl leave}：离开竞技场并恢复进图前快照；不在竞技场时只提示。
 *
 * <p>离场后刷新地图选择页的人数。
 *
 * <p>生命周期：组合根创建一次，随根命令存活。
 */
public final class LeaveCommand implements SubCommand {
    private final ArenaService arenaService;
    private final MenuFactory menus;
    private final Messages messages;

    public LeaveCommand(ArenaService arenaService, MenuFactory menus, Messages messages) {
        this.arenaService = arenaService;
        this.menus = menus;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "leave";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        Optional<Arena> arena = arenaService.arenaOf(player);
        if (arena.isEmpty()) {
            messages.send(player, "command.leave-not-in-arena");
            return;
        }
        arenaService.leave(player, true);
        menus.updateWorldSelectIcon(arena.get());
    }
}
