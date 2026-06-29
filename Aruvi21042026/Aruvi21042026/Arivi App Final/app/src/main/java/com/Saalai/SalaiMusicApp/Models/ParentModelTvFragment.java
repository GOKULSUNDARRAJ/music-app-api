package com.Saalai.SalaiMusicApp.Models;

import java.util.List;

public class ParentModelTvFragment {
    private String categoryId;
    private String categoryName;
    private List<ChildModelTvFragment> childList;

    public ParentModelTvFragment(String categoryId, String categoryName, List<ChildModelTvFragment> childList) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.childList = childList;
    }

    // --- Getters & Setters ---
    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<ChildModelTvFragment> getChildList() {
        return childList;
    }

    public void setChildList(List<ChildModelTvFragment> childList) {
        this.childList = childList;
    }
}
