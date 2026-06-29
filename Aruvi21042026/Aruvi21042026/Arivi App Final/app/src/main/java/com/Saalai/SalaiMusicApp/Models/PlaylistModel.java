package com.Saalai.SalaiMusicApp.Models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class PlaylistModel implements Serializable {
    private String id;
    private String name;
    private String imageUrl;
    private ArrayList<AudioModel> songs;
    private Date createdDate;
    private int songCount;
    private String originalCategoryId; // Add this field for tracking which category this playlist came from

    public PlaylistModel() {
        this.createdDate = new Date();
        this.songs = new ArrayList<>();
        this.originalCategoryId = "";
    }

    public PlaylistModel(String id, String name, String imageUrl, ArrayList<AudioModel> songs) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.songs = songs != null ? songs : new ArrayList<>();
        this.createdDate = new Date();
        this.songCount = this.songs.size();
        this.originalCategoryId = "";
    }

    // Add new constructor with category ID
    public PlaylistModel(String id, String name, String imageUrl, ArrayList<AudioModel> songs, String originalCategoryId) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.songs = songs != null ? songs : new ArrayList<>();
        this.createdDate = new Date();
        this.songCount = this.songs.size();
        this.originalCategoryId = originalCategoryId != null ? originalCategoryId : "";
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public ArrayList<AudioModel> getSongs() {
        return songs;
    }

    public void setSongs(ArrayList<AudioModel> songs) {
        this.songs = songs;
        this.songCount = songs.size();
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int songCount) {
        this.songCount = songCount;
    }

    // Add getter and setter for originalCategoryId
    public String getOriginalCategoryId() {
        return originalCategoryId;
    }

    public void setOriginalCategoryId(String originalCategoryId) {
        this.originalCategoryId = originalCategoryId != null ? originalCategoryId : "";
    }

    public void addSong(AudioModel song) {
        if (songs == null) {
            songs = new ArrayList<>();
        }
        songs.add(song);
        songCount++;
    }

    public void removeSong(AudioModel song) {
        if (songs != null) {
            songs.remove(song);
            songCount--;
        }
    }

    @Override
    public String toString() {
        return "PlaylistModel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", songCount=" + songCount +
                ", originalCategoryId='" + originalCategoryId + '\'' +
                '}';
    }
}