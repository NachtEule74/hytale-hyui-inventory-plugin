package com.nachteuele74.hytale.inventory;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.nachteuele74.hytale.inventory.command.InventoryCommand;
import com.nachteuele74.hytale.inventory.config.ConfigManager;
import com.nachteuele74.hytale.inventory.data.PlayerInventoryDataManager;
import com.nachteuele74.hytale.inventory.event.PlayerEventListener;

import javax.annotation.Nonnull;

/**
 * Main plugin entry point for the Hytale Inventory Plugin.
 * Manages initialization, command registration, and event listeners.
 */
public class HytaleInventoryPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private ConfigManager configManager;
    private PlayerInventoryDataManager dataManager;
    private PlayerEventListener eventListener;

    public HytaleInventoryPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Initializing " + this.getName() + " v" + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Starting plugin setup...");
        try {
            // Initialize configuration
            this.configManager = new ConfigManager();
            configManager.loadConfig();
            LOGGER.atInfo().log("✓ Config loaded successfully");

            // Initialize data manager with thread pool
            this.dataManager = new PlayerInventoryDataManager(
                    configManager.getConfig().getThreading().getThreadPoolSize()
            );
            LOGGER.atInfo().log("✓ Data manager initialized");

            // Setup event listeners
            this.eventListener = new PlayerEventListener(this, dataManager, configManager);
            LOGGER.atInfo().log("✓ Event listeners registered");

            // Register commands
            this.getCommandRegistry().registerCommand(
                    new InventoryCommand(this, dataManager, configManager)
            );
            LOGGER.atInfo().log("✓ Commands registered");
            
            LOGGER.atInfo().log("✓ Plugin successfully loaded!");
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("Failed to initialize plugin!");
            throw new RuntimeException("Plugin initialization failed", e);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerInventoryDataManager getDataManager() {
        return dataManager;
    }
}
