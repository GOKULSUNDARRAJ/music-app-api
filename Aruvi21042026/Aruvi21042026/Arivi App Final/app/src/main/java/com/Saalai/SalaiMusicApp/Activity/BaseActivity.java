package com.Saalai.SalaiMusicApp.Activity;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set orientation based on device type
        if (isTablet()) {
            // Allow rotation on tablets
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        } else {
            // Lock to portrait on phones
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Handle configuration changes for tablets
    }

    // Method to check if the device is a tablet
    private boolean isTablet() {
        // Method 1: Check screen size
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        float widthInches = metrics.widthPixels / metrics.xdpi;
        float heightInches = metrics.heightPixels / metrics.ydpi;
        double diagonalInches = Math.sqrt(Math.pow(widthInches, 2) + Math.pow(heightInches, 2));

        // Consider device as tablet if screen diagonal is 7 inches or more
        return diagonalInches >= 7.0;

        // Alternative method: Check screen smallest width
        // Configuration config = getResources().getConfiguration();
        // return config.smallestScreenWidthDp >= 600;
    }

    // Method to manually allow rotation (for specific fragments like video players)
    public void allowRotation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    // Method to lock to portrait (override tablet setting if needed)
    public void lockToPortrait() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    // Method to lock to landscape (useful for video players)
    public void lockToLandscape() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    // Method to check if current device is tablet
    public boolean isDeviceTablet() {
        return isTablet();
    }
}