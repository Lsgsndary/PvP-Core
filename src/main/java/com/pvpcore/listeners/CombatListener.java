package com.pvpcore.listeners;

import com.pvpcore.PvPCore;
import com.pvpcore.combat.CombatManager;
import com.pvpcore.config.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;

/**
 * Tags players when they fight, and handles death and logging out.
 */
public final class CombatListener implements Listener {

    private final PvPCore plugin;
    private final CombatManager combat;

    public CombatListener(PvPCore plugin, CombatManager combat) {
        this.plugin = plugin;
        this.combat = combat;
    }

    /*
     * MONITOR + ignoreCancelled: we only react to hits that actually happened,
     * so hits blocked by spawn protection, WorldGuard regions, teams, etc. never tag anyone.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        PluginSettings settings = plugin.settings();
        if (!settings.combatTagEnabled()) {
            return;
        }

        Entity victim = event.getEntity();
        Entity attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.equals(victim)) {
            return; // no real attacker, or hurting yourself (own arrow, own pearl, own TNT)
        }

        Player victimPlayer = asRealPlayer(victim);
        Player attackerPlayer = asRealPlayer(attacker);

        if (victimPlayer != null && attackerPlayer != null) {
            // Player vs player
            combat.tag(victimPlayer);
            combat.tag(attackerPlayer);
            return;
        }

        if (!settings.tagOnMobDamage()) {
            return;
        }
        if (victimPlayer != null && isMob(attacker)) {
            combat.tag(victimPlayer);
        } else if (attackerPlayer != null && isMob(victim)) {
            combat.tag(attackerPlayer);
        }
    }

    /** Finds who is really responsible for the damage (the shooter of an arrow, the lighter of TNT, ...). */
    private Entity resolveAttacker(Entity damager) {
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof Entity entity ? entity : null;
        }
        if (damager instanceof AreaEffectCloud cloud) { // lingering potions
            ProjectileSource source = cloud.getSource();
            return source instanceof Entity entity ? entity : null;
        }
        if (damager instanceof TNTPrimed tnt) {
            return tnt.getSource();
        }
        return damager;
    }

    /** Returns the player, ignoring NPCs from plugins like Citizens (they set "NPC" metadata). */
    private Player asRealPlayer(Entity entity) {
        if (entity instanceof Player player && !player.hasMetadata("NPC")) {
            return player;
        }
        return null;
    }

    private boolean isMob(Entity entity) {
        return entity instanceof LivingEntity
                && !(entity instanceof ArmorStand)
                && asRealPlayer(entity) == null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (plugin.settings().untagOnDeath()) {
            combat.untag(player, false);
        } else {
            // Stay tagged, but hide the countdown while dead. It comes back after respawn.
            plugin.display().clear(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PluginSettings settings = plugin.settings();

        if (settings.combatTagEnabled() && settings.combatLogEnabled() && combat.isTagged(player)) {
            boolean kicked = event.getReason() == PlayerQuitEvent.QuitReason.KICKED;
            if (!kicked || settings.combatLogPunishOnKick()) {
                punishCombatLogger(player, settings);
            }
        }
        combat.forget(player);
    }

    private void punishCombatLogger(Player player, PluginSettings settings) {
        if (player.isDead()) {
            return;
        }
        // Killing the player during the quit event drops their items normally.
        player.setHealth(0.0);
        if (settings.combatLogBroadcast()) {
            settings.messages().send(Bukkit.getServer(), "combat-log-broadcast",
                    Map.of("player", player.getName()));
        }
    }
}
