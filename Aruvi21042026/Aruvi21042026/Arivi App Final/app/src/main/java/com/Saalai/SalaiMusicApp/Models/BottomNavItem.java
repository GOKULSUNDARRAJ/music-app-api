package com.Saalai.SalaiMusicApp.Models;

public class BottomNavItem {
    private int bottommenuId;
    private String bottommenuName;
    private int bottommenuStatusId;
    private String bottommenuStatus;
    private String bottommenuActiveIcon;
    private String bottommenuInActiveIcon;
    private boolean isSelected = false;

    // Constructor
    public BottomNavItem(int bottommenuId, String bottommenuName,
                         int bottommenuStatusId, String bottommenuStatus,
                         String bottommenuActiveIcon, String bottommenuInActiveIcon) {
        this.bottommenuId = bottommenuId;
        this.bottommenuName = bottommenuName;
        this.bottommenuStatusId = bottommenuStatusId;
        this.bottommenuStatus = bottommenuStatus;
        this.bottommenuActiveIcon = bottommenuActiveIcon;
        this.bottommenuInActiveIcon = bottommenuInActiveIcon;
    }

    // Getters and Setters
    public int getBottommenuId() { return bottommenuId; }
    public void setBottommenuId(int bottommenuId) { this.bottommenuId = bottommenuId; }

    public String getBottommenuName() { return bottommenuName; }
    public void setBottommenuName(String bottommenuName) { this.bottommenuName = bottommenuName; }

    public int getBottommenuStatusId() { return bottommenuStatusId; }
    public void setBottommenuStatusId(int bottommenuStatusId) { this.bottommenuStatusId = bottommenuStatusId; }

    public String getBottommenuStatus() { return bottommenuStatus; }
    public void setBottommenuStatus(String bottommenuStatus) { this.bottommenuStatus = bottommenuStatus; }

    public String getBottommenuActiveIcon() { return bottommenuActiveIcon; }
    public void setBottommenuActiveIcon(String bottommenuActiveIcon) { this.bottommenuActiveIcon = bottommenuActiveIcon; }

    public String getBottommenuInActiveIcon() { return bottommenuInActiveIcon; }
    public void setBottommenuInActiveIcon(String bottommenuInActiveIcon) { this.bottommenuInActiveIcon = bottommenuInActiveIcon; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    public boolean isActive() { return bottommenuStatusId == 1; }
}