package com.Saalai.SalaiMusicApp.Fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Rational;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
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

public class CatchUpDetailFragment extends Fragment implements EpisodeListFragment.EpisodeClickListener {

    private static final String TAG = "CatchUpDetailFragment";

    private TextView tvChannelName, tvError, channelName;
    private RecyclerView recyclerViewTabs, shimmerRecyclerViewTabs, recyclerViewShimmer, recyclerViewTabs1;
    private ViewPager2 viewPager,viewPager1;
    private TabAdapter tabAdapter;
    private List<String> tabDates = new ArrayList<>();
    private int selectedTab = 0;
    private CatchUpChannelDetailsResponse channelDetails;
    private ShimmerAdapterfocatchuptab shimmerTabAdapter;
    private ShimmerAdapterfocatchupdetail shimmerContentAdapter;

    private LinearLayout rightDrawerContainer;
    private RecyclerView drawerRecyclerView;
    private ImageView btnCloseDrawer;
    private View drawerOverlay;
    private boolean isDrawerOpen = false;
    private ValueAnimator drawerAnimator;
    private static final long DRAWER_ANIMATION_DURATION = 300;
    private ImageView btnOpenDrawer;


    // Enhanced Player Components
    private ExoPlayer player;
    private PlayerView playerView;
    private ProgressBar progressBar;
    private ScaleGestureDetector scaleGestureDetector;
    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private boolean isFullscreen = false;
    private boolean isInPiPMode = false;
    private SeekBar playerSeekBar;
    private boolean isSeeking = false;
    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    private TextView tvCurrentTime, tvTotalTime;
    private ImageButton exoPlay, exoPause, btnRewind, btnForward;
    private ImageView btnFullscreen;
    private ImageView backbtn;
    private AspectRatioFrameLayout playerContainer;
    final int SEEK_INTERVAL = 10000; // 10 seconds

    private String channelId;

    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    private boolean shouldResumeOnAudioFocusGain = false;

    // Factory method to create new instance
    public static CatchUpDetailFragment newInstance(String channelId) {
        CatchUpDetailFragment fragment = new CatchUpDetailFragment();
        Bundle args = new Bundle();
        args.putString("CHANNEL_ID", channelId);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onEpisodeClicked(CatchUpChannelDetailsResponse.Episode episode) {
        playEpisode(episode);
    }

    public interface CatchUpPlayerListener {
        void onCatchUpPlayerStarted();
        void onCatchUpPlayerFinished();
    }

    private CatchUpPlayerListener catchUpPlayerListener;

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof CatchUpPlayerListener) {
            catchUpPlayerListener = (CatchUpPlayerListener) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_catch_up_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Notify listener
        if (catchUpPlayerListener != null) {
            catchUpPlayerListener.onCatchUpPlayerStarted();
        } else {
            sendNavigationBarVisibility(false);
        }

        PlayerManager.pausePlayback();
        initViews(view);
        setupClickListeners();
        setupPlayer();
        setupNetworkMonitoring();

        // Get channel ID from arguments
        Bundle args = getArguments();
        if (args != null) {
            channelId = args.getString("CHANNEL_ID");
            if (channelId != null) {
                fetchCatchUpChannelDetails(Integer.parseInt(channelId));
            } else {
                showError("No channel ID provided");
            }
        }


        // Check if we're in landscape mode on fragment creation
        if (isLandscape() && !isFullscreen && !isInPiPMode) {
            // Small delay to ensure views are properly initialized
            new Handler().postDelayed(() -> {
                if (getView() != null && !isFullscreen && !isInPiPMode) {
                    toggleFullscreen();
                    openDrawer();
                }
            }, 300);

        }
    }

    private boolean isLandscape() {
        if (getContext() == null) return false;
        int orientation = getContext().getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }


