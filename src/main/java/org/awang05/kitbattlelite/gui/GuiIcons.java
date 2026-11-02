package org.awang05.kitbattlelite.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * 只用于展示、不需要身份识别的 GUI 物品（边框、页码、地图图标）的构造函数。
 *
 * <p>这些物品不带 PDC。纯函数、无状态，也不缓存任何 {@link ItemStack}。
 */
public final class GuiIcons {
    private static final Component EMPTY_NAME = Component.empty();

    private GuiIcons() {
    }

    /** 空白边框格：材质给定，名称留空，避免悬停时出现英文原名。 */
    public static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = requireMeta(item);
        meta.displayName(EMPTY_NAME);
        item.setItemMeta(meta);
        return item;
    }

    /** 带名称与说明的展示物品；{@code lore} 可以为空列表。 */
    public static ItemStack of(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = requireMeta(item);
        meta.displayName(name);
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemMeta requireMeta(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("材质 " + item.getType() + " 没有 ItemMeta，无法用于 GUI");
        }
        return meta;
    }
}
