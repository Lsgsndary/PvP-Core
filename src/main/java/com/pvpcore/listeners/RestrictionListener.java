package com.pvpcore.listeners;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.pvpcore.PvPCore;
import com.pvpcore.combat.CombatManager;
import com.pvpcore.config.PluginSettings;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;

/**
 * Blocks configured commands and ender pearls while a player is tagged.
 */
public final class RestrictionListener implements Listener {

    private final PvPCore plugin;
    private final CombatManager combat;
    private final NoticeCooldown notices = new NoticeCooldown(1000L);

    public RestrictionListener(PvPCore plugin, CombatManager combat) {
        this.plugin = plugin;
        this.combat = combat;
    }

    // LOW so we cancel before most other plugins handle the command
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        PluginSettings settings = plugin.settings();
        if (!settings.combatTagEnabled() || !settings.blockedCommandsEnabled() || !combat.isTagged(player)) {
            return;
        }
        String label = commandLabel(event.getMessage());
        if (label.isEmpty() || !settings.isCommandBlocked(label)) {
            return;
        }
        event.setCancelled(true);
        settings.messages().send(player, "command-blocked");
    }

    /** "/Essentials:Spawn arg" -> "spawn" */
    static String commandLabel(String message) {
        String text = message.startsWith("/") ? message.substring(1) : message;
        text = text.trim();
        int space = text.indexOf(' ');
        String label = space == -1 ? text : text.substring(0, space);
        int colon = label.indexOf(':');
        if (colon != -1) {
            label = label.substring(colon + 1);
        }
        return label.toLowerCase(Locale.ROOT);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPearl(PlayerLaunchProjectileEvent event) {
        if (!(event.getProjectile() instanceof EnderPearl)) {
            return;
        }
        Player player = event.getPlayer();
        PluginSettings settings = plugin.settings();
        if (!settings.combatTagEnabled() || !settings.blockEnderPearls() || !combat.isTagged(player)) {
            return;
        }
        event.setCancelled(true);
        event.setShouldConsume(false);
        // Re-sync the hotbar so the client does not show a missing pearl
        plugin.getServer().getScheduler().runTask(plugin, player::updateInventory);
        if (notices.tryNotify(player, "pearl")) {
            settings.messages().send(player, "ender-pearl-denied");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        notices.forget(event.getPlayer());
    }
}
