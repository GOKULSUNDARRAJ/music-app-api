package com.Saalai.SalaiMusicApp.Activity;



import android.app.Application;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import com.Saalai.SalaiMusicApp.BackgroundDownloadHelper;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.VideoDownloadManager;

public class MyApplication extends Application implements ViewModelStoreOwner {
    private ViewModelStore viewModelStore = new ViewModelStore();
    private static final String TAG = "MyApplication";

    private static MyApplication instance;

    @Override
    public ViewModelStore getViewModelStore() {
        return viewModelStore;
    }

    // Helper method to get shared ViewModel
    public <T extends androidx.lifecycle.ViewModel> T getSharedViewModel(Class<T> modelClass) {
        return new ViewModelProvider(this).get(modelClass);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "Application created");

        // Initialize download helper
        BackgroundDownloadHelper.getInstance(this);

        // Restore downloads if app was killed
        restoreBackgroundDownloads();

        PlayerManager.init(this);
    }

    private void restoreBackgroundDownloads() {
        BackgroundDownloadHelper helper = BackgroundDownloadHelper.getInstance(this);
        if (helper.isServiceRunning()) {
            // Service already running, nothing to do
            return;
        }

        // Check if there were active downloads
        VideoDownloadManager tempManager = new VideoDownloadManager(this);
        if (tempManager.restoreDownloadState()) {
            // Start service to resume download

            Log.d(TAG, "Restarting download service for background download");
        }
    }

    public static MyApplication getInstance() {
        return instance;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "Application terminating");
    }

}