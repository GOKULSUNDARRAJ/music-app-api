package com.Saalai.SalaiMusicApp.Utils;


import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.Saalai.SalaiMusicApp.Models.AudioModel;

import java.io.File;
import java.util.ArrayList;

public class DownloadManager {
    private static final String TAG = "DownloadManager";
    private static DownloadManager instance;
    private Context context;

    // Directory where songs are downloaded
    private static final String DOWNLOAD_DIR = "SalaiMusic/Downloads";

    private DownloadManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized DownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new DownloadManager(context);
        }
        return instance;
    }

    // Get the download directory
    public File getDownloadDirectory() {
        File downloadsDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), DOWNLOAD_DIR);

        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }

        return downloadsDir;
    }

    // Check if a song is downloaded locally
    public boolean isSongDownloaded(AudioModel song) {
        if (song == null) return false;

        // First check if song already has download info
        if (song.isDownloaded() && song.hasLocalFile()) {
            return true;
        }

        // Try to find the file in download directory
        String fileName = getFileNameFromUrl(song.getAudioUrl());
        if (fileName == null) return false;

        File downloadDir = getDownloadDirectory();
        File[] files = downloadDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().equals(fileName) ||
                        file.getName().contains(song.getAudioName())) {
                    // Found the file! Update song info
                    song.setDownloaded(true);
                    song.setDownloadPath(file.getAbsolutePath());
                    song.setFileSize(file.length());
                    Log.d(TAG, "Found downloaded file: " + file.getAbsolutePath());
                    return true;
                }
            }
        }

        return false;
    }

    // Get all downloaded songs
    public ArrayList<AudioModel> getAllDownloadedSongs(ArrayList<AudioModel> allSongs) {
        ArrayList<AudioModel> downloadedSongs = new ArrayList<>();

        if (allSongs == null || allSongs.isEmpty()) {
            return downloadedSongs;
        }

        File downloadDir = getDownloadDirectory();
        File[] files = downloadDir.listFiles();

        if (files == null || files.length == 0) {
            Log.d(TAG, "No files in download directory");
            return downloadedSongs;
        }

        Log.d(TAG, "Found " + files.length + " files in download directory");

        // For each file, try to match with a song
        for (File file : files) {
            if (file.isFile() && isAudioFile(file)) {
                // Try to find matching song
                for (AudioModel song : allSongs) {
                    if (isMatchingSong(song, file)) {
                        // Update song with local file info
                        AudioModel downloadedSong = new AudioModel();
                        downloadedSong.setAudioName(song.getAudioName());
                        downloadedSong.setcategoryName(song.getcategoryName());
                        downloadedSong.setAudioUrl(song.getAudioUrl());
                        downloadedSong.setImageUrl(song.getImageUrl());
                        downloadedSong.setDownloaded(true);
                        downloadedSong.setDownloadPath(file.getAbsolutePath());
                        downloadedSong.setFileSize(file.length());
                        downloadedSong.setDuration(song.getDuration());
                        downloadedSong.setDurationInMillis(song.getDurationInMillis());

                        downloadedSongs.add(downloadedSong);
                        Log.d(TAG, "Added downloaded song: " + song.getAudioName());
                        break;
                    }
                }
            }
        }

        Log.d(TAG, "Total downloaded songs found: " + downloadedSongs.size());
        return downloadedSongs;
    }

    private boolean isAudioFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".m4a") ||
                name.endsWith(".wav") || name.endsWith(".ogg");
    }

    private boolean isMatchingSong(AudioModel song, File file) {
        String fileName = file.getName().toLowerCase();
        String songName = song.getAudioName().toLowerCase();

        // Simple matching: check if file name contains song name
        return fileName.contains(songName.replace(" ", "")) ||
                songName.contains(fileName.replace(".mp3", "").replace(".m4a", "").replace(".wav", "").replace(".ogg", ""));
    }

    private String getFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;

        try {
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash != -1 && lastSlash < url.length() - 1) {
                return url.substring(lastSlash + 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting filename from URL: " + e.getMessage());
        }

        return null;
    }
}