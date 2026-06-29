package com.Saalai.SalaiMusicApp.Models;

import java.io.Serializable;

public class RadioModel implements Serializable {
    private int channelId;
    private String channelName;
    private String channelLogo;
    private String channelURL;

    // Constructor
    public RadioModel(int channelId, String channelName, String channelLogo, String channelURL) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelLogo = channelLogo;
        this.channelURL = channelURL;
    }



    // Getters and Setters
    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelLogo() {
        return channelLogo;
    }

    public void setChannelLogo(String channelLogo) {
        this.channelLogo = channelLogo;
    }

    public String getChannelURL() {
        return channelURL;
    }

    public void setChannelURL(String channelURL) {
        this.channelURL = channelURL;
    }
}
