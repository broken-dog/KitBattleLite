package org.awang05.kitbattlelite.gui.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.awang05.kitbattlelite.arena.ArenaService;
import org.awang05.kitbattlelite.gui.GuiIcons;
import org.awang05.kitbattlelite.gui.GuiLayout;
import org.awang05.kitbattlelite.gui.Menu;
import org.awang05.kitbattlelite.gui.MenuFactory;
import org.awang05.kitbattlelite.item.ItemService;
import org.awang05.kitbattlelite.item.PluginItem;
import org.awang05.kitbattlelite.kit.Kit;
import org.awang05.kitbattlelite.kit.KitPage;
import org.awang05.kitbattlelite.kit.KitRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 职业页（只读）：28 个职业图标 + 底部翻页/页码。
 *
 * <p>点击矩阵：顶部普通槽取消、玩家背包取消、Shift 点击取消。左键选职业（应用套装并传送到战斗点），
 * 右键看详情；翻页按底部按钮。
 *
 * <p>生命周期：由 {@link MenuFactory#openKitPage(Player, int)} 每次打开时新建，关闭即丢弃。
 */
public final class KitPageMenu extends Menu {
    private final MenuFactory menus;
    private final KitRepository kits;
    private final ItemService items;
    private final ArenaService arenaService;
    private final int pageIndex;

    public KitPageMenu(JavaPlugin plugin, MenuFactory menus, KitRepository kits, ItemService items,
                       ArenaService arenaService, int pageIndex) {
        super(plugin);
        this.menus = menus;
        this.kits = kits;
        this.items = items;
        this.arenaService = arenaService;
        this.pageIndex = pageIndex;
    }

    @Override
    protected Component title() {
        return Component.text("选择职业 · 第 " + pageIndex + " 页");
    }

    @Override
    protected int size() {
        return GuiLayout.SIZE;
    }

    @Override
    protected void render(Inventory inventory) {
        fillBorder(inventory);
        clearPageGrid(inventory);
        KitPage page = kits.page(pageIndex);
        if (page != null) {
            for (int position = 0; position < KitPage.SIZE; position++) {
                String kitId = page.kitId(position);
                if (kitId == null) {
                    continue;
                }
                Kit kit = kits.findKit(kitId).orElse(null);
                if (kit == null) {
                    // 加载时已经告警过；运行时按空处理，不在菜单里造非法物品
                    continue;
                }
                inventory.setItem(GuiLayout.pageSlot(position), items.tagKit(kit.icon(), kitId));
            }
        }
        Integer previous = kits.previousPage(pageIndex);
        if (previous != null) {
            inventory.setItem(GuiLayout.PAGE_PREV, items.create(PluginItem.PREV));
        }
        Integer next = kits.nextPage(pageIndex);
        if (next != null) {
            inventory.setItem(GuiLayout.PAGE_NEXT, items.create(PluginItem.NEXT));
        }
        inventory.setItem(GuiLayout.PAGE_INDICATOR, indicator());
    }

    private ItemStack indicator() {
        List<Component> lore = new ArrayList<>(2);
        lore.add(Component.text("左键：选择职业", NamedTextColor.GREEN));
        lore.add(Component.text("右键：查看详情", NamedTextColor.YELLOW));
        Component name = Component.text("第 " + pageIndex + " 页", NamedTextColor.WHITE);
        return GuiIcons.of(Material.PAPER, name, lore);
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, boolean topClick) {
        event.setCancelled(true);
        if (!topClick) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == GuiLayout.PAGE_PREV) {
            Integer previous = kits.previousPage(pageIndex);
            if (previous != null) {
                menus.openKitPage(player, previous);
            }
            return;
        }
        if (slot == GuiLayout.PAGE_NEXT) {
            Integer next = kits.nextPage(pageIndex);
            if (next != null) {
                menus.openKitPage(player, next);
            }
            return;
        }
        int position = GuiLayout.pagePosition(slot);
        if (position < 0) {
            return;
        }
        KitPage page = kits.page(pageIndex);
        if (page == null) {
            return;
        }
        String kitId = page.kitId(position);
        if (kitId == null || kits.findKit(kitId).isEmpty()) {
            return;
        }
        if (event.isRightClick()) {
            menus.openKitDetail(player, kitId, pageIndex);
            return;
        }
        if (!event.isLeftClick()) {
            return;
        }
        runNextTick(player.getUniqueId(), online -> {
            online.closeInventory();
            try {
                if (!arenaService.selectKit(online, kitId)) {
                    online.sendMessage(Component.text("不在竞技场里，不能选择职业", NamedTextColor.RED));
                }
            } catch (IllegalArgumentException e) {
                // 点击到下一 tick 之间职业被删/被 reload 掉了：提示玩家，避免调度器里冒出堆栈
                plugin.getLogger().warning("玩家 " + online.getName() + " 选择职业失败：" + e.getMessage());
                online.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
            }
        });
    }
}