    private void initViews(View view) {
        rightDrawerContainer = view.findViewById(R.id.rightDrawerContainer);
        drawerRecyclerView = view.findViewById(R.id.drawerRecyclerView);
        btnCloseDrawer = view.findViewById(R.id.btnCloseDrawer);
        drawerOverlay = view.findViewById(R.id.drawerOverlay);
        btnOpenDrawer = view.findViewById(R.id.btnOpenDrawer);
        // Setup swipe gesture for drawer
        setupDrawerSwipeGestures();



        btnCloseDrawer.setOnClickListener(v -> closeDrawer());
        // Setup swipe gesture for drawer
        setupDrawerSwipeGestures();



        if (btnOpenDrawer != null) {
            btnOpenDrawer.setOnClickListener(v -> openDrawer());
        }



        tvChannelName = view.findViewById(R.id.tvChannelName);
        tvError = view.findViewById(R.id.tvError);
        recyclerViewTabs = view.findViewById(R.id.recyclerViewTabs);
        recyclerViewTabs1 = view.findViewById(R.id.recyclerViewTabs1);

        shimmerRecyclerViewTabs = view.findViewById(R.id.shimmerrecyclerViewTabs);
        recyclerViewShimmer = view.findViewById(R.id.recyclerViewShimmer);
        viewPager = view.findViewById(R.id.viewPager);
        viewPager1 = view.findViewById(R.id.viewPager1);
        channelName = view.findViewById(R.id.channel_name);
        backbtn = view.findViewById(R.id.backbtn);

        // Player views
        playerView = view.findViewById(R.id.player_view);
        progressBar = view.findViewById(R.id.progressBar);
        playerContainer = view.findViewById(R.id.playerContainer);

        // Player controls
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        playerSeekBar = view.findViewById(R.id.playerSeekBar);
        exoPlay = view.findViewById(R.id.exo_play);
        exoPause = view.findViewById(R.id.exo_pause);
        btnRewind = view.findViewById(R.id.btnRewind);
        btnForward = view.findViewById(R.id.btnForward);
        btnFullscreen = view.findViewById(R.id.btnFullscreen);

        viewPager.setUserInputEnabled(false);
        viewPager1.setUserInputEnabled(false);
        setupTabRecyclerView();
        setupShimmerRecyclerViews();
    }


