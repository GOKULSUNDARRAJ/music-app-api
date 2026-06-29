package com.Saalai.SalaiMusicApp.Models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public class PlaylistSection implements Serializable {

    @SerializedName("sectionId")
    private String sectionId;

    @SerializedName("sectionTitle")
    private String sectionTitle;

    @SerializedName("categories")  // ← CRITICAL: Maps JSON "categories" to this field
    private List<ArtistCategory> artistCategories;

    @SerializedName("layoutType")
    private int layoutType;

    @SerializedName("spanCount")
    private int spanCount;

    // Constructors
    public PlaylistSection() {
        this.spanCount = 2;
    }

    public PlaylistSection(String sectionTitle, List<ArtistCategory> artistCategories, int layoutType) {
        this();
        this.sectionTitle = sectionTitle;
        this.artistCategories = artistCategories;
        this.layoutType = layoutType;
    }

    public PlaylistSection(String sectionTitle, List<ArtistCategory> artistCategories) {
        this(sectionTitle, artistCategories, 1);
    }

    public PlaylistSection(String sectionTitle, List<ArtistCategory> artistCategories, int layoutType, int spanCount) {
        this();
        this.sectionTitle = sectionTitle;
        this.artistCategories = artistCategories;
        this.layoutType = layoutType;
        this.spanCount = spanCount;
    }

    public PlaylistSection(String sectionId, String sectionTitle, List<ArtistCategory> artistCategories, int layoutType, int spanCount) {
        this.sectionId = sectionId;
        this.sectionTitle = sectionTitle;
        this.artistCategories = artistCategories;
        this.layoutType = layoutType;
        this.spanCount = spanCount;
    }

    // Getters and Setters
    public String getSectionId() {
        return sectionId;
    }

    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    public String getSectionName() {
        return sectionTitle;
    }

    public void setSectionName(String sectionName) {
        this.sectionTitle = sectionName;
    }

    public List<ArtistCategory> getArtistCategories() {
        if (artistCategories == null) {
            artistCategories = new ArrayList<>();
        }
        return artistCategories;
    }

    public void setArtistCategories(List<ArtistCategory> artistCategories) {
        this.artistCategories = artistCategories;
    }

    public List<ArtistCategory> getArtists() {
        return getArtistCategories();
    }

    public void setArtists(List<ArtistCategory> artists) {
        this.artistCategories = artists;
    }

    public int getLayoutType() {
        return layoutType;
    }

    public void setLayoutType(int layoutType) {
        this.layoutType = layoutType;
    }

    public int getSpanCount() {
        return spanCount;
    }

    public void setSpanCount(int spanCount) {
        this.spanCount = spanCount;
    }

    @Override
    public String toString() {
        return "PlaylistSection{" +
                "sectionId='" + sectionId + '\'' +
                ", sectionTitle='" + sectionTitle + '\'' +
                ", artistCategoriesCount=" + (artistCategories != null ? artistCategories.size() : 0) +
                ", layoutType=" + layoutType +
                ", spanCount=" + spanCount +
                '}';
    }
}