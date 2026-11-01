package org.awang05.kitbattlelite.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginSettingsTest {

    @Test
    void missingKeysFallBackToDefaults() {
        PluginSettings settings = PluginSettings.load(new YamlConfiguration());

        assertEquals(3, settings.protectionSeconds());
        assertEquals(40, settings.deathBlindnessTicks());
        assertTrue(settings.sidebarEnabled());
        assertTrue(settings.healthBelowNameEnabled());
    }

    @Test
    void readsConfiguredValues() throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(String.join("\n",
                "protection-seconds: 5",
                "death-blindness-ticks: 60",
                "sidebar:",
                "  enabled: false",
                "health-below-name:",
                "  enabled: false"));

        PluginSettings settings = PluginSettings.load(config);

        assertEquals(5, settings.protectionSeconds());
        assertEquals(60, settings.deathBlindnessTicks());
        assertFalse(settings.sidebarEnabled());
        assertFalse(settings.healthBelowNameEnabled());
    }

    @Test
    void negativeDurationsBecomeZero() throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(String.join("\n",
                "protection-seconds: -5",
                "death-blindness-ticks: -1"));

        PluginSettings settings = PluginSettings.load(config);

        assertEquals(0, settings.protectionSeconds());
        assertEquals(0, settings.deathBlindnessTicks());
    }
}
