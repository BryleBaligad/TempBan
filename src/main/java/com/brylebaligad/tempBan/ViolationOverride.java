package com.brylebaligad.tempBan;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ViolationOverride {
    public static boolean command(TempBan plugin, CommandSender sender, Command command, String[] args) {
        if (args.length < 3) {
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

        long maxViolationLevel = violationType.getLong("max-vl");
        if (args[2].equals("reset")) {
            violationType.set("overrides." + player.getName(), null);
            plugin.setConfig(TempBan.config);
            sender.sendMessage("[TempBan] Removed \"" + violationName + "\" override for " + player.getName());
            return true;
        }

        if (!args[2].equals("preserve")) {
            maxViolationLevel = Long.parseLong(args[2]);
        }

        long expirationSeconds = violationType.getLong("expiration");
        if (args.length >= 4) {
            if (!args[3].equals("preserve")) {
                expirationSeconds = Long.parseLong(args[3]);
            }
        }

        long banDuration = violationType.getLong("ban-duration");
        if (args.length >= 5) {
            if (!args[4].equals("preserve")) {
                banDuration = Long.parseLong(args[4]);
            }
        }

        String banMessage = violationType.getString("ban-message", "unset");
        if (args.length >= 6) {
            if (!args[5].equals("preserve")) {
                banMessage = Arrays.stream(args).skip(5).collect(Collectors.joining(" "));
            }
        }

        banMessage = banMessage.replaceAll("&","§");

        TempBan.config.set("violation-types." + violationName + ".overrides." + player.getName() + ".max-vl", maxViolationLevel);
        TempBan.config.set("violation-types." + violationName + ".overrides." + player.getName() + ".expiration", expirationSeconds);
        TempBan.config.set("violation-types." + violationName + ".overrides." + player.getName() + ".ban-duration", banDuration);
        TempBan.config.set("violation-types." + violationName + ".overrides." + player.getName() + ".ban-message", banMessage);

        plugin.setConfig(TempBan.config);

        sender.sendMessage("[TempBan] Successfully overrode violation type \"" + violationName + "\" for " + player.getName());
        sender.sendMessage(" -   Max VL: " + maxViolationLevel);
        sender.sendMessage(" -   Expiration: " + expirationSeconds);
        sender.sendMessage(" -   Ban Duration: " + banDuration);
        sender.sendMessage(" -   Ban Message: " + banMessage);

        return true;
    }
}
