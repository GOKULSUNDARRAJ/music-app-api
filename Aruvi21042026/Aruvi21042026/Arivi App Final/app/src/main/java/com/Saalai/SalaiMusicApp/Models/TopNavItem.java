package com.Saalai.SalaiMusicApp.Models;

import com.google.gson.annotations.SerializedName;

public class TopNavItem {
    @SerializedName("topmenuId")
    private int topmenuId;

    @SerializedName("topmenuName")
    private String topmenuName;

    @SerializedName("topmenuStatusId")
    private int topmenuStatusId;

    @SerializedName("topmenuStatus")
    private String topmenuStatus;

    @SerializedName("topmenuActiveIcon")
    private String topmenuActiveIcon;

    @SerializedName("topmenuInActiveIcon")
    private String topmenuInActiveIcon;

    @SerializedName("is_active") // If this field exists in your API
    private boolean isActive;

    // Getters and Setters
    public int getTopmenuId() {
        return topmenuId;
    }

    public void setTopmenuId(int topmenuId) {
        this.topmenuId = topmenuId;
    }

    public String getTopmenuName() {
        return topmenuName;
    }

    public void setTopmenuName(String topmenuName) {
        this.topmenuName = topmenuName;
    }

    public int getTopmenuStatusId() {
        return topmenuStatusId;
    }

    public void setTopmenuStatusId(int topmenuStatusId) {
        this.topmenuStatusId = topmenuStatusId;
    }

    public String getTopmenuStatus() {
        return topmenuStatus;
    }

    public void setTopmenuStatus(String topmenuStatus) {
        this.topmenuStatus = topmenuStatus;
    }

    public String getTopmenuActiveIcon() {
        return topmenuActiveIcon;
    }

    public void setTopmenuActiveIcon(String topmenuActiveIcon) {
        this.topmenuActiveIcon = topmenuActiveIcon;
    }

    public String getTopmenuInActiveIcon() {
        return topmenuInActiveIcon;
    }

    public void setTopmenuInActiveIcon(String topmenuInActiveIcon) {
        this.topmenuInActiveIcon = topmenuInActiveIcon;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}