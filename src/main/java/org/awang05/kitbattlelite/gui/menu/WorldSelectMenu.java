package org.awang05.kitbattlelite.gui.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * <p>点击矩阵：顶部普通槽取消、玩家背包取消、Shift 点击取消——菜单完全只读。
 * 人数在打开那一刻定格（不随玩家进出实时变化），重开即刷新。
 *
 * <p>生命周期：由 {@link MenuFactory#openWorldSelect(Player)} 每次打开时新建，关闭即丢弃。
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
        return GuiLayout.WORLD_SLOT_COUNT;
    }

    @Override
    protected void render(Inventory inventory) {
        int slot = 0;
        for (Arena arena : arenas.all()) {
            if (slot >= size()) {
                // plugin.getLogger().warning("地图数量超过 " + size()
                //        + " 个，选择页只显示前 " + size() + " 张（其余用命令进入）");
                break;
            }
            inventory.setItem(slot++, icon(arena));
        }
    }

    public void updateIcon(Arena arena) {
        int i = new ArrayList<>(arenas.all()).indexOf(arena);
        if (i >= 0 && i < GuiLayout.WORLD_SLOT_COUNT) {
            getInventory().setItem(i, icon(arena));
        }
    }

    private ItemStack icon(Arena arena) {
        int count = arenaService.participantCount(arena);
        List<Component> lore = new ArrayList<>(2);
        if (arena.enabled()) {
            lore.add(Component.text("人数：", NamedTextColor.DARK_PURPLE)
                    .append(Component.text(Integer.toString(count), NamedTextColor.GREEN)));
            lore.add(Component.text("左键进入", NamedTextColor.YELLOW));
            Material material = count == 0 ? Material.LIME_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE;
            return GuiIcons.of(material, Component.text(arena.displayName(), NamedTextColor.GREEN), lore);
        }
        lore.add(Component.text("敬请期待", NamedTextColor.GRAY));
        lore.add(Component.text("管理员可左键预览", NamedTextColor.DARK_GRAY));
        return GuiIcons.of(Material.BLACK_STAINED_GLASS_PANE,
                Component.text(arena.displayName()), lore);
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
        });
    }

    /** 第 n 个地图格对应的地图；超出或空槽返回 null。 */
    private Arena arenaAt(int guiSlot) {
        if (guiSlot < 0 || guiSlot >= GuiLayout.WORLD_SLOT_COUNT) {
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
}
