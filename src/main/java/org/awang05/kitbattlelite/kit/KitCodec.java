package org.awang05.kitbattlelite.kit;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * 物品与 YAML 节点之间的读写，负责“坏数据不阻断启动”。
 *
 * <p>写：直接用 Bukkit 的 {@link ConfigurationSection#set(String, Object)} 存 {@link ItemStack}，
 * 由服务端序列化成可读的 YAML（含 PDC 的 {@code PublicBukkitValues}）。
 *
 * <p>读：节点缺失或不是物品时返回 null 并记一条带上下文的 WARNING，调用方按空槽处理。
 */
public final class KitCodec {
    private static final int MAX_PREVIEW_LENGTH = 120;

    private final Logger logger;

    public KitCodec(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * 读取物品。
     *
     * @param parent  父配置段，null 直接返回 null
     * @param path    相对父段的路径
     * @param context 日志上下文（文件 + 路径），如 {@code kits.warrior.contents.0}
     * @return 物品；空槽/读取失败返回 null
     */
    public ItemStack read(ConfigurationSection parent, String path, String context) {
        if (parent == null || !parent.contains(path)) {
            return null;
        }
        ItemStack item;
        try {
            item = parent.getItemStack(path);
        } catch (RuntimeException e) {
            logger.warning("读取物品失败，已跳过 " + context + "：" + preview(parent.get(path))
                    + "（" + e.getMessage() + "）");
            return null;
        }
        if (item == null) {
            logger.warning("读取物品失败，已跳过 " + context + "：" + preview(parent.get(path))
                    + "（不是有效物品）");
            return null;
        }
        if (item.getType().isAir() || item.getAmount() <= 0) {
            return null;
        }
        return item;
    }

    /** 写入物品；null/AIR 会移除该键，而不是写成空气。 */
    public void write(ConfigurationSection parent, String path, ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            parent.set(path, null);
            return;
        }
        parent.set(path, item);
    }

    private static String preview(Object raw) {
        if (raw == null) {
            return "null";
        }
        String text = raw.toString();
        if (text.length() > MAX_PREVIEW_LENGTH) {
            return text.substring(0, MAX_PREVIEW_LENGTH) + "...";
        }
        return text;
    }
}
