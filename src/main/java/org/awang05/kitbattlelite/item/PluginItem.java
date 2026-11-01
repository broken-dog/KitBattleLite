package org.awang05.kitbattlelite.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * 本插件需要按身份识别的物品类型。
 *
 * <p>外部通过 {@link ItemService#create(PluginItem)}创建，
 * 插件物品的身份存在 PersistentDataContainer 里。
 */
public enum PluginItem {
    SELECT("select", Material.CHEST, "选择职业", NamedTextColor.GREEN, true),
    LEAVE("leave", Material.RED_BED, "离开", NamedTextColor.RED, true),
    BACK("back", Material.ARROW, "返回", NamedTextColor.GREEN, false),
    SAVE("save", Material.CHEST, "保存", NamedTextColor.GREEN, false, "千万别忘了点！"),
    CANCEL("cancel", Material.BARRIER, "取消", NamedTextColor.RED, false),
    PREV("prev", Material.RED_STAINED_GLASS_PANE, "上一页", NamedTextColor.WHITE, false),
    NEXT("next", Material.GREEN_STAINED_GLASS_PANE, "下一页", NamedTextColor.WHITE, false),
    KIT("kit", Material.PAPER, "职业", NamedTextColor.YELLOW, false);

    private final String id;
    private final Material material;
    private final Component displayName;
    private final List<Component> lore;

    PluginItem(String id, Material material, String name, NamedTextColor color, boolean bold, String... lore) {
        this.id = id;
        this.material = material;
        Component display = Component.text(name, color);
        if (bold) {
            display = display.decorate(TextDecoration.BOLD);
        }
        this.displayName = display;
        this.lore = Arrays.stream(lore)
                .<Component>map(line -> Component.text(line, NamedTextColor.GRAY))
                .toList();
    }

    /** PDC 里保存的识别 id。 */
    public final String id() {
        return id;
    }

    /** 物品材质（不含 PDC，仅用于展示与校验）。 */
    public final Material material() {
        return material;
    }

    /**
     * 创建一个只含展示信息的物品实例，**没有**插件身份标记。
     *
     * <p>包级可见：包外必须走 {@link ItemService#create(PluginItem)}，否则打出来的物品识别不了。
     */
    final ItemStack newDisplay() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("材质 " + material + " 没有 ItemMeta，无法创建插件物品");
        }
        meta.displayName(displayName);
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
}
