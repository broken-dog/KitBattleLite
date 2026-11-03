package org.awang05.kitbattlelite.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.awang05.kitbattlelite.command.SubCommand;
import org.awang05.kitbattlelite.config.Messages;
import org.awang05.kitbattlelite.gui.MenuFactory;
import org.awang05.kitbattlelite.kit.KitRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /kbl edit [页码]}：打开指定页的页面编辑器（管理员），页码从 1 开始。
 *
 * <p>页面是否存在不影响打开：空页编辑器关闭时会把这一页建出来，方便从零排版。
 *
 * <p>生命周期：组合根创建一次，随根命令存活；不持有玩家。
 */
public final class EditCommand implements SubCommand {
    private final MenuFactory menus;
    private final KitRepository kits;
    private final Messages messages;

    public EditCommand(MenuFactory menus, KitRepository kits, Messages messages) {
        this.menus = menus;
        this.kits = kits;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "edit";
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
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException e) {
                messages.send(player, "command.edit-usage");
                return;
            }
            if (page < 1) {
                messages.send(player, "command.edit-usage");
                return;
            }
        }
        menus.openPageEditor(player, page);
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length > 1) {
            return List.of();
        }
        List<String> pages = new ArrayList<>();
        for (int index : kits.pageIndexes()) {
            pages.add(Integer.toString(index));
        }
        pages.add(Integer.toString(Math.max(1, kits.maxPageIndex() + 1)));
        return StringUtil.copyPartialMatches(args.length == 0 ? "" : args[0], pages, new ArrayList<>());
    }
}
