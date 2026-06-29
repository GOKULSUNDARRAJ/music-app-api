package com.Saalai.SalaiMusicApp;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.lang.ref.WeakReference;

public class BackgroundDownloadHelper {
    private static final String TAG = "BackgroundDownloadHelper";
    private static BackgroundDownloadHelper instance;

    private VideoDownloadService downloadService;
    private boolean isBound = false;
    private Context context;

    // For tracking current download
    private String currentDownloadUrl = "";
    private String currentDownloadTitle = "";
    private int currentDownloadProgress = 0;
    private boolean isDownloading = false;

    // Listener for fragment communication
    private DownloadListener downloadListener;
    private Handler mainHandler;

    // Listener interface
    public interface DownloadListener {
        void onProgress(String videoUrl, String title, int progress);
        void onComplete(String videoUrl, String title, String filePath);
        void onError(String videoUrl, String title, String error);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            VideoDownloadService.LocalBinder binder = (VideoDownloadService.LocalBinder) service;
            downloadService = binder.getService();
            isBound = true;
            Log.d(TAG, "Service connected");

            // Set up listener to get updates from service
            if (downloadService != null && downloadService.getDownloadManager() != null) {
                setupServiceListener();
            }

            // Check for existing downloads
            if (downloadService != null && downloadService.getDownloadManager().hasActiveDownloads()) {
                Log.d(TAG, "Found active downloads, tracking...");
                updateCurrentDownloadState();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            downloadService = null;
            Log.d(TAG, "Service disconnected");
        }
    };

