package com.Saalai.SalaiMusicApp.Notification;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.Saalai.SalaiMusicApp.PlayerManager;

public class NotificationActionService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction());
        }
        return START_NOT_STICKY;
    }

    private void handleAction(String action) {
        switch (action) {
            case "PLAY":
                if (PlayerManager.getCurrentAudio() != null) {
                    PlayerManager.resumePlayback();
                }
                break;
            case "PAUSE":
                PlayerManager.pausePlayback();
                break;
            case "PREVIOUS":
                playPrevious();
                break;
            case "NEXT":
                playNext();
                break;
            case "CLOSE":
                PlayerManager.stopPlayback();
                break;
        }
    }

    private void playPrevious() {
        PlayerManager.playPrevious(() -> {
            // Callback when previous song is prepared
            Toast.makeText(this, "Previous song", Toast.LENGTH_SHORT).show();
        });
    }

    private void playNext() {
        PlayerManager.playNext(() -> {
            // Callback when next song is prepared
            Toast.makeText(this, "Next song", Toast.LENGTH_SHORT).show();
        });
    }
}