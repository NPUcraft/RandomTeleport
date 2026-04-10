package com.npucraft.randomteleport.teleport;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LastLocationStore {

    private final Map<UUID, Location> lastRtpFrom = new ConcurrentHashMap<>();

    public void rememberBeforeRtp(Player player, Location from) {
        lastRtpFrom.put(player.getUniqueId(), from.clone());
    }

    /** View stored return point without removing (e.g. before economy checks for /rtp back). */
    public Optional<Location> peek(UUID id) {
        Location l = lastRtpFrom.get(id);
        if (l == null) {
            return Optional.empty();
        }
        return Optional.of(l.clone());
    }

    public Optional<Location> take(UUID id) {
        return Optional.ofNullable(lastRtpFrom.remove(id));
    }

    public boolean has(UUID id) {
        return lastRtpFrom.containsKey(id);
    }
}
