package com.npucraft.randomteleport.economy;

import com.npucraft.randomteleport.RandomTeleportPlugin;
import com.npucraft.randomteleport.config.PluginSettings;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultEconomy implements RtpEconomy {

    private final RandomTeleportPlugin plugin;
    private final PluginSettings settings;

    public VaultEconomy(RandomTeleportPlugin plugin, PluginSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public static boolean isHooked(RandomTeleportPlugin plugin) {
        Economy e = resolve(plugin);
        return e != null;
    }

    private static Economy resolve(RandomTeleportPlugin plugin) {
        RegisteredServiceProvider<Economy> reg =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (reg == null) {
            return null;
        }
        Economy e = reg.getProvider();
        return e != null && e.isEnabled() ? e : null;
    }

    private Economy hook() {
        return resolve(plugin);
    }

    @Override
    public boolean chargesInWorld(World world) {
        return settings.economy().enabled() && settings.economy().costFor(world) > 0;
    }

    @Override
    public double costFor(World world) {
        return settings.economy().costFor(world);
    }

    @Override
    public double balance(Player player) {
        Economy e = hook();
        if (e == null) {
            return 0;
        }
        return e.getBalance(player);
    }

    @Override
    public RtpPaymentResult precheckRtpPayment(Player player, World world, boolean bypassCost) {
        return runRtpPayment(player, world, bypassCost, false);
    }

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
        Economy e = hook();
        if (e == null) {
            return RtpPaymentResult.NO_CURRENCY;
        }
        double need = settings.economy().costFor(world);
        return withdrawIfPossible(player, takeMoney, e, need);
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
        Economy e = hook();
        if (e == null) {
            return RtpPaymentResult.NO_CURRENCY;
        }
        double need = settings.economy().backCostFor(world);
        return withdrawIfPossible(player, takeMoney, e, need);
    }

    private static RtpPaymentResult withdrawIfPossible(Player player, boolean takeMoney, Economy e, double need) {
        if (!e.has(player, need)) {
            return RtpPaymentResult.INSUFFICIENT_FUNDS;
        }
        if (takeMoney) {
            EconomyResponse resp = e.withdrawPlayer(player, need);
            if (resp.type != EconomyResponse.ResponseType.SUCCESS) {
                return RtpPaymentResult.INSUFFICIENT_FUNDS;
            }
        }
        return RtpPaymentResult.CHARGED;
    }

    @Override
    public void refundRtp(Player player, double amount) {
        if (amount <= 0) {
            return;
        }
        Economy e = hook();
        if (e == null) {
            plugin.getLogger().warning("Cannot refund RTP: Vault Economy hook missing for " + player.getName());
            return;
        }
        EconomyResponse r = e.depositPlayer(player, amount);
        if (r == null || r.type != EconomyResponse.ResponseType.SUCCESS) {
            String detail = r == null ? "null response" : r.errorMessage;
            plugin.getLogger().warning("Vault RTP refund failed for " + player.getName() + ": " + detail);
        }
    }

    @Override
    public boolean isBackendOperational() {
        return hook() != null;
    }

    @Override
    public String backendLabel() {
        return "Vault";
    }
}
