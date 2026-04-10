package com.npucraft.randomteleport.config;

import org.bukkit.configuration.file.FileConfiguration;

public record SafetySettings(
        boolean avoidWater,
        boolean avoidWebs,
        boolean avoidCaveVines,
        boolean avoidDripstone
) {
    static SafetySettings from(FileConfiguration c) {
        String p = "safety.";
        return new SafetySettings(
                c.getBoolean(p + "avoid-water", true),
                c.getBoolean(p + "avoid-webs", true),
                c.getBoolean(p + "avoid-cave-vines", true),
                c.getBoolean(p + "avoid-dripstone", true)
        );
    }
}
