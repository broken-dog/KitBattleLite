package org.awang05.kitbattlelite.gui.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.awang05.kitbattlelite.arena.Arena;
import org.awang05.kitbattlelite.arena.ArenaRegistry;
import org.awang05.kitbattlelite.arena.ArenaService;
import org.awang05.kitbattlelite.gui.GuiIcons;
import org.awang05.kitbattlelite.gui.GuiLayout;
import org.awang05.kitbattlelite.gui.Menu;
import org.awang05.kitbattlelite.gui.MenuFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 地图选择：每张地图一格，左键进入；未开放的地图只有管理员能左键预览。
 *
 * <p>点击矩阵：顶部普通槽取消、玩家背包取消、Shift 点击取消——菜单完全只读。只读是它能长期存活、
 * 被多名玩家同时看见的前提。
 *
 * <p>生命周期：与其它菜单不同，本菜单由 {@link MenuFactory} 长期持有一个实例（库存惰性创建、反复复用），
 * 因为地图顺序与人数需要实时反映：进退场后调 {@link #updateIcon(Arena)} 原地改一格，
 * {@code /kbl reload} 后用 {@link Menu#refresh()} 整体重画；地图数跨行变化时尺寸自适应
 * （{@link GuiLayout#worldSize(int)}），需要换库存，换前由 {@code MenuFactory} 关掉正在查看的玩家。
 *
 * <p>顺序取自 {@link ArenaRegistry} 的注册顺序（{@code arenas.yml} 的书写顺序）。
 */
public final class WorldSelectMenu extends Menu {
    private final ArenaRegistry arenas;
    private final ArenaService arenaService;

    public WorldSelectMenu(JavaPlugin plugin, ArenaRegistry arenas, ArenaService arenaService) {
        super(plugin);
        this.arenas = arenas;
        this.arenaService = arenaService;
    }

    @Override
    protected Component title() {
        return Component.text("选择地图");
    }

    @Override
    protected int size() {
        return GuiLayout.worldSize(arenas.size());
    }

    @Override
    protected void render(Inventory inventory) {
        int slot = 0;
        for (Arena arena : arenas.all()) {
            if (slot >= inventory.getSize()) {
                // plugin.getLogger().warning("地图数量超过 " + size()
                //        + " 个，选择页只显示前 " + size() + " 张（其余用命令进入）");
                break;
            }
            inventory.setItem(slot++, icon(arena));
        }
    }

    /**
     * 原地刷新一张地图的图标（人数/开放状态变化后调用）。
     *
     * <p>按 id 定位 slot。菜单还没打开过时什么都不做。
     */
    public void updateIcon(Arena arena) {
        if (arena == null || !hasInventory()) {
            return;
        }
        int index = indexOf(arena.id());
        if (index < 0 || index >= getInventory().getSize()) {
            return;
        }
        getInventory().setItem(index, icon(arena));
    }

    private ItemStack icon(Arena arena) {
        int count = arenaService.participantCount(arena);
        List<Component> lore = new ArrayList<>(2);
        Component name = MiniMessage.miniMessage().deserialize(arena.displayName());
        if (arena.enabled()) {
            lore.add(Component.text("人数：", NamedTextColor.DARK_PURPLE)
                    .append(Component.text(Integer.toString(count), NamedTextColor.GREEN)));
            lore.add(Component.text("点击进入", NamedTextColor.YELLOW));
            Material material = count == 0 ? Material.GREEN_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE;
            return GuiIcons.of(material, name, lore);
        }
        lore.add(Component.text("敬请期待", NamedTextColor.GRAY));
        return GuiIcons.of(Material.BLACK_STAINED_GLASS_PANE, name, lore);
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, boolean topClick) {
        event.setCancelled(true);
        if (!topClick) {
            return;
        }
        Arena arena = arenaAt(event.getRawSlot());
        if (arena == null) {
            return;
        }
        if (!arena.enabled()) {
            if (!player.hasPermission(MenuFactory.ADMIN_PERMISSION)) {
                player.sendMessage(Component.text("这张地图尚未开放", NamedTextColor.YELLOW));
                return;
            }
            runNextTick(player.getUniqueId(), online -> {
                online.closeInventory();
                arenaService.preview(online, arena);
            });
            return;
        }
        runNextTick(player.getUniqueId(), online -> {
            online.closeInventory();
            arenaService.join(online, arena);
            // 共享库存：进场后把所有正在看这张菜单的人的人数一起刷新
            updateIcon(arena);
        });
    }

    /** 第 n 个地图格对应的地图；超出或空槽返回 null。 */
    private Arena arenaAt(int guiSlot) {
        if (guiSlot < 0) {
            return null;
        }
        int index = 0;
        for (Arena arena : arenas.all()) {
            if (index == guiSlot) {
                return arena;
            }
            index++;
        }
        return null;
    }

    /** 按 id 找地图在菜单里的下标；不存在返回 -1。 */
    private int indexOf(String arenaId) {
        int index = 0;
        for (Arena arena : arenas.all()) {
            if (arena.id().equals(arenaId)) {
                return index;
            }
            index++;
        }
        return -1;
    }
}
