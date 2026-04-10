package com.npucraft.randomteleport.command;

import com.npucraft.randomteleport.RandomTeleportPlugin;
import com.npucraft.randomteleport.config.MessageBundle;
import com.npucraft.randomteleport.config.PluginSettings;
import com.npucraft.randomteleport.economy.RtpPaymentResult;
import com.npucraft.randomteleport.feedback.PlayerFeedback;
import com.npucraft.randomteleport.util.Text;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class RtpCommand implements CommandExecutor, TabCompleter {

    private final RandomTeleportPlugin plugin;

    public RtpCommand(RandomTeleportPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.isEnabled() || plugin.settings() == null) {
            sender.sendMessage("RandomTeleport is not ready.");
            return true;
        }
        PluginSettings s = plugin.settings();

        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("reload")) {
                return handleReload(sender, s);
            }
            if (sub.equals("back")) {
                if (!(sender instanceof Player player)) {
                    MessageBundle c = s.messagesFor(sender);
                    Text.send(sender, c.prefix(), c.playersOnly());
                    return true;
                }
                return handleBack(player, s);
            }
            if (sub.equals("help")) {
                handleHelp(sender, s);
                return true;
            }
            MessageBundle m = s.messagesFor(sender);
            Text.send(sender, m.prefix(), m.unknownSubcommand());
            return true;
        }

        if (!(sender instanceof Player player)) {
            MessageBundle c = s.messagesFor(sender);
            Text.send(sender, c.prefix(), c.playersOnly());
            return true;
        }

        return handleRandomTeleport(player, s);
    }

    private boolean handleReload(CommandSender sender, PluginSettings s) {
        MessageBundle m = s.messagesFor(sender);
        if (!sender.hasPermission("randomteleport.admin.reload")) {
            Text.send(sender, m.prefix(), m.reloadDenied());
            return true;
        }
        try {
            plugin.reloadEverything();
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "RandomTeleport configuration reload failed", t);
            Text.send(sender, m.prefix(), m.reloadFailed());
            return true;
        }
        plugin.getLogger().info("Configuration reloaded by " + sender.getName() + ".");
        plugin.debugLog("Configuration reloaded by " + sender.getName());
        MessageBundle after = plugin.settings().messagesFor(sender);
        if (sender instanceof Player pl) {
            var fx = plugin.settings().effects();
            PlayerFeedback.apply(fx, fx.reload(), pl, pl.getLocation(), str -> str);
        }
        Text.send(sender, after.prefix(), after.reloadSuccess());
        return true;
    }

    private boolean handleBack(Player player, PluginSettings s) {
        MessageBundle m = s.messagesFor(player);
        if (!player.hasPermission("randomteleport.back")) {
            Text.send(player, m.prefix(), m.noPermission());
            return true;
        }
        Optional<Location> opt = plugin.lastLocations().peek(player.getUniqueId());
        if (opt.isEmpty()) {
            Text.send(player, m.prefix(), m.backNone());
            return true;
        }
        Location loc = opt.get();
        World destWorld = loc.getWorld();
        if (destWorld == null) {
            plugin.lastLocations().take(player.getUniqueId());
            Text.send(player, m.prefix(), m.backNone());
            return true;
        }

        World economyWorld = player.getWorld();
        if (!s.worlds().allows(economyWorld)) {
            Text.send(player, m.prefix(), m.worldDenied());
            return true;
        }

        var econ = plugin.economy();
        if (econ == null) {
            Text.send(player, m.prefix(), m.economyNotReady());
            return true;
        }
        boolean bypassCost = player.hasPermission("randomteleport.bypass.cost");
        RtpPaymentResult pre = econ.precheckRtpBackPayment(player, economyWorld, bypassCost);
        if (notifyEconomyFailure(player, economyWorld, m, pre, econ.backCostFor(economyWorld))) {
            return true;
        }

        plugin.completeBackTeleport(player, loc, economyWorld, m);
        return true;
    }

    private void handleHelp(CommandSender sender, PluginSettings s) {
        MessageBundle m = s.messagesFor(sender);
        if (!sender.hasPermission("randomteleport.use")) {
            String deny = m.helpNoPermission().isBlank() ? m.noPermission() : m.helpNoPermission();
            Text.send(sender, m.prefix(), deny);
            return;
        }
        var econ = plugin.economy();
        if (econ == null) {
            Text.send(sender, m.prefix(), m.economyNotReady());
            return;
        }
        boolean ecOn = s.economy().enabled();
        List<World> worlds = new ArrayList<>(plugin.getServer().getWorlds());
        worlds.sort(Comparator.comparing(World::getName, String.CASE_INSENSITIVE_ORDER));
        boolean printedAny = false;
        for (World w : worlds) {
            if (!s.worlds().allows(w)) {
                continue;
            }
            if (!printedAny && !m.helpHeader().isBlank()) {
                Text.send(sender, m.prefix(), m.helpHeader());
            }
            printedAny = true;
            String dim = m.helpDimension(w.getEnvironment());
            String rtp = ecOn ? String.valueOf(econ.costFor(w)) : "0";
            String back = ecOn ? String.valueOf(econ.backCostFor(w)) : "0";
            Optional<PluginSettings.AxisRange> ro = s.effectiveRtpRange(w);
            if (ro.isPresent()) {
                PluginSettings.AxisRange ar = ro.get();
                String line = m.helpLineRange()
                        .replace("{world}", w.getName())
                        .replace("{dim}", dim)
                        .replace("{rtp}", rtp)
                        .replace("{back}", back)
                        .replace("{minx}", String.valueOf(ar.minX()))
                        .replace("{maxx}", String.valueOf(ar.maxX()))
                        .replace("{minz}", String.valueOf(ar.minZ()))
                        .replace("{maxz}", String.valueOf(ar.maxZ()));
                Text.send(sender, m.prefix(), line);
            } else {
                String line = m.helpLineNoRange()
                        .replace("{world}", w.getName())
                        .replace("{dim}", dim)
                        .replace("{rtp}", rtp)
                        .replace("{back}", back);
                Text.send(sender, m.prefix(), line);
            }
        }
        if (printedAny && !m.helpFooter().isBlank()) {
            Text.send(sender, m.prefix(), m.helpFooter());
        }
        if (!printedAny) {
            Text.send(sender, m.prefix(), m.helpNoWorlds());
        }
    }

    private boolean handleRandomTeleport(Player player, PluginSettings s) {
        MessageBundle m = s.messagesFor(player);

        if (!player.hasPermission("randomteleport.use")) {
            Text.send(player, m.prefix(), m.noPermission());
            return true;
        }

        if (plugin.pendingRtp().isPending(player.getUniqueId())) {
            Text.send(player, m.prefix(), m.rtpPending());
            return true;
        }

        World economyWorld = player.getWorld();
        if (!s.worlds().allows(economyWorld)) {
            Text.send(player, m.prefix(), m.worldDenied());
            return true;
        }

        var econ = plugin.economy();
        if (econ == null) {
            Text.send(player, m.prefix(), m.economyNotReady());
            return true;
        }
        boolean bypassCost = player.hasPermission("randomteleport.bypass.cost");
        RtpPaymentResult pre = econ.precheckRtpPayment(player, economyWorld, bypassCost);
        if (notifyEconomyFailure(player, economyWorld, m, pre, econ.costFor(economyWorld))) {
            return true;
        }

        if (!player.hasPermission("randomteleport.bypass.cooldown")) {
            long left = plugin.cooldowns().remainingSeconds(
                    player.getUniqueId(),
                    s.cooldownSeconds(),
                    System.currentTimeMillis()
            );
            if (left > 0) {
                String msg = m.cooldown().replace("{seconds}", String.valueOf(left));
                Text.send(player, m.prefix(), msg);
                return true;
            }
        }

        Optional<PluginSettings.AxisRange> rangeOpt = s.effectiveRtpRange(economyWorld);
        if (rangeOpt.isEmpty()) {
            var fx = s.effects();
            PlayerFeedback.apply(fx, fx.failed(), player, player.getLocation(), str -> str);
            Text.send(player, m.prefix(), m.noValidRange());
            return true;
        }

        if (plugin.locationFinder() == null) {
            Text.send(player, m.prefix(), m.failed());
            return true;
        }

        Location base = player.getLocation();
        if (base.getWorld() == null) {
            Text.send(player, m.prefix(), m.failed());
            return true;
        }

        var fx = s.effects();
        PlayerFeedback.apply(fx, fx.searching(), player, player.getLocation(), str -> str);
        Text.actionBar(player, m.searching());

        Location dest = plugin.locationFinder().findSafe(
                base.getWorld(),
                rangeOpt.get(),
                base,
                player
        );

        Text.actionBar(player, "");
        if (dest == null) {
            plugin.getLogger().warning("No safe RTP location found for " + player.getName() + " in world '" + economyWorld.getName()
                    + "' (range/border/min-distance may be too tight).");
            plugin.debugLog("handleRandomTeleport: no safe location for " + player.getName() + " in " + economyWorld.getName());
            PlayerFeedback.apply(fx, fx.failed(), player, player.getLocation(), str -> str);
            Text.send(player, m.prefix(), m.failed());
            return true;
        }

        String delayCostDetail = (!s.economy().enabled() || bypassCost || !econ.chargesInWorld(economyWorld))
                ? m.delayCostFree()
                : m.delayCostWillCharge().replace("{cost}", String.valueOf(econ.costFor(economyWorld)));

        World finalEconomyWorld = economyWorld;
        plugin.pendingRtp().scheduleTeleport(player, m, delayCostDetail,
                () -> plugin.completeRandomTeleport(player, dest, finalEconomyWorld, m));

        return true;
    }

    private boolean notifyEconomyFailure(Player player, World world, MessageBundle m, RtpPaymentResult r, double requiredCost) {
        var econ = plugin.economy();
        if (econ == null) {
            Text.send(player, m.prefix(), m.economyNotReady());
            return true;
        }
        return switch (r) {
            case FREE, CHARGED -> false;
            case NO_CURRENCY -> {
                Text.send(player, m.prefix(), m.economyNotReady());
                yield true;
            }
            case INSUFFICIENT_FUNDS -> {
                String msg = m.notEnoughMoney()
                        .replace("{cost}", String.valueOf(requiredCost))
                        .replace("{balance}", String.valueOf(econ.balance(player)));
                Text.send(player, m.prefix(), msg);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> opts = new ArrayList<>();
        if (sender.hasPermission("randomteleport.admin.reload")) {
            opts.add("reload");
        }
        if (sender instanceof Player && sender.hasPermission("randomteleport.back")) {
            opts.add("back");
        }
        if (sender.hasPermission("randomteleport.use")) {
            opts.add("help");
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return opts.stream()
                .filter(o -> o.startsWith(prefix))
                .collect(Collectors.toList());
    }
}
