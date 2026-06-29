package com.Saalai.SalaiMusicApp.Response;


public class LikeRequest {
    private String categoryId;


    public LikeRequest(String categoryId) {
        this.categoryId = categoryId;

    }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

}