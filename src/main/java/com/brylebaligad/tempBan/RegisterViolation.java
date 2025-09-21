package com.brylebaligad.tempBan;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.stream.Collectors;

public class RegisterViolation {
    public static boolean command(TempBan plugin, CommandSender sender, Command command, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("[TempBan] Missing arguments!");
            return false;
        }

        String violationName = args[0];
        int maxViolationLevel = Integer.parseInt(args[1]);
        int expirationSeconds = Integer.parseInt(args[2]);

        int banDuration = 0;
        if (args.length >= 4) {
            String banDurationString = args[3];
            if (banDurationString.equals("default")) {
                banDuration = TempBan.config.getInt("ban-duration");
            } else {
                banDuration = Integer.parseInt(banDurationString);
            }
        }


        String banMessage;
        if (args.length >= 5) {
            banMessage = Arrays.stream(args).skip(4).collect(Collectors.joining(" "));
        } else {
            banMessage = TempBan.config.getString("ban-message", "unset");
        }

        banMessage = banMessage.replaceAll("&","§");

        TempBan.config.set("violation-types." + violationName + ".max-vl", maxViolationLevel);
        TempBan.config.set("violation-types." + violationName + ".expiration", expirationSeconds);
        TempBan.config.set("violation-types." + violationName + ".ban-duration", banDuration);
        TempBan.config.set("violation-types." + violationName + ".ban-message", banMessage);

        plugin.setConfig(TempBan.config);

        sender.sendMessage("[TempBan] Successfully registered violation type \"" + violationName + "\"");
        sender.sendMessage(" -   Max VL: " + maxViolationLevel);
        sender.sendMessage(" -   Expiration: " + expirationSeconds);
        sender.sendMessage(" -   Ban Duration: " + banDuration);
        sender.sendMessage(" -   Ban Message: " + banMessage);

        return true;
    }
}
