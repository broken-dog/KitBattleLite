package org.awang05.kitbattlelite.gui.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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
import org.awang05.kitbattlelite.kit.KitPage;
import org.awang05.kitbattlelite.kit.KitRepository;

import java.util.List;
import java.util.Optional;

/**
 * 页面编辑器
 *
 * <p>每次点击/拖拽之后把 GUI 的 28 格映射回 {@link KitPage}——普通物品立即成为新职业
 * 的图标，已有职业图标只是引用；关闭界面时落盘一次。
 *
 * <p>点击矩阵：只有 28 个职业格可放物品；边框/翻页/页码全部取消；玩家背包可点击，
 * Shift 点击会被改写成“放进第一个空格”，避免物品落进不可编辑的格子。
 * 右键职业格打开该职业的编辑器；空格给提示。
 *
 * <p>生命周期：由 {@link MenuFactory#openPageEditor(Player, int)} 每次打开时新建；
 * {@link #onClose(Player, Inventory)} 里回写模型并保存 {@code kits.yml}。
 */
public final class KitPageEditMenu extends Menu {
    private final MenuFactory menus;
    private final KitRepository kits;
    private final ItemService items;
    private final int pageIndex;

    public KitPageEditMenu(JavaPlugin plugin, MenuFactory menus, KitRepository kits, ItemService items,
                           int pageIndex) {
        super(plugin);
        this.menus = menus;
        this.kits = kits;
        this.items = items;
        this.pageIndex = pageIndex;
    }

