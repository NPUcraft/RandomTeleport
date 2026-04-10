package com.npucraft.randomteleport.config;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

public record EffectEntry(
        boolean enabled,
        Sound sound,
        float volume,
        float pitch,
        String title,
        String subtitle,
        int fadeIn,
        int stay,
        int fadeOut
) {
    static EffectEntry from(ConfigurationSection sec) {
        if (sec == null) {
            return disabled();
        }
        Sound s = null;
        String sn = sec.getString("sound", "");
        if (sn != null && !sn.isEmpty() && !sn.equalsIgnoreCase("none")) {
            try {
                s = Sound.valueOf(sn.trim().toUpperCase().replace('-', '_'));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return new EffectEntry(
                sec.getBoolean("enabled", true),
                s,
                (float) sec.getDouble("volume", 1.0),
                (float) sec.getDouble("pitch", 1.0),
                sec.getString("title", ""),
                sec.getString("subtitle", ""),
                sec.getInt("fade-in", 5),
                sec.getInt("stay", 25),
                sec.getInt("fade-out", 10)
        );
    }

    static EffectEntry disabled() {
        return new EffectEntry(false, null, 1f, 1f, "", "", 5, 25, 10);
    }
}
