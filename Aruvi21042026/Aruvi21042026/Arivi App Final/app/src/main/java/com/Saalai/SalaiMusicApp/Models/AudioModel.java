package com.Saalai.SalaiMusicApp.Models;

import android.util.Log;
import java.io.File;
import java.io.Serializable;

public class AudioModel implements Serializable {
    private String songId;
    private String audioName;
    private String audioUrl;
    private String categoryName;
    private String categoryId; // Added for category ID tracking
    private String imageUrl;
    private String downloadPath;
    private boolean isDownloaded;
    private long fileSize;
    private String duration;
    private long durationInMillis;
    private String playlistId;


    // Copy constructor
    public AudioModel(AudioModel other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot copy from null AudioModel");
        }

        this.songId = other.songId;
        this.audioName = other.audioName;
        this.audioUrl = other.audioUrl;
        this.categoryName = other.categoryName;
        this.categoryId = other.categoryId;
        this.imageUrl = other.imageUrl;
        this.downloadPath = other.downloadPath;
        this.isDownloaded = other.isDownloaded;
        this.fileSize = other.fileSize;
        this.duration = other.duration;
        this.durationInMillis = other.durationInMillis;
        this.playlistId = other.playlistId;
    }


    // Constructors
    public AudioModel() {
        this.songId = "song_" + System.currentTimeMillis();
    }

    public AudioModel(String audioName, String audioUrl, String imageUrl) {
        this();
        this.audioName = audioName;
        this.audioUrl = audioUrl;
        this.imageUrl = imageUrl;
        this.isDownloaded = false;
    }

    public AudioModel(String audioName, String audioUrl, String categoryName, String imageUrl) {
        this();
        this.audioName = audioName;
        this.audioUrl = audioUrl;
        this.categoryName = categoryName;
        this.imageUrl = imageUrl;
        this.isDownloaded = false;
    }

    // Constructor with songId
    public AudioModel(String songId, String audioName, String audioUrl, String categoryName, String imageUrl) {
        this.songId = songId;
        this.audioName = audioName;
        this.audioUrl = audioUrl;
        this.categoryName = categoryName;
        this.imageUrl = imageUrl;
        this.isDownloaded = false;
    }

    // Full constructor with categoryId
    public AudioModel(String songId, String audioName, String audioUrl,
                      String categoryName, String categoryId, String imageUrl) {
        this.songId = songId;
        this.audioName = audioName;
        this.audioUrl = audioUrl;
        this.categoryName = categoryName;
        this.categoryId = categoryId;
        this.imageUrl = imageUrl;
        this.isDownloaded = false;
    }

    // Getters and Setters
    public String getSongId() {
        return songId;
    }

    public void setSongId(String songId) {
        this.songId = songId;
    }

    public String getAudioName() {
        return audioName;
    }

    public void setAudioName(String audioName) {
        this.audioName = audioName;
    }

    public String getAudioUrl() {
        if (audioUrl != null && audioUrl.startsWith("http://")) {
            return audioUrl.replace("http://", "https://");
        }
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getcategoryName() {
        return categoryName;
    }

    public void setcategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getImageUrl() {
        if (imageUrl != null && imageUrl.startsWith("http://")) {
            return imageUrl.replace("http://", "https://");
        }
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public long getDurationInMillis() {
        return durationInMillis;
    }

    public void setDurationInMillis(long durationInMillis) {
        this.durationInMillis = durationInMillis;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
    }

    // Helper method to convert string duration to milliseconds
    public long parseDurationToMillis() {
        if (duration == null || duration.isEmpty()) {
            return 0;
        }

        try {
            String[] parts = duration.split(":");
            if (parts.length == 2) {
                // Format: MM:SS
                long minutes = Long.parseLong(parts[0]);
                long seconds = Long.parseLong(parts[1]);
                return (minutes * 60 + seconds) * 1000;
            } else if (parts.length == 3) {
                // Format: HH:MM:SS
                long hours = Long.parseLong(parts[0]);
                long minutes = Long.parseLong(parts[1]);
                long seconds = Long.parseLong(parts[2]);
                return (hours * 3600 + minutes * 60 + seconds) * 1000;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Get the best path for playback (local first, then online)
    public String getPlaybackPath() {
        // First priority: Local download path if file exists
        if (hasLocalFile()) {
            Log.d("AudioModel", "Using local file for playback: " + downloadPath);
            return downloadPath;
        }

        // Second priority: Online URL
        Log.d("AudioModel", "Using online URL for playback: " + audioUrl);
        return audioUrl;
    }

    // Check if local file exists and is readable
    public boolean hasLocalFile() {
        if (!isDownloaded || downloadPath == null || downloadPath.isEmpty()) {
            return false;
        }

        try {
            File file = new File(downloadPath);
            boolean exists = file.exists();
            boolean canRead = file.canRead();
            boolean isFile = file.isFile();
            long size = file.length();

            Log.d("AudioModel", "Local file check - " + downloadPath +
                    " | exists: " + exists +
                    " | canRead: " + canRead +
                    " | isFile: " + isFile +
                    " | size: " + size);

            return exists && canRead && isFile && size > 0;
        } catch (Exception e) {
            Log.e("AudioModel", "Error checking local file: " + e.getMessage());
            return false;
        }
    }

    // Check if this is the same song (by ID or URL or name)
    public boolean isSameSong(AudioModel other) {
        if (other == null) return false;

        // Check by ID first (most reliable)
        if (this.songId != null && other.songId != null &&
                this.songId.equals(other.songId)) {
            return true;
        }

        // Check by URL (second most reliable)
        if (this.audioUrl != null && other.audioUrl != null &&
                this.audioUrl.equals(other.audioUrl)) {
            return true;
        }

        // Check by name and artist (fallback)
        if (this.audioName != null && other.audioName != null &&
                this.audioName.equals(other.audioName) &&
                this.categoryName != null && other.categoryName != null &&
                this.categoryName.equals(other.categoryName)) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "AudioModel{" +
                "songId='" + songId + '\'' +
                ", audioName='" + audioName + '\'' +
                ", audioUrl='" + audioUrl + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", downloadPath='" + downloadPath + '\'' +
                ", isDownloaded=" + isDownloaded +
                ", fileSize=" + fileSize +
                ", duration='" + duration + '\'' +
                ", durationInMillis=" + durationInMillis +
                ", playlistId='" + playlistId + '\'' +
                ", hasLocalFile=" + hasLocalFile() +
                '}';
    }
}