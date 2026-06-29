package com.Saalai.SalaiMusicApp.Activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.BluetoothDeviceListBottomSheet;
import com.Saalai.SalaiMusicApp.Fragments.OverAllSearchFragment;
import com.Saalai.SalaiMusicApp.Fragments.PodcastsHomeFragment;
import com.Saalai.SalaiMusicApp.Fragments.HomeFragment;
import com.Saalai.SalaiMusicApp.Fragments.LibraryFragment;
import com.Saalai.SalaiMusicApp.Fragments.ProfileFragment;
import com.Saalai.SalaiMusicApp.Fragments.SearchFragment;
import com.Saalai.SalaiMusicApp.Receivers.NetworkChangeReceiver;
import com.bumptech.glide.Glide;
import com.Saalai.SalaiMusicApp.Adapters.BottomNavAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Fragments.AudioDownloadFragment;
import com.Saalai.SalaiMusicApp.Fragments.CatchUpDetailFragment;
import com.Saalai.SalaiMusicApp.Fragments.MovieVideoPlayerFragment;
import com.Saalai.SalaiMusicApp.Fragments.RadioFragment;
import com.Saalai.SalaiMusicApp.Fragments.RadioPlayerFragment;
import com.Saalai.SalaiMusicApp.Fragments.SaalaiFragment;
import com.Saalai.SalaiMusicApp.Fragments.TopMenuViewModel;
import com.Saalai.SalaiMusicApp.Fragments.TvShowEpisodeFragment;
import com.Saalai.SalaiMusicApp.Fragments.VideoPlayerFragment;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Models.BottomNavItem;
import com.Saalai.SalaiMusicApp.Models.NavigationDataManager;
import com.Saalai.SalaiMusicApp.Models.TopNavItem;
import com.Saalai.SalaiMusicApp.PlayerBottomSheetFragment;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;