    @Override
    protected Component title() {
        return Component.text("编辑列表 · 第 " + pageIndex + " 页");
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
                if (kit != null) {
                    inventory.setItem(GuiLayout.pageSlot(position), items.tagKit(kit.icon(), kitId));
                }
            }
        }
        Integer previous = kits.previousPage(pageIndex);
        if (previous != null) {
            inventory.setItem(GuiLayout.PAGE_PREV, items.create(PluginItem.PREV));
        }
        inventory.setItem(GuiLayout.PAGE_NEXT, items.create(PluginItem.NEXT));  // 编辑页直接允许下一页，若无则新建
        inventory.setItem(GuiLayout.PAGE_INDICATOR, indicator());
    }

    private ItemStack indicator() {
        List<Component> lore = List.of(
                Component.text("右键：编辑职业", NamedTextColor.GREEN),
                Component.text("左键：放置/移动图标", NamedTextColor.YELLOW),
                Component.text("关闭界面即保存", NamedTextColor.GRAY));
        return GuiIcons.of(Material.PAPER,
                Component.text("第 " + pageIndex + " 页", NamedTextColor.WHITE), lore);
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, boolean topClick) {
        int rawSlot = event.getRawSlot();
        if (!topClick) {
            if (rawSlot < 0) {
                // 丢到窗口外：图标会掉在地上，取消掉更可控
                event.setCancelled(true);
                return;
            }
            if (event.isShiftClick()) {
                event.setCancelled(true);
                moveIntoFirstFreeSlot(player, event);
                return;
            }
            return;
        }
        if (rawSlot == GuiLayout.PAGE_PREV) {
            event.setCancelled(true);
            Integer previous = kits.previousPage(pageIndex);
            if (previous != null) {
                reconcile(event.getView().getTopInventory());
                menus.openPageEditor(player, previous);
            }
            return;
        }
        if (rawSlot == GuiLayout.PAGE_NEXT) {
            event.setCancelled(true);
            Integer next = kits.nextPage(pageIndex);
            // 直接允许下一页，若无则新建
            if (next == null) {
                next = pageIndex + 1;
            }
            reconcile(event.getView().getTopInventory());
            menus.openPageEditor(player, next);
            return;
        }
        if (rawSlot == GuiLayout.PAGE_INDICATOR) {
            event.setCancelled(true);
            return;
        }
        int position = GuiLayout.pagePosition(rawSlot);
        if (position < 0) {
            event.setCancelled(true);
            return;
        }
        Inventory top = event.getView().getTopInventory();
        ItemStack i = top.getItem(rawSlot);
        if (event.isRightClick() && i != null && !i.getType().isAir()) {
            event.setCancelled(true);
            openKitEditor(player, position, top);
            return;
        }
        // 放置/Shift 点击：先让服务端完成移动，下一 tick 再回写模型
        scheduleReconcile(player);
    }

    @Override
    public void onDrag(Player player, InventoryDragEvent event) {
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < GuiLayout.SIZE && !GuiLayout.isPageSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
        scheduleReconcile(player);
    }

    @Override
    public void onClose(Player player, Inventory closed) {
        reconcile(closed);
        kits.save();
    }

    /** 把 GUI 的 28 格回写成页面模型；普通物品转成新职业。 */
    private void reconcile(Inventory inventory) {
        KitPage page = kits.ensurePage(pageIndex);
        for (int position = 0; position < KitPage.SIZE; position++) {
            int guiSlot = GuiLayout.pageSlot(position);
            ItemStack item = inventory.getItem(guiSlot);
            if (item == null || item.getType().isAir()) {
                page.setKitId(position, null);
                continue;
            }
            Optional<String> existing = items.kitId(item).filter(id -> kits.findKit(id).isPresent());
            String kitId;
            if (existing.isPresent()) {
                kitId = existing.get();
            } else {
                kitId = nextKitId();
                kits.putKit(new Kit(kitId, item, null));
                inventory.setItem(guiSlot, items.tagKit(item.clone(), kitId));
            }
            page.setKitId(position, kitId);
        }
    }

    /**
     * 右键：打开职业编辑器。
     *
     * <p>调用前已经 {@link #reconcile(Inventory)} 过，所以普通物品在这一步已经变成“只有图标、内容为空”的
     * 职业（页面编辑本来就是改动即入内存，没有页面级取消）。只有引用了不存在职业的槽位才在这里
     * 重新建一个，避免把悬挂 id 带进编辑器。
     */
    private void openKitEditor(Player player, int position, Inventory inventory) {
        int guiSlot = GuiLayout.pageSlot(position);
        ItemStack item = inventory.getItem(guiSlot);
        if (item == null || item.getType().isAir()) {
            return;
        }
        KitPage page = kits.ensurePage(pageIndex);
        String kitId = items.kitId(item).filter(id -> kits.findKit(id).isPresent()).orElse(null);
        if (kitId == null) {
            kitId = nextKitId();
            kits.putKit(new Kit(kitId, item, null));
            inventory.setItem(guiSlot, items.tagKit(item.clone(), kitId));
            page.setKitId(position, kitId);
        }
        Kit source = kits.findKit(kitId).orElse(null);
        if (source == null) {
            player.sendMessage(Component.text("这个职业已经不存在了", NamedTextColor.RED));
            return;
        }
        menus.openKitEditor(player, pageIndex, position, page.kitId(position), source.copy());
    }

    /** 生成一个未被占用的职业 id（{@code kit-1}、{@code kit-2}…）。 */
    private String nextKitId() {
        for (int index = 1; ; index++) {
            String candidate = "kit-" + index;
            if (kits.findKit(candidate).isEmpty()) {
                return candidate;
            }
        }
    }

    private void scheduleReconcile(Player player) {
        runNextTick(player.getUniqueId(), online -> {
            Inventory open = openInventory(online);
            if (open != null) {
                reconcile(open);
            }
        });
    }

    /** Shift 点击玩家背包：改写成放进第一个空职业格。 */
    private void moveIntoFirstFreeSlot(Player player, InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        int freePosition = -1;
        for (int position = 0; position < KitPage.SIZE; position++) {
            if (top.getItem(GuiLayout.pageSlot(position)) == null) {
                freePosition = position;
                break;
            }
        }
        if (freePosition < 0) {
            // player.sendMessage(Component.text("这一页已经满了", NamedTextColor.YELLOW));
            return;
        }
        top.setItem(GuiLayout.pageSlot(freePosition), item.clone());
        Inventory clicked = event.getClickedInventory();
        if (clicked != null) {
            clicked.setItem(event.getSlot(), null);
        }
        scheduleReconcile(player);
    }
}
