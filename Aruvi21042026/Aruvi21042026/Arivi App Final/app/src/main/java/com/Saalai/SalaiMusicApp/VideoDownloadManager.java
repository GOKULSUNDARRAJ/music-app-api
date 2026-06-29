package com.Saalai.SalaiMusicApp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

public class VideoDownloadManager {
    private static final String TAG = "VideoDownloadManager";

    private Context context;
    private SharedPreferences prefs;
    private OnDownloadProgressListener progressListener;
    private Handler mainHandler;
    private boolean isCancelled = false;

    // Active download tracking
    private String currentDownloadUrl = "";
    private String currentDownloadTitle = "";
    private boolean isDownloadActive = false;
    private int currentDownloadProgress = 0;
    private static final String KEY_ACTIVE_DOWNLOAD = "active_download";
    private static final String KEY_ACTIVE_URL = "active_url";
    private static final String KEY_ACTIVE_TITLE = "active_title";
    private static final String KEY_ACTIVE_PROGRESS = "active_progress";

    // Download state persistence
    private static final String PREF_DOWNLOAD_STATE = "download_state_prefs";
    private static final String KEY_DOWNLOAD_JSON = "active_download_json";

    // Download parameters
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 2000;
    private static final int CONNECTION_TIMEOUT = 120000; // 2 minutes
    private static final int READ_TIMEOUT = 300000;       // 5 minutes
    private static final int BUFFER_SIZE = 8192;
    private static final long CHUNK_SIZE = 10 * 1024 * 1024; // 10MB chunks

    // Current retry state
    private int retryCount = 0;
    private String lastError = "";
    private long lastUpdateTime = 0;

    // File tracking
    private File currentOutputFile;
    private long downloadedBytes = 0;
    private long totalFileSize = 0;

    // Current connections (make them instance variables)
    private HttpURLConnection currentConnection;
    private InputStream currentInputStream;
    private FileOutputStream currentOutputStream;

    public interface OnDownloadProgressListener {
        void onProgress(int progress);
        void onComplete(String filePath);
        void onError(String error);
        void onPaused();
        void onResumed();
    }

    public VideoDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences("downloaded_videos", Context.MODE_PRIVATE);
        this.mainHandler = new Handler(Looper.getMainLooper());

        loadActiveDownloadState();
        restoreDownloadState();

