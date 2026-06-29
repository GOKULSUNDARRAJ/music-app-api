package com.Saalai.SalaiMusicApp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.Saalai.SalaiMusicApp.Activity.MainActivity;
import com.Saalai.SalaiMusicApp.Models.AudioModel;

public class AudioDownloadManager {
    private Context context;
    private SharedPreferences prefs;
    private OnDownloadProgressListener progressListener;

    public interface OnDownloadProgressListener {
        void onProgress(int progress);
        void onComplete();
        void onError(String error);
    }

    public AudioDownloadManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("downloaded_songs", Context.MODE_PRIVATE);
    }

    public void setProgressListener(OnDownloadProgressListener listener) {
        this.progressListener = listener;
    }

    public void downloadAudio(AudioModel audioModel) {
        new Thread(() -> {
            try {
                URL url = new URL(audioModel.getAudioUrl());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                int fileLength = connection.getContentLength();

                // Create app's private directory
                File storageDir = new File(context.getFilesDir(), "downloaded_songs");
                if (!storageDir.exists()) {
                    storageDir.mkdirs();
                }

                // Create unique file name
                String fileName = "song_" + System.currentTimeMillis() + ".mp3";
                File outputFile = new File(storageDir, fileName);

                InputStream input = new BufferedInputStream(connection.getInputStream());
                OutputStream output = new FileOutputStream(outputFile);

                byte[] data = new byte[8192];
                long total = 0;
                int count;

                while ((count = input.read(data)) != -1) {
                    total += count;

                    // Publish progress
                    if (fileLength > 0 && progressListener != null) {
                        int progress = (int) (total * 100 / fileLength);
                        progressListener.onProgress(progress);
                    }

                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

                // Save song info to SharedPreferences
                saveSongInfo(audioModel, outputFile.getAbsolutePath());

                // Notify completion
                if (progressListener != null) {
                    progressListener.onComplete();
                }

                // Show success message
                if (context instanceof MainActivity) {
                    ((MainActivity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Downloaded: " + audioModel.getAudioName(), Toast.LENGTH_SHORT).show()
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (progressListener != null) {
                    progressListener.onError(e.getMessage());
                }
                if (context instanceof MainActivity) {
                    ((MainActivity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }).start();
    }

    private void saveSongInfo(AudioModel audioModel, String filePath) {
        String key = "song_" + System.currentTimeMillis();

        // Save: songName||artist||filePath||imageUrl||originalAudioUrl
        String songData = audioModel.getAudioName() + "||" +
                audioModel.getcategoryName() + "||" +
                filePath + "||" +
                audioModel.getImageUrl() + "||" +
                audioModel.getAudioUrl();  // Save original URL for reference

        prefs.edit().putString(key, songData).apply();

        // Add to the list of all downloaded songs keys
        String allSongs = prefs.getString("all_downloaded_songs", "");
        if (!allSongs.isEmpty()) {
            allSongs += "," + key;
        } else {
            allSongs = key;
        }
        prefs.edit().putString("all_downloaded_songs", allSongs).apply();

        Log.d("AudioDownloadManager", "Song saved: " + audioModel.getAudioName() +
                " with key: " + key +
                " | Path: " + filePath +
                " | Downloaded: true");
    }

    public List<AudioModel> getDownloadedSongs() {
        List<AudioModel> songs = new ArrayList<>();
        String allSongsKeys = prefs.getString("all_downloaded_songs", "");

        Log.d("AudioDownloadManager", "getDownloadedSongs - allSongsKeys: " + allSongsKeys);

        if (!allSongsKeys.isEmpty()) {
            String[] keys = allSongsKeys.split(",");
            Log.d("AudioDownloadManager", "Found " + keys.length + " keys");

            for (String key : keys) {
                if (!key.isEmpty() && !key.equals("")) {
                    String songData = prefs.getString(key, "");
                    Log.d("AudioDownloadManager", "Key: " + key + ", Data: " + songData);

                    if (!songData.isEmpty()) {
                        String[] parts = songData.split("\\|\\|");
                        if (parts.length >= 4) {
                            // parts[0] = song name
                            // parts[1] = category name (artist)
                            // parts[2] = file path
                            // parts[3] = image URL
                            // parts[4] = original audio URL

                            // 🔴 FIX: Create AudioModel with proper fields
                            AudioModel audioModel = new AudioModel(parts[0], parts[4], parts[3]);
                            audioModel.setcategoryName(parts[1]);

                            // 🔴 CRITICAL: Set the download path and downloaded flag
                            audioModel.setDownloadPath(parts[2]); // Set the local file path
                            audioModel.setDownloaded(true); // Mark as downloaded

                            // Also set the audio URL to the local path for offline playback
                            audioModel.setAudioUrl(parts[2]); // Use local path for offline

                            songs.add(audioModel);
                            Log.d("AudioDownloadManager", "Added downloaded song: " + parts[0] +
                                    " | Path: " + parts[2] +
                                    " | Downloaded: true");
                        }
                    }
                }
            }
        }
        Log.d("AudioDownloadManager", "Total songs loaded: " + songs.size());
        return songs;
    }
    public boolean isSongDownloaded(String audioUrl, String audioName) {
        List<AudioModel> downloadedSongs = getDownloadedSongs();
        if (downloadedSongs != null) {
            for (AudioModel song : downloadedSongs) {
                // Check by URL (for local files) OR by song name
                if (song.getAudioUrl().equals(audioUrl) ||
                        song.getAudioName().equals(audioName)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Overloaded method for backward compatibility
    public boolean isSongDownloaded(String audioUrl) {
        return isSongDownloaded(audioUrl, null);
    }

    // Better method: Check by song name (recommended)
    public boolean isSongDownloadedByName(String audioName) {
        List<AudioModel> downloadedSongs = getDownloadedSongs();
        if (downloadedSongs != null) {
            for (AudioModel song : downloadedSongs) {
                if (song.getAudioName().equals(audioName)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Fixed delete method that works with your key-based storage system
    public boolean deleteDownloadedSong(AudioModel audioModel) {
        try {
            Log.d("AudioDownloadManager", "Deleting song: " + audioModel.getAudioName());

            // Get the file path from the audio model
            String filePath = audioModel.getAudioUrl();
            if (filePath != null && !filePath.isEmpty()) {
                File file = new File(filePath);
                if (file.exists()) {
                    boolean deleted = file.delete();
                    Log.d("AudioDownloadManager", "File deletion: " + (deleted ? "success" : "failed"));

                    // Also remove from shared preferences
                    if (deleted) {
                        boolean removedFromPrefs = removeFromDownloadedSongs(audioModel);
                        Log.d("AudioDownloadManager", "Removed from prefs: " + removedFromPrefs);
                        return removedFromPrefs;
                    }
                } else {
                    Log.d("AudioDownloadManager", "File doesn't exist: " + filePath);
                    // File doesn't exist, but still remove from SharedPreferences
                    return removeFromDownloadedSongs(audioModel);
                }
            } else {
                Log.d("AudioDownloadManager", "File path is null or empty");
            }
            return false;
        } catch (Exception e) {
            Log.e("AudioDownloadManager", "Error deleting song: " + e.getMessage());
            return false;
        }
    }

    private boolean removeFromDownloadedSongs(AudioModel audioModelToRemove) {
        try {
            String allSongsKeys = prefs.getString("all_downloaded_songs", "");
            Log.d("AudioDownloadManager", "removeFromDownloadedSongs - Initial keys: " + allSongsKeys);

            if (allSongsKeys.isEmpty()) {
                Log.d("AudioDownloadManager", "No songs to remove");
                return false;
            }

            String[] keys = allSongsKeys.split(",");
            List<String> remainingKeys = new ArrayList<>();
            String keyToRemove = null;

            // Find the key that contains this song
            for (String key : keys) {
                if (!key.isEmpty() && !key.equals("")) {
                    String songData = prefs.getString(key, "");
                    if (!songData.isEmpty()) {
                        String[] parts = songData.split("\\|\\|");
                        if (parts.length >= 4) {
                            String localFilePath = parts[2]; // Local file path is at index 2
                            String songName = parts[0]; // Song name is at index 0

                            // Match by local file path or song name
                            if (localFilePath.equals(audioModelToRemove.getAudioUrl()) ||
                                    songName.equals(audioModelToRemove.getAudioName())) {
                                keyToRemove = key;
                                // Remove the individual song entry
                                prefs.edit().remove(key).apply();
                                Log.d("AudioDownloadManager", "Removed key: " + key + " for song: " + songName);
                            } else {
                                remainingKeys.add(key);
                            }
                        }
                    }
                }
            }

            // Update the list of all downloaded songs keys
            if (keyToRemove != null) {
                String updatedAllSongs = String.join(",", remainingKeys);
                prefs.edit().putString("all_downloaded_songs", updatedAllSongs).apply();
                Log.d("AudioDownloadManager", "Updated keys: " + updatedAllSongs);
                Log.d("AudioDownloadManager", "Successfully removed song: " + audioModelToRemove.getAudioName());
                return true;
            } else {
                Log.d("AudioDownloadManager", "No matching key found for song: " + audioModelToRemove.getAudioName());
                return false;
            }
        } catch (Exception e) {
            Log.e("AudioDownloadManager", "Error removing from shared preferences: " + e.getMessage());
            return false;
        }
    }

    // Restore method for undo functionality
    public boolean restoreDownloadedSong(AudioModel audioModel) {
        try {
            Log.d("AudioDownloadManager", "Attempting to restore song: " + audioModel.getAudioName());

            // Since we can't restore the actual file after deletion,
            // this method will just re-add the song info if the file still exists
            String filePath = audioModel.getAudioUrl();
            if (filePath != null && !filePath.isEmpty()) {
                File file = new File(filePath);
                if (file.exists()) {
                    // Re-save the song info using your existing method
                    saveSongInfo(audioModel, filePath);
                    Log.d("AudioDownloadManager", "Song restored: " + audioModel.getAudioName());
                    return true;
                } else {
                    Log.d("AudioDownloadManager", "File doesn't exist, cannot restore: " + filePath);
                }
            }
            return false;
        } catch (Exception e) {
            Log.e("AudioDownloadManager", "Error restoring song: " + e.getMessage());
            return false;
        }
    }

    // Helper method to clear all downloaded songs (for testing)
    public void clearAllDownloads() {
        try {
            String allSongsKeys = prefs.getString("all_downloaded_songs", "");
            if (!allSongsKeys.isEmpty()) {
                String[] keys = allSongsKeys.split(",");
                for (String key : keys) {
                    if (!key.isEmpty()) {
                        // Get file path before removing
                        String songData = prefs.getString(key, "");
                        if (!songData.isEmpty()) {
                            String[] parts = songData.split("\\|\\|");
                            if (parts.length >= 3) {
                                String filePath = parts[2];
                                File file = new File(filePath);
                                if (file.exists()) {
                                    file.delete();
                                }
                            }
                        }
                        prefs.edit().remove(key).apply();
                    }
                }
            }
            prefs.edit().remove("all_downloaded_songs").apply();
            Log.d("AudioDownloadManager", "All downloads cleared");
        } catch (Exception e) {
            Log.e("AudioDownloadManager", "Error clearing downloads: " + e.getMessage());
        }
    }
}