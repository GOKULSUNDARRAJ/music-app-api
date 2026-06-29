package com.Saalai.SalaiMusicApp.Models;

public class SliderItemTv {
    private int channelId;   // new field
    private String imageUrl;
    private String title;

    public SliderItemTv(int channelId, String imageUrl) {
        this.channelId = channelId;
        this.imageUrl = imageUrl;
    }

    public SliderItemTv(int channelId, String imageUrl, String title) {
        this.channelId = channelId;
        this.imageUrl = imageUrl;
        this.title = title;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
