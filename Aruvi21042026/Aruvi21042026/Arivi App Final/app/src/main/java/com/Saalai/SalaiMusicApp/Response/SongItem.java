package com.Saalai.SalaiMusicApp.Models;

public class SongItem {
    private String songId;
    private String audioName;
    private String audioUrl;
    private String category;
    private String imageUrl;
    private String categoryId;
    private boolean isLiked;

    public String getSongId() { return songId; }
    public void setSongId(String songId) { this.songId = songId; }

    public String getAudioName() { return audioName; }
    public void setAudioName(String audioName) { this.audioName = audioName; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }

    // Convert to AudioModel
    public AudioModel toAudioModel() {
        AudioModel audio = new AudioModel();
        audio.setSongId(songId);
        audio.setAudioName(audioName);
        audio.setAudioUrl(audioUrl);
        audio.setcategoryName(category);
        audio.setCategoryId(categoryId != null ? categoryId : this.categoryId);
        audio.setImageUrl(imageUrl);
        return audio;
    }
}