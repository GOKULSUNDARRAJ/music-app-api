package com.Saalai.SalaiMusicApp.Models;

import java.util.List;

public class PlaylistModels {

    // Create Playlist Request
    public static class CreatePlaylistRequest {
        private String name;

        public CreatePlaylistRequest(String name) {
            this.name = name;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // Create Playlist Response
    public static class CreatePlaylistResponse {
        private boolean status;
        private boolean success;
        private String message;
        private int playlistId;
        private String categoryId;
        private String name;

        public boolean isStatus() { return status; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getPlaylistId() { return playlistId; }
        public String getCategoryId() { return categoryId; }
        public String getName() { return name; }
    }

    // Add Item Request - Supports both playlistId and categoryId
    public static class AddItemRequest {
        private Integer playlistId;
        private String categoryId;
        private String songId;

        // Constructor using playlistId
        public AddItemRequest(int playlistId, String songId) {
            this.playlistId = playlistId;
            this.songId = songId;
        }

        // Constructor using categoryId
        public AddItemRequest(String categoryId, String songId) {
            this.categoryId = categoryId;
            this.songId = songId;
        }

        public Integer getPlaylistId() { return playlistId; }
        public String getCategoryId() { return categoryId; }
        public String getSongId() { return songId; }
    }

    // Add Item Response
    public static class AddItemResponse {
        private boolean status;
        private String message;
        private boolean isAdded;

        public boolean isStatus() { return status; }
        public String getMessage() { return message; }
        public boolean isAdded() { return isAdded; }
    }

    // Get User Playlists Response
    public static class PlaylistCategoriesResponse {
        private boolean status;
        private boolean success;
        private List<Section> sections;

        public boolean isStatus() { return status; }
        public boolean isSuccess() { return success; }
        public List<Section> getSections() { return sections; }
    }

    public static class Section {
        private String sectionId;
        private String sectionTitle;
        private int layoutType;
        private int spanCount;
        private List<PlaylistCategory> categories;

        public String getSectionId() { return sectionId; }
        public String getSectionTitle() { return sectionTitle; }
        public int getLayoutType() { return layoutType; }
        public int getSpanCount() { return spanCount; }
        public List<PlaylistCategory> getCategories() { return categories; }
    }

    public static class PlaylistCategory {
        private String categoryId;
        private String categoryName;
        private String categoryImage;
        private int adapterType;
        private boolean isLiked;
        private int playlistId;
        private List<Song> songs;

        public String getCategoryId() { return categoryId; }
        public String getCategoryName() { return categoryName; }
        public String getCategoryImage() { return categoryImage; }
        public int getAdapterType() { return adapterType; }
        public boolean isLiked() { return isLiked; }
        public int getPlaylistId() { return playlistId; }
        public List<Song> getSongs() { return songs; }
    }

    public static class Song {
        private String songId;
        private String songName;
        private String songUrl;
        private String imageUrl;
        private String artistName;

        public String getSongId() { return songId; }
        public String getSongName() { return songName; }
        public String getSongUrl() { return songUrl; }
        public String getImageUrl() { return imageUrl; }
        public String getArtistName() { return artistName; }
    }

    // Add this to your PlaylistModels class
    public static class PlaylistStatusResponse {
        private boolean status;
        private boolean success;
        private String result;
        private boolean isInPlaylist;
        private boolean isAdded;
        private boolean inPlaylist;  // Alternative field name
        private boolean added;        // Alternative field name

        // Getters
        public boolean isStatus() { return status; }
        public boolean isSuccess() { return success; }
        public String getResult() { return result; }
        public boolean isInPlaylist() { return isInPlaylist || inPlaylist; }
        public boolean isAdded() { return isAdded || added; }

        // Alternative getters for different field names
        public boolean isInPlaylistAlt() { return inPlaylist; }
        public boolean isAddedAlt() { return added; }

        // Setters
        public void setStatus(boolean status) { this.status = status; }
        public void setSuccess(boolean success) { this.success = success; }
        public void setResult(String result) { this.result = result; }
        public void setIsInPlaylist(boolean isInPlaylist) { this.isInPlaylist = isInPlaylist; }
        public void setIsAdded(boolean isAdded) { this.isAdded = isAdded; }
        public void setInPlaylist(boolean inPlaylist) { this.inPlaylist = inPlaylist; }
        public void setAdded(boolean added) { this.added = added; }
    }

    public static class PlaylistDetailsResponse {
        private boolean status;
        private boolean success;
        private List<PlaylistItem> items;
        private String message;

        public boolean isStatus() { return status; }
        public boolean isSuccess() { return success; }
        public List<PlaylistItem> getItems() { return items; }
        public String getMessage() { return message; }

        public void setStatus(boolean status) { this.status = status; }
        public void setSuccess(boolean success) { this.success = success; }
        public void setItems(List<PlaylistItem> items) { this.items = items; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class PlaylistItem {
        private String id;
        private String songId;
        private String title;
        private String artist;
        private String imageUrl;
        private String songUrl;

        public String getId() { return id; }
        public String getSongId() { return songId; }
        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public String getImageUrl() { return imageUrl; }
        public String getSongUrl() { return songUrl; }

        public void setId(String id) { this.id = id; }
        public void setSongId(String songId) { this.songId = songId; }
        public void setTitle(String title) { this.title = title; }
        public void setArtist(String artist) { this.artist = artist; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public void setSongUrl(String songUrl) { this.songUrl = songUrl; }
    }
}