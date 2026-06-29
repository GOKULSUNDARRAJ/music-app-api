package com.Saalai.SalaiMusicApp.Models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.io.Serializable;
import java.util.ArrayList;

public class ArtistCategory implements Serializable {

    @SerializedName("categoryId")
    private String categoryId;

    @SerializedName("categoryName")
    private String categoryName;

    @SerializedName("songs")
    private List<AudioModel> songs;

    @SerializedName("categoryImage")
    private String categoryImage;

    @SerializedName("adapterType")
    private int adapterType;

    // Constructors
    public ArtistCategory() {
        this.songs = new ArrayList<>();
    }

    public ArtistCategory(String categoryName, List<AudioModel> songs, String categoryImage, int adapterType) {
        this();
        this.categoryName = categoryName;
        this.songs = songs != null ? songs : new ArrayList<>();
        this.categoryImage = categoryImage;
        this.adapterType = adapterType;
    }

    public ArtistCategory(String categoryId, String categoryName, List<AudioModel> songs, String categoryImage, int adapterType) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.songs = songs != null ? songs : new ArrayList<>();
        this.categoryImage = categoryImage;
        this.adapterType = adapterType;
    }

    // Getters and Setters
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

    public String getArtistName() {
        return categoryName;
    }

    public void setArtistName(String artistName) {
        this.categoryName = artistName;
    }

    public List<AudioModel> getSongs() {
        if (songs == null) {
            songs = new ArrayList<>();
        }
        return songs;
    }

    public void setSongs(List<AudioModel> songs) {
        this.songs = songs != null ? songs : new ArrayList<>();
    }

    public String getCategoryImage() {
        if (categoryImage != null && categoryImage.startsWith("http://")) {
            return categoryImage.replace("http://", "https://");
        }
        return categoryImage;
    }

    public void setCategoryImage(String categoryImage) {
        this.categoryImage = categoryImage;
    }

    public String getArtistImageUrl() {
        if (categoryImage != null && categoryImage.startsWith("http://")) {
            return categoryImage.replace("http://", "https://");
        }
        return categoryImage;
    }

    public void setArtistImageUrl(String artistImageUrl) {
        this.categoryImage = artistImageUrl;
    }

    public int getAdapterType() {
        return adapterType;
    }

    public void setAdapterType(int adapterType) {
        this.adapterType = adapterType;
    }

    @Override
    public String toString() {
        return "ArtistCategory{" +
                "categoryId='" + categoryId + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", songsCount=" + (songs != null ? songs.size() : 0) +
                ", categoryImage='" + categoryImage + '\'' +
                ", adapterType=" + adapterType +
                '}';
    }
}