package com.Saalai.SalaiMusicApp.Models;

public class MovieItem {
    private int channelId;
    private String channelName;
    private String channelLogo;

    public MovieItem(int channelId, String channelName, String channelLogo) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelLogo = channelLogo;
    }

    public int getChannelId() { return channelId; }
    public String getChannelName() { return channelName; }
    public String getChannelLogo() { return channelLogo; }
}
