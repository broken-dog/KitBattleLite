package org.awang05.kitbattlelite.gui.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.awang05.kitbattlelite.gui.GuiIcons;
import org.awang05.kitbattlelite.gui.GuiLayout;
import org.awang05.kitbattlelite.gui.Menu;
import org.awang05.kitbattlelite.gui.MenuFactory;
import org.awang05.kitbattlelite.item.ItemService;
import org.awang05.kitbattlelite.item.PluginItem;
import org.awang05.kitbattlelite.kit.Kit;
import org.awang05.kitbattlelite.kit.KitRepository;
import org.awang05.kitbattlelite.kit.KitSlots;

import java.util.List;

/**
 * 职业详情（只读）：显示整套装备，返回上一页。
 *
 * <p>点击矩阵：顶部普通槽取消、玩家背包取消、Shift 点击取消；返回按按钮。
 * 职业在打开后被删除时显示占位提示，不抛异常。
 *
 * <p>生命周期：由 {@link MenuFactory#openKitDetail(Player, String, int)} 每次打开时新建，关闭即丢弃。
 */
public final class KitDetailMenu extends Menu {
    private final MenuFactory menus;
    private final KitRepository kits;
    private final ItemService items;
    private final String kitId;
    private final int pageIndex;

    public KitDetailMenu(JavaPlugin plugin, MenuFactory menus, KitRepository kits, ItemService items,
                         String kitId, int pageIndex) {
        super(plugin);
        this.menus = menus;
        this.kits = kits;
        this.items = items;
        this.kitId = kitId;
        this.pageIndex = pageIndex;
    }

    @Override
    protected Component title() {
        return Component.text("查看职业 · " + kitId);
    }

    @Override
    protected int size() {
        return GuiLayout.SIZE;
    }

    @Override
    protected void render(Inventory inventory) {
        fillBorder(inventory);
        clearKitGrid(inventory);
        Kit kit = kits.findKit(kitId).orElse(null);
        if (kit == null) {
            inventory.setItem(GuiLayout.PAGE_INDICATOR, GuiIcons.of(Material.BARRIER,
                    Component.text("职业 " + kitId + " 不存在", NamedTextColor.RED),
                    List.of(Component.text("可能已被删除", NamedTextColor.GRAY))));
        } else {
            for (int kitSlot = 0; kitSlot < KitSlots.SIZE; kitSlot++) {
                ItemStack item = kit.content(kitSlot);
                if (item != null) {
                    inventory.setItem(GuiLayout.kitSlot(kitSlot), item);
                }
            }
            inventory.setItem(GuiLayout.POTION_SLOT, items.potionDisplay(kit.potion()));
        }
        inventory.setItem(GuiLayout.BACK, items.create(PluginItem.BACK));
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, boolean topClick) {
        event.setCancelled(true);
        if (topClick && event.getRawSlot() == GuiLayout.BACK) {
            menus.openKitPage(player, pageIndex);
        }
    }
}
