package org.awang05.kitbattlelite.gui.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.awang05.kitbattlelite.gui.GuiLayout;
import org.awang05.kitbattlelite.gui.Menu;
import org.awang05.kitbattlelite.gui.MenuFactory;
import org.awang05.kitbattlelite.item.ItemService;
import org.awang05.kitbattlelite.item.PluginItem;
import org.awang05.kitbattlelite.kit.Kit;
import org.awang05.kitbattlelite.kit.KitPage;
import org.awang05.kitbattlelite.kit.KitRepository;
import org.awang05.kitbattlelite.kit.KitSlots;

import java.util.Objects;

/**
 * 职业编辑器（管理员，模态）：编辑整套装备，显式保存或取消。
 *
 * <p>改的是工作副本（{@link Kit}），点保存才写回 {@link KitRepository} 内存；取消/ESC 丢弃。
 * 保存前用 {@code expectedKitId} 校验来源页面槽位没被别人改过，替代旧版的“原物品 equals”比较。
 *
 * <p>点击矩阵：套装 41 格、图标格与药水格可放取物品；返回=取消、保存按按钮；其余格子取消；
 * 玩家背包可点击，Shift 点击改写成“放进第一个空格”。
 *
 * <p>生命周期：由 {@link MenuFactory#openKitEditor} 每次打开时新建，关闭即丢弃工作副本。
 */
public final class KitEditMenu extends Menu {
    private final MenuFactory menus;
    private final KitRepository kits;
    private final ItemService items;
    private final int pageIndex;
    private final int position;
    private final String expectedKitId;
    private final Kit working;

    /**
     * @param pageIndex     来源页面，保存/取消后回到该页
     * @param position      来源页面位置 0..27
     * @param expectedKitId 打开时页面槽位上的 id；null 表示当时是空槽
     * @param working       编辑工作副本，保存时整体写回仓库
     */
    public KitEditMenu(JavaPlugin plugin, MenuFactory menus, KitRepository kits, ItemService items,
                       int pageIndex, int position, String expectedKitId, Kit working) {
        super(plugin);
        this.menus = menus;
        this.kits = kits;
        this.items = items;
        this.pageIndex = pageIndex;
        this.position = position;
        this.expectedKitId = expectedKitId;
        this.working = Objects.requireNonNull(working, "working");
    }

    @Override
    protected Component title() {
        return Component.text("编辑职业 · " + working.id());
    }

    @Override
    protected int size() {
        return GuiLayout.SIZE;
    }

    @Override
    protected void render(Inventory inventory) {
        fillBorder(inventory);
        clearKitGrid(inventory);
        for (int kitSlot = 0; kitSlot < KitSlots.SIZE; kitSlot++) {
            ItemStack item = working.content(kitSlot);
            if (item != null) {
                inventory.setItem(GuiLayout.kitSlot(kitSlot), item);
            }
        }
        inventory.setItem(GuiLayout.KIT_ICON, working.icon());
        inventory.setItem(GuiLayout.POTION_SLOT, items.potionDisplay(working.potion()));
        inventory.setItem(GuiLayout.BACK, items.create(PluginItem.CANCEL));
        inventory.setItem(GuiLayout.KIT_SAVE, items.create(PluginItem.SAVE));
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, boolean topClick) {
        int rawSlot = event.getRawSlot();
        if (!topClick) {
            if (rawSlot < 0) {
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
        if (rawSlot == GuiLayout.KIT_SAVE) {
            event.setCancelled(true);
            save(event.getView().getTopInventory(), player);
            return;
        }
        if (rawSlot == GuiLayout.BACK) {
            event.setCancelled(true);
            menus.openPageEditor(player, pageIndex);
            return;
        }
        if (rawSlot == GuiLayout.KIT_ICON || rawSlot == GuiLayout.POTION_SLOT
                || GuiLayout.isKitSlot(rawSlot)) {
            return;
        }
        event.setCancelled(true);
    }

    @Override
    public void onDrag(Player player, InventoryDragEvent event) {
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= GuiLayout.SIZE) {
                continue;
            }
            if (rawSlot != GuiLayout.KIT_ICON && rawSlot != GuiLayout.POTION_SLOT
                    && !GuiLayout.isKitSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @Override
    public void onClose(Player player, Inventory closed) {
        // 职业编辑是模态的，关闭等于取消，工作副本随菜单丢弃
    }

    /** 把 GUI 内容回写成工作副本；图标格为空时保留原图标。 */
    private void capture(Inventory inventory) {
        for (int kitSlot = 0; kitSlot < KitSlots.SIZE; kitSlot++) {
            working.setContent(kitSlot, inventory.getItem(GuiLayout.kitSlot(kitSlot)));
        }
        ItemStack icon = inventory.getItem(GuiLayout.KIT_ICON);
        if (icon != null && !icon.getType().isAir()) {
            working.setIcon(icon);
        }
        working.setPotion(potionMetaOf(inventory.getItem(GuiLayout.POTION_SLOT)));
    }

    /** 取药水格物品的 {@link PotionMeta}；为空或不是药水（无 PotionMeta）时返回 null，视为无效果。 */
    private static PotionMeta potionMetaOf(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        return meta instanceof PotionMeta potion ? potion : null;
    }

    /** 保存：校验来源槽位未变 → 写回仓库 → 回到页面编辑器。 */
    private void save(Inventory inventory, Player player) {
        capture(inventory);
        KitPage page = kits.page(pageIndex);
        if (page == null) {
            player.sendMessage(Component.text("页面 " + pageIndex + " 已不存在，保存失败", NamedTextColor.RED));
            menus.openPageEditor(player, pageIndex);
            return;
        }
        String currentKitId = page.kitId(position);
        if (!Objects.equals(currentKitId, expectedKitId)) {
            player.sendMessage(Component.text("原位置已发生改动，已放弃这次保存", NamedTextColor.RED));
            menus.openPageEditor(player, pageIndex);
            return;
        }
        kits.putKit(working);
        page.setKitId(position, working.id());
        menus.openPageEditor(player, pageIndex);
    }

    /** Shift 点击玩家背包：改写成放进第一个空套装格。 */
    private void moveIntoFirstFreeSlot(Player player, InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        int freeKitSlot = -1;
        for (int kitSlot = 0; kitSlot < KitSlots.SIZE; kitSlot++) {
            if (top.getItem(GuiLayout.kitSlot(kitSlot)) == null) {
                freeKitSlot = kitSlot;
                break;
            }
        }
        if (freeKitSlot < 0) {
            player.sendMessage(Component.text("这个职业已经塞满了", NamedTextColor.YELLOW));
            return;
        }
        top.setItem(GuiLayout.kitSlot(freeKitSlot), item.clone());
        Inventory clicked = event.getClickedInventory();
        if (clicked != null) {
            clicked.setItem(event.getSlot(), null);
        }
    }
}
