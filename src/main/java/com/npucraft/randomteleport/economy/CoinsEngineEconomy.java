package com.npucraft.randomteleport.economy;

import com.npucraft.randomteleport.RandomTeleportPlugin;
import com.npucraft.randomteleport.config.PluginSettings;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

public final class CoinsEngineEconomy implements RtpEconomy {

    private final RandomTeleportPlugin plugin;
    private final PluginSettings settings;

    public CoinsEngineEconomy(RandomTeleportPlugin plugin, PluginSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    /** True when CoinsEngine is enabled and {@code currency-id} resolves. Used for {@code economy.provider: auto} ordering. */
    public static boolean isReady(RandomTeleportPlugin plugin, PluginSettings settings) {
        Plugin p = plugin.getServer().getPluginManager().getPlugin("CoinsEngine");
        if (p == null || !p.isEnabled()) {
            return false;
        }
        Currency c = CoinsEngineAPI.getCurrency(settings.economy().currencyId());
        return c != null;
    }

    @Override
    public boolean chargesInWorld(World world) {
        return settings.economy().enabled() && settings.economy().costFor(world) > 0;
    }

    @Override
    public double costFor(World world) {
        return settings.economy().costFor(world);
    }

    public boolean isEnginePresent() {
        Plugin p = plugin.getServer().getPluginManager().getPlugin("CoinsEngine");
        return p != null && p.isEnabled();
    }

    public Currency currency() {
        if (!isEnginePresent()) {
            return null;
        }
        return CoinsEngineAPI.getCurrency(settings.economy().currencyId());
    }

    @Override
    public double balance(Player player) {
        Currency c = currency();
        if (c == null) {
            return 0;
        }
        return CoinsEngineAPI.getBalance(player, c);
    }

    /**
     * Dry-run: same rules as {@link #executeRtpPayment} but does not remove balance.
     */
    @Override
    public RtpPaymentResult precheckRtpPayment(Player player, World world, boolean bypassCost) {
        return runRtpPayment(player, world, bypassCost, false);
    }

    /**
     * Performs withdrawal when applicable. Call after {@link #precheckRtpPayment} passed (may still fail if balance changed).
     */
    @Override
    public RtpPaymentResult executeRtpPayment(Player player, World world, boolean bypassCost) {
        return runRtpPayment(player, world, bypassCost, true);
    }

    private RtpPaymentResult runRtpPayment(Player player, World world, boolean bypassCost, boolean takeMoney) {
        if (world == null) {
            return RtpPaymentResult.FREE;
        }
        if (!chargesInWorld(world) || bypassCost) {
            return RtpPaymentResult.FREE;
        }
        Currency c = currency();
        if (c == null) {
            return RtpPaymentResult.NO_CURRENCY;
        }
        double need = settings.economy().costFor(world);
        return takeCoins(player, takeMoney, c, need);
    }

    @Override
    public boolean chargesBackInWorld(World world) {
        return settings.economy().chargesBackInWorld(world);
    }

    @Override
    public double backCostFor(World world) {
        return settings.economy().backCostFor(world);
    }

    @Override
    public RtpPaymentResult precheckRtpBackPayment(Player player, World world, boolean bypassCost) {
        return runBackPayment(player, world, bypassCost, false);
    }

    @Override
    public RtpPaymentResult executeRtpBackPayment(Player player, World world, boolean bypassCost) {
        return runBackPayment(player, world, bypassCost, true);
    }

    private RtpPaymentResult runBackPayment(Player player, World world, boolean bypassCost, boolean takeMoney) {
        if (world == null) {
            return RtpPaymentResult.FREE;
        }
        if (!chargesBackInWorld(world) || bypassCost) {
            return RtpPaymentResult.FREE;
        }
        Currency c = currency();
        if (c == null) {
            return RtpPaymentResult.NO_CURRENCY;
        }
        double need = settings.economy().backCostFor(world);
        return takeCoins(player, takeMoney, c, need);
    }

    private RtpPaymentResult takeCoins(Player player, boolean takeMoney, Currency c, double need) {
        double bal = CoinsEngineAPI.getBalance(player, c);
        if (bal < need) {
            return RtpPaymentResult.INSUFFICIENT_FUNDS;
        }
        if (!takeMoney) {
            return RtpPaymentResult.CHARGED;
        }
        CoinsEngineAPI.removeBalance(player, c, need);
        double after = CoinsEngineAPI.getBalance(player, c);
        double expected = bal - need;
        if (Math.abs(after - expected) <= 1e-4) {
            return RtpPaymentResult.CHARGED;
        }
        double restore = bal - after;
        if (restore > 1e-6) {
            CoinsEngineAPI.addBalance(player, c, restore);
            plugin.getLogger().warning("CoinsEngine balance inconsistent after withdraw for "
                    + player.getName() + " (before=" + bal + " need=" + need + " after=" + after
                    + ", expected=" + expected + "); restored to pre-withdraw, RTP treated as failed.");
            return RtpPaymentResult.INSUFFICIENT_FUNDS;
        }
        if (restore < -1e-6) {
            plugin.getLogger().severe("CoinsEngine post-withdraw balance higher than pre-balance for "
                    + player.getName() + " (before=" + bal + " need=" + need + " after=" + after
                    + "); RTP aborted — check economy integration.");
        } else {
            plugin.getLogger().warning("CoinsEngine balance inconsistent after withdraw for "
                    + player.getName() + " (before=" + bal + " need=" + need + " after=" + after
                    + ", expected=" + expected + "); RTP treated as failed (no restore applied).");
        }
        return RtpPaymentResult.INSUFFICIENT_FUNDS;
    }

    /**
     * Restores balance after a charge when teleport was cancelled or failed (e.g. PlayerTeleportEvent).
     */
    @Override
    public void refundRtp(Player player, double amount) {
        if (amount <= 0) {
            return;
        }
        Currency c = currency();
        if (c == null) {
            plugin.getLogger().warning("Cannot refund RTP: CoinsEngine currency missing for " + player.getName());
            return;
        }
        CoinsEngineAPI.addBalance(player, c, amount);
    }

    @Override
    public boolean isBackendOperational() {
        return isReady(plugin, settings);
    }

    @Override
    public String backendLabel() {
        return "CoinsEngine";
    }
}
