package org.awang05.kitbattlelite.player;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 战绩仓库：stats.yml 的读写与按 UUID 缓存的战绩。
 *
 * <p>生命周期：组合根创建，onEnable 时 {@link #load()}；{@code /kbl save} 与 onDisable 时
 * {@link #save()}。玩家退服只从会话表移除，战绩留在内存里随上述落盘点写回（与旧版一致）。
 */
public final class StatsRepository {
    public static final String FILE_NAME = "stats.yml";
    public static final int DATA_VERSION = 1;

    private final Logger logger;
    private final File file;
    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();

    public StatsRepository(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.logger = plugin.getLogger();
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
    }

    /** 读取 stats.yml；单条损坏只告警跳过。 */
    public void load() {
        profiles.clear();
        YamlConfiguration config = readFile();
        checkVersion(config);

        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            logger.info(FILE_NAME + " 未配置战绩（players 段缺失）");
            return;
        }
        for (String rawId : players.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(rawId);
            } catch (IllegalArgumentException e) {
                logger.warning("跳过战绩 " + rawId + "：不是合法的 UUID");
                continue;
            }
            ConfigurationSection section = players.getConfigurationSection(rawId);
            if (section == null) {
                logger.warning("跳过战绩 " + rawId + "：不是配置段");
                continue;
            }
            String name = section.getString("name", rawId);
            int kills = Math.max(0, section.getInt("kills", 0));
            int deaths = Math.max(0, section.getInt("deaths", 0));
            profiles.put(playerId, new PlayerProfile(playerId, name, kills, deaths));
        }
        logger.info("已加载 " + profiles.size() + " 条战绩");
    }

    /** 写回 stats.yml；失败只记日志。 */
    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", DATA_VERSION);
        ConfigurationSection players = config.createSection("players");
        for (PlayerProfile profile : sortedProfiles()) {
            ConfigurationSection section = players.createSection(profile.playerId().toString());
            section.set("name", profile.name());
            section.set("kills", profile.kills());
            section.set("deaths", profile.deaths());
        }
        saveQuietly(config);
    }

    /** 取战绩，没有则按 0/0 创建并缓存。 */
    public PlayerProfile getOrCreate(UUID playerId, String name) {
        Objects.requireNonNull(playerId, "playerId");
        PlayerProfile profile = profiles.get(playerId);
        if (profile == null) {
            profile = new PlayerProfile(playerId, name, 0, 0);
            profiles.put(playerId, profile);
            return profile;
        }
        if (name != null && !name.isBlank() && !name.trim().equals(profile.name())) {
            profile.setName(name);
        }
        return profile;
    }

    public Optional<PlayerProfile> find(UUID playerId) {
        return playerId == null ? Optional.empty() : Optional.ofNullable(profiles.get(playerId));
    }

    /** 只读视图（顺序稳定，便于侧边栏/调试）。 */
    public Collection<PlayerProfile> all() {
        return Collections.unmodifiableList(sortedProfiles());
    }

    public int size() {
        return profiles.size();
    }

    private List<PlayerProfile> sortedProfiles() {
        List<PlayerProfile> list = new ArrayList<>(profiles.values());
        list.sort(Comparator.comparing(profile -> profile.playerId().toString()));
        return list;
    }

    private YamlConfiguration readFile() {
        if (!file.exists()) {
            YamlConfiguration created = new YamlConfiguration();
            created.set("version", DATA_VERSION);
            saveQuietly(created);
            logger.info(FILE_NAME + " 不存在，已创建空文件");
            return created;
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void checkVersion(YamlConfiguration config) {
        int version = config.getInt("version", -1);
        if (version < 0) {
            logger.warning(FILE_NAME + " 缺少 version，按当前版本格式读取");
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
