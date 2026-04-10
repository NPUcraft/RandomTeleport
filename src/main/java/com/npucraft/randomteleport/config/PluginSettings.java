package com.npucraft.randomteleport.config;

import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;

public record PluginSettings(
        boolean debug,
        int cooldownSeconds,
        int maxLocationAttempts,
        /**
         * Max synchronous chunk loads per {@code /rtp} search; {@code 0} = no cap (legacy behaviour, can lag).
         * When unset in config, defaults to {@code min(32, max-location-attempts)}.
         */
        int maxSyncChunkLoadsPerRtp,
        int teleportDelayTicks,
        double delayCancelMoveBlocks,
        double minBlocksFromOtherPlayers,
        boolean respectWorldBorder,
        double worldBorderMargin,
        EconomySettings economy,
        SafetySettings safety,
        EffectsSettings effects,
        WorldsSettings worlds,
        AxisRange overworld,
        AxisRange nether,
        AxisRange theEnd,
        LanguageMode languageMode,
        LanguageMode fallbackLanguage,
        MessageBundle messagesZh,
        MessageBundle messagesEn
) {
    public static PluginSettings from(FileConfiguration c) {
        LanguageMode mode = LanguageMode.fromString(c.getString("language", "auto"));
        LanguageMode fallback = LanguageMode.fromString(c.getString("fallback-language", "en"));
        if (fallback == LanguageMode.AUTO) {
            fallback = LanguageMode.EN;
        }
        int maxLocAttempts = Math.max(8, c.getInt("max-location-attempts", 48));
        int defaultChunkCap = Math.min(32, maxLocAttempts);
        int maxSyncChunkLoads = c.isSet("max-sync-chunk-loads-per-rtp")
                ? Math.max(0, c.getInt("max-sync-chunk-loads-per-rtp"))
                : defaultChunkCap;
        return new PluginSettings(
                c.getBoolean("debug", false),
                c.getInt("cooldown-seconds", 120),
                maxLocAttempts,
                maxSyncChunkLoads,
                Math.max(0, c.getInt("teleport-delay-ticks", 0)),
                Math.max(0, c.getDouble("delay-cancel-move-blocks", 0.35)),
                Math.max(0, c.getDouble("min-blocks-from-other-players", 0)),
                c.getBoolean("respect-world-border", true),
                Math.max(0, c.getDouble("world-border-margin", 2.0)),
                EconomySettings.from(c),
                SafetySettings.from(c),
                EffectsSettings.from(c),
                WorldsSettings.from(c),
                AxisRange.fromSection(c, "ranges.overworld"),
                AxisRange.fromSection(c, "ranges.nether"),
                AxisRange.fromSection(c, "ranges.the_end"),
                mode,
                fallback,
                MessageBundle.from(c, "messages.zh"),
                MessageBundle.from(c, "messages.en")
        );
    }

    public MessageBundle messagesFor(Player player) {
        return switch (languageMode) {
            case ZH -> messagesZh;
            case EN -> messagesEn;
            case AUTO -> player.getLocale().toLowerCase(Locale.ROOT).startsWith("zh")
                    ? messagesZh
                    : messagesEn;
        };
    }

    public MessageBundle messagesFor(CommandSender sender) {
        if (sender instanceof Player p) {
            return messagesFor(p);
        }
        return fallbackLanguage == LanguageMode.ZH ? messagesZh : messagesEn;
    }

    public AxisRange rangeFor(org.bukkit.World.Environment env) {
        return switch (env) {
            case NORMAL -> overworld;
            case NETHER -> nether;
            case THE_END -> theEnd;
            default -> overworld;
        };
    }

    /**
     * Config range intersected with world border (when enabled). Empty if no overlap.
     */
    public Optional<AxisRange> effectiveRtpRange(World world) {
        AxisRange base = rangeFor(world.getEnvironment());
        if (!respectWorldBorder) {
            return Optional.of(base);
        }
        AxisRange clipped = AxisRange.intersectWorldBorder(world, base, worldBorderMargin);
        return Optional.ofNullable(clipped);
    }

    public record AxisRange(int minX, int maxX, int minZ, int maxZ) {
        static AxisRange fromSection(FileConfiguration c, String path) {
            int minX = c.getInt(path + ".min-x", -5000);
            int maxX = c.getInt(path + ".max-x", 5000);
            int minZ = c.getInt(path + ".min-z", -5000);
            int maxZ = c.getInt(path + ".max-z", 5000);
            if (minX > maxX) {
                int t = minX;
                minX = maxX;
                maxX = t;
            }
            if (minZ > maxZ) {
                int t = minZ;
                minZ = maxZ;
                maxZ = t;
            }
            return new AxisRange(minX, maxX, minZ, maxZ);
        }

        /**
         * @return null if config range does not overlap border (inclusive rectangle approximation).
         */
        static AxisRange intersectWorldBorder(World world, AxisRange cfg, double marginBlocks) {
            WorldBorder b = world.getWorldBorder();
            double half = b.getSize() / 2.0 - marginBlocks;
            if (half < 1) {
                half = 1;
            }
            double cx = b.getCenter().getX();
            double cz = b.getCenter().getZ();
            int bMinX = (int) Math.floor(cx - half);
            int bMaxX = (int) Math.floor(cx + half);
            int bMinZ = (int) Math.floor(cz - half);
            int bMaxZ = (int) Math.floor(cz + half);
            int nx = Math.max(cfg.minX, bMinX);
            int xx = Math.min(cfg.maxX, bMaxX);
            int nz = Math.max(cfg.minZ, bMinZ);
            int xz = Math.min(cfg.maxZ, bMaxZ);
            if (nx > xx || nz > xz) {
                return null;
            }
            return new AxisRange(nx, xx, nz, xz);
        }
    }
}
