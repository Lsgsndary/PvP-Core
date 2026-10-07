package com.pvpcore.combat;

import com.pvpcore.PvPCore;
import com.pvpcore.config.PluginSettings;
import com.pvpcore.display.CombatDisplay;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players are in combat and when their tag runs out.
 * <p>
 * All tagging happens on the main server thread (events and the scheduler
 * task), and the map is a ConcurrentHashMap so read-only lookups from
 * anywhere else are still safe.
 */
public final class CombatManager {

    public static final String BYPASS_PERMISSION = "pvpcore.bypass";

    private final PvPCore plugin;
    private final CombatDisplay display;
    /** Player UUID -> time (epoch millis) when their combat tag expires. */
    private final Map<UUID, Long> expiries = new ConcurrentHashMap<>();

    public CombatManager(PvPCore plugin, CombatDisplay display) {
        this.plugin = plugin;
        this.display = display;
    }

    /**
     * Combat tags a player, respecting every config rule. Returns true when the
     * player is tagged after the call.
     */
    public boolean tag(Player player) {
        PluginSettings settings = plugin.settings();
        if (!settings.combatTagEnabled() || !canBeTagged(player, settings)) {
            return false;
        }

        long now = System.currentTimeMillis();
        Long current = expiries.get(player.getUniqueId());
        boolean newTag = current == null || current <= now;

        if (newTag || settings.resetTimerOnNewHit()) {
            expiries.put(player.getUniqueId(), now + settings.durationMillis());
        }

        if (newTag) {
            if (settings.displayMode().sendsMessages()) {
                settings.messages().send(player, "tagged");
            }
            // Knock a player out of an elytra glide the moment they enter combat
            if (settings.noElytraEnabled() && player.isGliding()) {
                player.setGliding(false);
                settings.messages().send(player, "elytra-denied");
            }
        }

        display.update(player, remainingMillis(player));
        return true;
    }

    private boolean canBeTagged(Player player, PluginSettings settings) {
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return false;
        }
        GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
            return false;
        }
        return !settings.isWorldDisabled(player.getWorld().getName());
    }

    public boolean isTagged(Player player) {
        Long expiry = expiries.get(player.getUniqueId());
        return expiry != null && expiry > System.currentTimeMillis();
    }

    /** Milliseconds of combat left, or 0 if not tagged. */
    public long remainingMillis(Player player) {
        Long expiry = expiries.get(player.getUniqueId());
        if (expiry == null) {
            return 0L;
        }
        return Math.max(0L, expiry - System.currentTimeMillis());
    }

    /**
     * Removes a player's tag and their timer display.
     *
     * @param notify send the "no longer in combat" message (if the display mode allows it)
     * @return true if the player was tagged
     */
    public boolean untag(Player player, boolean notify) {
        Long removed = expiries.remove(player.getUniqueId());
        display.clear(player);
        if (removed == null) {
            return false;
        }
        PluginSettings settings = plugin.settings();
        if (notify && settings.displayMode().sendsMessages()) {
            settings.messages().send(player, "untagged");
        }
        return true;
    }

    /** Removes every tag (used on reload when combat tag is turned off, and on shutdown). */
    public void untagAll(boolean notify) {
        for (UUID id : expiries.keySet()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                untag(player, notify);
            } else {
                expiries.remove(id);
            }
        }
        display.clearAll();
    }

    /** Called when a player leaves: drop their tag and display without messaging them. */
    public void forget(Player player) {
        expiries.remove(player.getUniqueId());
        display.forget(player);
    }

    /** Runs every few ticks: expires tags and refreshes the countdown. */
    public void tick() {
        if (expiries.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = expiries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }
            long remaining = entry.getValue() - now;
            if (remaining <= 0) {
                untag(player, true);
            } else if (!player.isDead()) {
                display.update(player, remaining);
            }
        }
    }
}
