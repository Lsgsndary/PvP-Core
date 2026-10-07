package com.pvpcore.config;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;

/**
 * Reads messages from config.yml and turns them into Adventure components
 * with MiniMessage. Placeholders like {time} are escaped before parsing so
 * they can never inject formatting tags.
 */
public final class Messages {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final ConfigurationSection section;
    private final String prefix;

    Messages(ConfigurationSection section) {
        this.section = section;
        this.prefix = section == null ? "" : section.getString("prefix", "");
    }

    /** Raw message text, or "" when missing. */
    public String raw(String key) {
        if (section == null) {
            return "";
        }
        String value = section.getString(key, "");
        return value == null ? "" : value;
    }

    /** Sends a message unless it is blank in the config. */
    public void send(Audience to, String key, Map<String, String> placeholders) {
        String raw = raw(key);
        if (raw.isBlank()) {
            return;
        }
        to.sendMessage(format(raw, placeholders));
    }

    public void send(Audience to, String key) {
        send(to, key, Map.of());
    }

    /** Parses any template string (e.g. boss bar title) with placeholders. */
    public Component format(String template, Map<String, String> placeholders) {
        String text = template.replace("{prefix}", prefix);
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            text = text.replace("{" + e.getKey() + "}", MINI.escapeTags(e.getValue()));
        }
        return MINI.deserialize(text);
    }
}
