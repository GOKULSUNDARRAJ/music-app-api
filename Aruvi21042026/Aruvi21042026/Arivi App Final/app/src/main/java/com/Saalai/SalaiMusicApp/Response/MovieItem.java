package com.Saalai.SalaiMusicApp.Response;


import com.google.gson.annotations.SerializedName;

public class MovieItem {

    @SerializedName("channelId")
    private int channelId;

    @SerializedName("channelName")
    private String channelName;

    @SerializedName("channelLogo")
    private String channelLogo;

    // Constructor
    public MovieItem(int channelId, String channelName, String channelLogo) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelLogo = channelLogo;
    }

    // Getters and setters
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
}