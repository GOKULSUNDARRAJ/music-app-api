package com.Saalai.SalaiMusicApp.ApiService;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;

public class ApiClient {
    private static Retrofit retrofit = null;
    private static final String FIREBASE_CONFIG_PATH = "urls";
    private static final String DEFAULT_STAGING_URL = "https://music-app-api-1.onrender.com/api/";
    private static boolean isInitialized = false;
    private static String currentBaseUrl = null;
    private static int currentUrlIndex = 0;
    private static List<String> fallbackUrls = new ArrayList<>();
    private static boolean hasWorkingUrl = false;

    public interface ApiClientCallback {
        void onUrlLoaded(String baseUrl);
        void onAllUrlsFailed(String error);
        void onNoUrlsAvailable();
    }

    public static void initialize(final ApiClientCallback callback) {
        if (isInitialized && retrofit != null) {
            callback.onUrlLoaded(currentBaseUrl);
            return;
        }

        // First, test the default staging URL
        Log.d("ApiClient", "Testing default staging URL: " + DEFAULT_STAGING_URL);
        testSingleUrl(DEFAULT_STAGING_URL, new ApiClientCallback() {
            @Override
            public void onUrlLoaded(String baseUrl) {
                // Default URL worked
                currentBaseUrl = baseUrl;
                currentUrlIndex = 0;
                fallbackUrls.add(0, baseUrl); // Add default URL as first option
                hasWorkingUrl = true;
                createRetrofitInstance(currentBaseUrl);
                isInitialized = true;
                Log.d("ApiClient", "Successfully initialized with default staging URL");
                callback.onUrlLoaded(currentBaseUrl);

                // Pre-fetch Firebase URLs for future fallbacks (in background)
                fetchFirebaseUrlsForFallback();
            }

            @Override
            public void onAllUrlsFailed(String error) {
                Log.w("ApiClient", "Default staging URL failed: " + error);
                // Default failed, try Firebase URLs
                fetchUrlsFromFirebase(callback);
            }

            @Override
            public void onNoUrlsAvailable() {
                callback.onNoUrlsAvailable();
            }
        });
    }

    private static void fetchFirebaseUrlsForFallback() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference configRef = database.getReference(FIREBASE_CONFIG_PATH);

        configRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    try {
                        // Extract URLs from Firebase and add them to fallback list (excluding duplicates)
                        String primary = getStringFromSnapshot(dataSnapshot, "primary");
                        String backup1 = getStringFromSnapshot(dataSnapshot, "backup1");
                        String backup2 = getStringFromSnapshot(dataSnapshot, "backup2");
                        String backup3 = getStringFromSnapshot(dataSnapshot, "backup3");

                        // Add Firebase URLs to fallback list (avoiding duplicates with default URL)
                        if (primary != null && !primary.equals(DEFAULT_STAGING_URL) && !fallbackUrls.contains(primary)) {
                            fallbackUrls.add(primary);
                        }
                        if (backup1 != null && !backup1.equals(DEFAULT_STAGING_URL) && !fallbackUrls.contains(backup1)) {
                            fallbackUrls.add(backup1);
                        }
                        if (backup2 != null && !backup2.equals(DEFAULT_STAGING_URL) && !fallbackUrls.contains(backup2)) {
                            fallbackUrls.add(backup2);
                        }
                        if (backup3 != null && !backup3.equals(DEFAULT_STAGING_URL) && !fallbackUrls.contains(backup3)) {
                            fallbackUrls.add(backup3);
                        }

                        Log.d("ApiClient", "Pre-fetched " + (fallbackUrls.size() - 1) + " fallback URLs from Firebase");
                    } catch (Exception e) {
                        Log.e("ApiClient", "Error parsing Firebase data for fallback: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ApiClient", "Firebase fallback fetch cancelled: " + databaseError.getMessage());
            }
        });
    }

    private static void fetchUrlsFromFirebase(final ApiClientCallback callback) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference configRef = database.getReference(FIREBASE_CONFIG_PATH);

        Log.d("ApiClient", "Fetching URLs from Firebase as fallback...");

        configRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("ApiClient", "Firebase data received, exists: " + dataSnapshot.exists());

                if (dataSnapshot.exists()) {
                    fallbackUrls.clear();

                    try {
                        // Extract URLs from Firebase in priority order
                        String primary = getStringFromSnapshot(dataSnapshot, "primary");
                        String backup1 = getStringFromSnapshot(dataSnapshot, "backup1");
                        String backup2 = getStringFromSnapshot(dataSnapshot, "backup2");
                        String backup3 = getStringFromSnapshot(dataSnapshot, "backup3");

                        // Add URLs in priority order (include default as first option if not already there)
                        if (!fallbackUrls.contains(DEFAULT_STAGING_URL)) {
                            fallbackUrls.add(DEFAULT_STAGING_URL);
                        }
                        if (primary != null && !fallbackUrls.contains(primary)) fallbackUrls.add(primary);
                        if (backup1 != null && !fallbackUrls.contains(backup1)) fallbackUrls.add(backup1);
                        if (backup2 != null && !fallbackUrls.contains(backup2)) fallbackUrls.add(backup2);
                        if (backup3 != null && !fallbackUrls.contains(backup3)) fallbackUrls.add(backup3);

                        Log.d("ApiClient", "Successfully loaded " + fallbackUrls.size() + " URLs from Firebase (including default)");

                        if (!fallbackUrls.isEmpty()) {
                            // Test URLs sequentially starting from index 0 (which is default URL)
                            currentUrlIndex = 0;
                            testUrlsSequentially(0, callback);
                        } else {
                            callback.onNoUrlsAvailable();
                        }
                    } catch (Exception e) {
                        Log.e("ApiClient", "Error parsing Firebase data: " + e.getMessage());
                        callback.onAllUrlsFailed("Error parsing Firebase data");
                    }
                } else {
                    Log.e("ApiClient", "No data found at Firebase path: " + FIREBASE_CONFIG_PATH);
                    callback.onNoUrlsAvailable();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ApiClient", "Firebase error: " + databaseError.getMessage());
                callback.onAllUrlsFailed("Firebase connection failed: " + databaseError.getMessage());
            }
        });
    }

    private static void testSingleUrl(final String url, final ApiClientCallback callback) {
        Log.d("ApiClient", "Testing URL: " + url);

        // Use a custom OkHttpClient with 30s timeouts.
        // OkHttp's default is only 10s — not enough for Render.com free-tier cold starts (30-60s).
        OkHttpClient testClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Gson gson = new GsonBuilder().setLenient().create();
        Retrofit testRetrofit = new Retrofit.Builder()
                .baseUrl(url)
                .client(testClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        ApiService testService = testRetrofit.create(ApiService.class);

        // Test with getTermsAndConditions API call (doesn't require authentication)
        retrofit2.Call<ResponseBody> testCall = testService.getTermsAndConditions();

        testCall.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    hasWorkingUrl = true;
                    Log.d("ApiClient", "URL test SUCCESS: " + url);
                    callback.onUrlLoaded(url);
                } else {
                    Log.w("ApiClient", "URL test FAILED (HTTP " + response.code() + "): " + url);
                    callback.onAllUrlsFailed("HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                if (call.isCanceled()) return;
                Log.w("ApiClient", "URL test FAILED (Network error): " + url + " - " + t.getMessage());
                callback.onAllUrlsFailed("Network error: " + t.getMessage());
            }
        });
    }

    private static void testUrlsSequentially(int index, final ApiClientCallback callback) {
        if (index >= fallbackUrls.size()) {
            // All URLs failed
            String error = "All URLs failed to connect";
            Log.e("ApiClient", error);

            if (fallbackUrls.isEmpty()) {
                callback.onNoUrlsAvailable();
            } else {
                callback.onAllUrlsFailed(error);
            }
            return;
        }

        final String testUrl = fallbackUrls.get(index);
        Log.d("ApiClient", "Testing URL [" + (index + 1) + "/" + fallbackUrls.size() + "]: " + testUrl);

        testSingleUrl(testUrl, new ApiClientCallback() {
            @Override
            public void onUrlLoaded(String baseUrl) {
                // URL is working
                currentBaseUrl = baseUrl;
                currentUrlIndex = index;
                hasWorkingUrl = true;
                createRetrofitInstance(currentBaseUrl);
                isInitialized = true;
                callback.onUrlLoaded(currentBaseUrl);
            }

            @Override
            public void onAllUrlsFailed(String error) {
                // URL failed, try next one
                testUrlsSequentially(index + 1, callback);
            }

            @Override
            public void onNoUrlsAvailable() {
                callback.onNoUrlsAvailable();
            }
        });
    }

    private static String getStringFromSnapshot(DataSnapshot dataSnapshot, String key) {
        try {
            String value = dataSnapshot.child(key).getValue(String.class);
            if (value != null && !value.trim().isEmpty()) {
                Log.d("ApiClient", "Found " + key + ": " + value);
                return value.trim();
            }
        } catch (Exception e) {
            Log.e("ApiClient", "Error getting " + key + ": " + e.getMessage());
        }
        return null;
    }

    private static void createRetrofitInstance(String baseUrl) {
        try {
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            Log.d("ApiClient", "Retrofit instance created successfully with URL: " + baseUrl);
        } catch (Exception e) {
            Log.e("ApiClient", "Error creating Retrofit instance: " + e.getMessage());
        }
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            throw new IllegalStateException("ApiClient not initialized. Call initialize() first.");
        }
        return retrofit;
    }

    public static boolean isInitialized() {
        return isInitialized && retrofit != null;
    }

    public static String getCurrentBaseUrl() {
        return currentBaseUrl;
    }

    public static int getCurrentUrlIndex() {
        return currentUrlIndex;
    }

    public static int getTotalUrlsCount() {
        return fallbackUrls.size();
    }

    public static List<String> getAvailableUrls() {
        return new ArrayList<>(fallbackUrls);
    }

    public static boolean hasWorkingUrl() {
        return hasWorkingUrl && currentBaseUrl != null;
    }

    // Method to manually switch to next URL if current fails during runtime
    public static void switchToNextUrl(final ApiClientCallback callback) {
        if (fallbackUrls.isEmpty() || !isInitialized) {
            callback.onAllUrlsFailed("No URLs available or API not initialized");
            return;
        }

        int nextIndex = (currentUrlIndex + 1) % fallbackUrls.size();
        if (nextIndex == currentUrlIndex) {
            callback.onAllUrlsFailed("No alternative URLs available");
            return;
        }

        Log.d("ApiClient", "Manually switching to next URL, index: " + nextIndex);
        testUrlsSequentially(nextIndex, callback);
    }

    // Method to test current URL and auto-switch if failed
    public static void validateCurrentUrl(final ApiClientCallback callback) {
        if (!isInitialized || currentBaseUrl == null) {
            callback.onAllUrlsFailed("API not properly initialized");
            return;
        }

        Log.d("ApiClient", "Validating current URL: " + currentBaseUrl);
        testSingleUrl(currentBaseUrl, new ApiClientCallback() {
            @Override
            public void onUrlLoaded(String baseUrl) {
                // Current URL is still working
                callback.onUrlLoaded(baseUrl);
            }

            @Override
            public void onAllUrlsFailed(String error) {
                Log.w("ApiClient", "Current URL failed validation: " + error);
                // Current URL failed, switch to next available
                switchToNextUrl(callback);
            }

            @Override
            public void onNoUrlsAvailable() {
                callback.onNoUrlsAvailable();
            }
        });
    }

    public static void reset() {
        retrofit = null;
        isInitialized = false;
        currentBaseUrl = null;
        currentUrlIndex = 0;
        hasWorkingUrl = false;
        fallbackUrls.clear();
        Log.d("ApiClient", "ApiClient reset");
    }
}