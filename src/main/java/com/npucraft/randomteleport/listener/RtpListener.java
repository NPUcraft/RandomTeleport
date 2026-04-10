package com.npucraft.randomteleport.listener;

import com.npucraft.randomteleport.RandomTeleportPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class RtpListener implements Listener {

    private final RandomTeleportPlugin plugin;

    public RtpListener(RandomTeleportPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.isEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!plugin.pendingRtp().isPending(player.getUniqueId())) {
            return;
        }
        var settings = plugin.settings();
        if (settings == null) {
            plugin.pendingRtp().cancelSilently(player.getUniqueId());
            return;
        }
        plugin.pendingRtp().cancelWithFeedback(player, settings.messagesFor(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.isEnabled()) {
            return;
        }
        plugin.pendingRtp().cancelSilently(event.getPlayer().getUniqueId());
    }
}
