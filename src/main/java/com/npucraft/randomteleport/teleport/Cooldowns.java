package com.npucraft.randomteleport.teleport;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Cooldowns {

    private final Map<UUID, Long> lastSuccessMillis = new ConcurrentHashMap<>();

    public long remainingSeconds(UUID id, int cooldownSeconds, long nowMillis) {
        if (cooldownSeconds <= 0) {
            return 0;
        }
        Long last = lastSuccessMillis.get(id);
        if (last == null) {
            return 0;
        }
        long elapsed = nowMillis - last;
        long need = cooldownSeconds * 1000L;
        if (elapsed >= need) {
            return 0;
        }
        return (long) Math.ceil((need - elapsed) / 1000.0);
    }

    public void markSuccess(UUID id, long nowMillis) {
        lastSuccessMillis.put(id, nowMillis);
    }
}
