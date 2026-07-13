package com.olziedev.skillranks.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Configuration {

    // Bump whenever config.yml gains/renames/removes a key in a way that
    // an admin's existing on-disk file needs migrating to pick up.
    public static final int CURRENT_CONFIG_VERSION = 2;

    private final JavaPlugin plugin;
    private static FileConfiguration config;

    public Configuration(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            plugin.saveResource(file.getName(), false);
        }

        config = YamlConfiguration.loadConfiguration(file);
        migrate(file);
    }

    private void migrate(File file) {
        int fileVersion = config.getInt("config-version", 1);
        if (fileVersion >= CURRENT_CONFIG_VERSION) return;

        backup(file, fileVersion);

        if (fileVersion < 2) {
            // "lang.no-ranking" was renamed to "lang.no-rank" (the code
            // always read the latter, so the old key was dead weight).
            if (config.contains("lang.no-ranking") && !config.contains("lang.no-rank")) {
                config.set("lang.no-rank", config.getString("lang.no-ranking"));
            }
            config.set("lang.no-ranking", null);
        }

        // Fill in any newly-introduced keys (auto-check.*, lang.help,
        // debug, ...) with their shipped defaults, without touching
        // anything the admin already has set (e.g. their ranks).
        try (InputStream defaultStream = plugin.getResource("config.yml")) {
            if (defaultStream != null) {
                FileConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                config.setDefaults(defaults);
                config.options().copyDefaults(true);
            }
        } catch (IOException ignored) {
        }

        config.set("config-version", CURRENT_CONFIG_VERSION);

        try {
            config.save(file);
            plugin.getLogger().info("Migrated config.yml from version " + fileVersion + " to " + CURRENT_CONFIG_VERSION
                    + ". Your previous config was backed up to config-v" + fileVersion + ".yml.bak "
                    + "(note: comments are not preserved by the migration save - refer to the backup or the "
                    + "freshly-documented config.yml for explanations of each option).");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save migrated config.yml: " + e.getMessage());
        }
    }

    private void backup(File file, int fileVersion) {
        File backup = new File(file.getParentFile(), "config-v" + fileVersion + ".yml.bak");
        try {
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to back up config.yml before migration: " + e.getMessage());
        }
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    public static String getString(ConfigurationSection section, String s) {
        if (section == null) return "";

        return section.getString(s, "");
    }

    public static String getString(YamlConfiguration config, String s) {
        return config.getString(s, "");
    }

    public static boolean isDebug() {
        return config.getBoolean("debug");
    }
}
