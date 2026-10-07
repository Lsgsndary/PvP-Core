package com.pvpcore;

import com.pvpcore.combat.CombatManager;
import com.pvpcore.commands.PvPCoreCommand;
import com.pvpcore.config.PluginSettings;
import com.pvpcore.display.CombatDisplay;
import com.pvpcore.listeners.CombatListener;
import com.pvpcore.listeners.ElytraListener;
import com.pvpcore.listeners.RestrictionListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class PvPCore extends JavaPlugin {

    /** How often (in ticks) tags are checked and countdowns refreshed. 5 ticks = 0.25 s. */
    private static final long TICK_INTERVAL = 5L;

    private volatile PluginSettings settings;
    private CombatDisplay display;
    private CombatManager combat;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = PluginSettings.load(getConfig(), getLogger());

        this.display = new CombatDisplay(this);
        this.combat = new CombatManager(this, display);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new CombatListener(this, combat), this);
        pm.registerEvents(new ElytraListener(this, combat), this);
        pm.registerEvents(new RestrictionListener(this, combat), this);

        PluginCommand command = getCommand("pvpcore");
        if (command != null) {
            PvPCoreCommand executor = new PvPCoreCommand(this, combat);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().severe("Command 'pvpcore' is missing from plugin.yml!");
        }

        getServer().getScheduler().runTaskTimer(this, combat::tick, TICK_INTERVAL, TICK_INTERVAL);
        logEnabledFeatures();
    }

    @Override
    public void onDisable() {
        if (combat != null) {
            combat.untagAll(false);
        }
    }

    /** Re-reads config.yml and applies it immediately. */
    public void reload() {
        saveDefaultConfig(); // recreate the file if someone deleted it
        reloadConfig();
        display.clearAll(); // countdowns are redrawn next tick using the new settings
        this.settings = PluginSettings.load(getConfig(), getLogger());
        if (!settings.combatTagEnabled()) {
            combat.untagAll(false);
        }
        logEnabledFeatures();
    }

    private void logEnabledFeatures() {
        PluginSettings s = settings;
        if (!s.combatTagEnabled()) {
            getLogger().info("Combat tag is disabled in config.yml - PvPCore is idle.");
            return;
        }
        List<String> on = new ArrayList<>();
        on.add("combat-tag (" + (s.durationMillis() / 1000) + "s, display " + s.displayMode() + ")");
        if (s.noElytraEnabled()) on.add("no-elytra");
        if (s.blockFireworkBoost()) on.add("block-firework-boost");
        if (s.combatLogEnabled()) on.add("combat-log");
        if (s.blockedCommandsEnabled()) on.add("blocked-commands");
        if (s.blockEnderPearls()) on.add("block-ender-pearls");
        if (s.untagOnDeath()) on.add("untag-on-death");
        getLogger().info("Enabled features: " + String.join(", ", on));
    }

    public PluginSettings settings() {
        return settings;
    }

    public CombatDisplay display() {
        return display;
    }

    public CombatManager combat() {
        return combat;
    }
}
