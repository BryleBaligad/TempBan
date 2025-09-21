package com.brylebaligad.tempBan;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class Violation {
    public static boolean command(TempBan plugin, CommandSender sender, Command command, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("[TempBan] Missing arguments!");
            return false;
        }

        Player player = plugin.getServer().getPlayerExact(args[0]);
        if (player == null) {
            sender.sendMessage("[TempBan] " + args[0] + " is not online");
            return true;
        }

        String violationName = args[1];
        ConfigurationSection violationType = TempBan.config.getConfigurationSection("violation-types." + violationName);
        if (violationType == null) {
            sender.sendMessage("[TempBan] Invalid violation type");
            return true;
        }

        ConfigurationSection violationOverride = violationType.getConfigurationSection("overrides." + player.getName());
        if (violationOverride != null) {
            violationType = violationOverride;
        }

        int maxViolationLevel = violationType.getInt("max-vl", 20);
        long banDuration = violationType.getLong("ban-duration", 30);
        String banMesssage = violationType.getString("ban-message", "unset");

        int violationCount = TempBan.config.getInt("violations." + player.getName() + "." + violationName + "." + "count", 0);
        violationCount++;
        TempBan.config.set("violations." + player.getName() + "." + violationName + "." + "count", violationCount);

        if (violationCount >= maxViolationLevel) {
            TempBan.tempBan(plugin, player, banDuration, banMesssage);
        }

        String violationMessage = "[TempBan] Violation \"" + violationName + "\" triggered by " + player.getName() + ". VL " + violationCount + "/" + maxViolationLevel;
        sender.sendMessage(violationMessage);

        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (sender.getName().equals(onlinePlayer.getName())) continue;

            if (onlinePlayer.hasPermission("tempban.violation.notify")) {
                onlinePlayer.sendMessage(Component.text(violationMessage));
            }
        }

        if (!(sender instanceof ConsoleCommandSender)) {
            plugin.getLogger().info("Violation \"" + violationName + "\" triggered by " + player.getName() + ". VL " + violationCount + "/" + maxViolationLevel);
        }

        plugin.setConfig(TempBan.config);

        return true;
    }
}
