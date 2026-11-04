package org.awang05.kitbattlelite.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * arenas.yml 的读写。
 *
 * <p>位置写成显式的 {@code x/y/z/yaw/pitch} 五个键（不用 {@code Location.serialize()}）。
 *
 * <p>地图绑定的世界未加载时，通过 {@link WorldLoader} 尝试从磁盘加载；加载不了的地图才告警跳过，
 * 不阻断其它地图。被跳过的地图段会深拷贝保留，{@link #save(ArenaRegistry)} 时原样写回，
 * 避免一次 {@code /kbl save} 把加载失败的地图从文件里抹掉。
 *
 * <p>生命周期：组合根创建；onEnable 调 {@link #loadInto(ArenaRegistry)}，{@code /kbl save}
 * 与 onDisable 调 {@link #save(ArenaRegistry)}。
 */
public final class ArenaRepository {
    public static final String FILE_NAME = "arenas.yml";
    public static final int DATA_VERSION = 1;

    private final Logger logger;
    private final File file;
    private final WorldLoader worldLoader;

    /** 上次加载时没能进注册表的地图段（深拷贝），保存时原样写回 */
    private YamlConfiguration preserved = new YamlConfiguration();

    public ArenaRepository(JavaPlugin plugin, WorldLoader worldLoader) {
        this.logger = plugin.getLogger();
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        this.worldLoader = worldLoader;
    }

    /** 重新读取 arenas.yml 并填充注册表；先清空旧内容。 */
    public void loadInto(ArenaRegistry registry) {
        registry.clear();
        preserved = new YamlConfiguration();
        YamlConfiguration config = readFile();
        checkVersion(config);

        ConfigurationSection arenasSection = config.getConfigurationSection("arenas");
        if (arenasSection == null) {
            // logger.info(FILE_NAME + " 未配置地图（arenas 段缺失）");
            return;
        }
        for (String rawId : arenasSection.getKeys(false)) {
            ConfigurationSection section = arenasSection.getConfigurationSection(rawId);
            if (section == null) {
                // 不是配置段（手写成标量之类）：原样保留，保存时不丢
                preserved.set(rawId, arenasSection.get(rawId));
                continue;
            }
            loadArena(registry, rawId, section);
        }
        logger.info("已加载 " + registry.size() + " 张地图");
    }

    /** 把注册表写回 arenas.yml；本次没加载进注册表的地图段原样保留。失败只记日志。 */
    public void save(ArenaRegistry registry) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", DATA_VERSION);
        ConfigurationSection arenasSection = config.createSection("arenas");
        for (Arena arena : registry.all()) {
            ConfigurationSection section = arenasSection.createSection(arena.id());
            section.set("world", arena.worldName());
            section.set("name", arena.displayName());
            section.set("enabled", arena.enabled());
            writePosition(section.createSection("spawn"), arena.spawn());
            writePosition(section.createSection("teleport"), arena.teleport());
        }
        int kept = carryOverUnloaded(registry, arenasSection);
        if (kept > 0) {
            logger.info("保存 " + FILE_NAME + " 时原样保留了 " + kept + " 个本次未加载的地图条目");
        }
        saveQuietly(config);
    }

    /** 把加载失败的条目写回文件；注册表里已有的 id 以注册表为准。返回保留条数。 */
    private int carryOverUnloaded(ArenaRegistry registry, ConfigurationSection target) {
        int kept = 0;
        for (String id : new TreeSet<>(preserved.getKeys(false))) {
            if (registry.find(id).isPresent()) {
                continue;
            }
            Object value = preserved.get(id);
            if (value instanceof ConfigurationSection section) {
                copyInto(section, target.createSection(id));
            } else {
                target.set(id, value);
            }
            kept++;
        }
        return kept;
    }

    private void preserve(String id, ConfigurationSection section) {
        copyInto(section, preserved.createSection(id));
    }

    private static void copyInto(ConfigurationSection from, ConfigurationSection to) {
        for (String key : from.getKeys(false)) {
            Object value = from.get(key);
            if (value instanceof ConfigurationSection child) {
                copyInto(child, to.createSection(key));
            } else {
                to.set(key, value);
            }
        }
    }

    private void loadArena(ArenaRegistry registry, String rawId, ConfigurationSection section) {
        String worldName = section.getString("world", rawId);
        if (worldName == null || worldName.isBlank()) {
            logger.warning("跳过地图 " + rawId + "：world 为空（保存时原样保留）");
            preserve(rawId, section);
            return;
        }
        worldName = worldName.trim();
        World world = worldLoader.load(worldName);
        if (world == null) {
            logger.warning("跳过地图 " + rawId + "：世界 " + worldName + " 无法加载（保存时原样保留）");
            preserve(rawId, section);
            return;
        }
        String displayName = section.getString("name", rawId);
        boolean enabled = section.getBoolean("enabled", false);
        Location spawn = readPosition(section.getConfigurationSection("spawn"), world, rawId + ".spawn");
        Location teleport = readPosition(section.getConfigurationSection("teleport"), world, rawId + ".teleport");
        try {
            registry.register(new Arena(rawId, worldName, displayName, enabled, spawn, teleport));
        } catch (RuntimeException e) {
            logger.warning("跳过地图 " + rawId + "：" + e.getMessage() + "（保存时原样保留）");
            preserve(rawId, section);
        }
    }

    private Location readPosition(ConfigurationSection section, World world, String context) {
        Location fallback = world.getSpawnLocation();
        if (section == null) {
            logger.info(context + " 未配置，使用世界出生点");
            return fallback;
        }
        double x = section.getDouble("x", fallback.getX());
        double y = section.getDouble("y", fallback.getY());
        double z = section.getDouble("z", fallback.getZ());
        float yaw = (float) section.getDouble("yaw", fallback.getYaw());
        float pitch = (float) section.getDouble("pitch", fallback.getPitch());
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void writePosition(ConfigurationSection section, Location location) {
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", (double) location.getYaw());
        section.set("pitch", (double) location.getPitch());
    }

    private YamlConfiguration readFile() {
        if (!file.exists()) {
            YamlConfiguration created = new YamlConfiguration();
            created.set("version", DATA_VERSION);
            saveQuietly(created);
            logger.info(FILE_NAME + " 不存在，已创建空文件供手工编辑");
            return created;
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void checkVersion(YamlConfiguration config) {
        int version = config.getInt("version", -1);
        if (version < 0) {
            logger.warning(FILE_NAME + " 缺少 version，按当前格式读取；下次保存会补上");
            return;
        }
        if (version > DATA_VERSION) {
            logger.warning(FILE_NAME + " 的 version=" + version
                    + " 高于本插件支持的 " + DATA_VERSION + "，未知字段会被忽略");
        }
    }

    private void saveQuietly(YamlConfiguration config) {
        try {
            File folder = file.getParentFile();
            if (folder != null && !folder.isDirectory() && !folder.mkdirs()) {
                throw new IOException("无法创建目录 " + folder);
            }
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "保存 " + FILE_NAME + " 失败", e);
        }
    }
}
