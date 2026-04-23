package com.nachteuele74.hytale.inventory.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages JSON-based configuration loading and saving with thread-safe access.
 */
public class ConfigManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String CONFIG_PATH = "plugins/HytaleInventoryPlugin/config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private PluginConfig config;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Loads configuration from JSON file with thread-safe access.
     */
    public void loadConfig() {
        lock.writeLock().lock();
        try {
            File configFile = new File(CONFIG_PATH);
            if (!configFile.exists()) {
                createDefaultConfig();
                return;
            }

            try (FileReader reader = new FileReader(configFile)) {
                config = GSON.fromJson(reader, PluginConfig.class);
                if (config == null) {
                    config = new PluginConfig();
                }
                LOGGER.atInfo().log("Config loaded from: " + CONFIG_PATH);
            } catch (IOException e) {
                LOGGER.atError().withThrowable(e).log("Error reading config file");
                config = new PluginConfig();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Saves configuration to JSON file with thread-safe access.
     */
    public void saveConfig() {
        lock.readLock().lock();
        try {
            File configDir = new File(CONFIG_PATH).getParentFile();
            if (!configDir.exists() && !configDir.mkdirs()) {
                LOGGER.atError().log("Failed to create config directory");
                return;
            }

            try (FileWriter writer = new FileWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
                LOGGER.atInfo().log("Config saved to: " + CONFIG_PATH);
            } catch (IOException e) {
                LOGGER.atError().withThrowable(e).log("Error writing config file");
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Creates default configuration file.
     */
    private void createDefaultConfig() {
        config = new PluginConfig();
        File configDir = new File(CONFIG_PATH).getParentFile();
        if (!configDir.exists() && !configDir.mkdirs()) {
            LOGGER.atError().log("Failed to create config directory");
            return;
        }
        saveConfig();
        LOGGER.atInfo().log("Default config created");
    }

    /**
     * Gets the current configuration with read lock.
     */
    public PluginConfig getConfig() {
        lock.readLock().lock();
        try {
            return config != null ? config : new PluginConfig();
        } finally {
            lock.readLock().unlock();
        }
    }
}