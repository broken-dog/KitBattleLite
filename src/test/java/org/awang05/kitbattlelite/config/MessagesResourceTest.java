package org.awang05.kitbattlelite.config;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验随包发布的 messages.yml：代码里引用的每个键都存在，且所有文案都能被 MiniMessage 解析。
 *
 * <p>这些检查不需要服务端，属于纯解析逻辑；真正的默认值兜底由 {@link Messages} 在运行时做。
 */
class MessagesResourceTest {
    /** 代码中引用的全部文案键，新增文案时必须同步这里。 */
    private static final Set<String> REQUIRED_KEYS = Set.of(
            "prefix",
            "command.no-permission",
            "command.player-only",
            "command.unknown",
            "command.usage",
            "command.reloaded",
            "command.saved",
            "command.open-in-arena",
            "command.leave-not-in-arena",
            "command.button-outside-arena",
            "command.import-usage",
            "command.import-success",
            "command.import-failed",
            "command.setworld-usage",
            "command.setworld-not-in-arena",
            "command.setworld-name",
            "command.setworld-spawn",
            "command.setworld-teleport",
            "command.setworld-enabled",
            "command.state-open",
            "command.state-closed",
            "command.edit-usage",
            "command.kit-usage",
            "command.kit-unknown",
            "command.kit-not-found",
            "command.kit-deleted",
            "command.kit-prune-none",
            "command.kit-prune-done",
            "arena.death-unknown",
            "arena.death-self",
            "arena.death-normal",
            "arena.death-killed",
            "arena.kill-victim-title",
            "arena.kill-killer-title",
            "sidebar.title",
            "sidebar.separator",
            "sidebar.streak",
            "sidebar.kills",
            "sidebar.deaths",
            "sidebar.kdr"
    );

    /** 文案里允许出现的占位符；用 unparsed 解析，确保名字写对。 */
    private static final Set<String> PLACEHOLDERS = Set.of(
            "arena", "name", "reason", "state", "id", "count", "ids", "victim", "killer", "value"
    );

    @Test
    void everyTextParsesAndRequiredKeysExist() throws Exception {
        YamlConfiguration config = loadBundledMessages();
        TagResolver resolver = placeholderResolver();
        MiniMessage miniMessage = MiniMessage.miniMessage();

        for (String key : config.getKeys(true)) {
            String value = config.getString(key);
            if (value == null) {
                continue;
            }
            assertDoesNotThrow(() -> miniMessage.deserialize(value, resolver), "文案解析失败: " + key);
        }
        for (String key : REQUIRED_KEYS) {
            assertNotNull(config.getString(key), "messages.yml 缺少键: " + key);
        }
    }

    @Test
    void bundledFileHasNoUnknownPlaceholder() throws Exception {
        YamlConfiguration config = loadBundledMessages();
        Set<String> used = new HashSet<>();
        for (String key : config.getKeys(true)) {
            String value = config.getString(key);
            if (value == null) {
                continue;
            }
            int index = 0;
            while ((index = value.indexOf('<', index)) >= 0) {
                if (index == 0 || value.charAt(index - 1) != '\\') {
                    int end = value.indexOf('>', index);
                    if (end < 0) {
                        break;
                    }
                    String token = value.substring(index + 1, end);
                    if (!token.startsWith("/") && !token.contains(":") && !token.contains(" ")) {
                        used.add(token);
                    }
                    index = end + 1;
                } else {
                    index++;
                }
            }
        }
        Set<String> unknown = new HashSet<>(used);
        unknown.removeAll(PLACEHOLDERS);
        unknown.removeAll(STYLE_TAGS);
        assertTrue(unknown.isEmpty(), "messages.yml 出现未知标签/占位符: " + unknown);
    }

    /** MiniMessage 自带的颜色/格式标签（只列出本项目实际用到的）。 */
    private static final Set<String> STYLE_TAGS = Set.of(
            "gray", "gold", "red", "green", "yellow", "white", "dark_red",
            "bold", "strikethrough", "reset"
    );

    private static YamlConfiguration loadBundledMessages() throws Exception {
        try (InputStream stream = MessagesResourceTest.class.getResourceAsStream("/messages.yml")) {
            assertNotNull(stream, "类路径里没有 messages.yml");
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        }
    }

    private static TagResolver placeholderResolver() {
        TagResolver.Builder builder = TagResolver.builder();
        for (String name : PLACEHOLDERS) {
            builder.resolver(Placeholder.unparsed(name, "x"));
        }
        return builder.build();
    }
}