    private void setupDrawerSwipeGestures() {
        View rootView = getView();
        if (rootView != null) {
            rootView.setOnTouchListener(new View.OnTouchListener() {
                private float startX = 0;
                private boolean isDrawerGesture = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Don't handle drawer gestures in fullscreen or PiP mode
                    if (isFullscreen || isInPiPMode) {
                        return false;
                    }

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getX();
                            isDrawerGesture = false;
                            break;

                        case MotionEvent.ACTION_MOVE:
                            float diffX = event.getX() - startX;

                            // Check if it's a left-to-right swipe from left edge
                            if (!isDrawerOpen && startX < 100 && diffX > 50) {
                                isDrawerGesture = true;
                                // Preview drawer opening
                                previewDrawer(diffX);
                                return true;
                            }
                            // Check if it's a right-to-left swipe on open drawer
                            else if (isDrawerOpen && startX > getView().getWidth() - 100 && diffX < -50) {
                                isDrawerGesture = true;
                                // Preview drawer closing
                                previewDrawerClose(diffX);
                                return true;
                            }
                            break;

                        case MotionEvent.ACTION_UP:
                            float finalDiffX = event.getX() - startX;

                            if (isDrawerGesture) {
                                // If swipe was significant enough, toggle drawer
                                if (!isDrawerOpen && finalDiffX > 150) {
                                    openDrawer();
                                } else if (isDrawerOpen && finalDiffX < -150) {
                                    closeDrawer();
                                } else {
                                    // Reset to original state
                                    resetDrawerPosition();
                                }
                                return true;
                            }
                            break;
                    }
                    return false;
                }
            });
        }
    }

    private void openDrawer() {
        if (isDrawerOpen || drawerAnimator != null && drawerAnimator.isRunning()) {
            return;
        }

        isDrawerOpen = true;
        rightDrawerContainer.setVisibility(View.VISIBLE);
        drawerOverlay.setVisibility(View.VISIBLE);

        drawerAnimator = ValueAnimator.ofFloat(rightDrawerContainer.getTranslationX(), 0);
        drawerAnimator.setDuration(DRAWER_ANIMATION_DURATION);
        drawerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        drawerAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            rightDrawerContainer.setTranslationX(value);

            // Update overlay alpha based on drawer position
            float progress = 1 - (value / rightDrawerContainer.getWidth());
            drawerOverlay.setAlpha(progress * 0.7f);
        });
        drawerAnimator.start();
    }

    private void closeDrawer() {
        if (!isDrawerOpen || drawerAnimator != null && drawerAnimator.isRunning()) {
            return;
        }

        drawerAnimator = ValueAnimator.ofFloat(rightDrawerContainer.getTranslationX(), rightDrawerContainer.getWidth());
        drawerAnimator.setDuration(DRAWER_ANIMATION_DURATION);
        drawerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        drawerAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            rightDrawerContainer.setTranslationX(value);

            // Update overlay alpha based on drawer position
            float progress = 1 - (value / rightDrawerContainer.getWidth());
            drawerOverlay.setAlpha(progress * 0.7f);
        });

        drawerAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                rightDrawerContainer.setVisibility(View.GONE);
                drawerOverlay.setVisibility(View.GONE);
                isDrawerOpen = false;
            }
        });
        drawerAnimator.start();
    }

    private void previewDrawer(float diffX) {
        float maxWidth = rightDrawerContainer.getWidth();
        float translation = Math.min(diffX - 50, maxWidth); // Subtract initial threshold
        if (translation < 0) translation = 0;

        rightDrawerContainer.setVisibility(View.VISIBLE);
        rightDrawerContainer.setTranslationX(maxWidth - translation);
        drawerOverlay.setVisibility(View.VISIBLE);
        drawerOverlay.setAlpha((translation / maxWidth) * 0.7f);
    }

    private void previewDrawerClose(float diffX) {
        float currentX = rightDrawerContainer.getTranslationX();
        float newX = currentX + Math.abs(diffX);
        float maxWidth = rightDrawerContainer.getWidth();

        if (newX > maxWidth) newX = maxWidth;

        rightDrawerContainer.setTranslationX(newX);
        drawerOverlay.setAlpha((1 - (newX / maxWidth)) * 0.7f);
    }

    private void resetDrawerPosition() {
        if (isDrawerOpen) {
            // Snap back to open position
            ValueAnimator animator = ValueAnimator.ofFloat(rightDrawerContainer.getTranslationX(), 0);
            animator.setDuration(200);
            animator.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                rightDrawerContainer.setTranslationX(value);
            });
            animator.start();
        } else {
            // Snap back to closed position
            ValueAnimator animator = ValueAnimator.ofFloat(rightDrawerContainer.getTranslationX(), rightDrawerContainer.getWidth());
            animator.setDuration(200);
            animator.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                rightDrawerContainer.setTranslationX(value);
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    rightDrawerContainer.setVisibility(View.GONE);
                    drawerOverlay.setVisibility(View.GONE);
                }
            });
            animator.start();
        }
    }




    private void setupPlayer() {
        // Initialize ExoPlayer with optimized settings
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(requireContext());
        trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoBitrate(Integer.MAX_VALUE));

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(8000, 30000, 1500, 3000)
                .build();

        player = new ExoPlayer.Builder(requireContext())
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .build();

        playerView.setPlayer(player);
        setupPlayerListeners();
        setupSeekBar();
        setupGestureDetector();

        // Initialize audio focus
        initializeAudioFocus();

        // Set initial control states
        exoPlay.setVisibility(View.INVISIBLE);
        exoPause.setVisibility(View.VISIBLE);
    }

    private void initializeAudioFocus() {
        audioManager = (AudioManager) requireContext().getSystemService(android.content.Context.AUDIO_SERVICE);

        audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        // Other app stopped playing audio, we can resume if we were playing
                        if (shouldResumeOnAudioFocusGain && player != null && !player.isPlaying()) {
                            player.play();
                            shouldResumeOnAudioFocusGain = false;

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (exoPlay != null && exoPause != null) {
                                        exoPlay.setVisibility(View.INVISIBLE);
                                        exoPause.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        }
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS:
                        // Another app started playing audio for an indefinite time
                        if (player != null && player.isPlaying()) {
                            player.pause();
                            shouldResumeOnAudioFocusGain = false; // Don't auto-resume

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (exoPlay != null && exoPause != null) {
                                        exoPlay.setVisibility(View.VISIBLE);
                                        exoPause.setVisibility(View.INVISIBLE);
                                    }
                                });
                            }
                        }
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        // Another app is playing audio temporarily
                        if (player != null && player.isPlaying()) {
                            player.pause();
                            shouldResumeOnAudioFocusGain = true; // Auto-resume when we get focus back

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (exoPlay != null && exoPause != null) {
                                        exoPlay.setVisibility(View.VISIBLE);
                                        exoPause.setVisibility(View.INVISIBLE);
                                    }
                                });
                            }
                        }
                        break;
                }
            }
        };
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
                        if (tvTotalTime != null) {
                            tvTotalTime.setText(formatTime(player.getDuration()));
                        }
                        break;
                    case Player.STATE_ENDED:
                        progressBar.setVisibility(View.GONE);
                        if (exoPlay != null) exoPlay.setVisibility(View.VISIBLE);
                        if (exoPause != null) exoPause.setVisibility(View.INVISIBLE);

                        // Release audio focus when playback ends
                        if (audioManager != null) {
                            audioManager.abandonAudioFocus(audioFocusChangeListener);
                        }
                        break;
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                progressBar.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), "Playback error occurred", Toast.LENGTH_SHORT).show();

                // Release audio focus on error
                if (audioManager != null) {
                    audioManager.abandonAudioFocus(audioFocusChangeListener);
                }

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
                        if (tvCurrentTime != null) {
                            tvCurrentTime.setText(formatTime(currentPos));
                        }
                    }
                }
                progressHandler.postDelayed(this, 500);
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void setupGestureDetector() {
        scaleGestureDetector = new ScaleGestureDetector(requireContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
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
        backbtn.setOnClickListener(v -> handleBackNavigation());

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

        if (btnFullscreen != null) {
            btnFullscreen.setOnClickListener(v -> toggleFullscreen());
        }
    }

    private void setupNetworkMonitoring() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        NetworkRequest networkRequest = new NetworkRequest.Builder().build();

        connectivityManager.registerNetworkCallback(networkRequest, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (player != null) {
                            progressBar.setVisibility(View.VISIBLE);
                            player.play();
                        }
                    });
                }
            }

            @Override
            public void onLost(Network network) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (player != null && player.isPlaying()) {
                            player.pause();
                            progressBar.setVisibility(View.VISIBLE);
                            Toast.makeText(requireContext(), "Network lost. Paused video.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void setupTabRecyclerView() {
        tabAdapter = new TabAdapter(tabDates, position -> selectTab(position));
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerViewTabs.setLayoutManager(layoutManager);
        recyclerViewTabs.setAdapter(tabAdapter);

        LinearLayoutManager layoutManager1 = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerViewTabs1.setLayoutManager(layoutManager1);
        recyclerViewTabs1.setAdapter(tabAdapter);



    }

    private void setupShimmerRecyclerViews() {
        shimmerTabAdapter = new ShimmerAdapterfocatchuptab(6);
        LinearLayoutManager shimmerTabLayout = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        shimmerRecyclerViewTabs.setLayoutManager(shimmerTabLayout);
        shimmerRecyclerViewTabs.setAdapter(shimmerTabAdapter);

        shimmerContentAdapter = new ShimmerAdapterfocatchupdetail(20);
        GridLayoutManager shimmerContentLayout = new GridLayoutManager(requireContext(), 3);
        recyclerViewShimmer.setLayoutManager(shimmerContentLayout);
        recyclerViewShimmer.setAdapter(shimmerContentAdapter);
    }

    private void fetchCatchUpChannelDetails(int channelId) {
        showLoading(true);
        SharedPrefManager sp = SharedPrefManager.getInstance(requireContext());
        String accessToken = sp.getAccessToken();
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Call<CatchUpChannelDetailsResponse> call = apiService.getCatchUpChannelDetails(accessToken, channelId);
        call.enqueue(new Callback<CatchUpChannelDetailsResponse>() {
            @Override
            public void onResponse(Call<CatchUpChannelDetailsResponse> call, Response<CatchUpChannelDetailsResponse> response) {
                // Check if fragment is still attached
                if (!isAdded() || getActivity() == null) {
                    Log.d(TAG, "Fragment not attached, ignoring API response");
                    return;
                }

                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    channelDetails = response.body();
                    updateUIWithChannelData();
                    Log.d(TAG, "Channel details loaded successfully for ID: " + channelId);
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<CatchUpChannelDetailsResponse> call, Throwable t) {
                // Check if fragment is still attached
                if (!isAdded() || getActivity() == null) {
                    Log.d(TAG, "Fragment not attached, ignoring API failure");
                    return;
                }

                showLoading(false);
                showError("Network error: " + t.getMessage());
                Log.e(TAG, "API Call failed: " + t.getMessage());
            }
        });
    }

    private void handleErrorResponse(Response<CatchUpChannelDetailsResponse> response) {
        try {
            if (response.errorBody() != null) {
                String errorStr = response.errorBody().string();
                JSONObject jsonObject = new JSONObject(errorStr);
                if (jsonObject.has("error_type") && "401".equals(jsonObject.getString("error_type"))) {
                    Toast.makeText(requireContext(), "Access Denied", Toast.LENGTH_SHORT).show();
                    callLogoutApi();
                } else {
                    Log.e("CatchUpDetail", "Server Error Code: " + response.code() + ", Error: " + errorStr);
                }
            }
        } catch (Exception e) {
            Log.e("CatchUpDetail", "Error parsing errorBody", e);
        }
    }

    private void callLogoutApi() {
        SharedPrefManager sp = SharedPrefManager.getInstance(requireContext());
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
                    sp.clearAccessToken();
                    sendLogoutIntent();
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

    private void sendLogoutIntent() {
        try {
            Intent intent = new Intent("LOGOUT_ACTION");
            requireContext().sendBroadcast(intent);
        } catch (Exception e) {
            Log.e("CatchUpDetail", "Error sending logout intent", e);
        }
    }

    private void updateUIWithChannelData() {
        // Check if fragment is still attached before updating UI
        if (!isAdded() || getActivity() == null) {
            Log.d(TAG, "Fragment not attached, skipping UI update");
            return;
        }

        if (channelDetails == null) return;

        tvChannelName.setText(channelDetails.getChannelName());
        setupTabs();
        setupViewPager();
        showContent(true);

        // Auto-play first episode if available
        if (channelDetails.getShowList() != null && !channelDetails.getShowList().isEmpty()) {
            CatchUpChannelDetailsResponse.ShowDate firstDate = channelDetails.getShowList().get(0);
            if (firstDate.getEpisodeList() != null && !firstDate.getEpisodeList().isEmpty()) {
                // Request audio focus before auto-playing
                int result = audioManager.requestAudioFocus(
                        audioFocusChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN
                );

                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    playEpisode(firstDate.getEpisodeList().get(0));
                }
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
        // Check if fragment is still attached
        if (!isAdded() || getActivity() == null) {
            Log.d(TAG, "Fragment not attached, skipping viewpager setup");
            return;
        }

        ViewPagerAdapter adapter = new ViewPagerAdapter((AppCompatActivity) requireActivity());
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0, false);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                selectTab(position);
            }
        });


        ViewPagerAdapter adapter2 = new ViewPagerAdapter((AppCompatActivity) requireActivity());
        viewPager1.setAdapter(adapter2);
        viewPager1.setCurrentItem(0, false);
        viewPager1.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
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
        viewPager1.setCurrentItem(tab, true);



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



        LinearLayoutManager layoutManager1 = (LinearLayoutManager) recyclerViewTabs1.getLayoutManager();
        if (layoutManager1 != null) {
            // Center the selected tab
            View tabView = layoutManager1.findViewByPosition(tab);
            if (tabView != null) {
                int parentWidth = recyclerViewTabs1.getWidth();
                int childWidth = tabView.getWidth();
                int offset = (parentWidth / 2) - (childWidth / 2);
                layoutManager1.scrollToPositionWithOffset(tab, offset);
            } else {
                // fallback
                recyclerViewTabs1.scrollToPosition(tab);
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
            // Request audio focus before playing
            int result = audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
            );

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                MediaItem mediaItem = MediaItem.fromUri(episode.getShowURL());
                player.setMediaItem(mediaItem);
                player.prepare();
                player.play();

                // Update control states
                exoPlay.setVisibility(View.INVISIBLE);
                exoPause.setVisibility(View.VISIBLE);
                shouldResumeOnAudioFocusGain = false;
            } else {
                Toast.makeText(requireContext(), "Cannot play video: Audio focus denied", Toast.LENGTH_SHORT).show();
            }
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
    public void toggleFullscreen() {
        if (isInPiPMode) return;

        View playerContainer = getView().findViewById(R.id.playerContainer);
        RecyclerView recyclerView = getView().findViewById(R.id.channelRecyclerView);

        boolean isTablet = isTablet();

        if (isFullscreen) {
            // Exit fullscreen logic
            boolean isCurrentlyLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            if (isTablet && isCurrentlyLandscape) {
                // For tablets in landscape: Don't allow exiting fullscreen
                // Show appropriate message
                Toast.makeText(getContext(), "On tablets, rotate to portrait to exit fullscreen", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isTablet && isCurrentlyLandscape) {
                // For mobile devices: Switch to portrait to exit to half-screen
                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }

            // Common exit fullscreen code for all devices
            requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);

            if (playerContainer != null) {
                android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
                playerContainer.setLayoutParams(params);
                playerContainer.requestLayout();
            }

            currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
            playerView.setResizeMode(currentResizeMode);
            showSystemUI();
            isFullscreen = false;

            ImageView fullscreenButton = getView().findViewById(R.id.btnFullscreen);
            fullscreenButton.setVisibility(VISIBLE);

            // Hide drawer button when exiting fullscreen for ALL devices
            ImageView btnOpenDrawer = getView().findViewById(R.id.btnOpenDrawer);
            if (btnOpenDrawer != null) {
                btnOpenDrawer.setVisibility(GONE);
            }

        } else {
            // Enter fullscreen logic
            requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            if (recyclerView != null) recyclerView.setVisibility(GONE);

            if (playerContainer != null) {
                android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT
                );
                playerContainer.setLayoutParams(params);
                playerContainer.requestLayout();
            }

            playerView.setResizeMode(currentResizeMode);
            hideSystemUI();
            isFullscreen = true;

            ImageView fullscreenButton = getView().findViewById(R.id.btnFullscreen);
            fullscreenButton.setVisibility(GONE);

            // Only show drawer button for tablets in fullscreen mode
            ImageView btnOpenDrawer = getView().findViewById(R.id.btnOpenDrawer);
            if (btnOpenDrawer != null) {
                if (isTablet) {
                    btnOpenDrawer.setVisibility(VISIBLE);
                } else {
                    btnOpenDrawer.setVisibility(GONE);
                }
            }

            // Only open drawer automatically for tablets
            if (isTablet) {
                openDrawer();
            }
        }
    }


    private void hideSystemUI() {
        View decorView = requireActivity().getWindow().getDecorView();
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
        View decorView = requireActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void setScalingMode(int newMode) {
        if (currentResizeMode != newMode) {
            currentResizeMode = newMode;
            playerView.setResizeMode(newMode);
        }
    }

    private void animateButton(View button, boolean forward) {
        if (button == null) return;

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
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
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

    // PiP Methods
    public void onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                isVideoPlaying() &&
                !isInPiPMode) {
            enterPipMode();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        isInPiPMode = isInPictureInPictureMode;
        updateUIForPiP(isInPictureInPictureMode);

        if (isInPictureInPictureMode) {
            // Entering PiP - ensure we have audio focus
            int result = audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
            );

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED && player != null && !player.isPlaying()) {
                player.play();
            }
        } else {
            // Exiting PiP
            if (player != null && player.isPlaying()) {
                player.pause();

                if (exoPlay != null && exoPause != null) {
                    exoPlay.setVisibility(View.VISIBLE);
                    exoPause.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        onPictureInPictureModeChanged(isInPictureInPictureMode, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void enterPipMode() {
        if (!requireContext().getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            return;
        }

        try {
            Rational aspectRatio = calculateAspectRatio();
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();

            requireActivity().enterPictureInPictureMode(params);
            isInPiPMode = true;
            updateUIForPiP(true);

            // Continue playing in PiP mode
            if (player != null && !player.isPlaying()) {
                player.play();
            }
        } catch (Exception e) {
            Log.e("CatchUpPiP", "Error entering PiP mode", e);
            isInPiPMode = false;
        }
    }

    private Rational calculateAspectRatio() {
        if (player != null && player.getVideoSize() != null) {
            int videoWidth = player.getVideoSize().width;
            int videoHeight = player.getVideoSize().height;
            if (videoWidth > 0 && videoHeight > 0) {
                return new Rational(videoWidth, videoHeight);
            }
        }
        return new Rational(16, 9);
    }

    private void updateUIForPiP(boolean inPiP) {
        if (getView() == null) return;

        if (inPiP) {
            // Hide UI elements for PiP
            if (recyclerViewTabs != null) recyclerViewTabs.setVisibility(View.GONE);
            if (viewPager != null) viewPager.setVisibility(View.GONE);
            if (backbtn != null) backbtn.setVisibility(View.GONE);
            if (tvChannelName != null) tvChannelName.setVisibility(View.GONE);
            if (channelName != null) channelName.setVisibility(View.GONE);
            if (playerView != null) {
                playerView.hideController();
                playerView.setUseController(false);
            }

            // Hide player controls
            View playerControls = getView().findViewById(R.id.playerControls);
            if (playerControls != null) playerControls.setVisibility(View.GONE);

        } else {
            // Restore UI elements when exiting PiP
            if (recyclerViewTabs != null) recyclerViewTabs.setVisibility(View.VISIBLE);
            if (viewPager != null) viewPager.setVisibility(View.VISIBLE);
            if (backbtn != null) backbtn.setVisibility(View.VISIBLE);
            if (tvChannelName != null) tvChannelName.setVisibility(View.VISIBLE);
            if (channelName != null) channelName.setVisibility(View.VISIBLE);
            if (playerView != null) {
                playerView.showController();
                playerView.setUseController(true);
            }

            // Restore player controls
            View playerControls = getView().findViewById(R.id.playerControls);
            if (playerControls != null) playerControls.setVisibility(View.VISIBLE);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void exitPiPMode() {
        if (isInPiPMode) {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(1, 1))
                    .build();
            requireActivity().setPictureInPictureParams(params);

            isInPiPMode = false;
            updateUIForPiP(false);
        }
    }

    private void sendNavigationBarVisibility(boolean showBars) {
        try {
            Intent intent = new Intent("NAVIGATION_BARS_VISIBILITY");
            intent.putExtra("show_bars", showBars);
            requireContext().sendBroadcast(intent);
        } catch (Exception e) {
            Log.e("CatchUpDetail", "Error sending navigation bar visibility", e);
        }
    }

    private void handleBackNavigation() {
        // First check if we're in PiP mode
        if (isInPiPMode) {
            // Handle PiP exit
            return;
        }

        // Then check if we're in fullscreen mode
        if (isFullscreen) {
            boolean isTablet = isTablet();
            boolean isCurrentlyLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            if (isTablet && isCurrentlyLandscape) {
                // For tablets in landscape fullscreen, show message
                Toast.makeText(getContext(), "Rotate to portrait to exit fullscreen", Toast.LENGTH_SHORT).show();
                return;
            }

            // Exit fullscreen for other cases
            toggleFullscreen();
            return;
        }

        // If neither, go back to previous fragment
        goBackToPreviousFragment();
    }


    private boolean isTablet() {
        if (getContext() == null) return false;

        // Check smallest screen width (most reliable method)
        int smallestScreenWidthDp = getResources().getConfiguration().smallestScreenWidthDp;

        // A tablet typically has smallestScreenWidthDp >= 600
        return smallestScreenWidthDp >= 600;
    }


    private void goBackToPreviousFragment() {
        // Reset orientation before going back
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        // Notify that video player is finishing
        if (catchUpPlayerListener != null) {
            catchUpPlayerListener.onCatchUpPlayerFinished();
        } else {
            sendNavigationBarVisibility(true);
        }

        // Remove this fragment from back stack
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    public boolean handleBackPress() {
        if (isInPiPMode) {
            // Handle PiP exit if needed
            return true; // Consume the back press
        }

        if (isFullscreen) {
            boolean isTablet = isTablet();
            boolean isCurrentlyLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            if (isTablet && isCurrentlyLandscape) {
                // For tablets in landscape fullscreen, don't consume back press
                // Let activity handle it (go back to previous fragment)
                return false;
            } else if (!isTablet && isCurrentlyLandscape) {
                // For mobile devices in landscape fullscreen, exit to half-screen
                toggleFullscreen();
                return true; // Consume the back press
            } else {
                // For portrait fullscreen (both devices), exit fullscreen
                toggleFullscreen();
                return true; // Consume the back press
            }
        }

        return false; // Don't consume the back press
    }


    public boolean isInFullscreen() {
        return isFullscreen;
    }

    public boolean isVideoPlaying() {
        return player != null && player.isPlaying();
    }

    public boolean isInPiPMode() {
        return isInPiPMode;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        // Abandon audio focus
        if (audioManager != null) {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        }

        if (catchUpPlayerListener != null) {
            catchUpPlayerListener.onCatchUpPlayerFinished();
        } else {
            sendNavigationBarVisibility(true);
        }

        if (player != null) {
            player.release();
            player = null;
        }

        if (progressHandler != null) {
            progressHandler.removeCallbacks(progressRunnable);
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

        @NonNull
        @Override
        public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tab, parent, false);
            return new TabViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
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

        public ViewPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (channelDetails != null && channelDetails.getShowList() != null &&
                    position < channelDetails.getShowList().size()) {
                CatchUpChannelDetailsResponse.ShowDate showDate = channelDetails.getShowList().get(position);
                EpisodeListFragment fragment = EpisodeListFragment.newInstance(
                        showDate.getEpisodeList(),
                        channelDetails.getChannelName(),
                        channelDetails.getChannelLogo(),
                        channelDetails.getChannelDescription(),
                        showDate.getDate()
                );

                // Set both parent fragment and click listener
                fragment.setParentFragment(CatchUpDetailFragment.this);
                fragment.setEpisodeClickListener(CatchUpDetailFragment.this);

                return fragment;
            }
            return EpisodeListFragment.newInstance(new ArrayList<>(), "", "", "", "");
        }

        @Override
        public int getItemCount() {
            return channelDetails != null && channelDetails.getShowList() != null ?
                    channelDetails.getShowList().size() : 0;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Resume video playback only if not in PiP mode
        if (player != null && !isInPiPMode) {
            if (!player.isPlaying()) {
                player.play();
            }

            // Update UI state
            if (exoPlay != null && exoPause != null) {
                exoPlay.setVisibility(View.INVISIBLE);
                exoPause.setVisibility(View.VISIBLE);
            }
        }

        // Restore normal UI if we were in PiP mode
        if (isInPiPMode) {
            updateUIForPiP(false);
            isInPiPMode = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Only pause if not entering PiP mode
        if (player != null && player.isPlaying() && !isInPiPMode) {
            player.pause();

            // Update UI
            if (exoPlay != null && exoPause != null) {
                exoPlay.setVisibility(View.VISIBLE);
                exoPause.setVisibility(View.INVISIBLE);
            }
        }


    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Update PiP actions when configuration changes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPiPMode) {
            return; // Don't handle orientation changes in PiP mode
        }

        // Handle orientation change
        boolean isTablet = isTablet();

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (!isFullscreen && !isInPiPMode) {
                // Enter fullscreen when rotated to landscape for both devices
                enterFullscreenOnRotation();
                ImageView fullscreenButton = getView().findViewById(R.id.btnFullscreen);
                fullscreenButton.setVisibility(GONE);

                // Only show drawer button for tablets
                ImageView btnOpenDrawer = getView().findViewById(R.id.btnOpenDrawer);
                if (btnOpenDrawer != null) {
                    if (isTablet) {
                        btnOpenDrawer.setVisibility(VISIBLE);
                    } else {
                        btnOpenDrawer.setVisibility(GONE);
                    }
                }

                // Only open drawer automatically for tablets
                if (isTablet) {
                    openDrawer();
                }
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (isFullscreen && !isInPiPMode) {
                if (isTablet) {
                    // For tablets, exit fullscreen when rotating to portrait
                    toggleFullscreen();
                } else {
                    // For mobile, check if we should exit fullscreen
                    toggleFullscreen();
                }
            }
        }
    }

    private void enterFullscreenOnRotation() {
        if (isInPiPMode) return;

        View playerContainer = getView().findViewById(R.id.playerContainer);
        RecyclerView recyclerView = getView().findViewById(R.id.channelRecyclerView);

        requireActivity().getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (recyclerView != null) recyclerView.setVisibility(View.GONE);

        if (playerContainer != null) {
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            );
            playerContainer.setLayoutParams(params);
            playerContainer.requestLayout();
        }

        playerView.setResizeMode(currentResizeMode);
        hideSystemUI();
        isFullscreen = true;

        // Update fullscreen button icon if needed
        //  updateFullscreenButtonIcon();
    }

    private void exitFullscreenOnRotation() {
        if (isInPiPMode) return;

        View playerContainer = getView().findViewById(R.id.playerContainer);
        RecyclerView recyclerView = getView().findViewById(R.id.channelRecyclerView);

        requireActivity().getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);

        if (playerContainer != null) {
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            playerContainer.setLayoutParams(params);
            playerContainer.requestLayout();
        }

        currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
        playerView.setResizeMode(currentResizeMode);
        showSystemUI();
        isFullscreen = false;

        // Update fullscreen button icon if needed
        // updateFullscreenButtonIcon();
    }

    @Override
    public void onStop() {
        super.onStop();

        // Enter PiP mode when going to background in fullscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                isVideoPlaying() &&
                !isInPiPMode &&
                isFullscreen) {
            enterPipMode();
        }
    }

}