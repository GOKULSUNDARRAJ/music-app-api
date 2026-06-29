package com.Saalai.SalaiMusicApp.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Models.PlaylistModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DownloadPlaylistManager {
    private static final String TAG = "DownloadPlaylistManager";
    private static final String PREF_NAME = "DownloadedPlaylists";
    private static final String KEY_PLAYLISTS = "saved_playlists";
    private static final String KEY_PLAYLIST_ORDER = "playlist_order"; // New key for playlist order

    private static DownloadPlaylistManager instance;
    private SharedPreferences preferences;
    private Gson gson;
    private Context context;

    private DownloadPlaylistManager(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized DownloadPlaylistManager getInstance(Context context) {
        if (instance == null) {
            instance = new DownloadPlaylistManager(context.getApplicationContext());
        }
        return instance;
    }

    // Save a playlist WITH category ID
    public boolean savePlaylistWithCategoryId(String id, String name, String imageUrl,
                                              ArrayList<AudioModel> songs, String categoryId) {
        try {
            // Get existing playlists
            Map<String, PlaylistModel> playlistsMap = getAllPlaylistsMap();

            // Create new playlist with category ID
            PlaylistModel playlist = new PlaylistModel();
            playlist.setId(id);
            playlist.setName(name);
            playlist.setImageUrl(imageUrl);
            playlist.setSongs(songs);

            // ADD "_download" SUFFIX to category ID for downloaded playlists
            String downloadCategoryId = categoryId;
            if (categoryId != null && !categoryId.isEmpty() && !categoryId.endsWith("_download")) {
                downloadCategoryId = categoryId + "_download";
            }
            playlist.setOriginalCategoryId(downloadCategoryId); // Set category ID with suffix

            // Add to map
            playlistsMap.put(id, playlist);

            // Save to SharedPreferences
            savePlaylistsMap(playlistsMap);

            Log.d(TAG, "Playlist saved with DOWNLOAD category ID: " + name +
                    " (ID: " + id + ", Category: " + downloadCategoryId + ") with " + songs.size() + " songs");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving playlist with category ID: " + e.getMessage());
            return false;
        }
    }

    // Save a playlist (original method for backward compatibility)
    public boolean savePlaylist(String id, String name, String imageUrl, ArrayList<AudioModel> songs) {
        return savePlaylistWithCategoryId(id, name, imageUrl, songs, "");
    }

    // NEW: Update playlist (for reordering)
    public boolean updatePlaylist(PlaylistModel updatedPlaylist) {
        try {
            if (updatedPlaylist == null || updatedPlaylist.getId() == null) {
                Log.e(TAG, "Cannot update null playlist");
                return false;
            }

            // Get existing playlists
            Map<String, PlaylistModel> playlistsMap = getAllPlaylistsMap();

            // Update the playlist
            playlistsMap.put(updatedPlaylist.getId(), updatedPlaylist);

            // Save to SharedPreferences
            savePlaylistsMap(playlistsMap);

            Log.d(TAG, "Playlist updated: " + updatedPlaylist.getName() +
                    " (ID: " + updatedPlaylist.getId() + ") with " +
                    (updatedPlaylist.getSongs() != null ? updatedPlaylist.getSongs().size() : 0) + " songs");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error updating playlist: " + e.getMessage());
            return false;
        }
    }

    // NEW: Update playlist songs order
    public boolean updatePlaylistOrder(String playlistId, ArrayList<AudioModel> reorderedSongs) {
        try {
            PlaylistModel playlist = getPlaylistById(playlistId);
            if (playlist == null) {
                Log.e(TAG, "Playlist not found for reordering: " + playlistId);
                return false;
            }

            // Update the songs list with new order
            playlist.setSongs(reorderedSongs);

            // Save the updated playlist
            return updatePlaylist(playlist);
        } catch (Exception e) {
            Log.e(TAG, "Error updating playlist order: " + e.getMessage());
            return false;
        }
    }

    // Get all playlists with order preserved
    public ArrayList<PlaylistModel> getAllPlaylists() {
        try {
            String playlistsJson = preferences.getString(KEY_PLAYLISTS, null);
            if (playlistsJson == null) {
                return new ArrayList<>();
            }

            Type type = new TypeToken<Map<String, PlaylistModel>>(){}.getType();
            Map<String, PlaylistModel> playlistsMap = gson.fromJson(playlistsJson, type);

            if (playlistsMap == null) {
                return new ArrayList<>();
            }

            // Get saved order
            ArrayList<String> playlistOrder = getPlaylistOrder();

            // Convert map to list and apply saved order
            ArrayList<PlaylistModel> playlists = new ArrayList<>();

            // First add playlists in saved order
            if (!playlistOrder.isEmpty()) {
                for (String id : playlistOrder) {
                    PlaylistModel playlist = playlistsMap.get(id);
                    if (playlist != null) {
                        playlists.add(playlist);
                        playlistsMap.remove(id);
                    }
                }
            }

            // Add remaining playlists (new ones) sorted by creation date
            ArrayList<PlaylistModel> remainingPlaylists = new ArrayList<>(playlistsMap.values());
            Collections.sort(remainingPlaylists, (p1, p2) ->
                    p2.getCreatedDate().compareTo(p1.getCreatedDate()));
            playlists.addAll(remainingPlaylists);

            Log.d(TAG, "Retrieved " + playlists.size() + " playlists with order preserved");

            // Debug log each playlist's category ID
            for (PlaylistModel playlist : playlists) {
                Log.d(TAG, "Playlist: " + playlist.getName() +
                        " | Category ID: " + playlist.getOriginalCategoryId() +
                        " | Song count: " + playlist.getSongCount());
            }

            return playlists;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving playlists: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // NEW: Save playlist order (for reordering playlists in the main list)
    public boolean savePlaylistOrder(ArrayList<PlaylistModel> orderedPlaylists) {
        try {
            ArrayList<String> orderList = new ArrayList<>();
            for (PlaylistModel playlist : orderedPlaylists) {
                orderList.add(playlist.getId());
            }

            String orderJson = gson.toJson(orderList);
            preferences.edit().putString(KEY_PLAYLIST_ORDER, orderJson).apply();

            Log.d(TAG, "Saved playlist order with " + orderList.size() + " playlists");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving playlist order: " + e.getMessage());
            return false;
        }
    }

    // NEW: Get playlist order
    private ArrayList<String> getPlaylistOrder() {
        try {
            String orderJson = preferences.getString(KEY_PLAYLIST_ORDER, null);
            if (orderJson == null) {
                return new ArrayList<>();
            }

            Type type = new TypeToken<ArrayList<String>>(){}.getType();
            ArrayList<String> orderList = gson.fromJson(orderJson, type);

            return orderList != null ? orderList : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error getting playlist order: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Get playlist by ID
    public PlaylistModel getPlaylistById(String id) {
        Map<String, PlaylistModel> playlistsMap = getAllPlaylistsMap();
        return playlistsMap.get(id);
    }

    // Get playlist by name
    public PlaylistModel getPlaylistByName(String name) {
        ArrayList<PlaylistModel> allPlaylists = getAllPlaylists();
        for (PlaylistModel playlist : allPlaylists) {
            if (playlist.getName().equalsIgnoreCase(name)) {
                return playlist;
            }
        }
        return null;
    }

    // Get playlist by category ID
    public PlaylistModel getPlaylistByCategoryId(String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) {
            return null;
        }

        ArrayList<PlaylistModel> allPlaylists = getAllPlaylists();
        for (PlaylistModel playlist : allPlaylists) {
            if (categoryId.equals(playlist.getOriginalCategoryId())) {
                Log.d(TAG, "Found playlist by category ID: " + categoryId +
                        " -> " + playlist.getName());
                return playlist;
            }
        }

        Log.d(TAG, "No playlist found for category ID: " + categoryId);
        return null;
    }

    // Check if playlist exists by name
    public boolean playlistExists(String name) {
        return getPlaylistByName(name) != null;
    }

    // Check if playlist exists by category ID
    public boolean playlistExistsByCategoryId(String categoryId) {
        return getPlaylistByCategoryId(categoryId) != null;
    }

    // Delete a playlist
    public boolean deletePlaylist(String id) {
        try {
            Map<String, PlaylistModel> playlistsMap = getAllPlaylistsMap();

            if (playlistsMap.containsKey(id)) {
                PlaylistModel removedPlaylist = playlistsMap.remove(id);

                // Save updated map
                savePlaylistsMap(playlistsMap);

                // Remove from order list
                ArrayList<String> orderList = getPlaylistOrder();
                orderList.remove(id);
                savePlaylistOrderFromList(orderList);

                Log.d(TAG, "Playlist deleted: " + removedPlaylist.getName() + " (ID: " + id + ")");
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting playlist: " + e.getMessage());
            return false;
        }
    }

    // Add songs to existing playlist (preserve category ID)
    public boolean addSongsToPlaylist(String playlistId, ArrayList<AudioModel> newSongs) {
        try {
            PlaylistModel playlist = getPlaylistById(playlistId);
            if (playlist == null) {
                return false;
            }

            // Get current category ID to preserve it
            String originalCategoryId = playlist.getOriginalCategoryId();

            ArrayList<AudioModel> existingSongs = playlist.getSongs();
            if (existingSongs == null) {
                existingSongs = new ArrayList<>();
            }

            // Create a set of existing song URLs for quick lookup
            Map<String, AudioModel> existingSongsMap = new HashMap<>();
            for (AudioModel song : existingSongs) {
                existingSongsMap.put(song.getAudioUrl(), song);
            }

            // Add only new songs
            for (AudioModel song : newSongs) {
                if (!existingSongsMap.containsKey(song.getAudioUrl())) {
                    existingSongs.add(song);
                }
            }

            // Update playlist
            playlist.setSongs(existingSongs);
            playlist.setOriginalCategoryId(originalCategoryId); // Preserve category ID

            // Save updated playlist
            return updatePlaylist(playlist);
        } catch (Exception e) {
            Log.e(TAG, "Error adding songs to playlist: " + e.getMessage());
            return false;
        }
    }

    // Remove songs from playlist
    public boolean removeSongsFromPlaylist(String playlistId, ArrayList<AudioModel> songsToRemove) {
        try {
            PlaylistModel playlist = getPlaylistById(playlistId);
            if (playlist == null) {
                return false;
            }

            // Get current category ID to preserve it
            String originalCategoryId = playlist.getOriginalCategoryId();

            ArrayList<AudioModel> existingSongs = playlist.getSongs();
            if (existingSongs == null || existingSongs.isEmpty()) {
                return false;
            }

            // Create a set of song URLs to remove
            Map<String, AudioModel> songsToRemoveMap = new HashMap<>();
            for (AudioModel song : songsToRemove) {
                songsToRemoveMap.put(song.getAudioUrl(), song);
            }

            // Remove songs
            ArrayList<AudioModel> updatedSongs = new ArrayList<>();
            for (AudioModel song : existingSongs) {
                if (!songsToRemoveMap.containsKey(song.getAudioUrl())) {
                    updatedSongs.add(song);
                }
            }

            // Update playlist
            playlist.setSongs(updatedSongs);
            playlist.setOriginalCategoryId(originalCategoryId); // Preserve category ID

            // Save updated playlist
            return updatePlaylist(playlist);
        } catch (Exception e) {
            Log.e(TAG, "Error removing songs from playlist: " + e.getMessage());
            return false;
        }
    }

    // Get total songs count in all playlists
    public int getTotalSongsCount() {
        int total = 0;
        ArrayList<PlaylistModel> playlists = getAllPlaylists();

        for (PlaylistModel playlist : playlists) {
            total += playlist.getSongCount();
        }

        return total;
    }

    // Clear all playlists
    public void clearAllPlaylists() {
        preferences.edit().remove(KEY_PLAYLISTS).remove(KEY_PLAYLIST_ORDER).apply();
        Log.d(TAG, "All playlists cleared");
    }

    // Update playlist with download info from AudioDownloadManager
    public boolean updatePlaylistWithDownloadInfo(String playlistId, Context context) {
        try {
            PlaylistModel playlist = getPlaylistById(playlistId);
            if (playlist == null) {
                Log.e(TAG, "Playlist not found: " + playlistId);
                return false;
            }

            ArrayList<AudioModel> songs = playlist.getSongs();
            if (songs == null || songs.isEmpty()) {
                Log.d(TAG, "Playlist is empty, nothing to update");
                return true;
            }

            AudioDownloadManager downloadManager = new AudioDownloadManager(context);
            boolean updated = false;

            for (int i = 0; i < songs.size(); i++) {
                AudioModel song = songs.get(i);

                // Check if song is already marked as downloaded
                if (!song.isDownloaded() || song.getDownloadPath() == null) {
                    // Check with AudioDownloadManager
                    AudioModel downloadedSong = downloadManager.getDownloadedSong(song.getAudioUrl());
                    if (downloadedSong != null && downloadedSong.isDownloaded()) {
                        // Update the song with download info
                        song.setDownloaded(true);
                        song.setDownloadPath(downloadedSong.getDownloadPath());
                        song.setFileSize(downloadedSong.getFileSize());

                        // Preserve category ID if it exists
                        if (song.getCategoryId() == null || song.getCategoryId().isEmpty()) {
                            // Try to get category ID from playlist
                            String playlistCategoryId = playlist.getOriginalCategoryId();
                            if (playlistCategoryId != null && !playlistCategoryId.isEmpty()) {
                                song.setCategoryId(playlistCategoryId);
                                Log.d(TAG, "Set category ID from playlist: " + playlistCategoryId);
                            }
                        }

                        updated = true;

                        Log.d(TAG, "Updated song with download info: " +
                                song.getAudioName() + " | Category ID: " + song.getCategoryId() +
                                " | Path: " + song.getDownloadPath());
                    }
                }
            }

            if (updated) {
                // Save the updated playlist
                return updatePlaylist(playlist);
            }

            return updated;

        } catch (Exception e) {
            Log.e(TAG, "Error updating playlist with download info: " + e.getMessage());
            return false;
        }
    }

    // Private helper method to save playlists map
    private void savePlaylistsMap(Map<String, PlaylistModel> playlistsMap) {
        String playlistsJson = gson.toJson(playlistsMap);
        preferences.edit().putString(KEY_PLAYLISTS, playlistsJson).apply();
    }

    // Private helper method to save playlist order from list
    private void savePlaylistOrderFromList(ArrayList<String> orderList) {
        String orderJson = gson.toJson(orderList);
        preferences.edit().putString(KEY_PLAYLIST_ORDER, orderJson).apply();
    }

    // Private helper method to get all playlists as map
    private Map<String, PlaylistModel> getAllPlaylistsMap() {
        try {
            String playlistsJson = preferences.getString(KEY_PLAYLISTS, null);
            if (playlistsJson == null) {
                return new HashMap<>();
            }

            Type type = new TypeToken<Map<String, PlaylistModel>>(){}.getType();
            Map<String, PlaylistModel> playlistsMap = gson.fromJson(playlistsJson, type);

            return playlistsMap != null ? playlistsMap : new HashMap<>();
        } catch (Exception e) {
            Log.e(TAG, "Error getting playlists map: " + e.getMessage());
            return new HashMap<>();
        }
    }

    // Helper method to find local download file
    private String findLocalDownloadFile(AudioModel song) {
        if (song == null || song.getAudioUrl() == null) {
            return null;
        }

        // Extract filename from URL
        String url = song.getAudioUrl();
        String fileName = getFileNameFromUrl(url);
        if (fileName == null) {
            return null;
        }

        // Check common download locations
        String[] downloadPaths = {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/SalaiMusic",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath(),
                context.getExternalFilesDir(null).getAbsolutePath(),
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath()
        };

        for (String path : downloadPaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                File file = new File(dir, fileName);
                if (file.exists() && file.isFile() && file.length() > 0) {
                    return file.getAbsolutePath();
                }

                // Also check for files with similar names
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().toLowerCase().contains(
                                song.getAudioName().toLowerCase().replace(" ", ""))) {
                            return f.getAbsolutePath();
                        }
                    }
                }
            }
        }

        return null;
    }

    private String getFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;

        try {
            // Remove query parameters
            int queryIndex = url.indexOf('?');
            if (queryIndex != -1) {
                url = url.substring(0, queryIndex);
            }

            // Get last segment
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash != -1 && lastSlash < url.length() - 1) {
                return url.substring(lastSlash + 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting filename: " + e.getMessage());
        }

        return null;
    }
}