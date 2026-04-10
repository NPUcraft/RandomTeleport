package com.npucraft.randomteleport.config;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record WorldsSettings(
        Mode mode,
        Set<String> worldNamesLowercase
) {
    public enum Mode {
        ALL,
        WHITELIST,
        BLACKLIST
    }

    static WorldsSettings from(FileConfiguration c) {
        String p = "worlds.";
        Mode m = Mode.ALL;
        String raw = c.getString(p + "mode", "all");
        if (raw != null) {
            switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "whitelist", "allow" -> m = Mode.WHITELIST;
                case "blacklist", "deny" -> m = Mode.BLACKLIST;
                default -> m = Mode.ALL;
            }
        }
        List<String> list = c.getStringList(p + "list");
        Set<String> set = new HashSet<>();
        for (String name : list) {
            if (name != null && !name.isBlank()) {
                set.add(name.trim().toLowerCase(Locale.ROOT));
            }
        }
        return new WorldsSettings(m, set);
    }

    public boolean allows(World world) {
        if (world == null) {
            return false;
        }
        String name = world.getName().toLowerCase(Locale.ROOT);
        return switch (mode) {
            case ALL -> true;
            case WHITELIST -> worldNamesLowercase.contains(name);
            case BLACKLIST -> !worldNamesLowercase.contains(name);
        };
    }

    public boolean whitelistButEmpty() {
        return mode == Mode.WHITELIST && worldNamesLowercase.isEmpty();
    }
}
