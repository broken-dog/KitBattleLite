package org.awang05.kitbattlelite.arena;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * 地图导入流程：校验参数 → 按需加载世界 → 登记为未开放地图。
 *
 * <p>与战斗流程无关，单独成类。
 * 只负责内存登记，落盘由调用方 {@code ImportCommand} 交给 {@link ArenaRepository}。
 *
 * <p>只在主线程调用（底层可能触发 {@code WorldCreator#createWorld()}）。
 */
public final class ArenaImportService {
    private final Logger logger;
    private final ArenaRegistry arenas;
    private final WorldLoader worldLoader;

    public ArenaImportService(JavaPlugin plugin, ArenaRegistry arenas, WorldLoader worldLoader) {
        this.logger = plugin.getLogger();
        this.arenas = arenas;
        this.worldLoader = worldLoader;
    }

    /**
     * 导入一个世界作为未开放地图：世界已加载就直接用，未加载则尝试从磁盘加载。
     *
     * <p>出生点与战斗点先都取世界出生点，之后由 {@code /kbl setworld} 调整。
     *
     * @return 新登记的地图
     * @throws IllegalArgumentException 世界名/地图 id 为空、世界不存在或加载失败、世界或 id 已被占用
     */
    public Arena importWorld(String arenaId, String worldName) {
        if (worldName == null || worldName.isBlank()) {
            throw new IllegalArgumentException("世界名不能为空");
        }
        if (arenaId == null || arenaId.isBlank()) {
            throw new IllegalArgumentException("地图 ID 不能为空");
        }
        String name = worldName.trim();
        World world = worldLoader.load(name);
        if (world == null) {
            throw new IllegalArgumentException("世界 " + name + " 不存在或加载失败");
        }
        if (arenas.byWorldName(name).isPresent()) {
            throw new IllegalArgumentException("世界 " + name + " 已经是竞技场");
        }
        if (arenas.find(arenaId).isPresent()) {
            throw new IllegalArgumentException("地图 id " + arenaId + " 已被占用");
        }
        Arena arena = new Arena(arenaId, name, name, false,
                world.getSpawnLocation(), world.getSpawnLocation());
        arenas.register(arena);
        logger.info("已导入地图 " + name);
        return arena;
    }
}
