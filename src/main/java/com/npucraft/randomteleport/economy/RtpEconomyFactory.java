package com.npucraft.randomteleport.economy;

import com.npucraft.randomteleport.RandomTeleportPlugin;
import com.npucraft.randomteleport.config.EconomyProviderKind;
import com.npucraft.randomteleport.config.PluginSettings;

public final class RtpEconomyFactory {

    private RtpEconomyFactory() {
    }

    public static RtpEconomy create(RandomTeleportPlugin plugin, PluginSettings settings) {
        EconomyProviderKind kind = settings.economy().provider();
        return switch (kind) {
            case VAULT -> new VaultEconomy(plugin, settings);
            case COINS_ENGINE -> new CoinsEngineEconomy(plugin, settings);
            case AUTO -> pickAuto(plugin, settings);
        };
    }

    /** Prefer Vault when hooked; otherwise CoinsEngine (may be non-operational until configured). */
    private static RtpEconomy pickAuto(RandomTeleportPlugin plugin, PluginSettings settings) {
        if (VaultEconomy.isHooked(plugin)) {
            return new VaultEconomy(plugin, settings);
        }
        return new CoinsEngineEconomy(plugin, settings);
    }
}
