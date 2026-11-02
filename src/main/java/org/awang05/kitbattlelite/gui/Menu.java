package org.awang05.kitbattlelite.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.awang05.kitbattlelite.kit.KitSlots;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 抽象菜单：自身就是 {@link InventoryHolder}，一份实例只对应一次“打开”。
 *
 * <p>生命周期：由 {@link MenuFactory} 在打开时新建，{@link #open(Player)} 里创建库存并渲染，
 * 关闭后即被丢弃；不缓存 {@link Player}，也不被会话或其它菜单持有。菜单之间跳转一律走
 * {@link MenuFactory}，避免互相引用。
 *
 * <p>点击策略由各菜单实现（见各自 Javadoc 的表格）；默认所有拖拽都被取消，只读菜单无需额外处理。
 * 延迟执行只捕获 {@link UUID}，下一 tick 再解析在线玩家。
 */
public abstract class Menu implements InventoryHolder {
    protected final JavaPlugin plugin;
    private Inventory inventory;

    protected Menu(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /** 菜单标题。 */
    protected abstract Component title();

    /** 库存大小 */
    protected abstract int size();

    /** 渲染内容 */
    protected abstract void render(Inventory inventory);

    /**
     * 点击处理。
     *
     * @param topClick 点击的是不是菜单上半部分（{@code rawSlot} 落在顶部库存内）
     */
    public abstract void onClick(Player player, InventoryClickEvent event, boolean topClick);

    /** 拖拽处理；默认全部取消，可编辑菜单按需放行。 */
    public void onDrag(Player player, InventoryDragEvent event) {
        event.setCancelled(true);
    }

    /**
     * 关闭处理（玩家 ESC、插件主动关闭、退服时都会触发）。
     *
     * @param closed 正在关闭的库存；此时 {@code player.getOpenInventory()} 不保证还指向它
     */
    public void onClose(Player player, Inventory closed) {
    }

    /** 打开菜单：每次新建库存，旧实例不重用。 */
    public final void open(Player player) {
        Objects.requireNonNull(player, "player");
        inventory = Bukkit.createInventory(this, size(), title());
        render(inventory);
        player.openInventory(inventory);
    }

    /** 当前是否仍由这名玩家打开着本菜单（写回前用它校验 holder）。 */
    protected final Inventory openInventory(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        return top.getHolder() == this ? top : null;
    }

    /** 下一 tick 执行，只捕获 UUID；玩家已离线时直接跳过。 */
    protected final void runNextTick(UUID playerId, Consumer<Player> action) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player online = Bukkit.getPlayer(playerId);
            if (online != null) {
                action.accept(online);
            }
        });
    }

    /** 用灰色玻璃板铺满整屏做边框；需要空着的位置由子类随后清空。 */
    protected static void fillBorder(Inventory inventory) {
        ItemStack pane = GuiIcons.pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < GuiLayout.SIZE; slot++) {
            inventory.setItem(slot, pane.clone());
        }
    }

    /** 清空职业页的 28 个格子（铺边框后必须调用，否则空格会留着边框）。 */
    protected static void clearPageGrid(Inventory inventory) {
        for (int position = 0; position < GuiLayout.PAGE_SLOT_COUNT; position++) {
            inventory.setItem(GuiLayout.pageSlot(position), null);
        }
    }

    /** 清空套装 41 格（装备 5 格 + 背包 36 格）。 */
    protected static void clearKitGrid(Inventory inventory) {
        for (int kitSlot = 0; kitSlot < KitSlots.SIZE; kitSlot++) {
            inventory.setItem(GuiLayout.kitSlot(kitSlot), null);
        }
    }

    @Override
    public final @NotNull Inventory getInventory() {
        return inventory;
    }
}
