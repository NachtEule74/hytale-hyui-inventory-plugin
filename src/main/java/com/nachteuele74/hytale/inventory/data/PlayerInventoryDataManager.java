package com.nachteuele74.hytale.inventory.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe manager for player inventory data with async operations.
 */
public class PlayerInventoryDataManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String DATA_DIR = "plugins/HytaleInventoryPlugin/playerdata/";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ExecutorService threadPool;
    private final ConcurrentHashMap<String, PlayerInventoryData> cache;
    private final ConcurrentHashMap<String, ReadWriteLock> playerLocks;

    public PlayerInventoryDataManager(int threadPoolSize) {
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
        this.cache = new ConcurrentHashMap<>();
        this.playerLocks = new ConcurrentHashMap<>();
        ensureDataDirectory();
        LOGGER.atInfo().log("PlayerInventoryDataManager initialized with " + threadPoolSize + " threads");
    }

    /**
     * Gets player data asynchronously.
     */
    public CompletableFuture<PlayerInventoryData> getPlayerDataAsync(String uuid) {
        return CompletableFuture.supplyAsync(() -> getPlayerData(uuid), threadPool);
    }

    /**
     * Gets or creates player data with read lock.
     */
    public PlayerInventoryData getPlayerData(String uuid) {
        ReadWriteLock lock = playerLocks.computeIfAbsent(uuid, k -> new ReentrantReadWriteLock());
        lock.readLock().lock();
        try {
            // Check cache first
            if (cache.containsKey(uuid)) {
                return cache.get(uuid);
            }
        } finally {
            lock.readLock().unlock();
        }

        // Load from file if not cached
        lock.writeLock().lock();
        try {
            PlayerInventoryData data = loadFromFile(uuid);
            if (data == null) {
                data = new PlayerInventoryData(uuid);
            }
            cache.put(uuid, data);
            return data;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Saves player data asynchronously.
     */
    public CompletableFuture<Void> savePlayerDataAsync(String uuid) {
        return CompletableFuture.runAsync(() -> savePlayerData(uuid), threadPool);
    }

    /**
     * Saves player data with write lock.
     */
    public void savePlayerData(String uuid) {
        ReadWriteLock lock = playerLocks.get(uuid);
        if (lock == null) return;

        lock.readLock().lock();
        try {
            PlayerInventoryData data = cache.get(uuid);
            if (data != null) {
                saveToFile(data);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Updates player level.
     */
    public CompletableFuture<Void> updatePlayerLevelAsync(String uuid, int newLevel) {
        return CompletableFuture.runAsync(() -> {
            ReadWriteLock lock = playerLocks.get(uuid);
            if (lock == null) return;

            lock.writeLock().lock();
            try {
                PlayerInventoryData data = cache.get(uuid);
                if (data != null) {
                    data.setPlayerLevel(newLevel);
                    LOGGER.atInfo().log("Player " + uuid + " level updated to " + newLevel);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }, threadPool);
    }

    private PlayerInventoryData loadFromFile(String uuid) {
        try {
            File file = new File(DATA_DIR + uuid + ".json");
            if (!file.exists()) {
                return null;
            }
            try (FileReader reader = new FileReader(file)) {
                return GSON.fromJson(reader, PlayerInventoryData.class);
            }
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Error loading player data");
            return null;
        }
    }

    private void saveToFile(PlayerInventoryData data) {
        try {
            File file = new File(DATA_DIR + data.getUuid() + ".json");
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Error saving player data");
        }
    }

    private void ensureDataDirectory() {
        File dir = new File(DATA_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            LOGGER.atSevere().log("Failed to create data directory");
        }
    }

    public void shutdown() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}