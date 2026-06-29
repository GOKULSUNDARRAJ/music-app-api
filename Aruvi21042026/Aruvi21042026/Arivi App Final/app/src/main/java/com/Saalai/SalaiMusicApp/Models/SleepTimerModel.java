package com.Saalai.SalaiMusicApp.Models;

public class SleepTimerModel {
    private int minutes;
    private String displayName;
    private boolean isSelected;

    public SleepTimerModel(int minutes, String displayName) {
        this.minutes = minutes;
        this.displayName = displayName;
        this.isSelected = false;
    }

    public int getMinutes() { return minutes; }
    public String getDisplayName() { return displayName; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}