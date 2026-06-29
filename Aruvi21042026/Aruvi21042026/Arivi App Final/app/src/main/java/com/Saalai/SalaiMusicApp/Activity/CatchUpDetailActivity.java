package com.Saalai.SalaiMusicApp.Activity;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Fragments.EpisodeListFragment;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.CatchUpChannelDetailsResponse;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerAdapterfocatchupdetail;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerAdapterfocatchuptab;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CatchUpDetailActivity extends AppCompatActivity {

    private static final String TAG = "CatchUpDetailActivity";
    private TextView tvChannelName, tvError, channelName;
    private RecyclerView recyclerViewTabs, shimmerRecyclerViewTabs, recyclerViewShimmer;
    private ViewPager2 viewPager;
    private TabAdapter tabAdapter;
    private List<String> tabDates = new ArrayList<>();
    private int selectedTab = 0;
    private CatchUpChannelDetailsResponse channelDetails;
    private ShimmerAdapterfocatchuptab shimmerTabAdapter;
    private ShimmerAdapterfocatchupdetail shimmerContentAdapter;

    // Enhanced Player Components
    private ExoPlayer player;
    private PlayerView playerView;
    private ProgressBar progressBar;
    private ScaleGestureDetector scaleGestureDetector;
    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private boolean isFullscreen = false;
    private SeekBar playerSeekBar;
    private boolean isSeeking = false;
    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    private TextView tvCurrentTime, tvTotalTime;
    private ImageButton exoPlay, exoPause, btnRewind, btnForward;
    ImageView btnFullscreen;
    private ImageView backbtn;
    private androidx.media3.ui.AspectRatioFrameLayout playerContainer;
    final int SEEK_INTERVAL = 10000; // 10 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catch_up_detail);

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.black)); // replace with your color
        }

        View rootView = findViewById(R.id.root_view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && Build.VERSION.SDK_INT <= 34) {
            // For Android 11 to 14, explicitly disable edge-to-edge
            getWindow().setDecorFitsSystemWindows(true);

            if (rootView != null) {
                rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                    v.setPadding(0, 0, 0, 0);
                    return insets;
                });
            }
        } else {
            // Pre-Android 12
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            if (rootView != null) {
                rootView.setFitsSystemWindows(true); // critical for proper layout
            }
        }



        PlayerManager.pausePlayback();

        backbtn = findViewById(R.id.backbtn);

        backbtn.setOnClickListener(v -> onBackPressed());

        setupWindowAndStatusBar();
        initializeViews();
        setupClickListeners();
        setupPlayer();
        setupNetworkMonitoring();

        // Get channel ID from intent
        String channelId = getIntent().getStringExtra("CHANNEL_ID");
        if (channelId != null) {
            fetchCatchUpChannelDetails(Integer.parseInt(channelId));
        } else {
            showError("No channel ID provided");
        }
    }

    private void setupWindowAndStatusBar() {
        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.black));
        }

        View rootView = findViewById(R.id.main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Build.VERSION.SDK_INT <= 34) {
            getWindow().setDecorFitsSystemWindows(true);
            if (rootView != null) {
                rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                    v.setPadding(0, 0, 0, 0);
                    return insets;
                });
            }
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            if (rootView != null) {
                rootView.setFitsSystemWindows(true);
            }
        }
    }

    private void initializeViews() {
        tvChannelName = findViewById(R.id.tvChannelName);
        tvError = findViewById(R.id.tvError);
        recyclerViewTabs = findViewById(R.id.recyclerViewTabs);
        shimmerRecyclerViewTabs = findViewById(R.id.shimmerrecyclerViewTabs);
        recyclerViewShimmer = findViewById(R.id.recyclerViewShimmer);
        viewPager = findViewById(R.id.viewPager);
        channelName = findViewById(R.id.channel_name);
        backbtn = findViewById(R.id.backimg);

        // Player views
        playerView = findViewById(R.id.player_view);
        progressBar = findViewById(R.id.progressBar);
        playerContainer = findViewById(R.id.playerContainer);

        // Player controls
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        playerSeekBar = findViewById(R.id.playerSeekBar);
        exoPlay = findViewById(R.id.exo_play);
        exoPause = findViewById(R.id.exo_pause);
        btnRewind = findViewById(R.id.btnRewind);
        btnForward = findViewById(R.id.btnForward);
        btnFullscreen = findViewById(R.id.btnFullscreen);

        viewPager.setUserInputEnabled(false);
        setupTabRecyclerView();
        setupShimmerRecyclerViews();
    }

    private void setupPlayer() {
        // Initialize ExoPlayer with optimized settings
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(this);
        trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoBitrate(Integer.MAX_VALUE));

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(8000, 30000, 1500, 3000)
                .build();

        player = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .build();

        playerView.setPlayer(player);
        setupPlayerListeners();
        setupSeekBar();
        setupGestureDetector();

        // Set initial control states
        exoPlay.setVisibility(View.INVISIBLE);
        exoPause.setVisibility(View.VISIBLE);
    }

    private void setupPlayerListeners() {
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                switch (playbackState) {
                    case Player.STATE_BUFFERING:
                        progressBar.setVisibility(View.VISIBLE);
                        break;
                    case Player.STATE_READY:
                        progressBar.setVisibility(View.GONE);
                        tvTotalTime.setText(formatTime(player.getDuration()));
                        break;
                    case Player.STATE_ENDED:
                        progressBar.setVisibility(View.GONE);
                        exoPlay.setVisibility(View.VISIBLE);
                        exoPause.setVisibility(View.INVISIBLE);
                        break;
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                progressBar.setVisibility(View.VISIBLE);
                Toast.makeText(CatchUpDetailActivity.this, "Playback error occurred", Toast.LENGTH_SHORT).show();

                // Retry after 2 seconds if network available
                if (isNetworkAvailable()) {
                    new Handler().postDelayed(() -> {
                        if (player != null) {
                            player.prepare();
                        }
                    }, 2000);
                }
            }

            @Override
            public void onVideoSizeChanged(VideoSize videoSize) {
                // Handle video size changes if needed
            }
        });
    }

    private void setupSeekBar() {
        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    long duration = player.getDuration();
                    long newPosition = (duration * progress) / 100;
                    player.seekTo(newPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeeking = false;
            }
        });

        // Progress updater
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (player != null && player.getPlaybackState() == Player.STATE_READY && !isSeeking) {
                    long currentPos = player.getCurrentPosition();
                    long duration = player.getDuration();
                    if (duration > 0) {
                        int progress = (int) ((currentPos * 100) / duration);
                        playerSeekBar.setProgress(progress);
                        tvCurrentTime.setText(formatTime(currentPos));
                    }
                }
                progressHandler.postDelayed(this, 500);
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void setupGestureDetector() {
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private static final float SCALE_THRESHOLD = 0.1f;
            private float scaleFactor = 1.0f;

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (!isFullscreen) {
                    return false;
                }

                scaleFactor *= detector.getScaleFactor();
                if (scaleFactor > 1.0f + SCALE_THRESHOLD) {
                    setScalingMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                    scaleFactor = 1.0f;
                } else if (scaleFactor < 1.0f - SCALE_THRESHOLD) {
                    setScalingMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    scaleFactor = 1.0f;
                }
                return true;
            }
        });

        playerView.setOnTouchListener((v, event) -> {
            if (isFullscreen) {
                scaleGestureDetector.onTouchEvent(event);
            }
            return false;
        });
    }

    private void setupClickListeners() {
        backbtn.setOnClickListener(v -> onBackPressed());

        // Player control listeners
        exoPlay.setOnClickListener(v -> {
            if (player != null) {
                player.play();
                exoPlay.setVisibility(View.INVISIBLE);
                exoPause.setVisibility(View.VISIBLE);
            }
        });

        exoPause.setOnClickListener(v -> {
            if (player != null) {
                player.pause();
                exoPlay.setVisibility(View.VISIBLE);
                exoPause.setVisibility(View.INVISIBLE);
            }
        });

        btnForward.setOnClickListener(v -> {
            if (player != null) {
                long newPos = player.getCurrentPosition() + SEEK_INTERVAL;
                if (newPos > player.getDuration()) newPos = player.getDuration();
                player.seekTo(newPos);
                updateSeekBar(newPos);
                animateButton(btnForward, true);
            }
        });

        btnRewind.setOnClickListener(v -> {
            if (player != null) {
                long newPos = player.getCurrentPosition() - SEEK_INTERVAL;
                if (newPos < 0) newPos = 0;
                player.seekTo(newPos);
                updateSeekBar(newPos);
                animateButton(btnRewind, false);
            }
        });

        btnFullscreen.setOnClickListener(v -> toggleFullscreen());
    }

    private void setupNetworkMonitoring() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkRequest networkRequest = new NetworkRequest.Builder().build();

        connectivityManager.registerNetworkCallback(networkRequest, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    if (player != null) {
                        progressBar.setVisibility(View.VISIBLE);
                        player.play();
                    }
                });
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> {
                    if (player != null && player.isPlaying()) {
                        player.pause();
                        progressBar.setVisibility(View.VISIBLE);
                        Toast.makeText(CatchUpDetailActivity.this, "Network lost. Paused video.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void setupTabRecyclerView() {
        tabAdapter = new TabAdapter(tabDates, position -> selectTab(position));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerViewTabs.setLayoutManager(layoutManager);
        recyclerViewTabs.setAdapter(tabAdapter);
    }

    private void setupShimmerRecyclerViews() {
        int columnCount = 6;
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            columnCount = 6;
        }

        shimmerTabAdapter = new ShimmerAdapterfocatchuptab(columnCount);
        LinearLayoutManager shimmerTabLayout = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        shimmerRecyclerViewTabs.setLayoutManager(shimmerTabLayout);
        shimmerRecyclerViewTabs.setAdapter(shimmerTabAdapter);

        int columnCount1 = 2;
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            columnCount1 = 4;
        }

        shimmerContentAdapter = new ShimmerAdapterfocatchupdetail(20);
        GridLayoutManager shimmerContentLayout = new GridLayoutManager(this,  columnCount1);
        recyclerViewShimmer.setLayoutManager(shimmerContentLayout);
        recyclerViewShimmer.setAdapter(shimmerContentAdapter);
    }

    private void fetchCatchUpChannelDetails(int channelId) {
        showLoading(true);
        SharedPrefManager sp = SharedPrefManager.getInstance(this);
        String accessToken = sp.getAccessToken();
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Call<CatchUpChannelDetailsResponse> call = apiService.getCatchUpChannelDetails(accessToken, channelId);
        call.enqueue(new Callback<CatchUpChannelDetailsResponse>() {
            @Override
            public void onResponse(Call<CatchUpChannelDetailsResponse> call, Response<CatchUpChannelDetailsResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    channelDetails = response.body();
                    updateUIWithChannelData();
                    Log.d(TAG, "Channel details loaded successfully for ID: " + channelId);
                }else {
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorStr);
                            if (jsonObject.has("error") && "access_denied".equals(jsonObject.getString("error"))) {
                                Toast.makeText(CatchUpDetailActivity.this, "Access Denied", Toast.LENGTH_SHORT).show();
                                callLogoutApi();
                            } else {
                                Log.e("Dashboard", "Server Error Code: " + response.code() + ", Error: " + errorStr);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Dashboard", "Error parsing errorBody", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<CatchUpChannelDetailsResponse> call, Throwable t) {
                showLoading(false);
                showError("Network error: " + t.getMessage());
                Log.e(TAG, "API Call failed: " + t.getMessage());
            }
        });
    }

    private void callLogoutApi() {
        SharedPrefManager sp = SharedPrefManager.getInstance(CatchUpDetailActivity.this);
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

                    Intent intent = new Intent(CatchUpDetailActivity.this, SignUpActivity.class);
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

    private void updateUIWithChannelData() {
        if (channelDetails == null) return;

        tvChannelName.setText(channelDetails.getChannelName());
        setupTabs();
        setupViewPager();
        showContent(true);

        // Auto-play first episode if available
        if (channelDetails.getShowList() != null && !channelDetails.getShowList().isEmpty()) {
            CatchUpChannelDetailsResponse.ShowDate firstDate = channelDetails.getShowList().get(0);
            if (firstDate.getEpisodeList() != null && !firstDate.getEpisodeList().isEmpty()) {
                playEpisode(firstDate.getEpisodeList().get(0));
            }
        }
    }

    private void setupTabs() {
        tabDates.clear();
        if (channelDetails.getShowList() != null && !channelDetails.getShowList().isEmpty()) {
            for (CatchUpChannelDetailsResponse.ShowDate showDate : channelDetails.getShowList()) {
                tabDates.add(showDate.getDate());
            }
        } else {
            showError("No episodes available for this channel");
            return;
        }
        tabAdapter.notifyDataSetChanged();
        selectTab(0);
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0, false);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                selectTab(position);
            }
        });
    }

    private void selectTab(int tab) {
        selectedTab = tab;
        tabAdapter.setSelectedTab(tab);
        viewPager.setCurrentItem(tab, true);

        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerViewTabs.getLayoutManager();
        if (layoutManager != null) {
            // Center the selected tab
            View tabView = layoutManager.findViewByPosition(tab);
            if (tabView != null) {
                int parentWidth = recyclerViewTabs.getWidth();
                int childWidth = tabView.getWidth();
                int offset = (parentWidth / 2) - (childWidth / 2);
                layoutManager.scrollToPositionWithOffset(tab, offset);
            } else {
                // fallback
                recyclerViewTabs.scrollToPosition(tab);
            }
        }
    }


    public void playEpisode(CatchUpChannelDetailsResponse.Episode episode) {
        if (episode == null || episode.getShowURL() == null) {
            Snackbar.make(playerView, "Invalid episode URL", Snackbar.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Playing episode: " + episode.getShowName());
        Log.d(TAG, "Video URL: " + episode.getShowURL());

        // Update the channel name with the current episode/show name
        if (episode.getShowName() != null && !episode.getShowName().isEmpty()) {
            channelName.setText(episode.getShowName());
        }

        if (player != null) {
            MediaItem mediaItem = MediaItem.fromUri(episode.getShowURL());
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();

            // Update control states
            exoPlay.setVisibility(View.INVISIBLE);
            exoPause.setVisibility(View.VISIBLE);
        }
    }

    // Player control methods
    private void updateSeekBar(long position) {
        if (playerSeekBar != null && player != null) {
            long duration = player.getDuration();
            if (duration > 0) {
                int progress = (int) ((position * 100) / duration);
                playerSeekBar.setProgress(progress);
                tvCurrentTime.setText(formatTime(position));
            }
        }
    }

    private String formatTime(long millis) {
        int totalSeconds = (int) (millis / 1000);
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void toggleFullscreen() {
        if (isFullscreen) {
            // Exit fullscreen → lock to portrait
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            recyclerViewTabs.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            playerContainer.setLayoutParams(params);
            playerContainer.requestLayout();

            currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
            playerView.setResizeMode(currentResizeMode);
            showSystemUI();
            isFullscreen = false;
        } else {
            // Enter fullscreen → lock to landscape
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            recyclerViewTabs.setVisibility(View.GONE);
            viewPager.setVisibility(View.GONE);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            );
            playerContainer.setLayoutParams(params);
            playerContainer.requestLayout();

            playerView.setResizeMode(currentResizeMode);
            hideSystemUI();
            isFullscreen = true;
        }
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void setScalingMode(int newMode) {
        if (currentResizeMode != newMode) {
            currentResizeMode = newMode;
            playerView.setResizeMode(newMode);
        }
    }

    private void animateButton(View button, boolean forward) {
        button.setAlpha(1f);
        float fromX = forward ? 0 : 0;
        float toX = forward ? 50 : -50;

        TranslateAnimation translate = new TranslateAnimation(fromX, toX, 0, 0);
        translate.setDuration(300);
        translate.setRepeatCount(0);

        AlphaAnimation alpha = new AlphaAnimation(1f, 0f);
        alpha.setDuration(300);
        alpha.setRepeatCount(0);

        AnimationSet set = new AnimationSet(true);
        set.addAnimation(translate);
        set.addAnimation(alpha);
        button.startAnimation(set);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        return cm.getActiveNetwork() != null;
    }

    private void showLoading(boolean show) {
        if (show) {
            recyclerViewShimmer.setVisibility(View.VISIBLE);
            shimmerRecyclerViewTabs.setVisibility(View.VISIBLE);
            recyclerViewTabs.setVisibility(View.GONE);
            viewPager.setVisibility(View.GONE);
            tvError.setVisibility(View.GONE);
        } else {
            recyclerViewShimmer.setVisibility(View.GONE);
            shimmerRecyclerViewTabs.setVisibility(View.GONE);
        }
    }

    private void showContent(boolean show) {
        if (show) {
            viewPager.setVisibility(View.VISIBLE);
            recyclerViewTabs.setVisibility(View.VISIBLE);
            recyclerViewShimmer.setVisibility(View.GONE);
            shimmerRecyclerViewTabs.setVisibility(View.GONE);
            tvError.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        recyclerViewShimmer.setVisibility(View.GONE);
        shimmerRecyclerViewTabs.setVisibility(View.GONE);
        recyclerViewTabs.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message);
    }

    @Override
    public void onBackPressed() {
        if (isFullscreen) {
            toggleFullscreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        if (progressHandler != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    // Picture-in-Picture support
    private void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.util.Rational aspectRatio;
            if (player != null && player.getVideoSize() != null) {
                int videoWidth = player.getVideoSize().width;
                int videoHeight = player.getVideoSize().height;
                if (videoWidth > 0 && videoHeight > 0) {
                    aspectRatio = new android.util.Rational(videoWidth, videoHeight);
                } else {
                    aspectRatio = new android.util.Rational(playerView.getWidth(), playerView.getHeight());
                }
            } else {
                aspectRatio = new android.util.Rational(playerView.getWidth(), playerView.getHeight());
            }

            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();
            enterPictureInPictureMode(params);
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPipMode();
            closeMainActivity();
            closeViewMoreMovieActivity();
        }
    }



    private void closeMainActivity() {
        // Send broadcast to close MainActivity
        Intent closeIntent = new Intent("CLOSE_MAIN_ACTIVITY");
        sendBroadcast(closeIntent);
    }


    private void closeViewMoreMovieActivity() {
        // Send broadcast to close MainActivity
        Intent closeIntent = new Intent("CLOSE_ViewMoreMovieActivity");
        sendBroadcast(closeIntent);
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPiPMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPiPMode, newConfig);
        if (isInPiPMode) {
            // Hide UI elements
            recyclerViewTabs.setVisibility(View.GONE);
            viewPager.setVisibility(View.GONE);
            backbtn.setVisibility(View.GONE);
            tvChannelName.setVisibility(View.GONE);
            channelName.setVisibility(View.GONE);
            playerView.hideController();

            closeMainActivity();
            closeViewMoreMovieActivity();

        } else {
            // Restore UI
            recyclerViewTabs.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);
            backbtn.setVisibility(View.VISIBLE);
            tvChannelName.setVisibility(View.VISIBLE);
            channelName.setVisibility(View.VISIBLE);
            playerView.showController();
        }
    }

    // ---------------- Tab Adapter -----------------
    public static class TabAdapter extends RecyclerView.Adapter<TabAdapter.TabViewHolder> {
        private final List<String> tabDates;
        private int selectedTab = 0;
        private final OnTabClickListener listener;

        public interface OnTabClickListener { void onTabClick(int position); }

        public TabAdapter(List<String> tabDates, OnTabClickListener listener) {
            this.tabDates = tabDates;
            this.listener = listener;
        }

        public void setSelectedTab(int position) {
            selectedTab = position;
            notifyDataSetChanged();
        }

        @Override
        public TabViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tab, parent, false);
            return new TabViewHolder(view);
        }

        @Override
        public void onBindViewHolder(TabViewHolder holder, int position) {
            holder.bind(tabDates.get(position), position == selectedTab);
            holder.itemView.setOnClickListener(v -> listener.onTabClick(position));
        }

        @Override
        public int getItemCount() { return tabDates.size(); }

        static class TabViewHolder extends RecyclerView.ViewHolder {
            TextView tabText;
            View tabLine;

            public TabViewHolder(View itemView) {
                super(itemView);
                tabText = itemView.findViewById(R.id.tabText);
                tabLine = itemView.findViewById(R.id.tabLine);
            }

            public void bind(String date, boolean isSelected) {
                tabText.setText(date);
                tabText.setTextColor(itemView.getContext().getResources()
                        .getColor(isSelected ? R.color.white : R.color.gray));
                tabLine.setBackgroundColor(itemView.getContext().getResources()
                        .getColor(isSelected ? R.color.white : android.R.color.transparent));
            }
        }
    }

    // ---------------- ViewPager Adapter -----------------
    private class ViewPagerAdapter extends FragmentStateAdapter {
        public ViewPagerAdapter(AppCompatActivity activity) { super(activity); }

        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            if (channelDetails != null && channelDetails.getShowList() != null &&
                    position < channelDetails.getShowList().size()) {
                CatchUpChannelDetailsResponse.ShowDate showDate = channelDetails.getShowList().get(position);
                return EpisodeListFragment.newInstance(showDate.getEpisodeList(),
                        channelDetails.getChannelName(),
                        channelDetails.getChannelLogo(),
                        channelDetails.getChannelDescription(),
                        showDate.getDate());
            }
            return EpisodeListFragment.newInstance(new ArrayList<>(), "", "", "", "");
        }

        @Override
        public int getItemCount() {
            return channelDetails != null && channelDetails.getShowList() != null ?
                    channelDetails.getShowList().size() : 0;
        }
    }
}