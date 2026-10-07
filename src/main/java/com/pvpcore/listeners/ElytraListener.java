package com.pvpcore.listeners;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import com.pvpcore.PvPCore;
import com.pvpcore.combat.CombatManager;
import com.pvpcore.config.PluginSettings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Stops tagged players from gliding with an elytra or boosting with fireworks.
 */
public final class ElytraListener implements Listener {

    private final PvPCore plugin;
    private final CombatManager combat;
    private final NoticeCooldown notices = new NoticeCooldown(2000L);

    public ElytraListener(PvPCore plugin, CombatManager combat) {
        this.plugin = plugin;
        this.combat = combat;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent event) {
        if (!event.isGliding() || !(event.getEntity() instanceof Player player)) {
            return; // always allow stopping a glide
        }
        PluginSettings settings = plugin.settings();
        if (!settings.combatTagEnabled() || !settings.noElytraEnabled() || !combat.isTagged(player)) {
            return;
        }
        event.setCancelled(true);
        if (notices.tryNotify(player, "elytra")) {
            settings.messages().send(player, "elytra-denied");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFireworkBoost(PlayerElytraBoostEvent event) {
        Player player = event.getPlayer();
        PluginSettings settings = plugin.settings();
        if (!settings.combatTagEnabled() || !settings.blockFireworkBoost() || !combat.isTagged(player)) {
            return;
        }
        event.setCancelled(true);
        event.setShouldConsume(false);
        if (notices.tryNotify(player, "firework")) {
            settings.messages().send(player, "firework-denied");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        notices.forget(event.getPlayer());
    }
}
