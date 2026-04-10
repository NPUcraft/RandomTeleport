package com.npucraft.randomteleport.config;

import java.util.Locale;

/**
 * Which economy integration handles RTP charges.
 */
public enum EconomyProviderKind {
    /** Prefer Vault when a Vault {@code Economy} service is registered; otherwise CoinsEngine. */
    AUTO,
    /** Vault + any plugin that registers Vault's Economy (EssentialsX, CMI, etc.). Ignores {@code currency-id}. */
    VAULT,
    /** NightExpress CoinsEngine; uses {@code currency-id}. */
    COINS_ENGINE;

    public static EconomyProviderKind parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return AUTO;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "vault" -> VAULT;
            case "coinsengine", "coins_engine", "coins-engine" -> COINS_ENGINE;
            default -> AUTO;
        };
    }
}
