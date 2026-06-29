package com.Saalai.SalaiMusicApp.Models;

public class SliderItemForMovie {
    private String imageUrl;
    private String title;
    private String description;
    private String videoUrl;
    private String channelId; // <-- add this

    public SliderItemForMovie(String imageUrl, String title, String description, String videoUrl, String channelId) {
        this.imageUrl = imageUrl;
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
}