import androidx.palette.graphics.Palette;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity implements
        VideoPlayerFragment.VideoPlayerListener,
        MovieVideoPlayerFragment.MoviePlayerListener,
        TvShowEpisodeFragment.TvShowPlayerListener,
        CatchUpDetailFragment.CatchUpPlayerListener,
        SaalaiFragment.DrawerToggleListener,
        RadioFragment.DrawerToggleListener,
        AudioDownloadFragment.DrawerToggleListener,
        RadioPlayerFragment.RadioPlayerListener {

    // Dynamic Bottom Navigation
    private RecyclerView bottomNavRecyclerView;
    private BottomNavAdapter bottomNavAdapter;
    private List<BottomNavItem> bottomNavItems = new ArrayList<>();

    // Broadcast Receivers
    private BroadcastReceiver closeReceiver;
    private BroadcastReceiver miniPlayerReceiver;
    private BroadcastReceiver bluetoothReceiver;

    // Mini Player Views
    private LinearLayout miniAudioPlayer;
    private TextView tvMiniSongName, tvMiniSongArtist;
    private ImageView btnMiniPlayPause, imgMiniAlbumArt, imgBluetoothIcon;
    private ProgressBar miniProgressBar;
    private final Handler seekBarHandler = new Handler();
    private Runnable updateSeekBarRunnable;
    private SharedPrefManager sharedPrefManager;

    DrawerLayout drawerLayout;

    // Internet connectivity
    private BroadcastReceiver networkChangeReceiver;
    private androidx.cardview.widget.CardView cardOffline;
    private LinearLayout offlineLayout;

    private TextView tvOfflineMessage;
    private boolean isInternetConnected = true;

    // Audio Manager for Bluetooth detection
    private AudioManager audioManager;

    // Track Bluetooth connection state
    private boolean isBluetoothConnected = false;

    // Flag to ignore noisy events after disconnect
    private boolean ignoreNoisyEvents = false;

    // Flag to track if we've just disconnected
    private boolean justDisconnected = false;

    // Last known good state to prevent false reconnects
    private boolean lastKnownBluetoothState = false;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.darkGray));
        }

        View rootView = findViewById(R.id.root_view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && Build.VERSION.SDK_INT <= 34) {
            getWindow().setDecorFitsSystemWindows(true);
            if (rootView != null) {
                rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                    v.setPadding(0, 0, 0, 0);
                    return insets;
                });
            }
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            if (rootView != null) {
                rootView.setFitsSystemWindows(true);
            }
        }

        Log.d("MainActivity", "=== MAIN ACTIVITY STARTED ===");

        // Initialize AudioManager for Bluetooth detection
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Load navigation from SharedPreferences
        loadNavigationFromSharedPrefs();

        PlayerManager.init(this);
        initViews();
        initDynamicBottomNavigation();
        setupCloseReceiver();
        initMiniPlayer();
        setupMiniPlayerReceiver();

        // Register Bluetooth receiver
        registerBluetoothReceiver();

        // Check initial Bluetooth state
        checkInitialBluetoothState();

        // Add view tree observer for focus changes
        getWindow().getDecorView().getViewTreeObserver().addOnWindowFocusChangeListener(
                new ViewTreeObserver.OnWindowFocusChangeListener() {
                    @Override
                    public void onWindowFocusChanged(boolean hasFocus) {
                        if (hasFocus) {
                            Log.d("MainActivity", "Window gained focus - refreshing UI");
                            refreshBluetoothState();
                        }
                    }
                });

        // Mini player click opens bottom sheet
        if (miniAudioPlayer != null) {
            miniAudioPlayer.setOnClickListener(v -> {
                PlayerBottomSheetFragment bottomSheet = new PlayerBottomSheetFragment();
                bottomSheet.show(getSupportFragmentManager(), "FullPlayerSheet");
            });
        }

        sharedPrefManager = SharedPrefManager.getInstance(this);

        String userName = sharedPrefManager.getUserName();
        String userMobile = sharedPrefManager.getUserMobile();

        drawerLayout = findViewById(R.id.drawer_layout);

        ImageView profileIcon = findViewById(R.id.iv_user_info);
        if (profileIcon != null) {
            profileIcon.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(Gravity.LEFT);
                }
            });
        }

        ConstraintLayout cardView = findViewById(R.id.cardView);
        LinearLayout btnHelp = findViewById(R.id.helpandsupportll);
        LinearLayout termsll = findViewById(R.id.termsll);
        ImageView closell = findViewById(R.id.close);
        ConstraintLayout lllogout = findViewById(R.id.lllogout);

        if (drawerLayout != null) {
            drawerLayout.setScrimColor(getResources().getColor(android.R.color.transparent));
        }

        TextView tvUserName = findViewById(R.id.username);
        TextView tvUserMobile = findViewById(R.id.usermobile);

        if (tvUserName != null && tvUserMobile != null) {
            tvUserName.setText(userName);
            tvUserMobile.setText(userMobile);
        }

        if (btnHelp != null) {
            btnHelp.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, HelpandSupportActivity.class));
            });
        }

        if (termsll != null) {
            termsll.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, TermsActivity.class));
            });
        }

        if (closell != null) {
            closell.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawers();
            });
        }

        if (lllogout != null) {
            lllogout.setOnClickListener(v -> {
                callLogoutApi();
            });
        }

        TextView tvVersionName = findViewById(R.id.versionname);

        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .versionName;
            if (tvVersionName != null) {
                tvVersionName.setText("Version: " + versionName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (tvVersionName != null) {
                tvVersionName.setText("Version: N/A");
            }
        }

        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        // Initialize offline card views
        initOfflineCard();

        // Register network change receiver
        registerNetworkReceiver();

        // Check initial internet connection
        checkInternetConnection();
    }

    /**
     * ===================== BLUETOOTH METHODS =====================
     */

    /**
     * Check initial Bluetooth connection state
     */
    private void checkInitialBluetoothState() {
        if (audioManager == null) return;

        isBluetoothConnected = isBluetoothDeviceConnected();
        lastKnownBluetoothState = isBluetoothConnected;
        Log.d("MainActivity", "Initial Bluetooth connected: " + isBluetoothConnected);

        runOnUiThread(() -> updateBluetoothIcon());
    }

    /**
     * Refresh Bluetooth state when activity gains focus
     */
    private void refreshBluetoothState() {
        // If we just disconnected, force hide and don't check state
        if (justDisconnected) {
            Log.d("MainActivity", "Skipping refresh - just disconnected, forcing icon hidden");
            runOnUiThread(() -> {
                if (imgBluetoothIcon != null) {
                    imgBluetoothIcon.animate().cancel();
                    imgBluetoothIcon.setVisibility(View.GONE);
                    imgBluetoothIcon.setAlpha(0f);
                    Log.d("MainActivity", "🔵 Bluetooth icon FORCE HIDDEN during refresh");
                }
            });
            return;
        }

        // Use last known good state if we're in a noisy period
        if (ignoreNoisyEvents) {
            Log.d("MainActivity", "Using last known state: " + lastKnownBluetoothState);
            isBluetoothConnected = lastKnownBluetoothState;
        } else {
            boolean previousState = isBluetoothConnected;
            isBluetoothConnected = isBluetoothDeviceConnected();
            lastKnownBluetoothState = isBluetoothConnected;

            Log.d("MainActivity", "refreshBluetoothState - Previous: " + previousState +
                    ", Current: " + isBluetoothConnected);
        }

        runOnUiThread(() -> {
            updateBluetoothIcon();
            updateMiniPlayerUI();
        });
    }

    /**
     * Check if any Bluetooth device is currently connected
     */
    private boolean isBluetoothDeviceConnected() {
        if (audioManager == null) return false;

        // Quick check
        if (audioManager.isBluetoothA2dpOn()) {
            return true;
        }

        // For Android 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                android.media.AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                for (android.media.AudioDeviceInfo device : devices) {
                    int type = device.getType();
                    if (type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                        return true;
                    }
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error checking Bluetooth devices: " + e.getMessage());
            }
        }

        return false;
    }

    /**
     * Update Bluetooth icon visibility
     */
    private void updateBluetoothIcon() {
        if (imgBluetoothIcon == null) return;

        Log.d("MainActivity", "Updating Bluetooth icon - Connected: " + isBluetoothConnected);

        if (isBluetoothConnected) {
            // Show icon with animation
            if (imgBluetoothIcon.getVisibility() != View.VISIBLE) {
                imgBluetoothIcon.animate().cancel();
                imgBluetoothIcon.setVisibility(View.VISIBLE);
                imgBluetoothIcon.setScaleX(0f);
                imgBluetoothIcon.setScaleY(0f);
                imgBluetoothIcon.setAlpha(0f);

                imgBluetoothIcon.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(300)
                        .setInterpolator(new OvershootInterpolator())
                        .withEndAction(() -> {
                            imgBluetoothIcon.invalidate();
                            Log.d("MainActivity", "🔵 Bluetooth icon SHOWN");
                        })
                        .start();
            }
        } else {
            // Hide icon immediately
            if (imgBluetoothIcon.getVisibility() == View.VISIBLE) {
                imgBluetoothIcon.animate().cancel();
                imgBluetoothIcon.setVisibility(View.GONE);
                imgBluetoothIcon.setAlpha(0f);
                Log.d("MainActivity", "🔵 Bluetooth icon HIDDEN");
            }
        }
    }

    /**
     * Register Bluetooth connection receiver - FINAL FIX
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerBluetoothReceiver() {
        bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;

                Log.d("MainActivity", "📡 Bluetooth: " + action);

                switch (action) {
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        Log.d("MainActivity", "✅ Bluetooth CONNECTED");
                        isBluetoothConnected = true;
                        lastKnownBluetoothState = true;
                        justDisconnected = false;
                        ignoreNoisyEvents = false;
                        runOnUiThread(() -> updateBluetoothIcon());
                        break;

                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        Log.d("MainActivity", "❌ Bluetooth DISCONNECTED");
                        // FORCE DISCONNECT
                        isBluetoothConnected = false;
                        lastKnownBluetoothState = false;
                        justDisconnected = true;

                        runOnUiThread(() -> {
                            if (imgBluetoothIcon != null) {
                                imgBluetoothIcon.animate().cancel();
                                imgBluetoothIcon.setVisibility(View.GONE);
                                imgBluetoothIcon.setAlpha(0f);
                                Log.d("MainActivity", "🔵 Bluetooth icon FORCE HIDDEN on disconnect");
                            }
                        });

                        ignoreNoisyEvents = true;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            ignoreNoisyEvents = false;
                            Log.d("MainActivity", "Now accepting noisy events again");
                        }, 2000);

                        // Clear disconnect flag after 3 seconds
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            justDisconnected = false;
                            Log.d("MainActivity", "Disconnect flag cleared");
                        }, 3000);
                        break;

                    case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                        Log.d("MainActivity", "🔊 Audio becoming noisy");
                        if (ignoreNoisyEvents) {
                            Log.d("MainActivity", "Ignoring noisy event - recently disconnected");
                            return;
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            boolean actualState = isBluetoothDeviceConnected();
                            Log.d("MainActivity", "Delayed check - Actual state: " + actualState +
                                    ", Current state: " + isBluetoothConnected);

                            if (isBluetoothConnected && !actualState) {
                                Log.d("MainActivity", "⚠️ State mismatch - forcing disconnect");
                                isBluetoothConnected = false;
                                lastKnownBluetoothState = false;
                                runOnUiThread(() -> {
                                    if (imgBluetoothIcon != null) {
                                        imgBluetoothIcon.animate().cancel();
                                        imgBluetoothIcon.setVisibility(View.GONE);
                                        imgBluetoothIcon.setAlpha(0f);
                                        Log.d("MainActivity", "🔵 Bluetooth icon FORCE HIDDEN - state mismatch");
                                    }
                                });
                            } else if (!isBluetoothConnected && actualState) {
                                // If we're disconnected but actually connected, update
                                isBluetoothConnected = true;
                                lastKnownBluetoothState = true;
                                runOnUiThread(() -> updateBluetoothIcon());
                            }
                        }, 500);
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(bluetoothReceiver, filter);
        }

        Log.d("MainActivity", "📡 Bluetooth Receiver Registered");
    }

    /**
     * ===================== INIT METHODS =====================
     */

    private void initOfflineCard() {
        cardOffline = findViewById(R.id.cardoffiline);
        offlineLayout = findViewById(R.id.offline);
        tvOfflineMessage = findViewById(R.id.tvOfflineMessage);

        if (cardOffline != null) {
            cardOffline.setVisibility(View.VISIBLE);
        }

        if (offlineLayout != null) {
            offlineLayout.setVisibility(View.GONE);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerNetworkReceiver() {
        networkChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (NetworkChangeReceiver.ACTION_INTERNET_STATUS.equals(intent.getAction())) {
                    boolean isConnected = intent.getBooleanExtra(
                            NetworkChangeReceiver.EXTRA_IS_CONNECTED, true);
                    updateOfflineCardVisibility(isConnected);
                }
            }
        };

        IntentFilter filter = new IntentFilter(NetworkChangeReceiver.ACTION_INTERNET_STATUS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(networkChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(networkChangeReceiver, filter);
        }

        IntentFilter connectivityFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(new ConnectivityReceiver(), connectivityFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(new ConnectivityReceiver(), connectivityFilter);
        }
    }

    private class ConnectivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                boolean isConnected = NetworkChangeReceiver.isNetworkAvailable(context);
                updateOfflineCardVisibility(isConnected);
            }
        }
    }

    private void checkInternetConnection() {
        boolean isConnected = NetworkChangeReceiver.isNetworkAvailable(this);
        updateOfflineCardVisibility(isConnected);
    }

    private void updateOfflineCardVisibility(boolean isConnected) {
        if (isConnected == this.isInternetConnected) {
            return;
        }

        this.isInternetConnected = isConnected;

        runOnUiThread(() -> {
            if (offlineLayout != null && tvOfflineMessage != null) {
                if (!isConnected) {
                    tvOfflineMessage.setText("Aruvi is set to Offline.");
                    offlineLayout.setVisibility(View.VISIBLE);
                    offlineLayout.setTranslationY(-offlineLayout.getHeight());
                    offlineLayout.animate()
                            .translationY(0)
                            .setDuration(300)
                            .start();
                    Toast.makeText(MainActivity.this,
                            "No internet connection", Toast.LENGTH_SHORT).show();
                } else {
                    if (offlineLayout.getVisibility() == View.VISIBLE) {
                        offlineLayout.animate()
                                .translationY(-offlineLayout.getHeight())
                                .setDuration(300)
                                .withEndAction(() -> offlineLayout.setVisibility(View.GONE))
                                .start();
                    }
                    Toast.makeText(MainActivity.this,
                            "Internet connected", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadNavigationFromSharedPrefs() {
        Log.d("MainActivity", "Loading navigation from SharedPreferences...");

        NavigationDataManager navManager = NavigationDataManager.getInstance(this);

        if (navManager.isNavigationLoaded()) {
            List<BottomNavItem> bottomItems = navManager.getBottomNavigation();
            if (!bottomItems.isEmpty()) {
                Log.d("MainActivity", "Bottom nav found: " + bottomItems.size() + " items");
                updateBottomNavigation(bottomItems);
            } else {
                Log.d("MainActivity", "No bottom navigation");
                loadFragment(new HomeFragment());
            }

            List<TopNavItem> topItems = navManager.getTopNavigation();
            if (!topItems.isEmpty()) {
                Log.d("MainActivity", "Top nav found: " + topItems.size() + " items");
                updateTopMenuInFragment(topItems);
            }
        } else {
            Log.d("MainActivity", "Navigation not loaded");
            loadFragment(new HomeFragment());
        }
    }

    private void initViews() {
        imgMiniAlbumArt = findViewById(R.id.imgMiniAlbumArt);
        imgBluetoothIcon = findViewById(R.id.imgbluetooth);

        if (imgBluetoothIcon != null) {
            imgBluetoothIcon.setVisibility(View.GONE);
        }

        LinearLayout oldBottomNav = findViewById(R.id.custom_bottom_navigation);
        if (oldBottomNav != null) {
            oldBottomNav.setVisibility(View.GONE);
        }

        if (bottomNavItems.isEmpty()) {
            loadFragment(new HomeFragment());
        }
    }

    private void initDynamicBottomNavigation() {

        bottomNavRecyclerView = findViewById(R.id.bottom_nav_recycler_view);

        if (bottomNavRecyclerView == null) {
            Log.e("MainActivity", "Bottom nav RecyclerView not found!");
            return;
        }

        int columnCount = calculateColumnCount(bottomNavItems);

        GridLayoutManager layoutManager =
                new GridLayoutManager(this, columnCount, GridLayoutManager.VERTICAL, false);

        bottomNavRecyclerView.setLayoutManager(layoutManager);
        bottomNavRecyclerView.setNestedScrollingEnabled(false);

        // 🔥 Handle center for single item
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {

                // 👉 If only 1 item → take full width (center)
                if (bottomNavItems.size() == 1) {
                    return columnCount;
                }

                // 👉 Normal items
                return 1;
            }
        });

        bottomNavAdapter = new BottomNavAdapter(bottomNavItems, position -> {
            if (position >= 0 && position < bottomNavItems.size()) {
                loadFragmentForBottomNav(bottomNavItems.get(position));
            }
        });

        bottomNavRecyclerView.setAdapter(bottomNavAdapter);
    }

    private int calculateColumnCount(List<BottomNavItem> items) {
        if (items == null || items.isEmpty()) return 3;

        int count = items.size();

        if (count <= 2) return 2;
        if (count == 3) return 3;
        if (count >= 4) return 4;

        return 3;
    }

    private void updateTopMenuInFragment(List<TopNavItem> topItems) {
        SaalaiFragment saalaiFragment = (SaalaiFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);

        if (saalaiFragment != null) {
            saalaiFragment.updateTopMenu(topItems);
        } else {
            TopMenuViewModel topMenuViewModel = new ViewModelProvider(this)
                    .get(TopMenuViewModel.class);
            topMenuViewModel.setTopMenu(topItems);
        }
    }

    private void updateBottomNavigation(List<BottomNavItem> items) {
        bottomNavItems.clear();
        bottomNavItems.addAll(items);

        if (bottomNavAdapter != null) {
            bottomNavAdapter.notifyDataSetChanged();
        }

        if (!bottomNavItems.isEmpty()) {
            if (bottomNavAdapter != null) {
                bottomNavAdapter.setSelectedPosition(0);
            }
            loadFragmentForBottomNav(bottomNavItems.get(0));
        }
    }

    private void loadFragmentForBottomNav(BottomNavItem item) {
        Log.d("MainActivity", "=== loadFragmentForBottomNav START ===");

        if (item == null) {
            Log.e("MainActivity", "❌ BottomNavItem is NULL!");
            return;
        }

        Log.d("MainActivity", "BottomNavItem Details:");
        Log.d("MainActivity", "  - ID: " + item.getBottommenuId());
        Log.d("MainActivity", "  - Name: '" + item.getBottommenuName() + "'");
        Log.d("MainActivity", "  - Name Lowercase: '" + item.getBottommenuName().toLowerCase() + "'");

        Fragment fragment = null;
        String name = item.getBottommenuName().toLowerCase().trim(); // Added trim()

        // Debug: Check for typos in the name
        Log.d("MainActivity", "Checking name: '" + name + "'");

        switch (name) {
            case "home":
                Log.d("MainActivity", "✅ Matched: Home");
                fragment = new HomeFragment();
                break;

            case "search":
                Log.d("MainActivity", "✅ Matched: Search");
                fragment = new OverAllSearchFragment();
                break;

            case "library":
                Log.d("MainActivity", "✅ Matched: Library");
                fragment = new LibraryFragment();
                break;

            case "potcast":  // Note: Keep as "potcast" since that's what you have
            case "podcast":
                Log.d("MainActivity", "✅ Matched: Podcast");
                fragment = new PodcastsHomeFragment();
                break;

            default:
                Log.d("MainActivity", "⚠️ No match by name, trying by ID: " + item.getBottommenuId());

                // Try by ID as fallback
                switch (item.getBottommenuId()) {
                    case 1:
                        Log.d("MainActivity", "✅ ID 1 matched: Home");
                        fragment = new HomeFragment();
                        break;

                    case 2:
                        Log.d("MainActivity", "✅ ID 2 matched: Search");
                        fragment = new OverAllSearchFragment();
                        break;

                    case 3:
                        Log.d("MainActivity", "✅ ID 3 matched: Library");
                        fragment = new LibraryFragment();
                        break;

                    case 4:
                        Log.d("MainActivity", "✅ ID 4 matched: Podcast");
                        fragment = new PodcastsHomeFragment();
                        break;

                    default:
                        Log.e("MainActivity", "❌ No match for ID: " + item.getBottommenuId());
                        Log.e("MainActivity", "❌ Unknown bottom menu item: " + name);
                        fragment = new HomeFragment(); // Default fallback
                        Log.d("MainActivity", "Using HomeFragment as default fallback");
                        break;
                }
                break;
        }

        if (fragment != null) {
            Log.d("MainActivity", "✅ Fragment created successfully: " + fragment.getClass().getSimpleName());
            loadFragment(fragment);
            Log.d("MainActivity", "✅ loadFragment() called");
        } else {
            Log.e("MainActivity", "❌ Fragment is STILL NULL! Cannot load fragment");
            // Emergency fallback - load HomeFragment
            Log.d("MainActivity", "🔄 Emergency fallback: Loading HomeFragment");
            loadFragment(new HomeFragment());
        }

        Log.d("MainActivity", "=== loadFragmentForBottomNav END ===");
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void setupCloseReceiver() {
        closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("CLOSE_MAIN_ACTIVITY".equals(intent.getAction())) {
                    finish();
                }
            }
        };

        IntentFilter filter = new IntentFilter("CLOSE_MAIN_ACTIVITY");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(closeReceiver, filter);
        }
    }

    private void initMiniPlayer() {
        miniAudioPlayer = findViewById(R.id.miniaudioplayer);
        tvMiniSongName = findViewById(R.id.tvMiniSongName);
        tvMiniSongArtist = findViewById(R.id.tvMiniSongArtist);
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause);
        miniProgressBar = findViewById(R.id.miniProgressBar);
        imgBluetoothIcon = findViewById(R.id.imgbluetooth);

        if (miniAudioPlayer != null) miniAudioPlayer.setVisibility(View.GONE);
        if (imgBluetoothIcon != null) imgBluetoothIcon.setVisibility(View.GONE);

        if (btnMiniPlayPause != null) {
            btnMiniPlayPause.setOnClickListener(v -> toggleMiniPlayer());
        }

        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (PlayerManager.getPlayer() != null && PlayerManager.getCurrentAudio() != null) {
                    int duration = PlayerManager.getPlayer().getDuration();
                    int current = PlayerManager.getPlayer().getCurrentPosition();
                    if (duration > 0) {
                        int progress = (int) (((float) current / duration) * 100);
                        miniProgressBar.setProgress(progress);
                    }
                }
                seekBarHandler.postDelayed(this, 500);
            }
        };
        seekBarHandler.post(updateSeekBarRunnable);

        setupBluetoothIconClick();
    }

    // Add this method to open Bluetooth device list bottom sheet
    private void openBluetoothDeviceList() {
        Log.d("MainActivity", "Opening Bluetooth device list bottom sheet");

        // Create and show the Bluetooth bottom sheet
        BluetoothDeviceListBottomSheet bluetoothSheet = new BluetoothDeviceListBottomSheet();
        bluetoothSheet.show(getSupportFragmentManager(), "BluetoothDeviceList");
    }

    // Add this method to setup click listener on Bluetooth icon
    private void setupBluetoothIconClick() {
        if (imgBluetoothIcon != null) {
            imgBluetoothIcon.setOnClickListener(v -> {
                openBluetoothDeviceList();
            });
            Log.d("MainActivity", "Bluetooth icon click listener setup");
        }
    }

    private void toggleMiniPlayer() {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) {
            if (miniAudioPlayer != null) miniAudioPlayer.setVisibility(View.GONE);
            return;
        }

        if (PlayerManager.isPlaying()) {
            PlayerManager.pausePlayback();
            if (btnMiniPlayPause != null) btnMiniPlayPause.setImageResource(R.drawable.play_player);
        } else {
            if (PlayerManager.getPlayer() != null) {
                PlayerManager.getPlayer().start();
                if (btnMiniPlayPause != null) btnMiniPlayPause.setImageResource(R.drawable.pause);
            }
        }

        loadMiniPlayerAlbumArt(currentAudio);
        sendBroadcast(new Intent("UPDATE_MINI_PLAYER"));
        sendBroadcast(new Intent("UPDATE_AUDIO_ADAPTER"));
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void setupMiniPlayerReceiver() {
        miniPlayerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("UPDATE_MINI_PLAYER".equals(intent.getAction())) {
                    updateMiniPlayerUI();
                }
            }
        };

        IntentFilter filter = new IntentFilter("UPDATE_MINI_PLAYER");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(miniPlayerReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(miniPlayerReceiver, filter);
        }
    }

    private void updateMiniPlayerUI() {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) {
            if (miniAudioPlayer != null) miniAudioPlayer.setVisibility(View.GONE);
            return;
        }

        if (tvMiniSongName != null) {
            tvMiniSongName.setText(currentAudio.getAudioName());
        }

        if (tvMiniSongArtist != null) {
            String artistText = "Unknown Artist";
            if (currentAudio.getcategoryName() != null &&
                    !currentAudio.getcategoryName().isEmpty()) {
                artistText = currentAudio.getcategoryName();
            }
            tvMiniSongArtist.setText(artistText);
        }

        if (miniAudioPlayer != null) miniAudioPlayer.setVisibility(View.VISIBLE);
        if (btnMiniPlayPause != null) {
            btnMiniPlayPause.setImageResource(PlayerManager.isPlaying() ?
                    R.drawable.pause : R.drawable.play_player);
        }

        loadMiniPlayerAlbumArt(currentAudio);
        updateBluetoothIcon();
    }

    private void loadMiniPlayerAlbumArt(AudioModel audio) {
        if (audio.getImageUrl() != null && !audio.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .asBitmap()
                    .load(audio.getImageUrl())
                    .placeholder(R.drawable.video_placholder)
                    .into(new com.bumptech.glide.request.target.BitmapImageViewTarget(imgMiniAlbumArt) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            super.setResource(resource);
                            if (resource != null) {
                                Palette.from(resource).generate(palette -> {
                                    int defaultColor = getResources().getColor(R.color.gray);
                                    int lightColor = palette.getLightVibrantColor(defaultColor);
                                    if (miniAudioPlayer != null) {
                                        miniAudioPlayer.setBackgroundColor(lightColor);
                                    }
                                });
                            }
                        }
                    });
        } else {
            if (imgMiniAlbumArt != null) imgMiniAlbumArt.setImageResource(R.drawable.video_placholder);
            if (miniAudioPlayer != null) miniAudioPlayer.setBackgroundColor(getResources().getColor(R.color.gray));
        }
    }

    private void callLogoutApi() {
        SharedPrefManager sp = SharedPrefManager.getInstance(this);
        String accessToken = sp.getAccessToken();

        LinearLayout btnLayout = findViewById(R.id.btnlayout);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e("Logout", "No token found");
            return;
        }

        if (btnLayout != null) btnLayout.setVisibility(View.GONE);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.logout(accessToken);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (btnLayout != null) btnLayout.setVisibility(View.VISIBLE);

                if (response.isSuccessful()) {
                    Log.d("Logout", "Logged out successfully");
                    sp.clearAccessToken();
                    if (drawerLayout != null) drawerLayout.closeDrawers();
                    Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Logout failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (btnLayout != null) btnLayout.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===================== PLAYER LISTENER METHODS =====================
    @Override public void onVideoPlayerStarted() { hideNavigationBars(); }
    @Override public void onVideoPlayerFinished() { showNavigationBars(); }
    @Override public void onMoviePlayerStarted() { hideNavigationBars(); }
    @Override public void onMoviePlayerFinished() { showNavigationBars(); }
    @Override public void onTvShowPlayerStarted() { hideNavigationBars(); }
    @Override public void onTvShowPlayerFinished() { showNavigationBars(); }
    @Override public void onCatchUpPlayerStarted() { hideNavigationBars(); }
    @Override public void onCatchUpPlayerFinished() { showNavigationBars(); }
    @Override public void onRadioPlayerStarted() { hideNavigationBars(); }
    @Override public void onRadioPlayerFinished() { showNavigationBars(); }

    // ===================== BACK PRESS HANDLING =====================
    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (fragment instanceof VideoPlayerFragment) {
            if (((VideoPlayerFragment) fragment).handleBackPress()) return;
            showNavigationBars();
            getSupportFragmentManager().popBackStack();
        } else if (fragment instanceof MovieVideoPlayerFragment) {
            if (((MovieVideoPlayerFragment) fragment).handleBackPress()) return;
            showNavigationBars();
            getSupportFragmentManager().popBackStack();
        } else if (fragment instanceof TvShowEpisodeFragment) {
            if (((TvShowEpisodeFragment) fragment).handleBackPress()) return;
            showNavigationBars();
            getSupportFragmentManager().popBackStack();
        } else if (fragment instanceof CatchUpDetailFragment) {
            if (((CatchUpDetailFragment) fragment).handleBackPress()) return;
            showNavigationBars();
            getSupportFragmentManager().popBackStack();
        } else {
            if (drawerLayout != null && drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                drawerLayout.closeDrawer(Gravity.LEFT);
            } else {
                super.onBackPressed();
            }
        }
    }

    // ===================== PiP HANDLING =====================
    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof VideoPlayerFragment) {
            ((VideoPlayerFragment) fragment).onUserLeaveHint();
        } else if (fragment instanceof MovieVideoPlayerFragment) {
            ((MovieVideoPlayerFragment) fragment).onUserLeaveHint();
        } else if (fragment instanceof TvShowEpisodeFragment) {
            ((TvShowEpisodeFragment) fragment).onUserLeaveHint();
        } else if (fragment instanceof CatchUpDetailFragment) {
            ((CatchUpDetailFragment) fragment).onUserLeaveHint();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof VideoPlayerFragment) {
            ((VideoPlayerFragment) fragment).onPictureInPictureModeChanged(isInPictureInPictureMode);
        } else if (fragment instanceof MovieVideoPlayerFragment) {
            ((MovieVideoPlayerFragment) fragment).onPictureInPictureModeChanged(isInPictureInPictureMode);
        } else if (fragment instanceof TvShowEpisodeFragment) {
            ((TvShowEpisodeFragment) fragment).onPictureInPictureModeChanged(isInPictureInPictureMode);
        } else if (fragment instanceof CatchUpDetailFragment) {
            ((CatchUpDetailFragment) fragment).onPictureInPictureModeChanged(isInPictureInPictureMode);
        }
    }

    // ===================== NAVIGATION BAR CONTROL =====================
    public void hideNavigationBars() {
        runOnUiThread(() -> {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );

            if (bottomNavRecyclerView != null) {
                bottomNavRecyclerView.animate()
                        .translationY(bottomNavRecyclerView.getHeight())
                        .setDuration(300)
                        .withEndAction(() -> bottomNavRecyclerView.setVisibility(View.GONE))
                        .start();
            }

            androidx.cardview.widget.CardView cardMiniPlayer = findViewById(R.id.cardMiniPlayer);
            if (cardMiniPlayer != null) {
                cardMiniPlayer.animate()
                        .alpha(0f)
                        .translationY(cardMiniPlayer.getHeight())
                        .setDuration(250)
                        .withEndAction(() -> cardMiniPlayer.setVisibility(View.GONE))
                        .start();
            }

            FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
            if (fragmentContainer != null) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) fragmentContainer.getLayoutParams();
                params.removeRule(RelativeLayout.ABOVE);
                params.removeRule(RelativeLayout.BELOW);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                fragmentContainer.setLayoutParams(params);
            }
        });
    }

    public void showNavigationBars() {
        runOnUiThread(() -> {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

            if (bottomNavRecyclerView != null) {
                bottomNavRecyclerView.setVisibility(View.VISIBLE);
                bottomNavRecyclerView.setTranslationY(bottomNavRecyclerView.getHeight());
                bottomNavRecyclerView.animate()
                        .translationY(0)
                        .setDuration(400)
                        .setInterpolator(new OvershootInterpolator(0.8f))
                        .start();
            }

            androidx.cardview.widget.CardView cardMiniPlayer = findViewById(R.id.cardMiniPlayer);
            if (cardMiniPlayer != null) {
                if (PlayerManager.getCurrentAudio() != null) {
                    cardMiniPlayer.setVisibility(View.VISIBLE);
                    cardMiniPlayer.setAlpha(0f);
                    cardMiniPlayer.setTranslationY(cardMiniPlayer.getHeight());
                    cardMiniPlayer.animate()
                            .alpha(1f)
                            .translationY(0)
                            .setDuration(350)
                            .setInterpolator(new OvershootInterpolator(0.6f))
                            .setStartDelay(100)
                            .start();
                } else {
                    cardMiniPlayer.setVisibility(View.GONE);
                }
            }

            FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
            if (fragmentContainer != null) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) fragmentContainer.getLayoutParams();
                params.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

                if (cardMiniPlayer != null && cardMiniPlayer.getVisibility() == View.VISIBLE) {
                    params.addRule(RelativeLayout.ABOVE, R.id.cardMiniPlayer);
                } else {
                    params.addRule(RelativeLayout.ABOVE, R.id.bottom_nav_recycler_view);
                }
                fragmentContainer.setLayoutParams(params);
            }

            updateBluetoothIcon();
        });
    }

    // ===================== DRAWER TOGGLE =====================
    @Override
    public void onToggleDrawer() {
        if (drawerLayout != null) {
            if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                drawerLayout.closeDrawer(Gravity.LEFT);
            } else {
                drawerLayout.openDrawer(Gravity.LEFT);
            }
        }
    }

    // ===================== LIFECYCLE METHODS =====================
    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "onResume called");

        if (bottomNavItems.isEmpty()) {
            loadNavigationFromSharedPrefs();
        }

        refreshBluetoothState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (closeReceiver != null) unregisterReceiver(closeReceiver);
        if (miniPlayerReceiver != null) unregisterReceiver(miniPlayerReceiver);
        if (bluetoothReceiver != null) unregisterReceiver(bluetoothReceiver);
        if (networkChangeReceiver != null) unregisterReceiver(networkChangeReceiver);
        seekBarHandler.removeCallbacks(updateSeekBarRunnable);
    }

    // ===================== PUBLIC METHODS =====================
    public void selectTab(int position) {
        if (bottomNavAdapter != null && position >= 0 && position < bottomNavItems.size()) {
            bottomNavAdapter.setSelectedPosition(position);
            loadFragmentForBottomNav(bottomNavItems.get(position));
        }
    }

    public List<BottomNavItem> getBottomNavItems() {
        return bottomNavItems;
    }
}