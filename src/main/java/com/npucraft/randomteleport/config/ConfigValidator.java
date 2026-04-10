package com.npucraft.randomteleport.config;

import com.npucraft.randomteleport.RandomTeleportPlugin;
import org.bukkit.World;

import java.util.logging.Logger;

public final class ConfigValidator {

    private ConfigValidator() {
    }

    public static void logIssues(RandomTeleportPlugin plugin, PluginSettings settings) {
        Logger log = plugin.getLogger();
        EconomySettings econ = settings.economy();

        if (econ.requiresEconomyCharge() && !plugin.economy().isBackendOperational()) {
            log.warning("economy is enabled with a positive RTP and/or back cost but backend '"
                    + plugin.economy().backendLabel() + "' is not ready (provider=" + econ.provider()
                    + "). For Vault install Vault + an economy plugin; for CoinsEngine install CoinsEngine and set currency-id.");
        }

        if (settings.worlds().whitelistButEmpty()) {
            log.severe("worlds.mode is whitelist but worlds.list is empty — random teleport is impossible in every world.");
        }

        for (World world : plugin.getServer().getWorlds()) {
            if (!settings.worlds().allows(world)) {
                continue;
            }
            if (settings.effectiveRtpRange(world).isEmpty()) {
                log.warning("World '" + world.getName()
                        + "': configured RTP range does not overlap the world border (or border margin leaves no area). /rtp will fail there.");
            }
        }

        double minDist = settings.minBlocksFromOtherPlayers();
        if (minDist > 0) {
            PluginSettings.AxisRange ow = settings.overworld();
            int span = Math.min(ow.maxX() - ow.minX(), ow.maxZ() - ow.minZ());
            if (span < minDist * 4) {
                log.warning("min-blocks-from-other-players (" + minDist
                        + ") is large relative to overworld X/Z span; finding a spot may often fail.");
            }
        }
    }
}
