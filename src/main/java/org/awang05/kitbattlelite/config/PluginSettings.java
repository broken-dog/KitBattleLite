package org.awang05.kitbattlelite.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

/**
 * config.yml 的强类型视图。
 *
 * <p>只做读取与范围兜底，不持有 Bukkit 对象；热重载时重新 {@link #load(FileConfiguration)} 即可。
 *
 * @param protectionSeconds      选完职业后的免伤秒数，0 表示不保护
 * @param deathBlindnessTicks    死亡重生后的失明 tick 数，0 表示不失明
 * @param sidebarEnabled         是否显示 FastBoard 侧边栏
 * @param healthBelowNameEnabled 是否用共享计分板显示玩家名下血量
 */
public record PluginSettings(
        int protectionSeconds,
        int deathBlindnessTicks,
        boolean sidebarEnabled,
        boolean healthBelowNameEnabled
) {
    public static final int DEFAULT_PROTECTION_SECONDS = 3;
    public static final int DEFAULT_DEATH_BLINDNESS_TICKS = 40;

    public PluginSettings {
        if (protectionSeconds < 0) {
            throw new IllegalArgumentException("protectionSeconds 不能为负: " + protectionSeconds);
        }
        if (deathBlindnessTicks < 0) {
            throw new IllegalArgumentException("deathBlindnessTicks 不能为负: " + deathBlindnessTicks);
        }
    }

    /** 从配置读取设置；缺失或类型错误时用默认值。 */
    public static PluginSettings load(FileConfiguration config) {
        Objects.requireNonNull(config, "config");
        return new PluginSettings(
                Math.max(0, config.getInt("protection-seconds", DEFAULT_PROTECTION_SECONDS)),
                Math.max(0, config.getInt("death-blindness-ticks", DEFAULT_DEATH_BLINDNESS_TICKS)),
                config.getBoolean("sidebar.enabled", true),
                config.getBoolean("health-below-name.enabled", true)
        );
    }
}
