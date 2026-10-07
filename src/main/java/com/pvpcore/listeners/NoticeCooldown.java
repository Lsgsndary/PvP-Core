package com.pvpcore.listeners;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stops "you can't do that" messages from spamming chat when an event fires
 * many times per second (e.g. a player repeatedly trying to glide).
 */
final class NoticeCooldown {

    private final long cooldownMillis;
    private final Map<UUID, Map<String, Long>> lastSent = new ConcurrentHashMap<>();

    NoticeCooldown(long cooldownMillis) {
        this.cooldownMillis = cooldownMillis;
    }

    /** Returns true (and starts the cooldown) if the player may be sent this notice now. */
    boolean tryNotify(Player player, String key) {
        long now = System.currentTimeMillis();
        Map<String, Long> perPlayer = lastSent.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>());
        Long last = perPlayer.get(key);
        if (last != null && now - last < cooldownMillis) {
            return false;
        }
        perPlayer.put(key, now);
        return true;
    }

    void forget(Player player) {
        lastSent.remove(player.getUniqueId());
    }
}
