package com.Saalai.SalaiMusicApp.Response;

import java.util.List;

public class LibrarySection {
    private String sectionId;
    private String sectionTitle;
    private int layoutType;
    private int spanCount;
    private List<LibraryCategory> categories;

    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }

    public String getSectionTitle() { return sectionTitle; }
    public void setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; }

    public int getLayoutType() { return layoutType; }
    public void setLayoutType(int layoutType) { this.layoutType = layoutType; }

    public int getSpanCount() { return spanCount; }
    public void setSpanCount(int spanCount) { this.spanCount = spanCount; }

    public List<LibraryCategory> getCategories() { return categories; }
    public void setCategories(List<LibraryCategory> categories) { this.categories = categories; }
}