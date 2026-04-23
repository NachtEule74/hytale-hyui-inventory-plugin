package com.nachteuele74.hytale.inventory.data;

import com.google.gson.annotations.SerializedName;

/**
 * Player inventory data with page states.
 */
public class PlayerInventoryData {
    @SerializedName("uuid")
    private String uuid;
    
    @SerializedName("playerLevel")
    private int playerLevel = 1;
    
    @SerializedName("currentPage")
    private int currentPage = 1;
    
    @SerializedName("page1Unlocked")
    private boolean page1Unlocked = true;
    
    @SerializedName("page2Unlocked")
    private boolean page2Unlocked = false;
    
    @SerializedName("lastUpdated")
    private long lastUpdated = System.currentTimeMillis();

    public PlayerInventoryData(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public int getPlayerLevel() {
        return playerLevel;
    }

    public void setPlayerLevel(int playerLevel) {
        this.playerLevel = playerLevel;
        this.lastUpdated = System.currentTimeMillis();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public boolean isPage1Unlocked() {
        return page1Unlocked;
    }

    public boolean isPage2Unlocked() {
        return page2Unlocked;
    }

    public void unlockPage2() {
        this.page2Unlocked = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public long getLastUpdated() {
        return lastUpdated;
    }
}