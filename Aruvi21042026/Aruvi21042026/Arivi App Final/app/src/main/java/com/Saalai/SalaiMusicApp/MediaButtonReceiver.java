package com.Saalai.SalaiMusicApp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.util.Log;

public class MediaButtonReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaButtonReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "Media button pressed in receiver: " + event.getKeyCode());

                // Forward to PlayerManager
                PlayerManager manager = PlayerManager.getInstance();
                if (manager != null) {
                    manager.handleMediaButtonEvent(intent);
                }

                // Prevent other apps from handling
                abortBroadcast();
            }
        }
    }
}