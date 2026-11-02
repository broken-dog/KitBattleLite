package org.awang05.kitbattlelite.gui;

import org.awang05.kitbattlelite.arena.ArenaRegistry;
import org.awang05.kitbattlelite.arena.ArenaService;
import org.awang05.kitbattlelite.gui.menu.KitDetailMenu;
import org.awang05.kitbattlelite.gui.menu.KitEditMenu;
import org.awang05.kitbattlelite.gui.menu.KitPageEditMenu;
import org.awang05.kitbattlelite.gui.menu.KitPageMenu;
import org.awang05.kitbattlelite.gui.menu.WorldSelectMenu;
import org.awang05.kitbattlelite.item.ItemService;
import org.awang05.kitbattlelite.kit.Kit;
import org.awang05.kitbattlelite.kit.KitRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 菜单的唯一入口：创建菜单实例并在下一 tick 打开。
 *
 * <p>菜单之间不互相持有，所有跳转都经过这里；点击事件里不直接 {@code openInventory}，
 * 统一 {@link #openNextTick(Player, Supplier)}，避免在事件处理中替换打开的库存。
 *
 * <p>打开时渲染的是快照（如地图人数在打开那一刻定格），这是有意为之：菜单只活一次。
 *
 * <p>生命周期：组合根创建一次并注入命令与菜单；无内部可变状态。
 */
public final class MenuFactory {
    /** 管理权限节点，与 {@code plugin.yml} 的 {@code permissions} 保持一致。 */
    public static final String ADMIN_PERMISSION = "kbl.admin";

    private final JavaPlugin plugin;
    private final ArenaRegistry arenas;
    private final ArenaService arenaService;
    private final KitRepository kits;
    private final ItemService items;

    public MenuFactory(JavaPlugin plugin, ArenaRegistry arenas, ArenaService arenaService,
                       KitRepository kits, ItemService items) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.arenas = Objects.requireNonNull(arenas, "arenas");
        this.arenaService = Objects.requireNonNull(arenaService, "arenaService");
        this.kits = Objects.requireNonNull(kits, "kits");
        this.items = Objects.requireNonNull(items, "items");
    }

    /** 地图选择（玩家入口）。 */
    public void openWorldSelect(Player player) {
        openNextTick(player, () -> new WorldSelectMenu(plugin, arenas, arenaService));
    }

    /** 职业页（只读，玩家选择职业）。 */
    public void openKitPage(Player player, int pageIndex) {
        openNextTick(player, () -> new KitPageMenu(plugin, this, kits, items, arenaService, pageIndex));
    }

    /** 职业详情（只读）。 */
    public void openKitDetail(Player player, String kitId, int pageIndex) {
        openNextTick(player, () -> new KitDetailMenu(plugin, this, kits, items, kitId, pageIndex));
    }

    /** 页面编辑器（管理员）。 */
    public void openPageEditor(Player player, int pageIndex) {
        openNextTick(player, () -> new KitPageEditMenu(plugin, this, kits, items, pageIndex));
    }

    /**
     * 职业编辑器（管理员）。
     *
     * @param pageIndex     来源页面，保存/取消后回到该页
     * @param position      来源页面位置 0..27
     * @param expectedKitId 打开时页面槽位上的 id，保存前用于并发校验
     * @param working       编辑工作副本；保存时写回 {@link KitRepository}，取消则丢弃
     */
    public void openKitEditor(Player player, int pageIndex, int position,
                              String expectedKitId, Kit working) {
        openNextTick(player, () -> new KitEditMenu(plugin, this, kits, items, pageIndex, position,
                expectedKitId, working));
    }

    /** 下一 tick 打开新菜单；只捕获 UUID，玩家已离线时跳过。 */
    public void openNextTick(Player player, Supplier<Menu> supplier) {
        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player online = Bukkit.getPlayer(playerId);
            if (online != null) {
                supplier.get().open(online);
            }
        });
    }
}
