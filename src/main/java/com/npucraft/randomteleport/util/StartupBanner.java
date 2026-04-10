package com.npucraft.randomteleport.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Startup banner and version lines for the server console.
 * Banner text is loaded from {@code banner.txt} on the classpath (UTF-8).
 */
public final class StartupBanner {

    /** Matches e.g. {@code v1_21_R8} inside CraftBukkit package names. */
    private static final Pattern NMS_VERSION = Pattern.compile("v\\d+_\\d+_R\\d+");

    private StartupBanner() {
    }

    public static void print(JavaPlugin plugin) {
        Logger log = plugin.getLogger();
        String version = plugin.getDescription().getVersion();
        // Do not log "Enabling … v…" here — the server already prints that when the plugin enables.

        for (String line : loadBannerLines(plugin)) {
            log.info(line);
        }
        log.info("Initialized version " + version + "!");

        String impl = implementationPackage();
        if (!impl.isEmpty()) {
            log.info("Supported server version detected: " + impl);
        }
        String ver = Bukkit.getVersion();
        log.info("Runtime: Bukkit " + Bukkit.getBukkitVersion() + (ver != null && !ver.isBlank() ? " — " + ver.trim() : ""));
    }

    private static List<String> loadBannerLines(JavaPlugin plugin) {
        try (InputStream in = plugin.getResource("banner.txt")) {
            if (in == null) {
                return List.of();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.toCollection(ArrayList::new));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load banner.txt: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * NMS-style segment when present (e.g. {@code v1_21_R8}); empty on unversioned {@code org.bukkit.craftbukkit}.
     */
    private static String implementationPackage() {
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            if (!pkg.contains("craftbukkit")) {
                return "";
            }
            Matcher m = NMS_VERSION.matcher(pkg);
            if (m.find()) {
                return m.group();
            }
            int i = pkg.lastIndexOf('.');
            String last = i >= 0 ? pkg.substring(i + 1) : "";
            if ("craftbukkit".equals(last)) {
                return "";
            }
            return last;
        } catch (Throwable ignored) {
            return "";
        }
    }
}
