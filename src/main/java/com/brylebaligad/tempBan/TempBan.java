package com.brylebaligad.tempBan;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

public final class TempBan extends JavaPlugin implements Listener {
    public static FileConfiguration config;

    public void setConfig(FileConfiguration newConfig) {
        config = newConfig;
        saveConfig();
        reloadConfig();
        config = getConfig();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("TempBan (c) Bryle Baligad, 2025");

        config = this.getConfig();
        config.addDefault("ban-duration", 30L);
        config.addDefault("ban-message", "You have been temporarily banned for {duration} seconds.");
        config.options().copyDefaults(true);
        saveConfig();

        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        // Repeating task to check for temporary ban
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, tempBanRunnable, 20L, 10L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, violationRunnable, 20L, 10L);
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        switch (command.getName()) {
            case "tempban" -> {
                return TempBan.command(this, sender, command, args);
            }

            case "violation" -> {
                return Violation.command(this, sender, command, args);
            }

            case "registerviolation" -> {
                return RegisterViolation.command(this, sender, command, args);
            }

            case "violationoverride" -> {
                return ViolationOverride.command(this, sender, command, args);
            }

            case "reload" -> {
                reloadConfig();
                config = getConfig();
                sender.sendMessage("[TempBan] Reloaded config");
                return true;
            }

            default -> {
                return true;
            }
        }
    }

    public static boolean command(TempBan plugin, CommandSender sender, Command command, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("[TempBan] Missing arguments!");
            return false;
        }

        Player playerToBan = Bukkit.getPlayerExact(args[0]);
        if (playerToBan == null) {
            sender.sendMessage("[TempBan] " + args[0] + " is not online");
            return true;
        }

        if (!playerToBan.isOnline()) {
            sender.sendMessage("[TempBan] " + args[0] + " is not online");
            return true;
        }

        long banDuration;
        if (args.length == 1) {
            banDuration = config.getLong("ban-duration", 30);
        } else {
            banDuration = Long.parseLong(args[1]);
        }

        String banMessage;
        if (args.length <= 2) {
            banMessage = config.getString("ban-message", "unset");
        } else {
            banMessage = args[2];
        }

        tempBan(plugin, playerToBan, banDuration, banMessage);

        sender.sendMessage("[TempBan] " + playerToBan + " is banned for " + banDuration + " seconds");

        return true;
    }

    public static void tempBan(TempBan plugin, Player playerToBan, long banDuration, String banMessage) {
        banMessage = banMessage.replaceAll("\\{duration}", String.valueOf((int) banDuration));
        banMessage = banMessage.replaceAll("\\\\n","\n");

        playerToBan.ban(banMessage, (Date) null, null, true);

        LocalDateTime dtNow = LocalDateTime.now();

        config.set("tempbans." + playerToBan.getName() + ".ban-date", dtNow.toString());
        config.set("tempbans." + playerToBan.getName() + ".ban-duration", banDuration);

        plugin.setConfig(config);
    }

    @SuppressWarnings("deprecation")
    Runnable tempBanRunnable = new Runnable() {
        @Override
        public void run() {
            LocalDateTime dtNow = LocalDateTime.now();
            ConfigurationSection tempbans = config.getConfigurationSection("tempbans");
            if (tempbans == null) return;
            Set<String> playerBans = tempbans.getKeys(false);

            for (String playerName : playerBans) {
                ConfigurationSection playerBan = config.getConfigurationSection("tempbans." + playerName);
                if (playerBan == null) continue;

                String banDate = playerBan.getString("ban-date");
                if (banDate == null) continue;

                // Ban duration is in seconds
                long banDuration = playerBan.getLong("ban-duration");
                LocalDateTime dtBan = LocalDateTime.parse(banDate);

                Duration banDelta = Duration.between(dtBan, dtNow);

                if (banDelta.toSeconds() >= banDuration) {
                    Bukkit.getBanList(BanList.Type.NAME).pardon(playerName);

                    config.set("tempbans." + playerName, null);

                    setConfig(config);
                    getLogger().info("Pardoned " + playerName + " after " + banDuration + " seconds");
                }
            }
        }
    };

    Runnable violationRunnable = new Runnable() {
        @Override
        public void run() {
            LocalDateTime dtNow = LocalDateTime.now();
            ConfigurationSection violations = config.getConfigurationSection("violations");
            if (violations == null) return;
            Set<String> playerViolations = violations.getKeys(false);

            for (String playerName : playerViolations) {
                ConfigurationSection playerViolation = config.getConfigurationSection("violations." + playerName);
                if (playerViolation == null) continue;

                Set<String> playerViolationTypes = playerViolation.getKeys(false);

                for (String violationName : playerViolationTypes) {
                    ConfigurationSection violationType = config.getConfigurationSection("violation-types." + violationName);
                    if (violationType == null) continue;

                    ConfigurationSection violationOverride = config.getConfigurationSection("violation-types." + violationName + ".overrides." + playerName);
                    if (violationOverride != null) {
                        violationType = violationOverride;
                    }

                    ConfigurationSection playerViolationType = playerViolation.getConfigurationSection(violationName);
                    if (playerViolationType == null) continue;

                    String lastExpiration = playerViolationType.getString("last-expiration");
                    if (lastExpiration == null) {
                        lastExpiration = dtNow.toString();
                        playerViolationType.set("last-expiration", lastExpiration);
                        setConfig(config);
                    }

                    long violationExpiration = violationType.getLong("expiration");

                    LocalDateTime dtExpiration = LocalDateTime.parse(lastExpiration);

                    Duration expirationDelta = Duration.between(dtExpiration, dtNow);

                    if (expirationDelta.toSeconds() >= violationExpiration) {
                        lastExpiration = dtNow.toString();
                        playerViolationType.set("last-expiration", lastExpiration);

                        int violationLevel = playerViolationType.getInt("count");
                        violationLevel--;
                        playerViolationType.set("count", violationLevel);

                        if (violationLevel <= 0) {
                            playerViolation.set(violationName, null);
                            getLogger().info("Violation \"" + violationName + "\" for " + playerName + " cleared");
                        }
                        setConfig(config);
                    }
                }
            }
        }
    };
}
