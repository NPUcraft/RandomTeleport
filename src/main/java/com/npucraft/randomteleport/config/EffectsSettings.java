package com.npucraft.randomteleport.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record EffectsSettings(
        boolean masterEnabled,
        boolean repeatCountdownTitle,
        EffectEntry searching,
        EffectEntry countdown,
        EffectEntry success,
        EffectEntry failed,
        EffectEntry delayCancel,
        EffectEntry reload
) {
    static EffectsSettings from(FileConfiguration c) {
        String p = "effects.";
        ConfigurationSection root = c.getConfigurationSection("effects");
        if (root == null) {
            return defaults();
        }
        return new EffectsSettings(
                c.getBoolean(p + "enabled", true),
                c.getBoolean(p + "repeat-countdown-title", true),
                EffectEntry.from(c.getConfigurationSection(p + "searching")),
                EffectEntry.from(c.getConfigurationSection(p + "countdown")),
                EffectEntry.from(c.getConfigurationSection(p + "success")),
                EffectEntry.from(c.getConfigurationSection(p + "failed")),
                EffectEntry.from(c.getConfigurationSection(p + "delay-cancel")),
                EffectEntry.from(c.getConfigurationSection(p + "reload"))
        );
    }

    private static EffectsSettings defaults() {
        return new EffectsSettings(true, true,
                EffectEntry.disabled(), EffectEntry.disabled(), EffectEntry.disabled(),
                EffectEntry.disabled(), EffectEntry.disabled(), EffectEntry.disabled());
    }
}
