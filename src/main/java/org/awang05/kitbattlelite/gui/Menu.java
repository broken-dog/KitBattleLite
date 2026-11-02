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
 * 抽象菜单：自身就是 {@link InventoryHolder}。
 *
 * <p>生命周期：默认由 {@link MenuFactory} 在每次打开时新建一个实例、关闭即丢弃；库存是
 * **惰性创建**的（第一次 {@link #open(Player)} 时建），同一实例再次打开会复用同一个库存。
 * 目前只有地图选择页利用这一点长期存活、原地刷新（见 {@link #refresh()} 与
 * {@code WorldSelectMenu#updateIcon(Arena)}），其余菜单仍是一次一份。
 *
 * <p>不缓存 {@link Player}，也不被会话或其它菜单持有；菜单之间跳转一律走 {@link MenuFactory}。
 * 点击策略由各菜单实现；默认所有拖拽都被取消，只读菜单无需额外处理。
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

    /** 打开菜单：库存惰性创建；同一实例再次打开时复用已有库存。 */
    public final void open(Player player) {
        Objects.requireNonNull(player, "player");
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, size(), title());
            render(inventory);
        }
        player.openInventory(inventory);
    }

    /**
     * 原地重渲染：清空后按当前模型重画同一个库存，不新建菜单实例、不换库存对象。
     *
     * <p>用于 {@code /kbl reload} 这类结构性变化（地图增删、顺序调整）；库存还没创建过时什么都不做。
     */
    public final void refresh() {
        if (inventory == null) {
            return;
        }
        inventory.clear();
        render(inventory);
    }

    /** 库存是否已经创建（菜单至少被打开过一次）；增量更新前用它判断。 */
    protected final boolean hasInventory() {
        return inventory != null;
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
