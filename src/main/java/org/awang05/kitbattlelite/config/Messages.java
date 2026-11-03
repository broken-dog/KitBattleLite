package org.awang05.kitbattlelite.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * {@code messages.yml} 的读取与 MiniMessage 解析。
 *
 * <p>文案中占位符不二次解析。
 * 实际配置文件缺键时回退到插件内置的 {@code messages.yml}；两边都没有时告警并显示键名。
 *
 * <p>生命周期：组合根创建一次并注入相关服务；{@code /kbl reload} 调 {@link #reload()}。
 */
public final class Messages {
    public static final String FILE_NAME = "messages.yml";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final File file;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Set<String> missingKeys = new HashSet<>();

    private YamlConfiguration config;
    private YamlConfiguration defaults;
    private Component prefix = Component.empty();

    public Messages(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = plugin.getLogger();
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        reload();
    }

    /** 重新读取 messages.yml；缺键只影响该键，不阻断启动。 */
    public void reload() {
        missingKeys.clear();
        defaults = loadDefaults();
        config = YamlConfiguration.loadConfiguration(file);
        if (!file.isFile()) {
            logger.warning(FILE_NAME + " 不存在，全部使用内置文案");
        }
        prefix = component("prefix");
    }

    /** 原始文案（未解析）；缺键时用内置默认，仍缺则返回键名。 */
    public String raw(String key) {
        String value = config.getString(key);
        if (value == null && defaults != null) {
            value = defaults.getString(key);
        }
        if (value == null) {
            if (missingKeys.add(key)) {
                logger.warning(FILE_NAME + " 缺少键 " + key + "（内置默认文案里也没有）");
            }
            return key;
        }
        return value;
    }

    /** 解析成组件；占位符按“名字, 值”成对传入，值可以是 String/Number/Component。 */
    public Component component(String key, Object... placeholders) {
        String raw = raw(key);
        try {
            if (placeholders.length == 0) {
                return miniMessage.deserialize(raw);
            }
            return miniMessage.deserialize(raw, resolver(placeholders));
        } catch (RuntimeException e) {
            logger.warning("解析文案 " + key + " 失败：" + e.getMessage());
            return Component.text(raw);
        }
    }

    /** 带前缀的组件。 */
    public Component prefixed(String key, Object... placeholders) {
        return prefix.append(component(key, placeholders));
    }

    /** 给命令接收者发送带前缀的消息。 */
    public void send(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(prefixed(key, placeholders));
    }

    /** 纯文本形式（不需要颜色时用）。 */
    public String plain(String key, Object... placeholders) {
        return PlainTextComponentSerializer.plainText().serialize(component(key, placeholders));
    }

    private TagResolver resolver(Object... placeholders) {
        TagResolver.Builder builder = TagResolver.builder();
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String name = String.valueOf(placeholders[i]);
            Object value = placeholders[i + 1];
            Component replacement = value instanceof Component component
                    ? component
                    : Component.text(String.valueOf(value));
            builder.resolver(Placeholder.component(name, replacement));
        }
        return builder.build();
    }

    private YamlConfiguration loadDefaults() {
        InputStream stream = plugin.getResource(FILE_NAME);
        if (stream == null) {
            logger.warning("插件内没有内置的 " + FILE_NAME + "，缺键时将直接显示键名");
            return null;
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (RuntimeException | java.io.IOException e) {
            logger.warning("读取内置 " + FILE_NAME + " 失败：" + e.getMessage());
            return null;
        }
    }
}
