package com.nachteuele74.hytale.inventory.config;

import com.google.gson.annotations.SerializedName;

/**
 * POJO for plugin configuration loaded from config.json
 */
public class PluginConfig {
    @SerializedName("unlockLevel")
    private int unlockLevel = 20;
    
    @SerializedName("itemsPerPage")
    private int itemsPerPage = 20;
    
    @SerializedName("storage")
    private StorageConfig storage = new StorageConfig();
    
    @SerializedName("threading")
    private ThreadingConfig threading = new ThreadingConfig();

    public int getUnlockLevel() {
        return unlockLevel;
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    public StorageConfig getStorage() {
        return storage;
    }

    public ThreadingConfig getThreading() {
        return threading;
    }

    public static class StorageConfig {
        @SerializedName("autoSaveInterval")
        private long autoSaveInterval = 30000;

        public long getAutoSaveInterval() {
            return autoSaveInterval;
        }
    }

    public static class ThreadingConfig {
        @SerializedName("threadPoolSize")
        private int threadPoolSize = 4;
        
        @SerializedName("enableAsyncEvents")
        private boolean enableAsyncEvents = true;

        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        public boolean isEnableAsyncEvents() {
            return enableAsyncEvents;
        }
    }
}