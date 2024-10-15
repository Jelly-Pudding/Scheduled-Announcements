package com.jellypudding.scheduledAnnouncements;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.util.*;

public class ScheduledAnnouncements extends JavaPlugin implements TabCompleter {

    private Map<String, AnnouncementConfig> announcements;
    private CronParser cronParser;
    private String globalPrefixText;
    private NamedTextColor globalPrefixColor;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        announcements = new HashMap<>();
        loadConfig();
        startAnnouncementTask();
        Objects.requireNonNull(getCommand("scheduledannouncements")).setExecutor(this);
        Objects.requireNonNull(getCommand("scheduledannouncements")).setTabCompleter(this);
        getLogger().info("ScheduledAnnouncements plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("ScheduledAnnouncements plugin has been disabled.");
    }

    private void loadConfig() {
        reloadConfig();
        announcements.clear();
        FileConfiguration config = getConfig();

        // Load global settings
        ConfigurationSection settingsSection = config.getConfigurationSection("settings");
        if (settingsSection != null) {
            ConfigurationSection prefixSection = settingsSection.getConfigurationSection("prefix");
            if (prefixSection != null) {
                globalPrefixText = prefixSection.getString("text", "[Announcement]");
                String colorName = prefixSection.getString("color", "gold");
                globalPrefixColor = NamedTextColor.NAMES.value(colorName.toLowerCase());
                if (globalPrefixColor == null) globalPrefixColor = NamedTextColor.GOLD;
            }
        }

        ConfigurationSection announcementsSection = config.getConfigurationSection("announcements");
        if (announcementsSection != null) {
            for (String key : announcementsSection.getKeys(false)) {
                String cronExpression = announcementsSection.getString(key + ".cron");
                String message = announcementsSection.getString(key + ".message");
                if (cronExpression != null && message != null) {
                    try {
                        Cron cron = cronParser.parse(cronExpression);
                        ExecutionTime executionTime = ExecutionTime.forCron(cron);

                        // Load announcement-specific prefix settings
                        String prefixText = globalPrefixText;
                        NamedTextColor prefixColor = globalPrefixColor;
                        ConfigurationSection annPrefixSection = announcementsSection.getConfigurationSection(key + ".prefix");
                        if (annPrefixSection != null) {
                            prefixText = annPrefixSection.getString("text", globalPrefixText);
                            String annColorName = annPrefixSection.getString("color");
                            if (annColorName != null) {
                                NamedTextColor annColor = NamedTextColor.NAMES.value(annColorName.toLowerCase());
                                if (annColor != null) prefixColor = annColor;
                            }
                        }

                        announcements.put(key, new AnnouncementConfig(message, executionTime, prefixText, prefixColor));
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Invalid cron expression for announcement: " + key);
                    }
                }
            }
        }
    }

    private void startAnnouncementTask() {
        Bukkit.getScheduler().runTaskTimer(this, this::checkAndBroadcastAnnouncements, 20L, 20L); // Check every second
    }

    private void checkAndBroadcastAnnouncements() {
        ZonedDateTime now = ZonedDateTime.now();
        for (Map.Entry<String, AnnouncementConfig> entry : announcements.entrySet()) {
            if (entry.getValue().executionTime.isMatch(now)) {
                broadcastAnnouncement(entry.getValue());
            }
        }
    }

    private void broadcastAnnouncement(AnnouncementConfig config) {
        Component message = parseAnnouncement(config);
        Bukkit.getServer().broadcast(message);
    }

    private Component parseAnnouncement(AnnouncementConfig config) {
        String[] parts = config.message.split("\\|");
        Component message = Component.empty();

        if (!config.prefixText.isEmpty()) {
            message = message.append(Component.text(config.prefixText + " ", config.prefixColor));
        }

        message = message.append(Component.text(parts[0].trim(), NamedTextColor.YELLOW));

        if (parts.length > 1) {
            message = message.append(Component.text(" [Click here]", NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.openUrl(parts[1].trim())));
        }

        return message;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("scheduledannouncements")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("scheduledannouncements.reload")) {
                    loadConfig();
                    sender.sendMessage(Component.text("ScheduledAnnouncements config reloaded!", NamedTextColor.GREEN));
                    return true;
                } else {
                    sender.sendMessage(Component.text("You don't have permission to reload the config.", NamedTextColor.RED));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("scheduledannouncements")) {
            if (args.length == 1) {
                completions.add("reload");
            }
        }
        return completions;
    }

    private static class AnnouncementConfig {
        String message;
        ExecutionTime executionTime;
        String prefixText;
        NamedTextColor prefixColor;

        AnnouncementConfig(String message, ExecutionTime executionTime, String prefixText, NamedTextColor prefixColor) {
            this.message = message;
            this.executionTime = executionTime;
            this.prefixText = prefixText;
            this.prefixColor = prefixColor;
        }
    }
}