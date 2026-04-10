package com.npucraft.randomteleport.economy;

import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * RTP pricing: Vault-backed or CoinsEngine-backed.
 */
public interface RtpEconomy {

    boolean chargesInWorld(World world);

    double costFor(World world);

    double balance(Player player);

    RtpPaymentResult precheckRtpPayment(Player player, World world, boolean bypassCost);

    RtpPaymentResult executeRtpPayment(Player player, World world, boolean bypassCost);

    boolean chargesBackInWorld(World world);

    double backCostFor(World world);

    RtpPaymentResult precheckRtpBackPayment(Player player, World world, boolean bypassCost);

    RtpPaymentResult executeRtpBackPayment(Player player, World world, boolean bypassCost);

    void refundRtp(Player player, double amount);

    /** When {@link com.npucraft.randomteleport.config.EconomySettings#requiresEconomyCharge()} is true, this should be true for charges to work. */
    boolean isBackendOperational();

    /** For logs and PlaceholderAPI context (e.g. {@code Vault}, {@code CoinsEngine}). */
    String backendLabel();
}
