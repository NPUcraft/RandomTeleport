package com.npucraft.randomteleport.economy;

/**
 * Outcome of {@link RtpEconomy} payment checks: RTP ({@code precheckRtpPayment} / {@code executeRtpPayment})
 * and {@code /rtp back} ({@code precheckRtpBackPayment} / {@code executeRtpBackPayment}).
 */
public enum RtpPaymentResult {
    /** No charge (disabled, zero price, or bypass). */
    FREE,
    /** Would charge / charged successfully. */
    CHARGED,
    /** No economy hook (Vault without provider, or CoinsEngine/currency missing). */
    NO_CURRENCY,
    /** Balance too low (only when actually charging). */
    INSUFFICIENT_FUNDS
}
