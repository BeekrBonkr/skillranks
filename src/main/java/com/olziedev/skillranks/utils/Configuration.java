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
    public static final int CURRENT_CONFIG_VERSION = 3;

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

        FileConfiguration defaults = null;
        try (InputStream defaultStream = plugin.getResource("config.yml")) {
            if (defaultStream != null) {
                defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
        }

        applyMigrations(config, defaults);

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

    // Static so migration logic can be exercised without a running server.
    static void applyMigrations(FileConfiguration config, FileConfiguration defaults) {
        int fileVersion = config.getInt("config-version", 1);
        if (fileVersion >= CURRENT_CONFIG_VERSION) return;

        if (fileVersion < 2) {
            // "lang.no-ranking" was renamed to "lang.no-rank" (the code
            // always read the latter, so the old key was dead weight).
            if (config.contains("lang.no-ranking") && !config.contains("lang.no-rank")) {
                config.set("lang.no-rank", config.getString("lang.no-ranking"));
            }
            config.set("lang.no-ranking", null);
        }

        if (fileVersion < 3) {
            // The single global "placeholder-to-listen-for" became a
            // per-tree "placeholder" key, so each rank tree can track its
            // own stat. Existing trees inherit the old global value.
            String globalPlaceholder = config.getString("placeholder-to-listen-for");
            ConfigurationSection ranks = config.getConfigurationSection("ranks");
            if (ranks != null && globalPlaceholder != null && !globalPlaceholder.isEmpty()) {
                for (String key : ranks.getKeys(false)) {
                    if (!ranks.isConfigurationSection(key)) continue;
                    if (!ranks.contains(key + ".placeholder")) {
                        ranks.set(key + ".placeholder", globalPlaceholder);
                    }
                }
            }
            config.set("placeholder-to-listen-for", null);
        }

        copyMissingDefaults(config, defaults);
        config.set("config-version", CURRENT_CONFIG_VERSION);
    }

    /**
     * Fills in newly-introduced keys (auto-check.*, lang.*, debug, ...)
     * with their shipped defaults without touching anything the admin
     * already has set. The `ranks` section is deliberately excluded: the
     * shipped example trees must never be merged into a customized config.
     */
    private static void copyMissingDefaults(FileConfiguration config, FileConfiguration defaults) {
        if (defaults == null) return;

        for (String key : defaults.getKeys(true)) {
            if (key.equals("ranks") || key.startsWith("ranks.")) continue;
            if (defaults.isConfigurationSection(key)) continue;
            if (!config.contains(key)) {
                config.set(key, defaults.get(key));
            }
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

    public static boolean isDebug() {
        return config.getBoolean("debug");
    }
}
