package org.awang05.kitbattlelite.arena;

import org.bukkit.Location;

import java.util.Objects;

/**
 * 一张地图（竞技场）：id、世界名、显示名、开关、出生点、战斗点。
 *
 * <p>参与人数由 {@code ArenaService} 从会话表统计（避免两份状态）。
 *
 * <p>纯数据对象，持久化由 {@link ArenaRepository} 负责。
 */
public final class Arena {
    private final String id;
    private final String worldName;
    private String displayName;
    private boolean enabled;
    private Location spawn;
    private Location teleport;

    /**
     * @param id          地图 id（会成为 arenas.yml 的键与命令参数，不含空白/点号）
     * @param worldName   对应世界名
     * @param displayName 显示名；空白时退回 id
     * @param enabled     是否开放（旧的 ok 字段）
     * @param spawn       出生点
     * @param teleport    选完职业后的战斗点
     */
    public Arena(String id, String worldName, String displayName, boolean enabled,
                 Location spawn, Location teleport) {
        this.id = normalizeId(id);
        if (worldName == null || worldName.isBlank()) {
            throw new IllegalArgumentException("地图 " + id + " 的 world 不能为空");
        }
        this.worldName = worldName.trim();
        this.displayName = displayName == null || displayName.isBlank() ? this.id : displayName.trim();
        this.enabled = enabled;
        this.spawn = Objects.requireNonNull(spawn, "spawn").clone();
        this.teleport = Objects.requireNonNull(teleport, "teleport").clone();
    }

    public String id() {
        return id;
    }

    public String worldName() {
        return worldName;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("地图显示名不能为空");
        }
        this.displayName = displayName.trim();
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Location spawn() {
        return spawn.clone();
    }

    public void setSpawn(Location spawn) {
        this.spawn = Objects.requireNonNull(spawn, "spawn").clone();
    }

    public Location teleport() {
        return teleport.clone();
    }

    public void setTeleport(Location teleport) {
        this.teleport = Objects.requireNonNull(teleport, "teleport").clone();
    }

    private static String normalizeId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("地图 id 不能为 null");
        }
        String trimmed = id.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("地图 id 不能为空");
        }
        if (trimmed.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("地图 id 不能包含空白字符: " + id);
        }
        if (trimmed.indexOf('.') >= 0) {
            throw new IllegalArgumentException("地图 id 不能包含点号: " + id);
        }
        return trimmed;
    }
}
