package com.Saalai.SalaiMusicApp.Activity;


import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.BottomNavItem;
import com.Saalai.SalaiMusicApp.Models.NavigationDataManager;
import com.Saalai.SalaiMusicApp.Models.NavigationResponse;
import com.Saalai.SalaiMusicApp.Models.TopNavItem;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//Currently Working
//Firebase-Saalaiphoneapp
//2026 Aruvi app
//Date -12/03/2026
//github name-Currently working Aruvi app
//github Latest commit -First commit after clip mode settings
public class SplashScreenActivity extends AppCompatActivity {

    private static final int MIN_SPLASH_DURATION = 5000; // Minimum 2 seconds
    private static final int MAX_SPLASH_DURATION = 10000; // Maximum 10 seconds

    private boolean minSplashReached = false;
    private boolean isNavigating = false;
    private boolean showingDialog = false;
    private Handler mainHandler;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isTablet()) {
            // Allow screen rotation for tablets
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            // Lock to portrait for phones
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        startTime = System.currentTimeMillis();

        // Make status bar transparent
        setupStatusBar();
        setContentView(R.layout.activity_splash_screen);
        setupWindowInsets();

        mainHandler = new Handler(Looper.getMainLooper());

        // Start minimum splash timer
        startMinSplashTimer();

        // Initialize API client immediately
        initializeApiClient();

        // Start maximum splash timer
        startMaxSplashTimer();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        } else {
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS |
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
            );
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
    }

    private void setupWindowInsets() {
        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                v.setPadding(
                        insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom()
                );
                return insets.consumeSystemWindowInsets();
            });
        }
    }

    private void startMinSplashTimer() {
        mainHandler.postDelayed(() -> {
            minSplashReached = true;
            checkAndNavigate();
        }, MIN_SPLASH_DURATION);
    }

    private void startMaxSplashTimer() {
        mainHandler.postDelayed(() -> {
            // Force check after maximum time
            Log.w("SplashScreen", "Maximum splash time reached");
            if (!isNavigating && !showingDialog) {
                if (!ApiClient.hasWorkingUrl()) {
                    showNoConnectionDialog("Cannot connect to server. Please check your internet connection.");
                } else {
                    navigateToNextScreen();
                }
            }
        }, MAX_SPLASH_DURATION);
    }

    private void initializeApiClient() {
        Log.d("SplashScreen", "Initializing API client...");

        ApiClient.initialize(new ApiClient.ApiClientCallback() {
            @Override
            public void onUrlLoaded(String baseUrl) {
                Log.d("SplashScreen", "API successfully initialized with URL: " + baseUrl);

                // Show toast if using backup server
                int urlIndex = ApiClient.getCurrentUrlIndex();
                if (urlIndex > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(SplashScreenActivity.this,
                                "Connected to backup server " + (urlIndex + 1),
                                Toast.LENGTH_SHORT).show();
                    });
                }

                // Load navigation data from API
                loadNavigationData();
            }

            @Override
            public void onAllUrlsFailed(String error) {
                Log.e("SplashScreen", "All API URLs failed: " + error);
                runOnUiThread(() -> {
                    showNoConnectionDialog("Cannot connect to server. Please contact support.");
                });
            }

            @Override
            public void onNoUrlsAvailable() {
                Log.e("SplashScreen", "No URLs available");
                runOnUiThread(() -> {
                    showNoConnectionDialog("No servers available. Please contact support.");
                });
            }
        });
    }

    private void loadNavigationData() {
        SharedPrefManager sp = SharedPrefManager.getInstance(this);
        String accessToken = sp.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.d("SplashScreen", "No access token, proceeding without navigation");
            // Clear any invalid token
            sp.clearAccessToken();
            checkAndNavigate();
            return;
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<NavigationResponse> call = apiService.getNavigationMenu(accessToken);

        call.enqueue(new Callback<NavigationResponse>() {
            @Override
            public void onResponse(Call<NavigationResponse> call, Response<NavigationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    NavigationResponse navResponse = response.body();
                    if (navResponse.isStatus()) {
                        // Filter active bottom items
                        List<BottomNavItem> activeBottomItems = new ArrayList<>();
                        if (navResponse.getBottomMenu() != null) {
                            for (BottomNavItem item : navResponse.getBottomMenu()) {
                                if (item.isActive()) {
                                    activeBottomItems.add(item);
                                }
                            }
                        }

                        // Filter active top items
                        List<TopNavItem> activeTopItems = new ArrayList<>();
                        if (navResponse.getTopMenu() != null) {
                            for (TopNavItem item : navResponse.getTopMenu()) {
                                if ("Active".equals(item.getTopmenuStatus()) || item.isActive()) {
                                    activeTopItems.add(item);
                                }
                            }
                        }

                        // Save to SharedPreferences using NavigationDataManager
                        NavigationDataManager.getInstance(SplashScreenActivity.this)
                                .saveNavigation(activeBottomItems, activeTopItems);

                        Log.d("SplashScreen", "Navigation loaded and saved: " +
                                activeBottomItems.size() + " bottom items, " +
                                activeTopItems.size() + " top items");

                        // Navigate
                        checkAndNavigate();

                    } else {
                        Log.e("SplashScreen", "Navigation API returned false status");
                        checkAndNavigate();
                    }
                } else {
                    // Handle specific HTTP error codes
                    if (response.code() == 401) {


                        Log.w("SplashScreen", "Access token expired or invalid (401). Clearing token.");

                        // Clear the invalid token
                        SharedPrefManager.getInstance(SplashScreenActivity.this).clearAccessToken();

                        // Optional: Show a message to user
                        runOnUiThread(() -> {
                            Toast.makeText(SplashScreenActivity.this,
                                    "Session expired. Please login again.",
                                    Toast.LENGTH_SHORT).show();
                        });

                        callLogoutApi();

                    } else if (response.code() == 403) {
                        Log.w("SplashScreen", "Forbidden access to navigation (403)");
                    } else {
                        Log.e("SplashScreen", "Navigation API failed with code: " + response.code());
                    }

                    // Proceed anyway - navigation data is optional
                    checkAndNavigate();
                }
            }

            @Override
            public void onFailure(Call<NavigationResponse> call, Throwable t) {
                Log.e("SplashScreen", "Navigation API call failed: " + t.getMessage());
                checkAndNavigate();
            }
        });
    }

    private void callLogoutApi() {
        SharedPrefManager sp = SharedPrefManager.getInstance(SplashScreenActivity.this);
        String accessToken = sp.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e("Logout", "No token found, user already logged out");
            return;
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.logout(accessToken);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("Logout", "User Logged Out successfully");

                    // Clear token locally
                    sp.clearAccessToken();

                    Intent intent = new Intent(SplashScreenActivity.this, SignUpActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Log.e("Logout", "Failed to logout, server code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Logout", "Logout API call failed", t);
            }
        });
    }

    private void showNoConnectionDialog(String message) {
        if (showingDialog || isFinishing() || isNavigating) return;

        showingDialog = true;

        // Create dialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_dialog_no_connection);

        // Get references
        TextView tvMessage = dialog.findViewById(R.id.tvMessage);
        Button btnRetry = dialog.findViewById(R.id.btnRetry);
        Button btnExit = dialog.findViewById(R.id.btnExit);

        // Set message
        tvMessage.setText(message);

        // Set button click listeners
        btnRetry.setOnClickListener(v -> {
            dialog.dismiss();
            showingDialog = false;
            retryConnection();
        });

        btnExit.setOnClickListener(v -> {
            dialog.dismiss();
            showingDialog = false;
            finishAffinity();
        });

        // Set dialog properties
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setGravity(Gravity.CENTER);

        dialog.setOnDismissListener(dialogInterface -> {
            showingDialog = false;
        });

        dialog.show();
    }
    private void retryConnection() {
        ApiClient.reset();
        Toast.makeText(this, "Retrying connection...", Toast.LENGTH_SHORT).show();
        initializeApiClient();
    }

    private void checkAndNavigate() {
        // Only navigate if:
        // 1. Minimum splash time reached
        // 2. API has a working URL
        // 3. Not currently navigating or showing dialog
        if (minSplashReached && ApiClient.hasWorkingUrl() &&
                !isFinishing() && !isNavigating && !showingDialog) {
            long elapsed = System.currentTimeMillis() - startTime;
            Log.d("SplashScreen", "Navigating after " + elapsed + "ms");
            navigateToNextScreen();
        }
    }

    private void navigateToNextScreen() {
        if (isFinishing() || isNavigating) return;

        isNavigating = true;

        try {
            SharedPrefManager sharedPrefManager = SharedPrefManager.getInstance(this);

            if (sharedPrefManager.isLoggedIn()) {
                Log.d("SplashScreen", "User is logged in, navigating to MainActivity");
                startActivity(new Intent(this, MainActivity.class));
            } else {
                boolean onboardingComplete = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                        .getBoolean("onboarding_complete", false);

                Class<?> targetActivity = onboardingComplete ? SignUpActivity.class : OnboardingActivity.class;
                Log.d("SplashScreen", "Navigating to: " + targetActivity.getSimpleName());
                startActivity(new Intent(this, targetActivity));
            }
            finish();

        } catch (Exception e) {
            Log.e("SplashScreen", "Navigation error: " + e.getMessage());
            finishAffinity();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }

    private boolean isTablet() {
        Configuration config = getResources().getConfiguration();
        return (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}