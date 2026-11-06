package org.awang05.kitbattlelite;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.awang05.kitbattlelite.arena.ArenaRegistry;
import org.awang05.kitbattlelite.arena.ArenaRepository;
import org.awang05.kitbattlelite.arena.ArenaService;
import org.awang05.kitbattlelite.arena.WorldLoader;
import org.awang05.kitbattlelite.command.KitBattleCommand;
import org.awang05.kitbattlelite.command.SubCommand;
import org.awang05.kitbattlelite.command.sub.EditCommand;
import org.awang05.kitbattlelite.command.sub.ImportCommand;
import org.awang05.kitbattlelite.command.sub.KitCommand;
import org.awang05.kitbattlelite.command.sub.LeaveCommand;
import org.awang05.kitbattlelite.command.sub.OpenCommand;
import org.awang05.kitbattlelite.command.sub.ReloadCommand;
import org.awang05.kitbattlelite.command.sub.SetWorldCommand;
import org.awang05.kitbattlelite.config.Messages;
import org.awang05.kitbattlelite.config.PluginSettings;
import org.awang05.kitbattlelite.gui.MenuFactory;
import org.awang05.kitbattlelite.gui.MenuListener;
import org.awang05.kitbattlelite.hud.HealthDisplayService;
import org.awang05.kitbattlelite.hud.SidebarService;
import org.awang05.kitbattlelite.item.ItemService;
import org.awang05.kitbattlelite.kit.KitRepository;
import org.awang05.kitbattlelite.listener.HealthUpdateListener;
import org.awang05.kitbattlelite.listener.HotbarButtonListener;
import org.awang05.kitbattlelite.listener.InventoryListener;
import org.awang05.kitbattlelite.listener.ItemDropListener;
import org.awang05.kitbattlelite.listener.PlayerDeathListener;
import org.awang05.kitbattlelite.listener.PlayerLifecycleListener;
import org.awang05.kitbattlelite.listener.ProjectileCleanupListener;
import org.awang05.kitbattlelite.listener.ProtectionListener;
import org.awang05.kitbattlelite.player.SessionRegistry;
import org.awang05.kitbattlelite.player.StatsRepository;

import java.util.List;

/**
 * 组合根：创建全部服务、注册监听与命令，并按固定顺序关闭。
 *
 * <p>构造顺序即依赖顺序；服务构造器不访问 Bukkit 世界，世界加载发生在 repository 的
 * {@code load} 里。没有静态状态，插件实例只由 Bukkit 持有。
 */
public final class KitBattleLite extends JavaPlugin {
    private PluginSettings settings;
    private Messages messages;
    private ItemService items;
    private KitRepository kits;
    private ArenaRegistry arenas;
    private ArenaRepository arenaRepository;
    private SessionRegistry sessions;
    private StatsRepository stats;
    private SidebarService sidebar;
    private HealthDisplayService health;
    private ArenaService arenaService;
    private MenuFactory menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource(Messages.FILE_NAME, false);

        settings = PluginSettings.load(getConfig());
        messages = new Messages(this);
        items = new ItemService(this);

        kits = new KitRepository(this);
        kits.load();

        arenas = new ArenaRegistry();
        WorldLoader worldLoader = new WorldLoader(this);
        arenaRepository = new ArenaRepository(this, worldLoader);
        arenaRepository.loadInto(arenas);

        sessions = new SessionRegistry();
        stats = new StatsRepository(this);
        stats.load();

        sidebar = new SidebarService(settings, messages, sessions, stats);
        health = new HealthDisplayService(settings);
        arenaService = new ArenaService(this, arenas, sessions, stats, kits, items, settings, messages,
                worldLoader, sidebar, health);
        menus = new MenuFactory(this, arenas, arenaService, kits, items);

        registerListeners();
        registerCommand();

        getLogger().info("已启用：职业 " + kits.kitCount() + " 个、地图 " + arenas.size()
                + " 张、战绩 " + stats.size() + " 条");
    }

    private void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new MenuListener(), this);
        pluginManager.registerEvents(new PlayerLifecycleListener(sessions, arenaService, menus), this);
        pluginManager.registerEvents(new PlayerDeathListener(this, arenaService), this);
        pluginManager.registerEvents(new ProtectionListener(arenaService), this);
        pluginManager.registerEvents(new HealthUpdateListener(this, health), this);
        pluginManager.registerEvents(new HotbarButtonListener(items, kits, arenaService, menus, messages), this);
        pluginManager.registerEvents(new ItemDropListener(arenaService), this);
        pluginManager.registerEvents(new ProjectileCleanupListener(arenas), this);
        pluginManager.registerEvents(new InventoryListener(arenas), this);
    }

    private void registerCommand() {
        PluginCommand command = getCommand("kitbattlelite");
        if (command == null) {
            getLogger().severe("plugin.yml 里找不到 kitbattlelite 命令，/kbl 不可用");
            return;
        }
        List<SubCommand> subCommands = List.of(
                new OpenCommand(arenaService, menus, messages),
                new LeaveCommand(arenaService, menus, messages),
                new EditCommand(menus, kits, messages),
                new ImportCommand(arenaService, arenas, arenaRepository, menus, messages),
                new SetWorldCommand(arenas, arenaRepository, menus, messages),
                new KitCommand(kits, messages, getLogger()),
                new ReloadCommand(messages, this::reloadAll));
        KitBattleCommand root = new KitBattleCommand(messages, subCommands);
        command.setExecutor(root);
        command.setTabCompleter(root);
    }

    /**
     * {@code /kbl reload} 的全部流程。
     *
     * <p>顺序：先关闭所有插件菜单，再请出在线参与者，然后重读四个数据文件并重画地图选择页。
     *
     * <p>config.yml 的数值在启用时注入服务，热重载不覆盖（需重启，已写在 config.yml 注释里）。
     */
    private void reloadAll() {
        menus.closeOpenMenus();
        arenaService.kickAll();
        kits.load();
        arenaRepository.loadInto(arenas);
        stats.load();
        messages.reload();
        menus.refreshWorldSelect();
        getLogger().info("重载完成：职业 " + kits.kitCount() + " 个、地图 " + arenas.size() + " 张");
    }

    @Override
    public void onDisable() {
        // 逆序：先摘 HUD，再请出玩家（恢复快照/传回返回点），最后统一落盘
        menus.closeOpenMenus();
        if (sidebar != null) {
            sidebar.shutdown();
        }
        if (health != null) {
            health.shutdown();
        }
        if (arenaService != null) {
            arenaService.shutdown();
        }
        if (kits != null) {
            kits.save();
        }
        if (arenaRepository != null) {
            arenaRepository.save(arenas);
        }
        if (stats != null) {
            stats.save();
        }
    }
}
