package com.Saalai.SalaiMusicApp.Models;

import java.io.Serializable;

public class MovieRelatedChannel implements Serializable {

    private String channelId;
    private String channelName;
    private String channelLogo;

    public MovieRelatedChannel() {
        // Default constructor
    }

    public MovieRelatedChannel(String channelId, String channelName, String channelLogo) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelLogo = channelLogo;
    }

    // Getters and Setters
    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
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
}