        Log.d(TAG, "VideoDownloadManager initialized");
    }

    // ==================== STATE MANAGEMENT ====================

    private void loadActiveDownloadState() {
        isDownloadActive = prefs.getBoolean(KEY_ACTIVE_DOWNLOAD, false);
        currentDownloadUrl = prefs.getString(KEY_ACTIVE_URL, "");
        currentDownloadTitle = prefs.getString(KEY_ACTIVE_TITLE, "");
        currentDownloadProgress = prefs.getInt(KEY_ACTIVE_PROGRESS, 0);

        if (isDownloadActive) {
            Log.d(TAG, "Loaded active download state: " + currentDownloadTitle +
                    ", Progress: " + currentDownloadProgress + "%");
        }
    }

    private void saveActiveDownloadState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_ACTIVE_DOWNLOAD, isDownloadActive);
        editor.putString(KEY_ACTIVE_URL, currentDownloadUrl);
        editor.putString(KEY_ACTIVE_TITLE, currentDownloadTitle);
        editor.putInt(KEY_ACTIVE_PROGRESS, currentDownloadProgress);
        editor.apply();
    }

    private void clearActiveDownloadState() {
        isDownloadActive = false;
        currentDownloadUrl = "";
        currentDownloadTitle = "";
        currentDownloadProgress = 0;

        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_ACTIVE_DOWNLOAD);
        editor.remove(KEY_ACTIVE_URL);
        editor.remove(KEY_ACTIVE_TITLE);
        editor.remove(KEY_ACTIVE_PROGRESS);
        editor.apply();

        Log.d(TAG, "Active download state cleared");
    }

    public void saveDownloadState() {
        if (isDownloadActive && !currentDownloadUrl.isEmpty()) {
            try {
                JSONObject state = new JSONObject();
                state.put("url", currentDownloadUrl);
                state.put("title", currentDownloadTitle);
                state.put("progress", currentDownloadProgress);
                state.put("downloaded_bytes", downloadedBytes);
                state.put("total_size", totalFileSize);
                state.put("timestamp", System.currentTimeMillis());

                if (currentOutputFile != null) {
                    state.put("file_path", currentOutputFile.getAbsolutePath());
                }

                SharedPreferences statePrefs = context.getSharedPreferences(PREF_DOWNLOAD_STATE, Context.MODE_PRIVATE);
                statePrefs.edit().putString(KEY_DOWNLOAD_JSON, state.toString()).apply();

                Log.d(TAG, "Download state saved: " + currentDownloadTitle +
                        ", Progress: " + currentDownloadProgress + "%");

            } catch (JSONException e) {
                Log.e(TAG, "Error saving download state", e);
            }
        }
    }

    public boolean restoreDownloadState() {
        SharedPreferences statePrefs = context.getSharedPreferences(PREF_DOWNLOAD_STATE, Context.MODE_PRIVATE);
        String stateJson = statePrefs.getString(KEY_DOWNLOAD_JSON, null);

        if (stateJson != null) {
            try {
                JSONObject state = new JSONObject(stateJson);
                currentDownloadUrl = state.getString("url");
                currentDownloadTitle = state.getString("title");
                currentDownloadProgress = state.getInt("progress");
                downloadedBytes = state.optLong("downloaded_bytes", 0);
                totalFileSize = state.optLong("total_size", 0);
                long timestamp = state.getLong("timestamp");

                // Check if download is recent (within last 2 hours)
                if (System.currentTimeMillis() - timestamp < 7200000) {
                    isDownloadActive = true;

                    if (state.has("file_path")) {
                        String filePath = state.getString("file_path");
                        currentOutputFile = new File(filePath);
                        if (!currentOutputFile.exists()) {
                            Log.w(TAG, "Saved file doesn't exist: " + filePath);
                            currentOutputFile = null;
                            downloadedBytes = 0;
                        }
                    }

                    Log.d(TAG, "Download state restored: " + currentDownloadTitle +
                            ", Progress: " + currentDownloadProgress + "%");
                    return true;
                } else {
                    // Clear stale state
                    clearDownloadState();
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error restoring download state", e);
                clearDownloadState();
            }
        }
        return false;
    }

    private void clearDownloadState() {
        SharedPreferences statePrefs = context.getSharedPreferences(PREF_DOWNLOAD_STATE, Context.MODE_PRIVATE);
        statePrefs.edit().remove(KEY_DOWNLOAD_JSON).apply();
        Log.d(TAG, "Download state cleared");
    }

    // ==================== PUBLIC METHODS ====================

    public void setProgressListener(OnDownloadProgressListener listener) {
        this.progressListener = listener;

        if (isDownloadActive && progressListener != null && currentDownloadProgress > 0) {
            mainHandler.post(() -> {
                progressListener.onProgress(currentDownloadProgress);
                Log.d(TAG, "Notified listener of existing progress: " + currentDownloadProgress + "%");
            });
        }
    }

    public void cancelDownload() {
        isCancelled = true;
        isDownloadActive = false;
        clearActiveDownloadState();
        clearDownloadState();

        closeCurrentResources();

        if (currentOutputFile != null && currentOutputFile.exists() && currentOutputFile.length() < 1024 * 1024) {
            boolean deleted = currentOutputFile.delete();
            Log.d(TAG, "Deleted partial file: " + deleted);
        }

        mainHandler.post(() -> {
            if (progressListener != null) {
                progressListener.onError("Download cancelled");
            }
        });

        Log.d(TAG, "Download cancelled");
    }

    public void pauseDownload() {
        isCancelled = true;
        saveDownloadState();

        mainHandler.post(() -> {
            if (progressListener != null) {
                progressListener.onPaused();
            }
        });

        Log.d(TAG, "Download paused at " + currentDownloadProgress + "%");
    }

    public void resumeDownload() {
        if (!isDownloadActive || currentDownloadUrl.isEmpty()) {
            Log.w(TAG, "No download to resume");
            return;
        }

        Log.d(TAG, "Resuming download: " + currentDownloadTitle);

        mainHandler.post(() -> {
            if (progressListener != null) {
                progressListener.onResumed();
                progressListener.onProgress(currentDownloadProgress);
            }
        });

        downloadVideo(currentDownloadUrl, currentDownloadTitle, "");
    }

    public void downloadVideo(String videoUrl, String videoTitle, String thumbnailUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            notifyError("No video URL provided");
            return;
        }

        if (isDownloadActive && currentDownloadUrl.equals(videoUrl)) {
            Log.w(TAG, "Download already in progress for: " + videoTitle);
            return;
        }

        // Reset state
        isCancelled = false;
        retryCount = 0;
        lastError = "";

        // Set active download state
        isDownloadActive = true;
        currentDownloadUrl = videoUrl;
        currentDownloadTitle = videoTitle != null ? videoTitle : "Unknown Video";
        currentDownloadProgress = 0;
        saveActiveDownloadState();

        Log.d(TAG, "Starting download: " + videoTitle);
        Log.d(TAG, "Video URL: " + videoUrl);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Convert M3U8 to MP4 if needed
                String downloadUrl = videoUrl;
                if (videoUrl.contains("/playlist.m3u8")) {
                    downloadUrl = videoUrl.replace("/playlist.m3u8", "");
                    Log.d(TAG, "Converted M3U8 to MP4: " + downloadUrl);
                } else if (videoUrl.endsWith(".m3u8")) {
                    downloadUrl = videoUrl.replace(".m3u8", ".mp4");
                    Log.d(TAG, "Converted .m3u8 to .mp4: " + downloadUrl);
                }

                downloadDirectVideo(downloadUrl, currentDownloadTitle, thumbnailUrl);
            } catch (Exception e) {
                Log.e(TAG, "Download failed: " + e.getMessage(), e);
                clearActiveDownloadState();
                clearDownloadState();

                final String errorMessage = e.getMessage();
                mainHandler.post(() -> {
                    if (progressListener != null) {
                        progressListener.onError(errorMessage != null ? errorMessage : "Unknown error");
                    }
                });
            }
        });
    }

    // ==================== DOWNLOAD IMPLEMENTATION ====================

    private void downloadDirectVideo(String videoUrl, String videoTitle, String thumbnailUrl) {
        retryCount = 0;
        downloadDirectVideoWithRetry(videoUrl, videoTitle, thumbnailUrl);
    }

    private void downloadDirectVideoWithRetry(String videoUrl, String videoTitle, String thumbnailUrl) {
        try {
            retryCount++;
            Log.d(TAG, "Download attempt " + retryCount + "/" + MAX_RETRY_ATTEMPTS +
                    " for: " + videoTitle);

            doDownloadDirectVideo(videoUrl, videoTitle, thumbnailUrl);

        } catch (SocketException | SocketTimeoutException e) {
            handleNetworkException(e, videoUrl, videoTitle, thumbnailUrl);
        } catch (IOException e) {
            handleIOException(e, videoUrl, videoTitle, thumbnailUrl);
        } catch (Exception e) {
            handleGenericException(e, videoUrl, videoTitle, thumbnailUrl);
        }
    }

    private void doDownloadDirectVideo(String videoUrl, String videoTitle, String thumbnailUrl) throws Exception {
        try {
            Log.d(TAG, "Attempting to download: " + videoUrl);

            // Get file info first
            URL url = new URL(videoUrl);
            currentConnection = (HttpURLConnection) url.openConnection();

            // Set connection properties
            Map<String, String> headers = getDownloadHeaders();
            for (Map.Entry<String, String> header : headers.entrySet()) {
                currentConnection.setRequestProperty(header.getKey(), header.getValue());
            }

            // Handle resume
            boolean isResume = false;
            if (downloadedBytes > 0 && currentOutputFile != null && currentOutputFile.exists()) {
                currentConnection.setRequestProperty("Range", "bytes=" + downloadedBytes + "-");
                isResume = true;
                Log.d(TAG, "Resuming download from byte: " + downloadedBytes);
            }

            currentConnection.setRequestMethod("GET");
            currentConnection.setConnectTimeout(CONNECTION_TIMEOUT);
            currentConnection.setReadTimeout(READ_TIMEOUT);
            currentConnection.setInstanceFollowRedirects(true);

            currentConnection.connect();

            int responseCode = currentConnection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK ||
                    responseCode == HttpURLConnection.HTTP_PARTIAL ||
                    responseCode == 206) {

                handleSuccessfulConnection(videoUrl, videoTitle, thumbnailUrl, isResume);

            } else if (responseCode >= 400 && responseCode < 500) {
                throw new Exception("Client error: " + responseCode + " - " + currentConnection.getResponseMessage());

            } else if (responseCode >= 500) {
                throw new Exception("Server error: " + responseCode + " - " + currentConnection.getResponseMessage());

            } else {
                throw new Exception("Unexpected response: " + responseCode);
            }

        } finally {
            closeCurrentResources();
        }
    }

    private Map<String, String> getDownloadHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.put("Accept", "video/mp4,video/*;q=0.9,*/*;q=0.8");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        headers.put("Accept-Encoding", "identity");
        headers.put("Connection", "keep-alive");
        headers.put("Keep-Alive", "timeout=60, max=1000");
        headers.put("Referer", "https://salaitvapp.com/");
        return headers;
    }

    private void handleSuccessfulConnection(String videoUrl,
                                            String videoTitle,
                                            String thumbnailUrl,
                                            boolean isResume) throws Exception {

        // Get content length
        totalFileSize = currentConnection.getContentLength();

        if (totalFileSize <= 0) {
            // Try to get from Content-Range header
            String contentRange = currentConnection.getHeaderField("Content-Range");
            if (contentRange != null && contentRange.contains("/")) {
                try {
                    String totalSizeStr = contentRange.substring(contentRange.indexOf("/") + 1);
                    totalFileSize = Long.parseLong(totalSizeStr);
                    Log.d(TAG, "Got file size from Content-Range: " + formatFileSize(totalFileSize));
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Could not parse total size from Content-Range: " + contentRange);
                }
            }

            // If still 0, estimate from server (for streaming)
            if (totalFileSize <= 0) {
                Log.w(TAG, "Server didn't provide file size, estimating...");
                totalFileSize = 500 * 1024 * 1024; // Estimate 500MB for movies
            }
        }

        Log.d(TAG, "Total file size: " + formatFileSize(totalFileSize));

        // Create or use existing file
        if (!isResume || currentOutputFile == null) {
            File appDir = createDownloadDirectory();
            String safeTitle = createSafeFilename(videoTitle);
            currentOutputFile = createOutputFile(appDir, safeTitle);
            downloadedBytes = 0;

            // Create empty file immediately
            if (!currentOutputFile.exists()) {
                boolean created = currentOutputFile.createNewFile();
                Log.d(TAG, "Created new file: " + created + ", path: " + currentOutputFile.getAbsolutePath());
            }
        } else if (isResume) {
            downloadedBytes = currentOutputFile.length();
            Log.d(TAG, "Resuming from existing file size: " + formatFileSize(downloadedBytes));
        }

        // Download in chunks
        downloadInChunks(videoUrl, videoTitle, thumbnailUrl);
    }

    private void downloadInChunks(String videoUrl, String videoTitle, String thumbnailUrl) throws Exception {
        long startByte = downloadedBytes;

        // Check if we need to create the file first
        if (currentOutputFile == null || !currentOutputFile.exists()) {
            Log.e(TAG, "Output file doesn't exist before download!");
            throw new IOException("Output file not created");
        }

        Log.d(TAG, "Starting chunked download from byte: " + startByte + ", total size: " + totalFileSize);

        while (startByte < totalFileSize && !isCancelled) {
            long endByte = Math.min(startByte + CHUNK_SIZE - 1, totalFileSize - 1);

            Log.d(TAG, "Downloading chunk: " + formatFileSize(startByte) + " - " +
                    formatFileSize(endByte) + " (" + formatFileSize(endByte - startByte + 1) + ")");

            if (downloadChunk(videoUrl, startByte, endByte)) {
                startByte = endByte + 1;
                downloadedBytes = startByte;

                // Update progress
                int progress = totalFileSize > 0 ? (int) ((startByte * 100) / totalFileSize) : 0;
                currentDownloadProgress = progress;
                saveActiveDownloadState();

                updateProgress(progress, startByte, totalFileSize);

                // Save state after each chunk
                saveDownloadState();

                // Small delay between chunks
                Thread.sleep(100);
            } else {
                throw new IOException("Chunk download failed");
            }
        }

        if (!isCancelled) {
            // Only verify if we actually downloaded something
            if (downloadedBytes > 0 && currentOutputFile != null && currentOutputFile.exists()) {
                // Verify download
                verifyDownload(currentOutputFile, totalFileSize, downloadedBytes);

                // Save video info
                saveVideoInfo(videoTitle, videoUrl, currentOutputFile.getAbsolutePath(), thumbnailUrl);

                // Clear all states
                clearActiveDownloadState();
                clearDownloadState();

                // Notify completion
                notifyCompletion(currentOutputFile.getAbsolutePath());

                Log.d(TAG, "Download completed successfully: " + videoTitle);
            } else {
                throw new IOException("No data downloaded or file doesn't exist");
            }
        }

        // Clean up
        currentOutputFile = null;
        downloadedBytes = 0;
        totalFileSize = 0;
    }

    private boolean downloadChunk(String videoUrl, long startByte, long endByte) {
        HttpURLConnection chunkConnection = null;
        InputStream chunkInput = null;
        RandomAccessFile raf = null;

        try {
            // Make sure file exists before trying to write
            if (currentOutputFile == null || !currentOutputFile.exists()) {
                Log.e(TAG, "Output file doesn't exist!");
                return false;
            }

            URL url = new URL(videoUrl);
            chunkConnection = (HttpURLConnection) url.openConnection();

            // Set headers
            Map<String, String> headers = getDownloadHeaders();
            for (Map.Entry<String, String> header : headers.entrySet()) {
                chunkConnection.setRequestProperty(header.getKey(), header.getValue());
            }

            // Set range for this chunk
            String range = "bytes=" + startByte + "-" + endByte;
            chunkConnection.setRequestProperty("Range", range);
            Log.d(TAG, "Requesting range: " + range);

            chunkConnection.setRequestMethod("GET");
            chunkConnection.setConnectTimeout(30000);
            chunkConnection.setReadTimeout(60000);
            chunkConnection.setInstanceFollowRedirects(true);

            chunkConnection.connect();

            int responseCode = chunkConnection.getResponseCode();
            Log.d(TAG, "Chunk response code: " + responseCode + ", size: " + formatFileSize(endByte - startByte + 1));

            if (responseCode == HttpURLConnection.HTTP_PARTIAL || responseCode == 206 || responseCode == 200) {
                chunkInput = new BufferedInputStream(chunkConnection.getInputStream(), BUFFER_SIZE);

                // Open file for random access
                raf = new RandomAccessFile(currentOutputFile, "rw");
                raf.seek(startByte);

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long chunkDownloaded = 0;

                while ((bytesRead = chunkInput.read(buffer)) != -1) {
                    if (isCancelled) {
                        return false;
                    }

                    raf.write(buffer, 0, bytesRead);
                    chunkDownloaded += bytesRead;

                    // Update progress periodically
                    if (chunkDownloaded % (1024 * 1024) == 0) {
                        long totalDownloaded = startByte + chunkDownloaded;
                        int progress = totalFileSize > 0 ? (int) ((totalDownloaded * 100) / totalFileSize) : 0;
                        updateProgress(progress, totalDownloaded, totalFileSize);
                    }
                }

                Log.d(TAG, "Chunk completed: downloaded " + formatFileSize(chunkDownloaded));
                return true;

            } else {
                Log.e(TAG, "Unexpected response for chunk: " + responseCode);
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error downloading chunk: " + e.getMessage());
            return false;

        } finally {
            try {
                if (chunkInput != null) chunkInput.close();
            } catch (Exception e) {
                Log.w(TAG, "Error closing chunk input", e);
            }

            try {
                if (raf != null) raf.close();
            } catch (Exception e) {
                Log.w(TAG, "Error closing file", e);
            }

            if (chunkConnection != null) {
                chunkConnection.disconnect();
            }
        }
    }

    private void closeCurrentResources() {
        try {
            if (currentInputStream != null) {
                currentInputStream.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error closing input stream", e);
        }

        try {
            if (currentOutputStream != null) {
                currentOutputStream.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error closing output stream", e);
        }

        if (currentConnection != null) {
            currentConnection.disconnect();
        }

        currentInputStream = null;
        currentOutputStream = null;
        currentConnection = null;
    }

    // ==================== EXCEPTION HANDLING ====================

    private void handleNetworkException(Exception e,
                                        String videoUrl,
                                        String videoTitle,
                                        String thumbnailUrl) {
        lastError = "Network error: " + e.getMessage();
        Log.e(TAG, lastError, e);

        if (retryCount < MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "Retrying download... (" + retryCount + "/" + MAX_RETRY_ATTEMPTS + ")");

            saveDownloadState();

            mainHandler.post(() -> {
                if (progressListener != null) {
                    progressListener.onError("Connection lost. Retrying... (Attempt " + retryCount + ")");
                }
            });

            try {
                Thread.sleep(RETRY_DELAY_MS * retryCount);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            downloadDirectVideoWithRetry(videoUrl, videoTitle, thumbnailUrl);

        } else {
            saveDownloadState();
            mainHandler.post(() -> {
                if (progressListener != null) {
                    progressListener.onError("Failed after " + MAX_RETRY_ATTEMPTS + " attempts. You can resume later.");
                }
            });
        }
    }

    private void handleIOException(IOException e,
                                   String videoUrl,
                                   String videoTitle,
                                   String thumbnailUrl) {
        lastError = "IO error: " + e.getMessage();
        Log.e(TAG, lastError, e);

        if (retryCount < MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "Retrying download due to IO error...");
            saveDownloadState();

            mainHandler.post(() -> {
                if (progressListener != null) {
                    progressListener.onError("IO Error. Retrying...");
                }
            });

            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            downloadDirectVideoWithRetry(videoUrl, videoTitle, thumbnailUrl);

        } else {
            saveDownloadState();
            mainHandler.post(() -> {
                if (progressListener != null) {
                    progressListener.onError(lastError);
                }
            });
        }
    }

    private void handleGenericException(Exception e,
                                        String videoUrl,
                                        String videoTitle,
                                        String thumbnailUrl) {
        lastError = "Download error: " + e.getMessage();
        Log.e(TAG, lastError, e);

        clearActiveDownloadState();
        clearDownloadState();

        mainHandler.post(() -> {
            if (progressListener != null) {
                progressListener.onError(lastError);
            }
        });
    }

    private void notifyError(String error) {
        mainHandler.post(() -> {
            if (progressListener != null) {
                progressListener.onError(error);
            }
        });
    }

    // ==================== FILE MANAGEMENT ====================

    private File createDownloadDirectory() throws IOException {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File appDir = new File(downloadsDir, "SalaiTV Videos");

        if (!appDir.exists()) {
            boolean created = appDir.mkdirs();
            if (!created) {
                throw new IOException("Failed to create download directory: " + appDir.getAbsolutePath());
            }
            Log.d(TAG, "Created download directory: " + appDir.getAbsolutePath());
        }

        // Create .nomedia file
        File noMedia = new File(appDir, ".nomedia");
        if (!noMedia.exists()) {
            noMedia.createNewFile();
        }

        return appDir;
    }

    private String createSafeFilename(String title) {
        if (title == null || title.isEmpty()) {
            return "video_" + System.currentTimeMillis();
        }

        String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .trim();

        if (safeTitle.length() > 100) {
            safeTitle = safeTitle.substring(0, 100);
        }

        return safeTitle;
    }

    private File createOutputFile(File directory, String safeTitle) throws IOException {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String fileName = safeTitle + "_" + timestamp + ".mp4";
        File outputFile = new File(directory, fileName);

        // Create parent directories if they don't exist
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Create the file immediately
        if (!outputFile.exists()) {
            boolean created = outputFile.createNewFile();
            Log.d(TAG, "Created output file: " + created + ", path: " + outputFile.getAbsolutePath());
        }

        return outputFile;
    }

    private void verifyDownload(File outputFile, long expectedSize, long actualSize) throws Exception {
        if (!outputFile.exists()) {
            throw new Exception("Downloaded file does not exist");
        }

        long fileSize = outputFile.length();
        Log.d(TAG, "Download verification:");
        Log.d(TAG, "  File path: " + outputFile.getAbsolutePath());
        Log.d(TAG, "  Expected size: " + formatFileSize(expectedSize));
        Log.d(TAG, "  Actual size: " + formatFileSize(fileSize));
        Log.d(TAG, "  Written bytes: " + formatFileSize(actualSize));

        if (fileSize < 1024 * 100 && outputFile.getName().toLowerCase().endsWith(".mp4")) {
            Log.w(TAG, "File is very small for a video: " + fileSize + " bytes");

            try (FileInputStream fis = new FileInputStream(outputFile)) {
                byte[] header = new byte[8];
                if (fis.read(header) >= 8) {
                    if (header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p') {
                        Log.d(TAG, "File has valid MP4 signature");
                        return;
                    }
                }
            }

            if (fileSize < 1024 * 10) {
                outputFile.delete();
                throw new Exception("Downloaded file is too small and not a valid video (" + fileSize + " bytes)");
            }
        }

        Log.d(TAG, "Download verification completed");
    }

    // ==================== PROGRESS UPDATES ====================

    private void updateProgress(int progress, long downloaded, long total) {
        Log.d(TAG, "Progress update: " + progress + "% (" +
                formatFileSize(downloaded) + "/" + formatFileSize(total) + ")");

        final int finalProgress = progress;
        mainHandler.post(() -> {
            if (progressListener != null) {
                progressListener.onProgress(finalProgress);
                Log.d(TAG, "Progress listener notified: " + finalProgress + "%");
            }
        });
    }

    private void notifyCompletion(String filePath) {
        mainHandler.post(() -> {
            if (progressListener != null) {
                progressListener.onProgress(100);
                progressListener.onComplete(filePath);
            }
        });
    }

    // ==================== PUBLIC QUERY METHODS ====================

    public boolean hasActiveDownloads() {
        return isDownloadActive;
    }

    public String getCurrentDownloadUrl() {
        return currentDownloadUrl;
    }

    public String getCurrentDownloadTitle() {
        return currentDownloadTitle;
    }

    public int getCurrentDownloadProgress() {
        return currentDownloadProgress;
    }

    public boolean isDownloadingSameVideo(String url, String title) {
        return isDownloadActive &&
                currentDownloadUrl.equals(url) &&
                currentDownloadTitle.equals(title);
    }

    public void retryDownload() {
        if (isDownloadActive && !currentDownloadUrl.isEmpty()) {
            Log.d(TAG, "Manually retrying download: " + currentDownloadTitle);
            downloadVideo(currentDownloadUrl, currentDownloadTitle, "");
        }
    }

    // ==================== VIDEO INFO MANAGEMENT ====================

    private void saveVideoInfo(String title, String originalUrl, String filePath, String thumbnailUrl) {
        String key = "video_" + UUID.randomUUID().toString();

        String videoData = title + "||" +
                originalUrl + "||" +
                filePath + "||" +
                (thumbnailUrl != null ? thumbnailUrl : "") + "||" +
                System.currentTimeMillis();

        prefs.edit().putString(key, videoData).apply();

        String allVideos = prefs.getString("all_downloaded_videos", "");
        if (!allVideos.isEmpty()) {
            allVideos += "," + key;
        } else {
            allVideos = key;
        }
        prefs.edit().putString("all_downloaded_videos", allVideos).apply();

        Log.d(TAG, "Saved video info for: " + title);
    }

    public List<DownloadedVideo> getDownloadedVideos() {
        List<DownloadedVideo> videos = new ArrayList<>();
        String allVideosKeys = prefs.getString("all_downloaded_videos", "");

        if (!allVideosKeys.isEmpty()) {
            String[] keys = allVideosKeys.split(",");

            for (String key : keys) {
                if (key != null && !key.trim().isEmpty()) {
                    String videoData = prefs.getString(key.trim(), "");

                    if (!videoData.isEmpty()) {
                        String[] parts = videoData.split("\\|\\|");
                        if (parts.length >= 4) {
                            DownloadedVideo video = new DownloadedVideo();
                            video.setTitle(parts[0]);
                            video.setOriginalUrl(parts[1]);
                            video.setFilePath(parts[2]);
                            video.setThumbnailUrl(parts[3]);

                            if (parts.length >= 5) {
                                try {
                                    video.setDownloadTime(Long.parseLong(parts[4]));
                                } catch (NumberFormatException e) {
                                    video.setDownloadTime(System.currentTimeMillis());
                                }
                            }

                            File file = new File(video.getFilePath());
                            if (file.exists() && file.length() > 1024) {
                                videos.add(video);
                            } else {
                                removeVideoKey(key.trim());
                            }
                        }
                    }
                }
            }
        }

        Log.d(TAG, "Found " + videos.size() + " downloaded videos");
        return videos;
    }

    private void removeVideoKey(String key) {
        String allVideosKeys = prefs.getString("all_downloaded_videos", "");
        if (!allVideosKeys.isEmpty()) {
            String[] keys = allVideosKeys.split(",");
            StringBuilder newKeys = new StringBuilder();

            for (String k : keys) {
                if (!k.trim().equals(key.trim())) {
                    if (newKeys.length() > 0) {
                        newKeys.append(",");
                    }
                    newKeys.append(k.trim());
                }
            }

            prefs.edit().putString("all_downloaded_videos", newKeys.toString()).apply();
            prefs.edit().remove(key).apply();

            Log.d(TAG, "Removed invalid video key: " + key);
        }
    }

    public boolean isVideoDownloaded(String videoUrl, String videoTitle) {
        List<DownloadedVideo> downloadedVideos = getDownloadedVideos();
        for (DownloadedVideo video : downloadedVideos) {
            if (video.getOriginalUrl().equals(videoUrl) ||
                    video.getTitle().equals(videoTitle)) {
                Log.d(TAG, "Video found in downloads: " + videoTitle);
                return true;
            }
        }
        Log.d(TAG, "Video not found in downloads: " + videoTitle);
        return false;
    }

    public void deleteVideo(DownloadedVideo video) {
        try {
            File file = new File(video.getFilePath());
            if (file.exists()) {
                boolean deleted = file.delete();
                Log.d(TAG, "File deleted: " + deleted + ", Path: " + video.getFilePath());
            }

            removeVideoByFilePath(video.getFilePath());

        } catch (Exception e) {
            Log.e(TAG, "Error deleting video: " + e.getMessage());
        }
    }

    private void removeVideoByFilePath(String filePath) {
        String allVideosKeys = prefs.getString("all_downloaded_videos", "");
        if (!allVideosKeys.isEmpty()) {
            String[] keys = allVideosKeys.split(",");
            StringBuilder newKeys = new StringBuilder();

            for (String key : keys) {
                if (!key.isEmpty()) {
                    String videoData = prefs.getString(key, "");
                    if (!videoData.isEmpty()) {
                        String[] parts = videoData.split("\\|\\|");
                        if (parts.length >= 3 && !parts[2].equals(filePath)) {
                            if (newKeys.length() > 0) {
                                newKeys.append(",");
                            }
                            newKeys.append(key);
                        } else {
                            prefs.edit().remove(key).apply();
                            Log.d(TAG, "Removed from prefs: " + key);
                        }
                    }
                }
            }

            prefs.edit().putString("all_downloaded_videos", newKeys.toString()).apply();
        }
    }

    // ==================== UTILITY METHODS ====================

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    // ==================== INNER CLASSES ====================

    public static class DownloadedVideo {
        private String title;
        private String originalUrl;
        private String filePath;
        private String thumbnailUrl;
        private long downloadTime;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getOriginalUrl() { return originalUrl; }
        public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

        public long getDownloadTime() { return downloadTime; }
        public void setDownloadTime(long downloadTime) { this.downloadTime = downloadTime; }

        public String getFormattedDownloadTime() {
            return android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", downloadTime).toString();
        }

        public String getFileExtension() {
            if (filePath != null && filePath.contains(".")) {
                return filePath.substring(filePath.lastIndexOf("."));
            }
            return "";
        }

        public boolean isM3U8File() {
            return getFileExtension().equalsIgnoreCase(".m3u8");
        }

        public boolean isMP4File() {
            return getFileExtension().equalsIgnoreCase(".mp4");
        }

        public boolean isValidVideoFile() {
            File file = new File(filePath);
            return file.exists() && file.length() > 1024;
        }

        public long getFileSize() {
            File file = new File(filePath);
            return file.exists() ? file.length() : 0;
        }

        public String getFormattedFileSize() {
            long bytes = getFileSize();
            if (bytes <= 0) return "0 B";
            final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
            int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
            return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
        }
    }


    public void clearDownloadState(String videoTitle) {
        if (currentDownloadTitle != null && currentDownloadTitle.equals(videoTitle)) {
            currentDownloadProgress = 0;
            currentDownloadTitle = "";
            currentDownloadUrl = "";
            saveDownloadState();
        }
    }
}