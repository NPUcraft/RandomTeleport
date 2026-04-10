package com.npucraft.randomteleport.teleport;

import com.npucraft.randomteleport.RandomTeleportPlugin;
import com.npucraft.randomteleport.config.EffectsSettings;
import com.npucraft.randomteleport.config.MessageBundle;
import com.npucraft.randomteleport.config.PluginSettings;
import com.npucraft.randomteleport.feedback.PlayerFeedback;
import com.npucraft.randomteleport.util.Text;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingRtpService {

    private final RandomTeleportPlugin plugin;
    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    public PendingRtpService(RandomTeleportPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isPending(UUID id) {
        return pending.contains(id);
    }

    /** How many players currently have a delayed RTP countdown. */
    public int pendingCount() {
        return pending.size();
    }

    public void cancelSilently(UUID id) {
        BukkitTask t = tasks.remove(id);
        if (t != null) {
            t.cancel();
        }
        pending.remove(id);
    }

    public void cancelWithFeedback(Player player, MessageBundle messages) {
        UUID id = player.getUniqueId();
        cancelSilently(id);
        Text.actionBar(player, "");
        PluginSettings s = plugin.settings();
        if (s != null && plugin.isEnabled()) {
            EffectsSettings fx = s.effects();
            PlayerFeedback.apply(fx, fx.delayCancel(), player, player.getLocation(), str -> str);
        }
        Text.send(player, messages.prefix(), messages.delayCanceled());
    }

    /**
     * Runs {@code onTeleport} on the main thread after {@code delayTicks} server ticks (or immediately if 0).
     *
     * @param delayCostDetail text for {@code {cost_detail}} on the delay action bar (free vs will-charge), already localized
     */
    public void scheduleTeleport(Player player, MessageBundle messages, String delayCostDetail, Runnable onTeleport) {
        PluginSettings s = plugin.settings();
        if (s == null || !plugin.isEnabled()) {
            return;
        }
        int delayTicks = s.teleportDelayTicks();
        if (delayTicks <= 0) {
            if (plugin.isEnabled()) {
                onTeleport.run();
            }
            return;
        }

        UUID id = player.getUniqueId();
        cancelSilently(id);
        Location frozen = player.getLocation().clone();
        if (frozen.getWorld() == null) {
            Text.send(player, messages.prefix(), messages.failed());
            return;
        }
        pending.add(id);
        double moveSq = s.delayCancelMoveBlocks() * s.delayCancelMoveBlocks();
        EffectsSettings fx = s.effects();
        String costDetail = delayCostDetail == null ? "" : delayCostDetail;

        final int[] remaining = {delayTicks};
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.isEnabled()) {
                cancelSilently(id);
                Text.actionBar(player, "");
                return;
            }
            if (plugin.settings() == null) {
                cancelSilently(id);
                Text.actionBar(player, "");
                return;
            }
            if (!player.isOnline()) {
                cancelSilently(id);
                return;
            }
            Location current = player.getLocation();
            World curWorld = current.getWorld();
            // distanceSquared throws if worlds differ (e.g. player entered a portal during countdown).
            if (curWorld == null || !curWorld.equals(frozen.getWorld())
                    || current.distanceSquared(frozen) > moveSq) {
                cancelWithFeedback(player, messages);
                return;
            }

            remaining[0]--;
            if (remaining[0] <= 0) {
                cancelSilently(id);
                Text.actionBar(player, "");
                onTeleport.run();
                return;
            }

            int sec = (remaining[0] + 19) / 20;
            String bar = messages.delayActionbar()
                    .replace("{seconds}", String.valueOf(sec))
                    .replace("{ticks}", String.valueOf(remaining[0]))
                    .replace("{cost_detail}", costDetail);
            Text.actionBar(player, bar);

            if (fx.repeatCountdownTitle() && fx.masterEnabled() && fx.countdown().enabled()) {
                PlayerFeedback.apply(fx, fx.countdown(), player, player.getLocation(),
                        str -> str.replace("{seconds}", String.valueOf(sec))
                                .replace("{ticks}", String.valueOf(remaining[0])));
            }
        }, 0L, 1L);

        tasks.put(id, task);
    }

    /** Clears all pending RTP timers (e.g. plugin disable); no player messages. */
    public void cancelAll() {
        for (UUID id : new HashSet<>(tasks.keySet())) {
            cancelSilently(id);
        }
    }

    /**
     * Cancels delayed RTP after config reload; notifies online players.
     *
     * @return how many pending countdowns were cleared
     */
    public int cancelAllNotifyingReload() {
        int cleared = pending.size();
        PluginSettings s = plugin.settings();
        for (UUID id : new HashSet<>(pending)) {
            if (s != null) {
                Player p = plugin.getServer().getPlayer(id);
                if (p != null && p.isOnline()) {
                    Text.actionBar(p, "");
                    MessageBundle m = s.messagesFor(p);
                    Text.send(p, m.prefix(), m.reloadRtpCancelled());
                }
            }
            cancelSilently(id);
        }
        return cleared;
    }
}
