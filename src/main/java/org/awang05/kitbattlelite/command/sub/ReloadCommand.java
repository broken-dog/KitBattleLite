package org.awang05.kitbattlelite.command.sub;

import org.bukkit.command.CommandSender;
import org.awang05.kitbattlelite.command.SubCommand;
import org.awang05.kitbattlelite.config.Messages;
import org.awang05.kitbattlelite.gui.MenuFactory;

import java.util.Objects;

/**
 * {@code /kbl reload}：重读设置/职业/地图/战绩/文案，并把在线参与者请出竞技场（管理员）。
 *
 * <p>具体流程放在组合根（它才拥有全部服务与 {@code MenuFactory}），命令只负责触发与回消息。
 *
 * <p>生命周期：组合根创建一次，随根命令存活；动作通过构造注入的 {@link Runnable} 传入。
 */
public final class ReloadCommand implements SubCommand {
    private final Messages messages;
    private final Runnable reloadAction;

    public ReloadCommand(Messages messages, Runnable reloadAction) {
        this.messages = Objects.requireNonNull(messages, "messages");
        this.reloadAction = Objects.requireNonNull(reloadAction, "reloadAction");
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String permission() {
        return MenuFactory.ADMIN_PERMISSION;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        reloadAction.run();
        messages.send(sender, "command.reloaded");
    }
}
