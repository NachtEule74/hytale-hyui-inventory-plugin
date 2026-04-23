package com.nachteuele74.hytale.inventory.event;

import com.hypixel.hytale.logger.HytaleLogger;
import com.nachteuele74.hytale.inventory.HytaleInventoryPlugin;
import com.nachteuele74.hytale.inventory.config.ConfigManager;
import com.nachteuele74.hytale.inventory.data.PlayerInventoryData;
import com.nachteuele74.hytale.inventory.data.PlayerInventoryDataManager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles player-related events asynchronously.
 */
public class PlayerEventListener {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    private final HytaleInventoryPlugin plugin;
    private final PlayerInventoryDataManager dataManager;
    private final ConfigManager configManager;
    private final ConcurrentHashMap<String, Long> lastSave;

    public PlayerEventListener(HytaleInventoryPlugin plugin, PlayerInventoryDataManager dataManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.configManager = configManager;
        this.lastSave = new ConcurrentHashMap<>();
        LOGGER.atInfo().log("PlayerEventListener initialized");
    }

    /**
     * Handles player join event.
     */
    public void onPlayerJoin(String playerUUID) {
        dataManager.getPlayerDataAsync(playerUUID)
                .thenAccept(data -> {
                    int unlockLevel = configManager.getConfig().getUnlockLevel();
                    if (data.getPlayerLevel() >= unlockLevel && !data.isPage2Unlocked()) {
                        data.unlockPage2();
                        dataManager.savePlayerData(playerUUID);
                    }
                    LOGGER.atInfo().log("Player " + playerUUID + " joined. Level: " + data.getPlayerLevel());
                })
                .exceptionally(e -> {
                    LOGGER.atSevere().withCause(e).log("Error handling player join");
                    return null;
                });
    }

    /**
     * Handles player level up event.
     */
    public void onPlayerLevelUp(String playerUUID, int newLevel) {
        dataManager.updatePlayerLevelAsync(playerUUID, newLevel)
                .thenRun(() -> {
                    int unlockLevel = configManager.getConfig().getUnlockLevel();
                    if (newLevel >= unlockLevel) {
                        PlayerInventoryData data = dataManager.getPlayerData(playerUUID);
                        if (!data.isPage2Unlocked()) {
                            data.unlockPage2();
                            dataManager.savePlayerData(playerUUID);
                            LOGGER.atInfo().log("Player " + playerUUID + " unlocked Page 2 at level " + newLevel);
                        }
                    }
                })
                .exceptionally(e -> {
                    LOGGER.atSevere().withCause(e).log("Error handling level up");
                    return null;
                });
    }

    /**
     * Handles player disconnect event.
     */
    public void onPlayerDisconnect(String playerUUID) {
        dataManager.savePlayerDataAsync(playerUUID)
                .thenRun(() -> {
                    lastSave.remove(playerUUID);
                    LOGGER.atInfo().log("Player " + playerUUID + " disconnected. Data saved.");
                })
                .exceptionally(e -> {
                    LOGGER.atSevere().withCause(e).log("Error saving player data on disconnect");
                    return null;
                });
    }
}