package org.awang05.kitbattlelite.arena;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 按需加载世界：已加载则直接返回，未加载时先确认世界文件夹存在，再 {@link WorldCreator#createWorld()}。
 *
 * <p>存在理由：{@code arenas.yml} 里配置的地图与 {@code /kbl import} 传入的世界可能还没被服务端加载，
 * 直接跳过会表现为“配置写好了但地图不出现”。两个调用点共用这里的实现，避免各写一份。
 *
 * <p>只在主线程调用（{@code createWorld()} 会改动服务端的世界表）。
 *
 * <p>生命周期：组合根创建，注入 {@link ArenaRepository} 与 {@link ArenaService}；无状态。
 */
public final class WorldLoader {
    private final Logger logger;

    public WorldLoader(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
    }

    /**
     * 取一个可用的世界，必要时从磁盘加载。
     *
     * @param rawName 世界名
     * @return 世界；名称为空、文件夹不存在、不是有效世界或加载失败时返回 {@code null}（已记日志）
     */
    public World load(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            logger.warning("无法加载世界：世界名为空");
            return null;
        }
        String name = rawName.trim();
        World loaded = Bukkit.getWorld(name);
        if (loaded != null) {
            return loaded;
        }
        if (!isSimpleName(name)) {
            logger.warning("无法加载世界 " + name + "：名称不能包含路径分隔符");
            return null;
        }
        File folder = new File(Bukkit.getWorldContainer(), name);
        if (!folder.isDirectory()) {
            logger.warning("世界文件夹不存在：" + folder.getPath());
            return null;
        }
        if (!new File(folder, "level.dat").isFile()) {
            // 只有文件夹没有 level.dat，多半是手建的空目录或维度子目录，交给服务端会生成新图
            logger.warning("世界文件夹不是有效世界（缺少 level.dat）：" + folder.getPath());
            return null;
        }
        try {
            World world = new WorldCreator(name).createWorld();
            if (world == null) {
                logger.warning("加载世界失败（服务端返回 null）：" + name);
                return null;
            }
            logger.info("世界已加载：" + name);
            return world;
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "加载世界失败：" + name, e);
            return null;
        }
    }

    private static boolean isSimpleName(String name) {
        return name.indexOf('/') < 0 && name.indexOf('\\') < 0 && !name.equals("..") && !name.equals(".");
    }
}
