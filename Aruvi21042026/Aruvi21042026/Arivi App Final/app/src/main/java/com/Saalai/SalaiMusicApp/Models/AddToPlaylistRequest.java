package com.Saalai.SalaiMusicApp.Models;


public class AddToPlaylistRequest {
    private String categoryId;

    public AddToPlaylistRequest(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
}