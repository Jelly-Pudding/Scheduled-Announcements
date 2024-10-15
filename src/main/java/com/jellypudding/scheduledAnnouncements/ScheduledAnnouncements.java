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
    private NamedTextColor globalMessageColor;

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
                String prefixColorName = prefixSection.getString("color", "gold");
                globalPrefixColor = NamedTextColor.NAMES.value(prefixColorName.toLowerCase());
                if (globalPrefixColor == null) globalPrefixColor = NamedTextColor.GOLD;
            }
            String messageColorName = settingsSection.getString("message_color", "yellow");
            globalMessageColor = NamedTextColor.NAMES.value(messageColorName.toLowerCase());
            if (globalMessageColor == null) globalMessageColor = NamedTextColor.YELLOW;
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

                        // Load announcement-specific settings
                        String prefixText = globalPrefixText;
                        NamedTextColor prefixColor = globalPrefixColor;
                        NamedTextColor messageColor = globalMessageColor;
                        ConfigurationSection annSection = announcementsSection.getConfigurationSection(key);
                        if (annSection != null) {
                            ConfigurationSection annPrefixSection = annSection.getConfigurationSection("prefix");
                            if (annPrefixSection != null) {
                                prefixText = annPrefixSection.getString("text", globalPrefixText);
                                String annPrefixColorName = annPrefixSection.getString("color");
                                if (annPrefixColorName != null) {
                                    NamedTextColor annPrefixColor = NamedTextColor.NAMES.value(annPrefixColorName.toLowerCase());
                                    if (annPrefixColor != null) prefixColor = annPrefixColor;
                                }
                            }
                            String annMessageColorName = annSection.getString("message_color");
                            if (annMessageColorName != null) {
                                NamedTextColor annMessageColor = NamedTextColor.NAMES.value(annMessageColorName.toLowerCase());
                                if (annMessageColor != null) messageColor = annMessageColor;
                            }
                        }

                        announcements.put(key, new AnnouncementConfig(message, executionTime, prefixText, prefixColor, messageColor));
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

        Component contentComponent = Component.text(parts[0].trim(), config.messageColor);

        if (parts.length > 1) {
            String url = parts[1].trim();
            contentComponent = contentComponent.clickEvent(ClickEvent.openUrl(url));
        }

        message = message.append(contentComponent);

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
        NamedTextColor messageColor;

        AnnouncementConfig(String message, ExecutionTime executionTime, String prefixText, NamedTextColor prefixColor, NamedTextColor messageColor) {
            this.message = message;
            this.executionTime = executionTime;
            this.prefixText = prefixText;
            this.prefixColor = prefixColor;
            this.messageColor = messageColor;
        }
    }
}