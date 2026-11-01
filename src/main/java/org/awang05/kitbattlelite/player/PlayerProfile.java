package org.awang05.kitbattlelite.player;

import java.util.Objects;
import java.util.UUID;

/**
 * 玩家战绩：击杀、死亡与 K/D。
 *
 * <p>只存 UUID 和值，不持有 {@link org.bukkit.entity.Player}；由
 * {@link StatsRepository} 负责读写 stats.yml。
 */
public final class PlayerProfile {
    private final UUID playerId;
    private String name;
    private int kills;
    private int deaths;

    public PlayerProfile(UUID playerId, String name, int kills, int deaths) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        setName(name);
        this.kills = Math.max(0, kills);
        this.deaths = Math.max(0, deaths);
    }

    public UUID playerId() {
        return playerId;
    }

    public String name() {
        return name;
    }

    /** 更新显示名（玩家改名后下次加载会以新名字写回）。 */
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("玩家名不能为空");
        }
        this.name = name.trim();
    }

    public int kills() {
        return kills;
    }

    public int deaths() {
        return deaths;
    }

    public void addKill() {
        kills++;
    }

    public void addDeath() {
        deaths++;
    }

    /** 击杀/死亡比；没有死亡时等于击杀数，0/0 记 0。 */
    public float kdr() {
        if (deaths == 0) {
            return kills;
        }
        return ((float) kills) / deaths;
    }
}
