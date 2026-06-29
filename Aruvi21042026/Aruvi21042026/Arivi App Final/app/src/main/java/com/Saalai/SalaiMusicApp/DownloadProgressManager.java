package com.Saalai.SalaiMusicApp;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class DownloadProgressManager {
    private static final String TAG = "DownloadProgressManager";

    private static Map<String, Integer> downloadProgressMap = new HashMap<>();
    private static Map<String, Boolean> downloadInProgressMap = new HashMap<>();
    private static Map<String, Long> downloadStartTimeMap = new HashMap<>();

    public static void startDownload(String songName) {
        downloadInProgressMap.put(songName, true);
        downloadProgressMap.put(songName, 0);
        downloadStartTimeMap.put(songName, System.currentTimeMillis());
        Log.d(TAG, "Download started for: " + songName);
    }

    public static void updateProgress(String songName, int progress) {
        if (downloadInProgressMap.containsKey(songName)) {
            downloadProgressMap.put(songName, progress);
            Log.d(TAG, "Progress updated for " + songName + ": " + progress + "%");
        } else {
            Log.w(TAG, "Progress update for non-existent download: " + songName);
        }
    }

    public static void downloadComplete(String songName) {
        downloadInProgressMap.remove(songName);
        downloadProgressMap.remove(songName);
        downloadStartTimeMap.remove(songName);
        Log.d(TAG, "Download completed for: " + songName);
    }

    public static void downloadError(String songName) {
        downloadInProgressMap.remove(songName);
        downloadProgressMap.remove(songName);
        downloadStartTimeMap.remove(songName);
        Log.d(TAG, "Download error for: " + songName);
    }

    public static boolean isDownloadInProgress(String songName) {
        boolean inProgress = downloadInProgressMap.containsKey(songName) &&
                downloadInProgressMap.get(songName);

        // Check if download is stuck (more than 30 minutes)
        if (inProgress && downloadStartTimeMap.containsKey(songName)) {
            long startTime = downloadStartTimeMap.get(songName);
            long currentTime = System.currentTimeMillis();
            long duration = currentTime - startTime;

            if (duration > 30 * 60 * 1000) { // 30 minutes
                Log.w(TAG, "Download appears stuck for: " + songName + ", clearing state");
                downloadInProgressMap.remove(songName);
                downloadProgressMap.remove(songName);
                downloadStartTimeMap.remove(songName);
                return false;
            }
        }

        return inProgress;
    }

    public static int getProgress(String songName) {
        return downloadProgressMap.getOrDefault(songName, 0);
    }

    public static void clearAll() {
        downloadInProgressMap.clear();
        downloadProgressMap.clear();
        downloadStartTimeMap.clear();
        Log.d(TAG, "All download progress cleared");
    }

    // Method to manually clear a specific download
    public static void clearDownload(String songName) {
        downloadInProgressMap.remove(songName);
        downloadProgressMap.remove(songName);
        downloadStartTimeMap.remove(songName);
        Log.d(TAG, "Cleared download state for: " + songName);
    }
}