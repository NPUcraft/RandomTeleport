package com.npucraft.randomteleport.config;

import org.bukkit.World;

public record EconomySettings(
        boolean enabled,
        EconomyProviderKind provider,
        String currencyId,
        double costDefault,
        double costOverworld,
        double costNether,
        double costEnd,
        double backCostDefault,
        double backCostOverworld,
        double backCostNether,
        double backCostEnd
) {
    static EconomySettings from(org.bukkit.configuration.file.FileConfiguration c) {
        String p = "economy.";
        double def = c.getDouble(p + "cost", 0);
        double backDef = c.contains(p + "cost-back") ? c.getDouble(p + "cost-back") : def;
        EconomyProviderKind prov = EconomyProviderKind.parse(c.getString(p + "provider", "auto"));
        String rawId = c.getString(p + "currency-id", "coins");
        if (rawId == null || rawId.isBlank()) {
            rawId = "coins";
        } else {
            rawId = rawId.trim();
        }
        return new EconomySettings(
                c.getBoolean(p + "enabled", true),
                prov,
                rawId,
                def,
                c.contains(p + "cost-overworld") ? c.getDouble(p + "cost-overworld") : def,
                c.contains(p + "cost-nether") ? c.getDouble(p + "cost-nether") : def,
                c.contains(p + "cost-end") ? c.getDouble(p + "cost-end") : def,
                backDef,
                c.contains(p + "cost-back-overworld") ? c.getDouble(p + "cost-back-overworld") : backDef,
                c.contains(p + "cost-back-nether") ? c.getDouble(p + "cost-back-nether") : backDef,
                c.contains(p + "cost-back-end") ? c.getDouble(p + "cost-back-end") : backDef
        );
    }

    public double costFor(World world) {
        if (world == null) {
            return costDefault;
        }
        return switch (world.getEnvironment()) {
            case NORMAL -> costOverworld;
            case NETHER -> costNether;
            case THE_END -> costEnd;
            default -> costDefault;
        };
    }

    public double backCostFor(World world) {
        if (world == null) {
            return backCostDefault;
        }
        return switch (world.getEnvironment()) {
            case NORMAL -> backCostOverworld;
            case NETHER -> backCostNether;
            case THE_END -> backCostEnd;
            default -> backCostDefault;
        };
    }

    public boolean chargesBackInWorld(World world) {
        return enabled && backCostFor(world) > 0;
    }

    /** True when economy is on and RTP or back has a positive price somewhere (needs a working backend). */
    public boolean requiresEconomyCharge() {
        if (!enabled) {
            return false;
        }
        return costOverworld > 0 || costNether > 0 || costEnd > 0
                || backCostOverworld > 0 || backCostNether > 0 || backCostEnd > 0;
    }
}
