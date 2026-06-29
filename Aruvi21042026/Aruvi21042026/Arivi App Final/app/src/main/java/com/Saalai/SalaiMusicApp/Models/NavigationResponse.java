package com.Saalai.SalaiMusicApp.Models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NavigationResponse {
    @SerializedName("status")
    private boolean status;

    @SerializedName("message")
    private String message;

    @SerializedName("topMenu")
    private List<TopNavItem> topMenu;

    @SerializedName("bottomMenu")
    private List<BottomNavItem> bottomMenu;

    // Getters and Setters
    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<TopNavItem> getTopMenu() {
        return topMenu;
    }

    public void setTopMenu(List<TopNavItem> topMenu) {
        this.topMenu = topMenu;
    }

    public List<BottomNavItem> getBottomMenu() {
        return bottomMenu;
    }

    public void setBottomMenu(List<BottomNavItem> bottomMenu) {
        this.bottomMenu = bottomMenu;
    }
}