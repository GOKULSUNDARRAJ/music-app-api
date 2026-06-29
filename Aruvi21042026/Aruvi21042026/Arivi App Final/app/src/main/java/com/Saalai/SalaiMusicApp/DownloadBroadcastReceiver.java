package com.Saalai.SalaiMusicApp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class DownloadBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "DownloadReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

            if (isConnected) {
                Log.d(TAG, "Network connected, resuming downloads if any");

                // Start service to resume downloads
                BackgroundDownloadHelper helper = BackgroundDownloadHelper.getInstance(context);
                helper.startDownloadService();

                // Resume downloads
                VideoDownloadService service = helper.getDownloadService();
                if (service != null) {
                    // Check if we need to resume downloads
                    VideoDownloadManager downloadManager = service.getDownloadManager();
                    if (downloadManager != null && downloadManager.hasActiveDownloads()) {
                        Log.d(TAG, "Resuming interrupted downloads after network restored");
                        downloadManager.resumeDownload();
                    }
                }
            }
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Log.d(TAG, "Device boot completed, checking for downloads to resume");

            // Check if we need to restart download service after boot
            BackgroundDownloadHelper helper = BackgroundDownloadHelper.getInstance(context);

            // Check for active downloads
            VideoDownloadManager tempManager = new VideoDownloadManager(context);
            if (tempManager.restoreDownloadState()) {
                helper.startDownloadService();
                Log.d(TAG, "Restarted download service after boot");
            }
        }
    }
}