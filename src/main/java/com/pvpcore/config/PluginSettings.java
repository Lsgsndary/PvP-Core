package com.pvpcore.config;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Immutable snapshot of config.yml. A new instance is built on every load or
 * reload, so the rest of the plugin never sees a half-updated config.
 */
public final class PluginSettings {

    // Combat tag
    private final boolean combatTagEnabled;
    private final long durationMillis;
    private final boolean resetTimerOnNewHit;
    private final boolean tagOnMobDamage;
    private final Set<String> disabledWorlds;

    // Display
    private final DisplayMode displayMode;
    private final String bossBarTitle;
    private final BossBar.Color bossBarColor;
    private final BossBar.Overlay bossBarStyle;
    private final String actionBarText;

    // No elytra
    private final boolean noElytraEnabled;
    private final boolean blockFireworkBoost;

    // Extras
    private final boolean combatLogEnabled;
    private final boolean combatLogPunishOnKick;
    private final boolean combatLogBroadcast;
    private final boolean blockedCommandsEnabled;
    private final Set<String> blockedCommands;
    private final boolean blockEnderPearls;
    private final boolean untagOnDeath;

    private final Messages messages;

    private PluginSettings(FileConfiguration cfg, Logger log) {
        // Every default below is "off", matching the shipped config.yml.
        this.combatTagEnabled = cfg.getBoolean("combat-tag.enabled", false);

        int seconds = cfg.getInt("combat-tag.duration-seconds", 15);
        if (seconds < 1) {
            log.warning("combat-tag.duration-seconds must be at least 1 (was " + seconds + "). Using 15.");
            seconds = 15;
        }
        this.durationMillis = seconds * 1000L;
        this.resetTimerOnNewHit = cfg.getBoolean("combat-tag.reset-timer-on-new-hit", true);
        this.tagOnMobDamage = cfg.getBoolean("combat-tag.tag-on-mob-damage", false);
        this.disabledWorlds = lowerCaseSet(cfg, "combat-tag.disabled-worlds");

        this.displayMode = parseEnum(DisplayMode.class, cfg.getString("display.display-mode"),
                DisplayMode.CHAT, "display.display-mode", log);
        this.bossBarTitle = cfg.getString("display.bossbar.title", "<red>In combat - {time}s");
        this.bossBarColor = parseEnum(BossBar.Color.class, cfg.getString("display.bossbar.color"),
                BossBar.Color.RED, "display.bossbar.color", log);
        this.bossBarStyle = parseEnum(BossBar.Overlay.class, cfg.getString("display.bossbar.style"),
                BossBar.Overlay.PROGRESS, "display.bossbar.style", log);
        this.actionBarText = cfg.getString("display.actionbar.text", "<red>In combat - {time}s");

        this.noElytraEnabled = cfg.getBoolean("no-elytra.enabled", false);
        this.blockFireworkBoost = cfg.getBoolean("no-elytra.block-firework-boost", false);

        this.combatLogEnabled = cfg.getBoolean("combat-log.enabled", false);
        this.combatLogPunishOnKick = cfg.getBoolean("combat-log.punish-on-kick", false);
        this.combatLogBroadcast = cfg.getBoolean("combat-log.broadcast", true);

        this.blockedCommandsEnabled = cfg.getBoolean("blocked-commands.enabled", false);
        Set<String> commands = new HashSet<>();
        for (String c : lowerCaseSet(cfg, "blocked-commands.commands")) {
            // Allow people to write "/spawn" as well as "spawn"
            commands.add(c.startsWith("/") ? c.substring(1) : c);
        }
        this.blockedCommands = Collections.unmodifiableSet(commands);

        this.blockEnderPearls = cfg.getBoolean("block-ender-pearls.enabled", false);
        this.untagOnDeath = cfg.getBoolean("untag-on-death.enabled", false);

        ConfigurationSection messageSection = cfg.getConfigurationSection("messages");
        this.messages = new Messages(messageSection);
    }

    public static PluginSettings load(FileConfiguration cfg, Logger log) {
        return new PluginSettings(cfg, log);
    }

    private static Set<String> lowerCaseSet(FileConfiguration cfg, String path) {
        Set<String> out = new HashSet<>();
        for (String s : cfg.getStringList(path)) {
            if (s != null && !s.isBlank()) {
                out.add(s.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Collections.unmodifiableSet(out);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E fallback, String path, Logger log) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warning("Invalid value '" + raw + "' for " + path + ". Using " + fallback.name() + ".");
            return fallback;
        }
    }

    public boolean combatTagEnabled() { return combatTagEnabled; }
    public long durationMillis() { return durationMillis; }
    public boolean resetTimerOnNewHit() { return resetTimerOnNewHit; }
    public boolean tagOnMobDamage() { return tagOnMobDamage; }
    public boolean isWorldDisabled(String worldName) { return disabledWorlds.contains(worldName.toLowerCase(Locale.ROOT)); }

    public DisplayMode displayMode() { return displayMode; }
    public String bossBarTitle() { return bossBarTitle; }
    public BossBar.Color bossBarColor() { return bossBarColor; }
    public BossBar.Overlay bossBarStyle() { return bossBarStyle; }
    public String actionBarText() { return actionBarText; }

    public boolean noElytraEnabled() { return noElytraEnabled; }
    public boolean blockFireworkBoost() { return blockFireworkBoost; }

    public boolean combatLogEnabled() { return combatLogEnabled; }
    public boolean combatLogPunishOnKick() { return combatLogPunishOnKick; }
    public boolean combatLogBroadcast() { return combatLogBroadcast; }

    public boolean blockedCommandsEnabled() { return blockedCommandsEnabled; }
    public boolean isCommandBlocked(String label) { return blockedCommands.contains(label); }

    public boolean blockEnderPearls() { return blockEnderPearls; }
    public boolean untagOnDeath() { return untagOnDeath; }

    public Messages messages() { return messages; }
}
