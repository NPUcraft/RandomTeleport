package com.npucraft.randomteleport;

import com.npucraft.randomteleport.command.RtpCommand;
import com.npucraft.randomteleport.config.ConfigValidator;
import com.npucraft.randomteleport.config.EconomyProviderKind;
import com.npucraft.randomteleport.config.MessageBundle;
import com.npucraft.randomteleport.config.PluginSettings;
import com.npucraft.randomteleport.economy.RtpEconomy;
import com.npucraft.randomteleport.economy.RtpEconomyFactory;
import com.npucraft.randomteleport.economy.RtpPaymentResult;
import com.npucraft.randomteleport.feedback.PlayerFeedback;
import com.npucraft.randomteleport.listener.RtpListener;
import com.npucraft.randomteleport.placeholder.RandomTeleportExpansion;
import com.npucraft.randomteleport.teleport.Cooldowns;
import com.npucraft.randomteleport.teleport.LastLocationStore;
import com.npucraft.randomteleport.teleport.PendingRtpService;
import com.npucraft.randomteleport.teleport.SafeLocationFinder;
import com.npucraft.randomteleport.util.StartupBanner;
import com.npucraft.randomteleport.util.Text;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class RandomTeleportPlugin extends JavaPlugin {

    private PluginSettings settings;
    private final Cooldowns cooldowns = new Cooldowns();
    private RtpEconomy economy;
    private SafeLocationFinder locationFinder;
    private final PendingRtpService pendingRtp = new PendingRtpService(this);
    private final LastLocationStore lastLocations = new LastLocationStore();
    private RandomTeleportExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        StartupBanner.print(this);
        saveDefaultConfig();
        reloadEverything();

        // Economy plugins often register Vault's Economy after our onEnable; refresh AUTO on the next tick.
        getServer().getScheduler().runTask(this, () -> {
            if (!isEnabled() || settings == null) {
                return;
            }
            if (settings.economy().provider() == EconomyProviderKind.AUTO) {
                this.economy = RtpEconomyFactory.create(this, settings);
            }
        });

        var cmd = getCommand("rtp");
        if (cmd == null) {
            getLogger().severe("Command 'rtp' missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        var executor = new RtpCommand(this);
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);

        getServer().getPluginManager().registerEvents(new RtpListener(this), this);
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPluginEnable(PluginEnableEvent event) {
                if (!"PlaceholderAPI".equals(event.getPlugin().getName())) {
                    return;
                }
                getServer().getScheduler().runTask(RandomTeleportPlugin.this, RandomTeleportPlugin.this::tryRegisterPlaceholderApi);
            }
        }, this);

        getServer().getScheduler().runTaskLater(this, this::tryRegisterPlaceholderApi, 1L);
    }

    void tryRegisterPlaceholderApi() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null
                || !getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        if (placeholderExpansion != null) {
            return;
        }
        placeholderExpansion = new RandomTeleportExpansion(this);
        if (!placeholderExpansion.register()) {
            getLogger().warning("PlaceholderAPI expansion failed to register.");
            placeholderExpansion = null;
        } else {
            getLogger().info("PlaceholderAPI expansion registered.");
        }
    }

    @Override
    public void onDisable() {
        int n = pendingRtp.pendingCount();
        pendingRtp.cancelAll();
        if (n > 0) {
            getLogger().info("Disabled: cleared " + n + " delayed RTP countdown(s).");
        }
        if (placeholderExpansion != null) {
            try {
                placeholderExpansion.unregister();
            } catch (Throwable ignored) {
            }
            placeholderExpansion = null;
        }
    }

    public void reloadEverything() {
        reloadConfig();
        this.settings = PluginSettings.from(getConfig());
        int reloadCancelled = pendingRtp.cancelAllNotifyingReload();
        if (reloadCancelled > 0) {
            getLogger().info("Config reload: cancelled " + reloadCancelled + " delayed RTP countdown(s) and notified players.");
        }
        this.economy = RtpEconomyFactory.create(this, settings);
        this.locationFinder = new SafeLocationFinder(this, settings);
        ConfigValidator.logIssues(this, settings);
        logRuntimeSummary();
    }

    /** One-shot console summary after config load (enable / reload). */
    private void logRuntimeSummary() {
        PluginSettings st = settings;
        RtpEconomy ec = economy;
        if (st == null || ec == null) {
            getLogger().warning("RandomTeleport: settings or economy not initialized after reload.");
            return;
        }
        var es = st.economy();
        getLogger().info("Economy: enabled=" + es.enabled() + ", provider=" + es.provider()
                + ", backend=" + ec.backendLabel() + ", operational=" + ec.isBackendOperational());
        getLogger().info("RTP timing: delay=" + st.teleportDelayTicks() + " ticks, cooldown=" + st.cooldownSeconds() + "s");
        int chunkCap = st.maxSyncChunkLoadsPerRtp();
        getLogger().info("RTP search: max attempts=" + st.maxLocationAttempts()
                + ", max sync chunk loads=" + (chunkCap <= 0 ? "unlimited" : String.valueOf(chunkCap)));
    }

    public void debugLog(String message) {
        if (settings != null && settings.debug()) {
            getLogger().info("[debug] " + message);
        }
    }

    public void completeRandomTeleport(Player player, Location destination, World economyWorld, MessageBundle messages) {
        if (!isEnabled()) {
            debugLog("completeRandomTeleport aborted: plugin disabled");
            return;
        }
        if (!player.isOnline()) {
            debugLog("completeRandomTeleport aborted: player offline");
            return;
        }
        if (destination == null || destination.getWorld() == null) {
            debugLog("completeRandomTeleport aborted: invalid destination (world unloaded?)");
            Text.send(player, messages.prefix(), messages.failed());
            return;
        }
        if (economyWorld == null) {
            debugLog("completeRandomTeleport aborted: economyWorld is null");
            Text.send(player, messages.prefix(), messages.failed());
            return;
        }

        PluginSettings s = settings;
        var econ = economy;
        if (s == null || econ == null) {
            debugLog("completeRandomTeleport aborted: settings or economy not ready");
            Text.send(player, messages.prefix(), messages.failed());
            return;
        }
        boolean bypassCost = player.hasPermission("randomteleport.bypass.cost");

        debugLog("completeRandomTeleport: " + player.getName() + " -> " + destination.getWorld().getName()
                + " (" + destination.getBlockX() + "," + destination.getBlockY() + "," + destination.getBlockZ()
                + ") economyWorld=" + economyWorld.getName());

        RtpPaymentResult pay = econ.executeRtpPayment(player, economyWorld, bypassCost);
        switch (pay) {
            case NO_CURRENCY -> {
                Text.send(player, messages.prefix(), messages.economyNotReady());
                return;
            }
            case INSUFFICIENT_FUNDS -> {
                String msg = messages.notEnoughMoney()
                        .replace("{cost}", String.valueOf(econ.costFor(economyWorld)))
                        .replace("{balance}", String.valueOf(econ.balance(player)));
                Text.send(player, messages.prefix(), msg);
                return;
            }
            case FREE, CHARGED -> {
            }
        }

        Location from = player.getLocation().clone();
        double chargedAmount = pay == RtpPaymentResult.CHARGED ? econ.costFor(economyWorld) : 0;
        if (!player.teleport(destination)) {
            if (chargedAmount > 0) {
                econ.refundRtp(player, chargedAmount);
                getLogger().warning("RTP teleport failed for " + player.getName() + " after charge; refunded " + chargedAmount
                        + " (" + econ.backendLabel() + "). Destination " + destination.getWorld().getName()
                        + " (" + destination.getBlockX() + "," + destination.getBlockY() + "," + destination.getBlockZ() + ")");
            } else {
                getLogger().warning("RTP teleport failed for " + player.getName() + " (no charge) to "
                        + destination.getWorld().getName()
                        + " (" + destination.getBlockX() + "," + destination.getBlockY() + "," + destination.getBlockZ() + ")");
            }
            Text.send(player, messages.prefix(), messages.failed());
            return;
        }

        lastLocations.rememberBeforeRtp(player, from);

        if (!player.hasPermission("randomteleport.bypass.cooldown")) {
            cooldowns.markSuccess(player.getUniqueId(), System.currentTimeMillis());
        }

        int bx = destination.getBlockX();
        int by = destination.getBlockY();
        int bz = destination.getBlockZ();
        var fx = s.effects();
        PlayerFeedback.apply(fx, fx.success(), player, destination, str -> str
                .replace("{x}", String.valueOf(bx))
                .replace("{y}", String.valueOf(by))
                .replace("{z}", String.valueOf(bz)));

        String paidFragment = "";
        if (pay == RtpPaymentResult.CHARGED && !bypassCost && chargedAmount > 0) {
            paidFragment = messages.successPaid().replace("{paid}", String.valueOf(chargedAmount));
        }
        String ok = messages.success()
                .replace("{x}", String.valueOf(bx))
                .replace("{y}", String.valueOf(by))
                .replace("{z}", String.valueOf(bz))
                .replace("{paid}", paidFragment);
        Text.send(player, messages.prefix(), ok);
        getLogger().info("RTP " + player.getName() + " -> " + destination.getWorld().getName()
                + " (" + bx + "," + by + "," + bz + ")"
                + (pay == RtpPaymentResult.CHARGED && chargedAmount > 0 && !bypassCost ? ", charged " + chargedAmount : ""));
    }

    /**
     * Charges using {@link com.npucraft.randomteleport.config.EconomySettings#backCostFor} for {@code economyWorld}.
     * Consumes the stored return point only after a successful teleport.
     */
    public void completeBackTeleport(Player player, Location destination, World economyWorld, MessageBundle messages) {
        if (!isEnabled()) {
            debugLog("completeBackTeleport aborted: plugin disabled");
            return;
        }
        if (!player.isOnline()) {
            debugLog("completeBackTeleport aborted: player offline");
            return;
        }
        if (destination == null || destination.getWorld() == null) {
            debugLog("completeBackTeleport aborted: invalid destination (world unloaded?)");
            Text.send(player, messages.prefix(), messages.backNone());
            lastLocations.take(player.getUniqueId());
            return;
        }
        if (economyWorld == null) {
            debugLog("completeBackTeleport aborted: economyWorld is null");
            Text.send(player, messages.prefix(), messages.failed());
            return;
        }

        PluginSettings s = settings;
        var econ = economy;
        if (s == null || econ == null) {
            debugLog("completeBackTeleport aborted: settings or economy not ready");
            Text.send(player, messages.prefix(), messages.failed());
            return;
        }
        boolean bypassCost = player.hasPermission("randomteleport.bypass.cost");

        debugLog("completeBackTeleport: " + player.getName() + " -> " + destination.getWorld().getName()
                + " economyWorld=" + economyWorld.getName());

        RtpPaymentResult pay = econ.executeRtpBackPayment(player, economyWorld, bypassCost);
        switch (pay) {
            case NO_CURRENCY -> {
                Text.send(player, messages.prefix(), messages.economyNotReady());
                return;
            }
            case INSUFFICIENT_FUNDS -> {
                String msg = messages.notEnoughMoney()
                        .replace("{cost}", String.valueOf(econ.backCostFor(economyWorld)))
                        .replace("{balance}", String.valueOf(econ.balance(player)));
                Text.send(player, messages.prefix(), msg);
                return;
            }
            case FREE, CHARGED -> {
            }
        }

        double chargedAmount = pay == RtpPaymentResult.CHARGED ? econ.backCostFor(economyWorld) : 0;
        if (!player.teleport(destination)) {
            if (chargedAmount > 0) {
                econ.refundRtp(player, chargedAmount);
                getLogger().warning("/rtp back teleport failed for " + player.getName() + " after charge; refunded " + chargedAmount
                        + " (" + econ.backendLabel() + ")");
            } else {
                getLogger().warning("/rtp back teleport failed for " + player.getName() + " (no charge)");
            }
            Text.send(player, messages.prefix(), messages.failed());
            return;
        }

        lastLocations.take(player.getUniqueId());

        String paidFragment = "";
        if (pay == RtpPaymentResult.CHARGED && !bypassCost && chargedAmount > 0) {
            paidFragment = messages.backSuccessPaid().replace("{paid}", String.valueOf(chargedAmount));
        }
        String backMsg = messages.backSuccess().replace("{paid}", paidFragment);
        Text.send(player, messages.prefix(), backMsg);
        Location d = destination;
        getLogger().info("/rtp back " + player.getName() + " -> " + d.getWorld().getName()
                + " (" + d.getBlockX() + "," + d.getBlockY() + "," + d.getBlockZ() + ")"
                + (pay == RtpPaymentResult.CHARGED && chargedAmount > 0 && !bypassCost ? ", charged " + chargedAmount : ""));
    }

    public PluginSettings settings() {
        return settings;
    }

    public Cooldowns cooldowns() {
        return cooldowns;
    }

    public RtpEconomy economy() {
        return economy;
    }

    public SafeLocationFinder locationFinder() {
        return locationFinder;
    }

    public PendingRtpService pendingRtp() {
        return pendingRtp;
    }

    public LastLocationStore lastLocations() {
        return lastLocations;
    }
}
