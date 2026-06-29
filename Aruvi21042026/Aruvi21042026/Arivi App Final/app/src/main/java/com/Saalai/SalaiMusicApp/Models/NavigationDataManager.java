package com.Saalai.SalaiMusicApp.Models;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class NavigationDataManager {
    private static final String PREFS_NAME = "NavigationData";
    private static final String KEY_BOTTOM_NAV = "bottom_navigation";
    private static final String KEY_TOP_NAV = "top_navigation";
    private static final String KEY_LOADED = "navigation_loaded";
    private static final String KEY_UPDATE_TIME = "last_update_time";

    private static NavigationDataManager instance;
    private SharedPreferences prefs;
    private Gson gson = new Gson();

    private NavigationDataManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static NavigationDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new NavigationDataManager(context);
        }
        return instance;
    }

    // Save navigation data
    public void saveNavigation(List<BottomNavItem> bottomItems, List<TopNavItem> topItems) {
        try {
            String bottomItemsJson = gson.toJson(bottomItems);
            String topItemsJson = gson.toJson(topItems);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_BOTTOM_NAV, bottomItemsJson);
            editor.putString(KEY_TOP_NAV, topItemsJson);
            editor.putBoolean(KEY_LOADED, true);
            editor.putLong(KEY_UPDATE_TIME, System.currentTimeMillis());
            editor.apply();

            Log.d("NavigationDataManager", "Navigation saved to SharedPreferences: " +
                    bottomItems.size() + " bottom, " + topItems.size() + " top");
        } catch (Exception e) {
            Log.e("NavigationDataManager", "Error saving navigation: " + e.getMessage());
        }
    }

    // Get bottom navigation items
    public List<BottomNavItem> getBottomNavigation() {
        try {
            String json = prefs.getString(KEY_BOTTOM_NAV, null);
            if (json == null) {
                Log.d("NavigationDataManager", "No bottom navigation in SharedPreferences");
                return new ArrayList<>();
            }

            Type type = new TypeToken<List<BottomNavItem>>(){}.getType();
            List<BottomNavItem> items = gson.fromJson(json, type);
            return items != null ? items : new ArrayList<>();
        } catch (Exception e) {
            Log.e("NavigationDataManager", "Error loading bottom nav: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Get top navigation items
    public List<TopNavItem> getTopNavigation() {
        try {
            String json = prefs.getString(KEY_TOP_NAV, null);
            if (json == null) {
                Log.d("NavigationDataManager", "No top navigation in SharedPreferences");
                return new ArrayList<>();
            }

            Type type = new TypeToken<List<TopNavItem>>(){}.getType();
            List<TopNavItem> items = gson.fromJson(json, type);
            return items != null ? items : new ArrayList<>();
        } catch (Exception e) {
            Log.e("NavigationDataManager", "Error loading top nav: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Check if navigation is loaded
    public boolean isNavigationLoaded() {
        return prefs.getBoolean(KEY_LOADED, false);
    }

    // Check if cache is still valid (optional - you can implement cache expiry)
    public boolean isCacheValid(long cacheDurationMillis) {
        long lastUpdate = prefs.getLong(KEY_UPDATE_TIME, 0);
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastUpdate) < cacheDurationMillis;
    }

    // Clear navigation data
    public void clearNavigation() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_BOTTOM_NAV);
        editor.remove(KEY_TOP_NAV);
        editor.remove(KEY_LOADED);
        editor.remove(KEY_UPDATE_TIME);
        editor.apply();
        Log.d("NavigationDataManager", "Navigation data cleared");
    }
}