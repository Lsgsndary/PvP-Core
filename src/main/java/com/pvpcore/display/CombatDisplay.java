package com.pvpcore.display;

import com.pvpcore.PvPCore;
import com.pvpcore.config.PluginSettings;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows the combat countdown as a boss bar or action bar, depending on
 * display.display-mode. CHAT and HIDDEN modes show no countdown.
 */
public final class CombatDisplay {

    private final PvPCore plugin;
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    /** Players who currently have our text in their action bar. */
    private final Map<UUID, Boolean> actionBarShown = new ConcurrentHashMap<>();

    public CombatDisplay(PvPCore plugin) {
        this.plugin = plugin;
    }

    public void update(Player player, long remainingMillis) {
        PluginSettings settings = plugin.settings();
        if (remainingMillis <= 0) {
            return;
        }
        String seconds = String.valueOf((remainingMillis + 999) / 1000);
        Map<String, String> placeholders = Map.of("time", seconds);

        switch (settings.displayMode()) {
            case BOSSBAR -> {
                float progress = (float) remainingMillis / (float) settings.durationMillis();
                progress = Math.max(0f, Math.min(1f, progress));
                Component title = settings.messages().format(settings.bossBarTitle(), placeholders);

                BossBar bar = bossBars.get(player.getUniqueId());
                if (bar == null) {
                    bar = BossBar.bossBar(title, progress, settings.bossBarColor(), settings.bossBarStyle());
                    bossBars.put(player.getUniqueId(), bar);
                    player.showBossBar(bar);
                } else {
                    bar.name(title);
                    bar.progress(progress);
                }
            }
            case ACTIONBAR -> {
                player.sendActionBar(settings.messages().format(settings.actionBarText(), placeholders));
                actionBarShown.put(player.getUniqueId(), Boolean.TRUE);
            }
            case CHAT, HIDDEN -> {
                // No countdown in these modes
            }
        }
    }

    /** Removes the countdown from an online player (combat ended, untag, death). */
    public void clear(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
        if (actionBarShown.remove(player.getUniqueId()) != null) {
            player.sendActionBar(Component.empty());
        }
    }

    /** Drops state for a player who is leaving the server. */
    public void forget(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
        actionBarShown.remove(player.getUniqueId());
    }

    /** Removes every countdown (reload / shutdown). Bars are recreated on the next tick if needed. */
    public void clearAll() {
        for (UUID id : bossBars.keySet()) {
            Player player = Bukkit.getPlayer(id);
            BossBar bar = bossBars.remove(id);
            if (player != null && bar != null) {
                player.hideBossBar(bar);
            }
        }
        for (UUID id : actionBarShown.keySet()) {
            actionBarShown.remove(id);
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.sendActionBar(Component.empty());
            }
        }
    }
}
