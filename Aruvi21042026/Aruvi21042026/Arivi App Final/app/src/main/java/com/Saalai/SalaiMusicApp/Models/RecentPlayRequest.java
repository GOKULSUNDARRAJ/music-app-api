package com.Saalai.SalaiMusicApp.Models;


public class RecentPlayRequest {
    private String categoryId;

    public RecentPlayRequest(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
}