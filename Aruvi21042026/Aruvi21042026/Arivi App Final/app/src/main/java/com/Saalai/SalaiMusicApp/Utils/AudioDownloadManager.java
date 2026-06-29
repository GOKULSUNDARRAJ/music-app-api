package com.Saalai.SalaiMusicApp.Utils;

import android.Manifest;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;


import com.Saalai.SalaiMusicApp.Activity.MainActivity;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioDownloadManager {
    private static final String TAG = "AudioDownloadManager";

    private static final String PREF_NAME = "DownloadedSongs";
    private static final String KEY_DOWNLOADED_SONGS = "downloaded_songs_list";

    private static final String NOTIFICATION_CHANNEL_ID = "audio_download_channel";
    private static final String NOTIFICATION_CHANNEL_NAME = "Audio Downloads";
    private static final int NOTIFICATION_ID = 1001;

    private Context context;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private ExecutorService executorService;
    private Handler mainHandler;
    private DownloadManager systemDownloadManager;

    // Interface for download progress callbacks
    public interface OnDownloadProgressListener {
        void onProgress(int progress);
        void onComplete();
        void onError(String error);
    }

    private OnDownloadProgressListener progressListener;

    // Singleton instance
    private static AudioDownloadManager instance;

    public static synchronized AudioDownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new AudioDownloadManager(context.getApplicationContext());
        }
        return instance;
    }

    public AudioDownloadManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newFixedThreadPool(3); // Allow 3 concurrent downloads

        // Initialize notification manager
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();

        // Initialize system DownloadManager
        systemDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        Log.d(TAG, "AudioDownloadManager initialized");
    }

    public void setProgressListener(OnDownloadProgressListener listener) {
        this.progressListener = listener;
    }

    // Create notification channel for Android Oreo and above
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Audio download notifications");
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Get app-specific download directory (works for all Android versions)
    private File getAppDownloadDirectory() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, use app-specific directory
            return context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        } else {
            // For older versions, use public Music directory
            File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            File appDir = new File(musicDir, "SalaiMusic");
            if (!appDir.exists()) {
                appDir.mkdirs();
            }
            return appDir;
        }
    }

    // Check storage permission
    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - Use MANAGE_EXTERNAL_STORAGE
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10
            return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 5 and below
            return true;
        }
    }

    // Download audio file
    public void downloadAudio(AudioModel audioModel) {
        if (audioModel == null || audioModel.getAudioUrl() == null || audioModel.getAudioUrl().isEmpty()) {
            showToast("Invalid audio file");
            return;
        }

        // Debug logging
        Log.d(TAG, "=== DOWNLOADING AUDIO ===");
        Log.d(TAG, "Name: " + audioModel.getAudioName());
        Log.d(TAG, "Category: " + audioModel.getcategoryName());
        Log.d(TAG, "URL: " + audioModel.getAudioUrl());
        Log.d(TAG, "==========================");

        // Check if already downloaded
        if (isSongDownloaded(audioModel.getAudioUrl(), audioModel.getAudioName())) {
            showToast("Song already downloaded");
            return;
        }

        // Check storage permission
        if (!hasStoragePermission()) {
            showToast("Storage permission required");
            return;
        }

        executorService.execute(() -> {
            downloadFile(audioModel);
        });
    }

    private void downloadFile(AudioModel audioModel) {
        InputStream input = null;
        FileOutputStream output = null;
        HttpURLConnection connection = null;

        try {
            URL url = new URL(audioModel.getAudioUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.connect();

            // Check HTTP response code
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                String error = "Server returned HTTP " + connection.getResponseCode();
                Log.e(TAG, error);
                notifyError(error);
                return;
            }

            // Get file length for progress calculation
            int fileLength = connection.getContentLength();
            Log.d(TAG, "File length: " + fileLength + " bytes");

            // Generate clean filename (remove query parameters)
            String fileName = generateCleanFileName(audioModel);
            File outputFile = new File(getAppDownloadDirectory(), fileName);

            Log.d(TAG, "Downloading to: " + outputFile.getAbsolutePath());

            // Create parent directories if they don't exist
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Start download
            input = new BufferedInputStream(connection.getInputStream());
            output = new FileOutputStream(outputFile);

            byte[] data = new byte[4096];
            long total = 0;
            int count;

            // Show download notification
            showDownloadNotification(audioModel.getAudioName(), 0);

            while ((count = input.read(data)) != -1) {
                total += count;
                output.write(data, 0, count);

                // Calculate progress percentage
                if (fileLength > 0) {
                    int progress = (int) (total * 100 / fileLength);
                    updateProgress(progress);

                    // Update notification
                    updateDownloadNotification(audioModel.getAudioName(), progress);
                }
            }

            output.flush();

            // Download complete
            Log.d(TAG, "Download complete: " + fileName);
            Log.d(TAG, "File saved at: " + outputFile.getAbsolutePath());
            Log.d(TAG, "File size: " + outputFile.length() + " bytes");

            // Save downloaded song info
            audioModel.setDownloadPath(outputFile.getAbsolutePath());
            audioModel.setDownloaded(true);
            audioModel.setFileSize(outputFile.length());
            saveDownloadedSong(audioModel);

            // Complete notification
            completeDownloadNotification(audioModel.getAudioName());

            // Notify success
            notifyComplete();

            // Show toast
            showToast("Downloaded: " + audioModel.getAudioName());

        } catch (Exception e) {
            Log.e(TAG, "Download error: " + e.getMessage(), e);
            notifyError(e.getMessage());
        } finally {
            try {
                if (output != null) output.close();
                if (input != null) input.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing streams: " + e.getMessage());
            }

            if (connection != null) connection.disconnect();
        }
    }

    // Generate clean filename (remove query parameters and special characters)
    private String generateCleanFileName(AudioModel audioModel) {
        String audioName = audioModel.getAudioName();
        String extension = ".mp3"; // Default extension

        // Extract extension from URL if available
        String url = audioModel.getAudioUrl();

        // Remove query parameters from URL
        int queryIndex = url.indexOf('?');
        if (queryIndex != -1) {
            url = url.substring(0, queryIndex);
        }

        // Extract extension
        int lastDot = url.lastIndexOf('.');
        if (lastDot != -1 && lastDot < url.length() - 1) {
            String urlExtension = url.substring(lastDot);
            // Check if it's a valid audio extension
            if (urlExtension.matches("(?i)\\.(mp3|m4a|wav|ogg|flac)$")) {
                extension = urlExtension;
            }
        }

        // Clean filename (remove invalid characters)
        String cleanName = audioName.replaceAll("[\\\\/:*?\"<>|]", "_"); // Remove invalid file characters
        cleanName = cleanName.replaceAll("\\s+", " ").trim(); // Normalize spaces

        // Add timestamp for uniqueness
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

        return cleanName + "_" + timeStamp + extension;
    }

    // Alternative: Use system DownloadManager (recommended for large files)
    public long downloadUsingSystemManager(AudioModel audioModel) {
        try {
            // Generate clean filename
            String fileName = generateCleanFileName(audioModel);

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(audioModel.getAudioUrl()));
            request.setTitle(audioModel.getAudioName());
            request.setDescription("Downloading audio file");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // Set destination
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+
                request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MUSIC, fileName);
            } else {
                // Android 9 and below
                File destinationFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                        "SalaiMusic/" + fileName);
                request.setDestinationUri(Uri.fromFile(destinationFile));
            }

            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setAllowedOverRoaming(false);

            // Enqueue the download
            long downloadId = systemDownloadManager.enqueue(request);

            Log.d(TAG, "Download enqueued with ID: " + downloadId);
            return downloadId;

        } catch (Exception e) {
            Log.e(TAG, "Error using system DownloadManager: " + e.getMessage());
            return -1;
        }
    }

    // Progress update
    private void updateProgress(final int progress) {
        mainHandler.post(() -> {
            if (progressListener != null) {
                progressListener.onProgress(progress);
            }
        });
    }

    // Download complete notification
    private void notifyComplete() {
        mainHandler.post(() -> {
            if (progressListener != null) {
                progressListener.onComplete();
            }
        });
    }

    // Error notification
    private void notifyError(final String error) {
        mainHandler.post(() -> {
            if (progressListener != null) {
                progressListener.onError(error);
            }
        });
    }

    // Show toast message
    private void showToast(final String message) {
        mainHandler.post(() -> {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });
    }

    // Check if song is already downloaded
    public boolean isSongDownloaded(String audioUrl, String audioName) {
        List<AudioModel> downloadedSongs = getDownloadedSongs();

        for (AudioModel song : downloadedSongs) {
            // Check by URL or name
            if ((song.getAudioUrl() != null && song.getAudioUrl().equals(audioUrl)) ||
                    (song.getAudioName() != null && song.getAudioName().equalsIgnoreCase(audioName))) {
                return true;
            }
        }

        return false;
    }

    // Check if song is downloaded by name only
    public boolean isSongDownloadedByName(String audioName) {
        List<AudioModel> downloadedSongs = getDownloadedSongs();

        for (AudioModel song : downloadedSongs) {
            if (song.getAudioName() != null && song.getAudioName().equalsIgnoreCase(audioName)) {
                return true;
            }
        }

        return false;
    }

    // Save downloaded song info to SharedPreferences
    // Save downloaded song info to SharedPreferences
    // Save downloaded song info to SharedPreferences
    private void saveDownloadedSong(AudioModel audioModel) {
        try {
            List<AudioModel> downloadedSongs = getDownloadedSongs();

            // Check if already exists
            boolean exists = false;
            for (int i = 0; i < downloadedSongs.size(); i++) {
                AudioModel song = downloadedSongs.get(i);
                if ((song.getAudioUrl() != null && song.getAudioUrl().equals(audioModel.getAudioUrl())) ||
                        (song.getAudioName() != null && song.getAudioName().equalsIgnoreCase(audioModel.getAudioName()))) {
                    // Update ONLY download-specific fields
                    AudioModel updatedSong = downloadedSongs.get(i);

                    // Preserve ALL existing metadata
                    // DO NOT overwrite any existing fields unless they're download-related

                    // Update download-related fields
                    updatedSong.setDownloadPath(audioModel.getDownloadPath());
                    updatedSong.setDownloaded(true);
                    updatedSong.setFileSize(audioModel.getFileSize());

                    // Only update these fields if they're missing/null in the existing record
                    if (updatedSong.getAudioName() == null || updatedSong.getAudioName().isEmpty()) {
                        updatedSong.setAudioName(audioModel.getAudioName());
                    }
                    if (updatedSong.getcategoryName() == null || updatedSong.getcategoryName().isEmpty()) {
                        updatedSong.setcategoryName(audioModel.getcategoryName());
                    }
                    if (updatedSong.getImageUrl() == null || updatedSong.getImageUrl().isEmpty()) {
                        updatedSong.setImageUrl(audioModel.getImageUrl());
                    }
                    if (updatedSong.getAudioUrl() == null || updatedSong.getAudioUrl().isEmpty()) {
                        updatedSong.setAudioUrl(audioModel.getAudioUrl());
                    }
                    if (updatedSong.getDuration() == null || updatedSong.getDuration().isEmpty()) {
                        updatedSong.setDuration(audioModel.getDuration());
                    }

                    downloadedSongs.set(i, updatedSong);
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                // Create new entry with ALL data from the audioModel
                // Since it's a new download, we should save everything
                AudioModel songToSave = new AudioModel();

                // Copy ALL fields from the provided audioModel
                songToSave.setAudioName(audioModel.getAudioName());
                songToSave.setcategoryName(audioModel.getcategoryName());
                songToSave.setImageUrl(audioModel.getImageUrl());
                songToSave.setAudioUrl(audioModel.getAudioUrl());
                songToSave.setDuration(audioModel.getDuration());

                // Set download info
                songToSave.setDownloadPath(audioModel.getDownloadPath());
                songToSave.setDownloaded(true);
                songToSave.setFileSize(audioModel.getFileSize());

                downloadedSongs.add(songToSave);
            }

            // Save to SharedPreferences using Gson
            String songsJson = new com.google.gson.Gson().toJson(downloadedSongs);
            SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            preferences.edit().putString(KEY_DOWNLOADED_SONGS, songsJson).apply();

            Log.d(TAG, "Saved downloaded song: " + audioModel.getAudioName() +
                    ", Category: " + audioModel.getcategoryName());
        } catch (Exception e) {
            Log.e(TAG, "Error saving downloaded song: " + e.getMessage());
        }
    }


    // Get all downloaded songs
    public List<AudioModel> getDownloadedSongs() {
        try {
            SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String songsJson = preferences.getString(KEY_DOWNLOADED_SONGS, null);

            if (songsJson == null || songsJson.isEmpty()) {
                return new ArrayList<>();
            }

            com.google.gson.reflect.TypeToken<List<AudioModel>> typeToken =
                    new com.google.gson.reflect.TypeToken<List<AudioModel>>() {};
            List<AudioModel> songs = new com.google.gson.Gson().fromJson(songsJson, typeToken.getType());

            return songs != null ? songs : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error getting downloaded songs: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Get downloaded song by URL

    // Get downloaded song by URL
    public AudioModel getDownloadedSong(String audioUrl) {
        List<AudioModel> downloadedSongs = getDownloadedSongs();

        for (AudioModel song : downloadedSongs) {
            if (song.getAudioUrl() != null && song.getAudioUrl().equals(audioUrl)) {
                // Log for debugging
                Log.d(TAG, "Retrieved downloaded song: " + song.getAudioName() +
                        ", Category: " + song.getcategoryName() +
                        ", Downloaded: " + song.isDownloaded());
                return song;
            }
        }

        return null;
    }


    // Get downloaded song by name
    public AudioModel getDownloadedSongByName(String audioName) {
        List<AudioModel> downloadedSongs = getDownloadedSongs();

        for (AudioModel song : downloadedSongs) {
            if (song.getAudioName() != null && song.getAudioName().equalsIgnoreCase(audioName)) {
                return song;
            }
        }

        return null;
    }

    // Get local file path for downloaded song
    public String getLocalFilePath(String audioUrl) {
        AudioModel song = getDownloadedSong(audioUrl);
        return song != null ? song.getDownloadPath() : null;
    }

    // Get File object for downloaded song
    public File getLocalFile(String audioUrl) {
        String path = getLocalFilePath(audioUrl);
        return path != null ? new File(path) : null;
    }

    // Get Uri for downloaded song (with FileProvider for Android 7+)
    public Uri getLocalFileUri(String audioUrl) {
        File file = getLocalFile(audioUrl);
        if (file == null || !file.exists()) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Use FileProvider for Android 7+
            return FileProvider.getUriForFile(context,
                    context.getPackageName() + ".provider", file);
        } else {
            // For older versions
            return Uri.fromFile(file);
        }
    }

    // Delete downloaded song
    public boolean deleteDownloadedSong(AudioModel audioModel) {
        try {
            // Delete file from storage
            if (audioModel.getDownloadPath() != null) {
                File file = new File(audioModel.getDownloadPath());
                if (file.exists()) {
                    boolean deleted = file.delete();
                    Log.d(TAG, "File deleted: " + deleted);
                }
            }

            // Remove from saved list
            List<AudioModel> downloadedSongs = getDownloadedSongs();
            List<AudioModel> updatedSongs = new ArrayList<>();

            for (AudioModel song : downloadedSongs) {
                if (!song.getAudioUrl().equals(audioModel.getAudioUrl())) {
                    updatedSongs.add(song);
                }
            }

            // Save updated list
            String songsJson = new com.google.gson.Gson().toJson(updatedSongs);
            SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            preferences.edit().putString(KEY_DOWNLOADED_SONGS, songsJson).apply();

            Log.d(TAG, "Deleted downloaded song: " + audioModel.getAudioName());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting downloaded song: " + e.getMessage());
            return false;
        }
    }

    // Clear all downloaded songs
    public void clearAllDownloads() {
        try {
            // Delete all files
            List<AudioModel> downloadedSongs = getDownloadedSongs();
            for (AudioModel song : downloadedSongs) {
                if (song.getDownloadPath() != null) {
                    File file = new File(song.getDownloadPath());
                    if (file.exists()) {
                        file.delete();
                    }
                }
            }

            // Clear SharedPreferences
            SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            preferences.edit().remove(KEY_DOWNLOADED_SONGS).apply();

            Log.d(TAG, "All downloads cleared");
            showToast("All downloads cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing downloads: " + e.getMessage());
        }
    }

    // Notification methods
    private void showDownloadNotification(String songName, int progress) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.downloadmusic)
                .setContentTitle("Downloading")
                .setContentText(songName)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, progress, false)
                .setContentIntent(pendingIntent);

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void updateDownloadNotification(String songName, int progress) {
        if (notificationBuilder != null) {
            notificationBuilder
                    .setContentText("Downloading: " + songName + " - " + progress + "%")
                    .setProgress(100, progress, false);

            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private void completeDownloadNotification(String songName) {
        if (notificationBuilder != null) {
            notificationBuilder
                    .setContentTitle("Download Complete")
                    .setContentText(songName)
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .setAutoCancel(true);

            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    // Get total downloaded songs count
    public int getDownloadedSongsCount() {
        return getDownloadedSongs().size();
    }

    // Get total download size
    public long getTotalDownloadSize() {
        long totalSize = 0;
        List<AudioModel> downloadedSongs = getDownloadedSongs();

        for (AudioModel song : downloadedSongs) {
            if (song.getDownloadPath() != null) {
                File file = new File(song.getDownloadPath());
                if (file.exists()) {
                    totalSize += file.length();
                }
            }
        }

        return totalSize;
    }

    // Format file size
    public String formatFileSize(long size) {
        if (size <= 0) return "0 B";

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return String.format(Locale.getDefault(), "%.1f %s",
                size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    // Clean up resources
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}