package com.Saalai.SalaiMusicApp.Models;



public class ChildModelTvFragment {
    private String id;
    private String name;
    private String imageUrl;
    private String description; // optional, in case API returns it
    private String videoUrl;    // optional, if TV show has a video link

    public ChildModelTvFragment(String id, String name, String imageUrl) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public ChildModelTvFragment(String id, String name, String imageUrl, String description, String videoUrl) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.description = description;
        this.videoUrl = videoUrl;
    }

    // --- Getters & Setters ---
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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
}
