package com.Saalai.SalaiMusicApp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class VideoDownloadService extends Service {
    private static final String TAG = "VideoDownloadService";
    private static final String CHANNEL_ID = "download_channel";
    private static final int NOTIFICATION_ID = 101;

    private VideoDownloadManager downloadManager;
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        VideoDownloadService getService() {
            return VideoDownloadService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        // Initialize download manager
        downloadManager = new VideoDownloadManager(getApplicationContext());

        // Create notification channel
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        // Create a foreground service notification
        Notification notification = createNotification("Video Downloader", "Ready to download videos");
        startForeground(NOTIFICATION_ID, notification);

        // If we get killed, restart the service
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Video Download Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Shows video download progress");
            serviceChannel.setSound(null, null);
            serviceChannel.setVibrationPattern(null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification(String title, String content) {
        // Replace YourMainActivity.class with your actual main activity class
        Intent notificationIntent = new Intent(this, com.Saalai.SalaiMusicApp.Activity.MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT :
                        PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.baseline_download_24) // Use your download icon
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSound(null)
                .setVibrate(null)
                .build();
    }

    public void updateNotification(String title, String content, int progress) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.baseline_download_24)
                .setProgress(100, progress, progress < 0)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSound(null)
                .setVibrate(null)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    public void stopNotification() {
        stopForeground(true);
        stopSelf();
    }

    public VideoDownloadManager getDownloadManager() {
        return downloadManager;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");

        // Save download state before service stops
        if (downloadManager != null) {
            downloadManager.saveDownloadState();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "Task removed, but service continues");

        // Save download state
        if (downloadManager != null) {
            downloadManager.saveDownloadState();
        }
    }
}