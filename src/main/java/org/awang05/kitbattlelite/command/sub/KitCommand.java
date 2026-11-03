package org.awang05.kitbattlelite.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;
import org.awang05.kitbattlelite.command.SubCommand;
import org.awang05.kitbattlelite.config.Messages;
import org.awang05.kitbattlelite.gui.MenuFactory;
import org.awang05.kitbattlelite.kit.Kit;
import org.awang05.kitbattlelite.kit.KitRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * {@code /kbl kit <delete <id>|prune>}：职业维护（管理员）。
 *
 * <ul>
 *   <li>{@code delete <id>}：删除职业并级联清掉所有页面引用，槽位清单写进日志；</li>
 *   <li>{@code prune}：先列出再删除所有未被页面引用的职业。</li>
 * </ul>
 *
 * <p>生命周期：组合根创建一次，随根命令存活。
 */
public final class KitCommand implements SubCommand {
    private static final List<String> ACTIONS = List.of("delete", "prune");

    private final KitRepository kits;
    private final Messages messages;
    private final Logger logger;

    public KitCommand(KitRepository kits, Messages messages, Logger logger) {
        this.kits = kits;
        this.messages = messages;
        this.logger = logger;
    }

    @Override
    public String name() {
        return "kit";
    }

    @Override
    public String permission() {
        return MenuFactory.ADMIN_PERMISSION;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "command.kit-usage");
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "delete" -> delete(sender, args);
            case "prune" -> prune(sender);
            default -> messages.send(sender, "command.kit-unknown");
        }
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "command.kit-usage");
            return;
        }
        String kitId = args[1].trim();
        if (kits.findKit(kitId).isEmpty()) {
            messages.send(sender, "command.kit-not-found", "id", kitId);
            return;
        }
        List<KitRepository.SlotRef> references = kits.referencingSlots(kitId);
        for (KitRepository.SlotRef reference : references) {
            logger.info("删除职业 " + kitId + "：清理 pages." + reference.page()
                    + ".slots." + reference.position());
        }
        int cleared = kits.deleteKit(kitId);
        messages.send(sender, "command.kit-deleted", "id", kitId, "count", cleared);
    }

    private void prune(CommandSender sender) {
        Set<String> orphans = kits.unreferencedKitIds();
        if (orphans.isEmpty()) {
            messages.send(sender, "command.kit-prune-none");
            return;
        }
        logger.info("清理未被引用的职业：" + String.join(", ", orphans));
        List<String> removed = kits.pruneKits();
        messages.send(sender, "command.kit-prune-done",
                "count", removed.size(), "ids", String.join(", ", removed));
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], ACTIONS, new ArrayList<>());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            List<String> ids = kits.kits().stream().map(Kit::id).toList();
            return StringUtil.copyPartialMatches(args[1], ids, new ArrayList<>());
        }
        return List.of();
    }
}
