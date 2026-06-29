package com.Saalai.SalaiMusicApp.Models;

import java.io.Serializable;

public class Channel implements Serializable {
    private int channelId;
    private String channelName;
    private String channelLogo;
    private String channelURL;

    public Channel(int channelId, String channelName, String channelLogo, String channelURL) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelLogo = channelLogo;
        this.channelURL = channelURL;
    }

    public int getChannelId() {
        return channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getChannelLogo() {
        return channelLogo;
    }

    public String getChannelURL() {
        return channelURL;
    }
}
