package com.Saalai.SalaiMusicApp.Models;

import java.util.List;

public class ParentItemMovieFragment {
    private int categoryId;
    private String sectionTitle;
    private String type;
    private List<ChildItemMovieFragment> childItemList;

    public ParentItemMovieFragment(int categoryId, String sectionTitle, String type, List<ChildItemMovieFragment> childItemList) {
        this.categoryId = categoryId;
        this.sectionTitle = sectionTitle;
        this.type = type;
        this.childItemList = childItemList;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<ChildItemMovieFragment> getChildItemList() {
        return childItemList;
    }

    public void setChildItemList(List<ChildItemMovieFragment> childItemList) {
        this.childItemList = childItemList;
    }
}
