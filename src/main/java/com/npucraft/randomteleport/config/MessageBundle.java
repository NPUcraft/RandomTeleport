package com.npucraft.randomteleport.config;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

public record MessageBundle(
        String prefix,
        String noPermission,
        String playersOnly,
        String economyNotReady,
        String notEnoughMoney,
        String cooldown,
        String searching,
        String delayActionbar,
        String delayCostFree,
        String delayCostWillCharge,
        String failed,
        String success,
        String successPaid,
        String worldDenied,
        String noValidRange,
        String reloadSuccess,
        String reloadDenied,
        String reloadFailed,
        String reloadRtpCancelled,
        String backNone,
        String backSuccess,
        String backSuccessPaid,
        String delayCanceled,
        String unknownSubcommand,
        String rtpPending,
        String helpHeader,
        String helpLineRange,
        String helpLineNoRange,
        String helpFooter,
        String helpDimOverworld,
        String helpDimNether,
        String helpDimEnd,
        String helpNoPermission,
        String helpNoWorlds
) {
    public static MessageBundle from(FileConfiguration c, String basePath) {
        String p = basePath + ".";
        boolean zh = basePath.endsWith(".zh");
        String helpNoFallback = zh
                ? "&7当前配置下没有可用于随机传送的世界。"
                : "&7No worlds allow RTP with the current config.";
        String reloadFailedDef = zh
                ? "&c重载失败，请查看控制台。"
                : "&cReload failed. See console for details.";

        String helpNoRaw = c.isSet(p + "help-no-worlds")
                ? Objects.requireNonNullElse(c.getString(p + "help-no-worlds"), "")
                : helpNoFallback;
        String helpNoWorlds = helpNoRaw.isBlank() ? helpNoFallback : helpNoRaw;

        String successPaid = c.getString(p + "success-paid", " &7· &f-&e{paid}");
        return new MessageBundle(
                c.getString(p + "prefix", ""),
                c.getString(p + "no-permission", ""),
                c.getString(p + "players-only", ""),
                c.getString(p + "economy-not-ready", ""),
                c.getString(p + "not-enough-money", ""),
                c.getString(p + "cooldown", ""),
                c.getString(p + "searching", ""),
                c.getString(p + "delay-actionbar", "&e{seconds}&7s &8│ {cost_detail}"),
                c.getString(p + "delay-cost-free", "&7Free"),
                c.getString(p + "delay-cost-will-charge", "&f-&e{cost}"),
                c.getString(p + "failed", ""),
                c.getString(p + "success", ""),
                successPaid,
                c.getString(p + "world-denied", ""),
                c.getString(p + "no-valid-range", ""),
                c.getString(p + "reload-success", ""),
                c.getString(p + "reload-denied", ""),
                c.getString(p + "reload-failed", reloadFailedDef),
                c.getString(p + "reload-rtp-cancelled",
                        "&7RTP countdown cancelled: configuration was reloaded."),
                c.getString(p + "back-none", ""),
                c.getString(p + "back-success", ""),
                c.getString(p + "back-success-paid", successPaid),
                c.getString(p + "delay-canceled", ""),
                c.getString(p + "unknown-subcommand", ""),
                c.getString(p + "rtp-pending", ""),
                c.getString(p + "help-header", ""),
                c.getString(p + "help-line-range", ""),
                c.getString(p + "help-line-no-range", ""),
                c.getString(p + "help-footer", ""),
                c.getString(p + "help-dim-overworld", "Overworld"),
                c.getString(p + "help-dim-nether", "Nether"),
                c.getString(p + "help-dim-end", "The End"),
                c.getString(p + "help-no-permission", ""),
                helpNoWorlds
        );
    }

    public String helpDimension(World.Environment env) {
        return switch (env) {
            case NORMAL -> helpDimOverworld;
            case NETHER -> helpDimNether;
            case THE_END -> helpDimEnd;
            default -> helpDimOverworld;
        };
    }
}
