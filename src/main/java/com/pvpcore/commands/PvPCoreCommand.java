package com.pvpcore.commands;

import com.pvpcore.PvPCore;
import com.pvpcore.combat.CombatManager;
import com.pvpcore.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /pvpcore reload | status [player] | untag <player>
 */
public final class PvPCoreCommand implements TabExecutor {

    private static final String ADMIN = "pvpcore.admin";
    private static final String STATUS = "pvpcore.status";

    private final PvPCore plugin;
    private final CombatManager combat;

    public PvPCoreCommand(PvPCore plugin, CombatManager combat) {
        this.plugin = plugin;
        this.combat = combat;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.settings().messages();
        if (args.length == 0) {
            messages.send(sender, "usage");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (!sender.hasPermission(ADMIN)) {
                    messages.send(sender, "no-permission");
                    return true;
                }
                plugin.reload();
                plugin.settings().messages().send(sender, "reload");
            }
            case "status" -> {
                if (!sender.hasPermission(STATUS)) {
                    messages.send(sender, "no-permission");
                    return true;
                }
                Player target = resolveTarget(sender, args);
                if (target == null) {
                    messages.send(sender, args.length < 2 ? "usage" : "player-not-found");
                    return true;
                }
                if (!plugin.settings().combatTagEnabled()) {
                    messages.send(sender, "feature-disabled");
                    return true;
                }
                if (combat.isTagged(target)) {
                    long seconds = (combat.remainingMillis(target) + 999) / 1000;
                    messages.send(sender, "status-tagged",
                            Map.of("player", target.getName(), "time", String.valueOf(seconds)));
                } else {
                    messages.send(sender, "status-not-tagged", Map.of("player", target.getName()));
                }
            }
            case "untag" -> {
                if (!sender.hasPermission(ADMIN)) {
                    messages.send(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    messages.send(sender, "usage");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    messages.send(sender, "player-not-found");
                    return true;
                }
                if (combat.untag(target, true)) {
                    messages.send(sender, "untag-success", Map.of("player", target.getName()));
                } else {
                    messages.send(sender, "untag-not-tagged", Map.of("player", target.getName()));
                }
            }
            default -> messages.send(sender, "usage");
        }
        return true;
    }

    /** "/pvpcore status" checks yourself; "/pvpcore status Steve" checks Steve. */
    private Player resolveTarget(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            return Bukkit.getPlayerExact(args[1]);
        }
        return sender instanceof Player player ? player : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission(ADMIN)) {
                options.add("reload");
                options.add("untag");
            }
            if (sender.hasPermission(STATUS)) {
                options.add("status");
            }
            return filter(options, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            boolean allowed = (sub.equals("status") && sender.hasPermission(STATUS))
                    || (sub.equals("untag") && sender.hasPermission(ADMIN));
            if (allowed) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!(sender instanceof Player viewer) || viewer.canSee(online)) {
                        options.add(online.getName());
                    }
                }
                return filter(options, args[1]);
            }
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(option);
            }
        }
        return out;
    }
}