    private BackgroundDownloadHelper(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized BackgroundDownloadHelper getInstance(Context context) {
        if (instance == null) {
            instance = new BackgroundDownloadHelper(context);
        }
        return instance;
    }

    public void startDownloadService() {
        if (!isServiceRunning()) {
            Intent serviceIntent = new Intent(context, VideoDownloadService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            // Bind to the service
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Download service started");
        } else if (!isBound) {
            // Service is running but not bound, bind to it
            Intent serviceIntent = new Intent(context, VideoDownloadService.class);
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void stopDownloadService() {
        if (isBound) {
            context.unbindService(serviceConnection);
            isBound = false;
        }

        if (isServiceRunning()) {
            Intent serviceIntent = new Intent(context, VideoDownloadService.class);
            context.stopService(serviceIntent);
            Log.d(TAG, "Download service stopped");
        }
    }

    public VideoDownloadService getDownloadService() {
        return downloadService;
    }

    public boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
            for (ActivityManager.RunningServiceInfo service : services) {
                if (VideoDownloadService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void startDownloadInBackground(String videoUrl, String videoTitle, String thumbnailUrl) {
        Log.d(TAG, "Starting background download for: " + videoTitle);

        // Update current download state
        currentDownloadUrl = videoUrl;
        currentDownloadTitle = videoTitle;
        currentDownloadProgress = 0;
        isDownloading = true;

        // Ensure service is running
        if (!isServiceRunning()) {
            startDownloadService();
        }

        // Wait a bit for service to connect
        mainHandler.postDelayed(() -> {
            if (downloadService != null && downloadService.getDownloadManager() != null) {
                VideoDownloadManager downloadManager = downloadService.getDownloadManager();

                // Set up progress listener
                final WeakReference<BackgroundDownloadHelper> helperRef = new WeakReference<>(this);
                final String finalVideoUrl = videoUrl;
                final String finalTitle = videoTitle;

                downloadManager.setProgressListener(new VideoDownloadManager.OnDownloadProgressListener() {
                    @Override
                    public void onProgress(int progress) {
                        BackgroundDownloadHelper helper = helperRef.get();
                        if (helper != null) {
                            helper.currentDownloadProgress = progress;
                            helper.isDownloading = true;

                            // Update notification
                            helper.updateDownloadNotification(finalTitle, "Downloading...", progress);

                            // Notify listener
                            helper.notifyProgress(finalVideoUrl, finalTitle, progress);
                        }
                    }

                    @Override
                    public void onComplete(String filePath) {
                        BackgroundDownloadHelper helper = helperRef.get();
                        if (helper != null) {
                            Log.d(TAG, "Download completed: " + filePath);

                            // Update notification
                            helper.updateDownloadNotification(finalTitle, "Download completed", 100);

                            // Notify listener
                            helper.notifyComplete(finalVideoUrl, finalTitle, filePath);

                            // Reset state
                            helper.resetDownloadState();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        BackgroundDownloadHelper helper = helperRef.get();
                        if (helper != null) {
                            Log.e(TAG, "Download failed: " + error);

                            // Update notification
                            helper.updateDownloadNotification(finalTitle, "Download failed: " + error, 0);

                            // Notify listener
                            helper.notifyError(finalVideoUrl, finalTitle, error);

                            // Reset state
                            helper.resetDownloadState();
                        }
                    }

                    @Override
                    public void onPaused() {
                        // Handle pause if needed
                    }

                    @Override
                    public void onResumed() {
                        // Handle resume if needed
                    }
                });

                // Start the download
                downloadManager.downloadVideo(videoUrl, videoTitle, thumbnailUrl);

            } else {
                Log.e(TAG, "Service not available for download, starting directly");
                // Fallback to direct download
                VideoDownloadManager downloadManager = new VideoDownloadManager(context);
                downloadManager.downloadVideo(videoUrl, videoTitle, thumbnailUrl);
            }
        }, 1000);
    }

    public void updateDownloadNotification(String title, String content, int progress) {
        if (downloadService != null) {
            downloadService.updateNotification(title, content, progress);
        }
    }

    public void pauseDownload() {
        if (downloadService != null && downloadService.getDownloadManager() != null) {
            downloadService.getDownloadManager().pauseDownload();
        }
    }

    public void resumeDownload() {
        if (downloadService != null && downloadService.getDownloadManager() != null) {
            downloadService.getDownloadManager().resumeDownload();
        }
    }

    public void cancelDownload() {
        if (downloadService != null && downloadService.getDownloadManager() != null) {
            downloadService.getDownloadManager().cancelDownload();
        }

        // Also reset local state
        resetDownloadState();
    }

    /**
     * Check if a specific video is currently being downloaded
     */
    public boolean isDownloadInProgress(String videoUrl, String videoTitle) {
        // First check our local state
        if (isDownloading && videoUrl.equals(currentDownloadUrl) && videoTitle.equals(currentDownloadTitle)) {
            return true;
        }

        // Then check service if available
        if (downloadService != null && downloadService.getDownloadManager() != null) {
            VideoDownloadManager downloadManager = downloadService.getDownloadManager();

            if (downloadManager.hasActiveDownloads()) {
                String currentUrl = downloadManager.getCurrentDownloadUrl();
                String currentTitle = downloadManager.getCurrentDownloadTitle();

                return videoUrl.equals(currentUrl) && videoTitle.equals(currentTitle);
            }
        }

        // Finally check VideoDownloadManager directly
        VideoDownloadManager downloadManager = new VideoDownloadManager(context);
        if (downloadManager.hasActiveDownloads()) {
            String currentUrl = downloadManager.getCurrentDownloadUrl();
            String currentTitle = downloadManager.getCurrentDownloadTitle();

            return videoUrl.equals(currentUrl) && videoTitle.equals(currentTitle);
        }

        return false;
    }

    /**
     * Get current download progress
     */
    public int getCurrentProgress() {
        if (downloadService != null && downloadService.getDownloadManager() != null) {
            return downloadService.getDownloadManager().getCurrentDownloadProgress();
        }

        return currentDownloadProgress;
    }

    /**
     * Get current download URL
     */
    public String getCurrentDownloadUrl() {
        if (downloadService != null && downloadService.getDownloadManager() != null) {
            return downloadService.getDownloadManager().getCurrentDownloadUrl();
        }

        return currentDownloadUrl;
    }

    /**
     * Get current download title
     */
    public String getCurrentDownloadTitle() {
        if (downloadService != null && downloadService.getDownloadManager() != null) {
            return downloadService.getDownloadManager().getCurrentDownloadTitle();
        }

        return currentDownloadTitle;
    }

    /**
     * Check if any download is in progress
     */
    public boolean isAnyDownloadInProgress() {
        // Check local state
        if (isDownloading) {
            return true;
        }

        // Check service
        if (downloadService != null && downloadService.getDownloadManager() != null) {
            return downloadService.getDownloadManager().hasActiveDownloads();
        }

        // Check directly
        VideoDownloadManager downloadManager = new VideoDownloadManager(context);
        return downloadManager.hasActiveDownloads();
    }

    /**
     * Cancel all downloads
     */
    public void cancelAllDownloads() {
        if (downloadService != null && downloadService.getDownloadManager() != null) {
            downloadService.getDownloadManager().cancelDownload();
        }

        VideoDownloadManager downloadManager = new VideoDownloadManager(context);
        downloadManager.cancelDownload();

        resetDownloadState();
    }

    /**
     * Set download listener for fragment communication
     */
    public void setDownloadListener(DownloadListener listener) {
        this.downloadListener = listener;

        // If we have a listener and a download is in progress, notify current state
        if (listener != null && isDownloading) {
            notifyProgress(currentDownloadUrl, currentDownloadTitle, currentDownloadProgress);
        }
    }

    /**
     * Remove download listener
     */
    public void removeDownloadListener() {
        this.downloadListener = null;
    }

    /**
     * Setup listener to get updates from service
     */
    private void setupServiceListener() {
        if (downloadService != null && downloadService.getDownloadManager() != null) {
            final WeakReference<BackgroundDownloadHelper> helperRef = new WeakReference<>(this);

            downloadService.getDownloadManager().setProgressListener(new VideoDownloadManager.OnDownloadProgressListener() {
                @Override
                public void onProgress(int progress) {
                    BackgroundDownloadHelper helper = helperRef.get();
                    if (helper != null) {
                        helper.updateCurrentDownloadState();
                        helper.currentDownloadProgress = progress;
                        helper.isDownloading = true;

                        String url = helper.getCurrentDownloadUrl();
                        String title = helper.getCurrentDownloadTitle();
                        helper.notifyProgress(url, title, progress);
                    }
                }

                @Override
                public void onComplete(String filePath) {
                    BackgroundDownloadHelper helper = helperRef.get();
                    if (helper != null) {
                        String url = helper.getCurrentDownloadUrl();
                        String title = helper.getCurrentDownloadTitle();
                        helper.notifyComplete(url, title, filePath);
                        helper.resetDownloadState();
                    }
                }

                @Override
                public void onError(String error) {
                    BackgroundDownloadHelper helper = helperRef.get();
                    if (helper != null) {
                        String url = helper.getCurrentDownloadUrl();
                        String title = helper.getCurrentDownloadTitle();
                        helper.notifyError(url, title, error);
                        helper.resetDownloadState();
                    }
                }

                @Override
                public void onPaused() {
                    // Handle pause if needed
                }

                @Override
                public void onResumed() {
                    // Handle resume if needed
                }
            });
        }
    }

    /**
     * Update current download state from service
     */
    private void updateCurrentDownloadState() {
        if (downloadService != null && downloadService.getDownloadManager() != null) {
            VideoDownloadManager downloadManager = downloadService.getDownloadManager();

            if (downloadManager.hasActiveDownloads()) {
                currentDownloadUrl = downloadManager.getCurrentDownloadUrl();
                currentDownloadTitle = downloadManager.getCurrentDownloadTitle();
                currentDownloadProgress = downloadManager.getCurrentDownloadProgress();
                isDownloading = true;
            } else {
                resetDownloadState();
            }
        }
    }

    /**
     * Reset download state
     */
    private void resetDownloadState() {
        currentDownloadUrl = "";
        currentDownloadTitle = "";
        currentDownloadProgress = 0;
        isDownloading = false;
    }

    /**
     * Notify listener about progress (on main thread)
     */
    private void notifyProgress(final String videoUrl, final String title, final int progress) {
        mainHandler.post(() -> {
            if (downloadListener != null) {
                downloadListener.onProgress(videoUrl, title, progress);
            }
        });
    }

    /**
     * Notify listener about completion (on main thread)
     */
    private void notifyComplete(final String videoUrl, final String title, final String filePath) {
        mainHandler.post(() -> {
            if (downloadListener != null) {
                downloadListener.onComplete(videoUrl, title, filePath);
            }
        });
    }

    /**
     * Notify listener about error (on main thread)
     */
    private void notifyError(final String videoUrl, final String title, final String error) {
        mainHandler.post(() -> {
            if (downloadListener != null) {
                downloadListener.onError(videoUrl, title, error);
            }
        });
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        removeDownloadListener();

        if (isBound) {
            context.unbindService(serviceConnection);
            isBound = false;
        }

        if (downloadService != null) {
            downloadService = null;
        }
    }

    /**
     * Bind to service if not already bound
     */
    public void bindServiceIfNeeded() {
        if (!isBound && isServiceRunning()) {
            Intent serviceIntent = new Intent(context, VideoDownloadService.class);
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Force update of current download state
     */
    public void refreshDownloadState() {
        updateCurrentDownloadState();
    }
}