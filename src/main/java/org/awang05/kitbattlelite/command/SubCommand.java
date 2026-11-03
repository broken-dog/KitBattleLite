package org.awang05.kitbattlelite.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * 一个子命令：参数校验、权限声明与补全。
 *
 * <p>实现类只做“校验参数 → 调服务 → 发消息”，不写业务流程；权限与发送者类型由
 * {@link KitBattleCommand} 统一检查，所以 {@code execute} 里可以直接把 sender 当成玩家使用
 * （{@link #playerOnly()} 为 true 时）。
 *
 * <p>生命周期：组合根创建并注册到 {@link KitBattleCommand}；无内部可变状态。
 */
public interface SubCommand {

    /** 子命令名（小写，用于匹配与补全）。 */
    String name();

    /** 需要的权限节点；null 表示不需要权限。 */
    default String permission() {
        return null;
    }

    /** 是否只能由玩家执行。 */
    default boolean playerOnly() {
        return false;
    }

    /**
     * 执行；{@code args} 是去掉子命令名之后的参数。
     *
     * <p>缺参时自行发用法消息并返回，不要抛异常。
     */
    void execute(CommandSender sender, String[] args);

    /** 补全参数；{@code args} 同样不含子命令名。 */
    default List<String> complete(CommandSender sender, String[] args) {
        return List.of();
    }
}
