package org.awang05.kitbattlelite.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * config.yml 当前生效的设置快照与重读入口。
 *
 * <p>持有最近一次读到的 {@link PluginSettings}。需要设置的服务注入本类，并在使用时用
 * {@link #current()} 取值。
 *
 * <p>生命周期：组合根创建一次并注入相关服务；{@code /kbl reload} 调 {@link #reload()}。
 */
public final class SettingsService {
    private final JavaPlugin plugin;
    private PluginSettings current;

    public SettingsService(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.current = PluginSettings.load(plugin.getConfig());
    }

    /** 当前生效的设置；不可变，可以安全地跨方法传递。 */
    public PluginSettings current() {
        return current;
    }

    /** 重新读取 config.yml；缺失或类型错误的键回退到默认值。 */
    public void reload() {
        plugin.reloadConfig();
        current = PluginSettings.load(plugin.getConfig());
    }
}
