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
        assertTrue(settings.mushroomStewEnabled());
        assertEquals(6.0, settings.mushroomStewHealAmount());
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
                "  enabled: false",
                "mushroom-stew:",
                "  enabled: false",
                "  heal-amount: 4.5"));

        PluginSettings settings = PluginSettings.load(config);

        assertEquals(5, settings.protectionSeconds());
        assertEquals(60, settings.deathBlindnessTicks());
        assertFalse(settings.sidebarEnabled());
        assertFalse(settings.healthBelowNameEnabled());
        assertFalse(settings.mushroomStewEnabled());
        assertEquals(4.5, settings.mushroomStewHealAmount());
    }

    @Test
    void negativeDurationsBecomeZero() throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(String.join("\n",
                "protection-seconds: -5",
                "death-blindness-ticks: -1",
                "mushroom-stew:",
                "  heal-amount: -2"));

        PluginSettings settings = PluginSettings.load(config);

        assertEquals(0, settings.protectionSeconds());
        assertEquals(0, settings.deathBlindnessTicks());
        assertEquals(0.0, settings.mushroomStewHealAmount());
    }
}
