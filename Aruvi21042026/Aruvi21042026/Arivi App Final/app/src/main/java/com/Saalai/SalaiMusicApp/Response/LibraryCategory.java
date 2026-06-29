package com.Saalai.SalaiMusicApp.Response;


import com.Saalai.SalaiMusicApp.Models.SongItem;

import java.util.List;

public class LibraryCategory {
    private String categoryId;
    private String categoryName;
    private String categoryImage;
    private int adapterType;
    private boolean isPlaylist;
    private List<SongItem> songs;

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryImage() { return categoryImage; }
    public void setCategoryImage(String categoryImage) { this.categoryImage = categoryImage; }

    public int getAdapterType() { return adapterType; }
    public void setAdapterType(int adapterType) { this.adapterType = adapterType; }

    public boolean isIsPlaylist() { return isPlaylist; }
    public void setIsPlaylist(boolean isPlaylist) { this.isPlaylist = isPlaylist; }

    public List<SongItem> getSongs() { return songs; }
    public void setSongs(List<SongItem> songs) { this.songs = songs; }
}