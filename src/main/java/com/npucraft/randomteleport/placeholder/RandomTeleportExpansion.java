package com.npucraft.randomteleport.placeholder;

import com.npucraft.randomteleport.RandomTeleportPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class RandomTeleportExpansion extends PlaceholderExpansion {

    private final RandomTeleportPlugin plugin;

    public RandomTeleportExpansion(RandomTeleportPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "randomteleport";
    }

    @Override
    public String getAuthor() {
        return "NPUCraft";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null || !plugin.isEnabled()) {
            return "";
        }
        var s = plugin.settings();
        var econ = plugin.economy();
        if (s == null || econ == null) {
            return "";
        }
        return switch (params.toLowerCase(Locale.ROOT)) {
            case "cooldown", "cooldown_seconds" -> {
                long left = plugin.cooldowns().remainingSeconds(
                        player.getUniqueId(),
                        s.cooldownSeconds(),
                        System.currentTimeMillis()
                );
                yield String.valueOf(left);
            }
            case "cost" -> {
                if (!s.economy().enabled()) {
                    yield "0";
                }
                var w = player.getWorld();
                yield w == null ? "0" : String.valueOf(econ.costFor(w));
            }
            case "cost_back" -> {
                if (!s.economy().enabled()) {
                    yield "0";
                }
                var w = player.getWorld();
                yield w == null ? "0" : String.valueOf(econ.backCostFor(w));
            }
            case "pending" -> plugin.pendingRtp().isPending(player.getUniqueId()) ? "true" : "false";
            case "has_back" -> plugin.lastLocations().has(player.getUniqueId()) ? "true" : "false";
            default -> null;
        };
    }
}
