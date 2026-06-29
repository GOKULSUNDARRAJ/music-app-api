package com.Saalai.SalaiMusicApp.Notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.Saalai.SalaiMusicApp.Activity.MainActivity;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.R;
import com.squareup.picasso.Picasso;

import java.io.IOException;

public class NotificationHelper {
    public static final String CHANNEL_ID = "music_channel";
    public static final int NOTIFICATION_ID = 1;
    private static final String TAG = "NotificationHelper";

    private Context context;
    private NotificationManagerCompat notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Music player notifications");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setSound(null, null); // Remove notification sound
            channel.enableVibration(false); // Remove vibration

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void showNotification(AudioModel audio, boolean isPlaying) {
        if (audio == null) {
            Log.e(TAG, "Audio model is null");
            return;
        }

        // Create intent for opening app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Create actions
        Intent previousIntent = new Intent(context, NotificationActionService.class)
                .setAction("PREVIOUS");
        PendingIntent previousPendingIntent = PendingIntent.getService(context, 0,
                previousIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent playPauseIntent = new Intent(context, NotificationActionService.class)
                .setAction(isPlaying ? "PAUSE" : "PLAY");
        PendingIntent playPausePendingIntent = PendingIntent.getService(context, 0,
                playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(context, NotificationActionService.class)
                .setAction("NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getService(context, 0,
                nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent closeIntent = new Intent(context, NotificationActionService.class)
                .setAction("CLOSE");
        PendingIntent closePendingIntent = PendingIntent.getService(context, 0,
                closeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Load album art synchronously first, then update notification if needed
        loadAlbumArtAndShowNotification(audio, isPlaying, pendingIntent, previousPendingIntent,
                playPausePendingIntent, nextPendingIntent, closePendingIntent);
    }

    private void loadAlbumArtAndShowNotification(AudioModel audio, boolean isPlaying,
                                                 PendingIntent pendingIntent, PendingIntent previousPendingIntent,
                                                 PendingIntent playPausePendingIntent, PendingIntent nextPendingIntent,
                                                 PendingIntent closePendingIntent) {

        Bitmap defaultAlbumArt = BitmapFactory.decodeResource(context.getResources(), R.drawable.video_placholder);

        // Show notification immediately with default image
        buildAndShowNotification(audio, isPlaying, defaultAlbumArt, pendingIntent,
                previousPendingIntent, playPausePendingIntent, nextPendingIntent, closePendingIntent);

        // Then try to load the actual image asynchronously and update
        if (audio.getImageUrl() != null && !audio.getImageUrl().isEmpty()) {
            loadAlbumArtAsync(audio, isPlaying, pendingIntent, previousPendingIntent,
                    playPausePendingIntent, nextPendingIntent, closePendingIntent);
        }
    }

    private void loadAlbumArtAsync(AudioModel audio, boolean isPlaying,
                                   PendingIntent pendingIntent, PendingIntent previousPendingIntent,
                                   PendingIntent playPausePendingIntent, PendingIntent nextPendingIntent,
                                   PendingIntent closePendingIntent) {

        new Thread(() -> {
            try {
                Bitmap albumArt = Picasso.get()
                        .load(audio.getImageUrl())
                        .resize(256, 256) // Resize to appropriate size for notification
                        .centerCrop()
                        .get();

                // Update notification with actual image
                buildAndShowNotification(audio, isPlaying, albumArt, pendingIntent,
                        previousPendingIntent, playPausePendingIntent, nextPendingIntent, closePendingIntent);

            } catch (IOException e) {
                Log.e(TAG, "Failed to load album art: " + e.getMessage());
                // Keep using default image, no need to update
            } catch (Exception e) {
                Log.e(TAG, "Error loading album art: " + e.getMessage());
            }
        }).start();
    }

    private void buildAndShowNotification(AudioModel audio, boolean isPlaying, Bitmap albumArt,
                                          PendingIntent pendingIntent, PendingIntent previousPendingIntent,
                                          PendingIntent playPausePendingIntent, PendingIntent nextPendingIntent,
                                          PendingIntent closePendingIntent) {

        Log.d(TAG, "Building notification for: " + audio.getAudioName() + ", image: " + (albumArt != null));

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_music_note_24)
                .setLargeIcon(albumArt)
                .setContentTitle(audio.getAudioName())
                .setContentText(audio.getcategoryName() != null ? audio.getcategoryName() : "Unknown Artist")
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .setSilent(true) // Make notification silent
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .addAction(R.drawable.baseline_skip_previous_24, "Previous", previousPendingIntent)
                .addAction(isPlaying ? R.drawable.ic_pause_active : R.drawable.baseline_play_circle_filled_24,
                        isPlaying ? "Pause" : "Play", playPausePendingIntent)
                .addAction(R.drawable.baseline_skip_next_24, "Next", nextPendingIntent)
                .addAction(R.drawable.baseline_close_24, "Close", closePendingIntent);

        // For Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            builder.setCategory(Notification.CATEGORY_TRANSPORT);
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }
}