package com.Saalai.SalaiMusicApp.Models;

import java.util.List;

public class TvCategory {
    private int categoryId;
    private String categoryName;
    private List<TvChannel> channels;

    // getters and setters
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public List<TvChannel> getChannels() { return channels; }
    public void setChannels(List<TvChannel> channels) { this.channels = channels; }
}
