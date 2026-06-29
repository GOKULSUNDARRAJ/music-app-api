package com.Saalai.SalaiMusicApp.Fragments;

import android.app.PictureInPictureParams;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Rational;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Activity.SignUpActivity;
import com.Saalai.SalaiMusicApp.Adapters.ChannelAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.Channel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.LiveTvResponse;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerAdapterforlivemore;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.ContentValues.TAG;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class VideoPlayerFragment extends Fragment {

    private PlayerView playerView;
    private ExoPlayer player;
    private ProgressBar progressBar;
    private RecyclerView channelRecyclerView;
    private RecyclerView shimmerRecyclerView;
    private String currentChannelName = null; // Add this field
    private TextView txtChannelName;
    private ChannelAdapter channelAdapter;
    private List<Channel> channelList = new ArrayList<>();
    private int currentPosition = 0;
    private ProgressBar paginationProgressBar;
    private boolean isFullscreen = false;
    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private ImageView backbtn;
    private TextView morechannelstext;
    private ScaleGestureDetector scaleGestureDetector;
    private ProgressBar playbackProgressBar;
    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    private boolean isFirstLoad = true;
    private long lastPlaybackPosition = 0;
    private int offset = 0;
    private final int limit = 15;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private String currentChannelUrl = null;

    // PiP variables
    private boolean isInPiPMode = false;
    private BroadcastReceiver closeReceiver;
    private BroadcastReceiver pipActionReceiver;

    // PiP Actions
    private static final String ACTION_PIP_PLAY = "PIP_ACTION_PLAY";
    private static final String ACTION_PIP_PAUSE = "PIP_ACTION_PAUSE";
    private static final String ACTION_PIP_CLOSE = "PIP_ACTION_CLOSE";


    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    private boolean shouldResumeOnAudioFocusGain = false;

    // Factory method to create new instance
    public static VideoPlayerFragment newInstance(String channelUrl, String channelName) {
        VideoPlayerFragment fragment = new VideoPlayerFragment();
        Bundle args = new Bundle();
        args.putString("CHANNEL_URL", channelUrl);
        args.putString("CHANNEL_NAME", channelName);
        fragment.setArguments(args);
        return fragment;
    }


    public interface VideoPlayerListener {
        void onVideoPlayerStarted();
        void onVideoPlayerFinished();
    }

    private VideoPlayerListener videoPlayerListener;



    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Make it optional instead of required
        if (context instanceof VideoPlayerListener) {
            videoPlayerListener = (VideoPlayerListener) context;
        }
        // No exception thrown if not implemented
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Notify only if listener is available
        if (videoPlayerListener != null) {
            videoPlayerListener.onVideoPlayerStarted();
        } else {
            // Fallback: Use broadcast or other method
            sendNavigationBarVisibility(false);
        }


        PlayerManager.pausePlayback();
        initViews(view);
        setupPlayer();
        setupListeners();
        setupCloseReceiver();
        setupPipActionReceiver();

        // Check if we're in landscape mode on fragment creation
        if (isLandscape() && !isFullscreen && !isInPiPMode) {
            // Small delay to ensure views are properly initialized
            new Handler().postDelayed(() -> {
                if (getView() != null && !isFullscreen && !isInPiPMode) {
                    toggleFullscreen();
                }
            }, 300);
        }

        txtChannelName = view.findViewById(R.id.channel_name1);

    }

    private boolean isLandscape() {
        if (getContext() == null) return false;
        int orientation = getContext().getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private void initViews(View view) {
        playerView = view.findViewById(R.id.player_view);
        progressBar = view.findViewById(R.id.progressBar);
        channelRecyclerView = view.findViewById(R.id.channelRecyclerView);
        shimmerRecyclerView = view.findViewById(R.id.shimmerRecyclerView);
        morechannelstext = view.findViewById(R.id.more_channels_text);
        backbtn = view.findViewById(R.id.backbtn);
        playbackProgressBar = view.findViewById(R.id.playbackProgressBar);
        paginationProgressBar = view.findViewById(R.id.paginationProgressBar); // Add this line
        txtChannelName = view.findViewById(R.id.channel_name1);
        // Setup shimmer recycler view
        int columnCount = getColumnCount();
        shimmerRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));
        shimmerRecyclerView.setAdapter(new ShimmerAdapterforlivemore(getContext(), 10));
    }

    private void setupCloseReceiver() {
        closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("CLOSE_MAIN_ACTIVITY".equals(intent.getAction())) {
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("CLOSE_MAIN_ACTIVITY");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(closeReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            requireContext().registerReceiver(closeReceiver, filter);
        }
    }

    private void setupPipActionReceiver() {
        pipActionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case ACTION_PIP_PLAY:
                            if (player != null && !player.isPlaying()) {
                                player.play();
                            }
                            break;
                        case ACTION_PIP_PAUSE:
                            if (player != null && player.isPlaying()) {
                                player.pause();
                            }
                            break;
                        case ACTION_PIP_CLOSE:
                            exitPiPMode();
                            break;
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PIP_PLAY);
        filter.addAction(ACTION_PIP_PAUSE);
        filter.addAction(ACTION_PIP_CLOSE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(pipActionReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            requireContext().registerReceiver(pipActionReceiver, filter);
        }
    }

    private void setupPlayer() {
        initializePlayer();
        initializeAudioFocus();

        // Get channel data from arguments
        Bundle args = getArguments();
        if (args != null) {
            String url = args.getString("CHANNEL_URL");
            String name = args.getString("CHANNEL_NAME");

            if (url != null && !url.isEmpty()) {
                // Store the channel name
                currentChannelName = name;

                // Display the channel name - make sure txtChannelName is initialized
                if (txtChannelName != null && currentChannelName != null) {
                    txtChannelName.setText(currentChannelName);
                    txtChannelName.setVisibility(View.VISIBLE); // Make sure it's visible
                } else {
                    Log.e("VideoPlayer", "txtChannelName is null or currentChannelName is null");
                    if (txtChannelName == null) {
                        Log.e("VideoPlayer", "TextView not initialized yet");
                    }
                    if (currentChannelName == null) {
                        Log.e("VideoPlayer", "Channel name is null");
                    }
                }

                setupChannelList();
                playChannel(url);
                loadLiveTvChannels(true);
            } else {
                Toast.makeText(getContext(), "Stream URL not found!", Toast.LENGTH_SHORT).show();
                requireActivity().onBackPressed();
            }
        }
    }

    private void initializeAudioFocus() {
        audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);

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
                                    ImageButton playButton = getView().findViewById(R.id.exo_play);
                                    ImageButton pauseButton = getView().findViewById(R.id.exo_pause);
                                    if (playButton != null && pauseButton != null) {
                                        playButton.setVisibility(GONE);
                                        pauseButton.setVisibility(View.VISIBLE);
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
                                    ImageButton playButton = getView().findViewById(R.id.exo_play);
                                    ImageButton pauseButton = getView().findViewById(R.id.exo_pause);
                                    if (playButton != null && pauseButton != null) {
                                        playButton.setVisibility(View.VISIBLE);
                                        pauseButton.setVisibility(GONE);
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
                                    ImageButton playButton = getView().findViewById(R.id.exo_play);
                                    ImageButton pauseButton = getView().findViewById(R.id.exo_pause);
                                    if (playButton != null && pauseButton != null) {
                                        playButton.setVisibility(View.VISIBLE);
                                        pauseButton.setVisibility(GONE);
                                    }
                                });
                            }
                        }
                        break;
                }
            }
        };
    }

    private void setupListeners() {
        backbtn.setOnClickListener(v -> {
            if (isInPiPMode) {
                // If in PiP mode, exit PiP first
                exitPiPMode();
            } else {
                // Handle back navigation manually
                handleBackNavigation();
            }
        });

        // Scale gesture detector
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private static final float SCALE_THRESHOLD = 0.1f;
            private float scaleFactor = 1.0f;

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (!isFullscreen || isInPiPMode) return false;

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
            if (isFullscreen && !isInPiPMode) scaleGestureDetector.onTouchEvent(event);
            return false;
        });

        // Play/Pause buttons
        ImageButton playButton = getView().findViewById(R.id.exo_play);
        ImageButton pauseButton = getView().findViewById(R.id.exo_pause);

        if (playButton != null && pauseButton != null) {
            playButton.setOnClickListener(v -> {
                if (player != null) {
                    // Request audio focus before playing
                    int result = audioManager.requestAudioFocus(
                            audioFocusChangeListener,
                            AudioManager.STREAM_MUSIC,
                            AudioManager.AUDIOFOCUS_GAIN
                    );

                    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        player.play();
                        playButton.setVisibility(GONE);
                        pauseButton.setVisibility(View.VISIBLE);
                        shouldResumeOnAudioFocusGain = false;
                        updatePiPActions();
                    } else {
                        Toast.makeText(getContext(), "Cannot play: Audio focus denied", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            pauseButton.setOnClickListener(v -> {
                if (player != null) {
                    player.pause();
                    pauseButton.setVisibility(GONE);
                    playButton.setVisibility(View.VISIBLE);
                    // Don't abandon audio focus here, as user might want to resume quickly
                    updatePiPActions();
                }
            });
        }

        // Fullscreen button
        ImageView fullscreenButton = getView().findViewById(R.id.btnFullscreen);
        if (fullscreenButton != null) {
            fullscreenButton.setOnClickListener(v -> {
                if (!isInPiPMode) {
                    toggleFullscreen();
                }
            });
        }

        // Scale button
        ImageView btnScale = getView().findViewById(R.id.btnScale);
        if (btnScale != null) {
            btnScale.setOnClickListener(v -> {
                if (isFullscreen && !isInPiPMode) {
                    toggleScalingMode();
                } else {
                    Toast.makeText(getContext(), "Switch to fullscreen to change scaling", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Pagination
        channelRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadLiveTvChannels(false);
                    }
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void enterPiPMode() {
        if (!requireContext().getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            Toast.makeText(getContext(), "PiP mode not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Request audio focus before entering PiP
            int result = audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
            );

            Rational aspectRatio = calculateAspectRatio();
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();

            requireActivity().enterPictureInPictureMode(params);
            isInPiPMode = true;
            updateUIForPiP(true);

            // Continue playing in PiP mode if we have audio focus
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED && player != null && !player.isPlaying()) {
                player.play();
            }
        } catch (Exception e) {
            Log.e("PiP", "Error entering PiP mode", e);
            Toast.makeText(getContext(), "Failed to enter PiP mode", Toast.LENGTH_SHORT).show();
            isInPiPMode = false;
        }
    }


    private void exitPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPiPMode) {
            try {
                // For exiting PiP, we need to set a different aspect ratio and update
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(1, 1))
                        .build();
                requireActivity().setPictureInPictureParams(params);
            } catch (Exception e) {
                Log.e("PiP", "Error exiting PiP mode", e);
            }
        }
        isInPiPMode = false;
        updateUIForPiP(false);
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updatePiPActions() {
        if (isInPiPMode) {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(calculateAspectRatio())

                    .build();
            requireActivity().setPictureInPictureParams(params);
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
        // Default aspect ratio (16:9)
        return new Rational(16, 9);
    }

    private void updateUIForPiP(boolean inPiP) {
        if (getView() == null) return;

        // Get the channel list container
        LinearLayout channelListContainer = getView().findViewById(R.id.recyclerViewcontainer);

        if (inPiP) {
            // Hide the ENTIRE channel list container in PiP mode
            if (channelListContainer != null) {
                channelListContainer.setVisibility(GONE);
            }

            // Also hide individual elements for safety
            backbtn.setVisibility(GONE);
            morechannelstext.setVisibility(GONE);
            playerView.hideController();
            paginationProgressBar.setVisibility(GONE);
            txtChannelName.setVisibility(GONE); // Hide channel name in PiP
            // Hide other controls
            ImageView fullscreenButton = getView().findViewById(R.id.btnFullscreen);
            ImageView btnScale = getView().findViewById(R.id.btnScale);


            if (fullscreenButton != null) fullscreenButton.setVisibility(GONE);
            if (btnScale != null) btnScale.setVisibility(GONE);


            // Hide individual RecyclerViews
            channelRecyclerView.setVisibility(GONE);
            shimmerRecyclerView.setVisibility(GONE);

        } else {
            // Restore the ENTIRE channel list container when exiting PiP
            if (channelListContainer != null) {
                channelListContainer.setVisibility(View.VISIBLE);
            }

            // Restore individual elements
            backbtn.setVisibility(View.VISIBLE);
            morechannelstext.setVisibility(View.VISIBLE);
            playerView.showController();
            txtChannelName.setVisibility(View.VISIBLE);
            // Show other controls
            ImageView fullscreenButton = getView().findViewById(R.id.btnFullscreen);
            ImageView btnScale = getView().findViewById(R.id.btnScale);


            if (fullscreenButton != null) fullscreenButton.setVisibility(View.VISIBLE);
            if (btnScale != null) btnScale.setVisibility(View.VISIBLE);


            // Show pagination progress bar if loading
            if (isLoading && !isFirstLoad) {
                paginationProgressBar.setVisibility(View.VISIBLE);
            }

            // Restore individual RecyclerView visibility based on state
            if (isFullscreen) {
                // In fullscreen, hide channel list (but keep container for proper layout)
                channelRecyclerView.setVisibility(GONE);
                shimmerRecyclerView.setVisibility(GONE);
            } else {
                // In half-screen, show appropriate view
                if (isLoading && isFirstLoad) {
                    shimmerRecyclerView.setVisibility(View.VISIBLE);
                    channelRecyclerView.setVisibility(GONE);
                } else {
                    shimmerRecyclerView.setVisibility(GONE);
                    channelRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        }
    }
    // Handle configuration changes for PiP

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Update PiP actions when configuration changes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPiPMode) {
            updatePiPActions();
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
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (isFullscreen && !isInPiPMode) {
                if (isTablet) {
                    // For tablets, exit fullscreen when rotating to portrait
                    toggleFullscreen();
                } else {
                    // For mobile, check if we should exit fullscreen
                    // You might want to keep it in fullscreen for mobile too
                    // depending on your requirements
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



    // Handle PiP mode changes through activity
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

                ImageButton playButton = getView().findViewById(R.id.exo_play);
                ImageButton pauseButton = getView().findViewById(R.id.exo_pause);
                if (playButton != null && pauseButton != null) {
                    playButton.setVisibility(View.VISIBLE);
                    pauseButton.setVisibility(GONE);
                }
            }
        }
    }

    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        onPictureInPictureModeChanged(isInPictureInPictureMode, null);
    }

    // Handle user leaving the app (home button pressed)
    public void onUserLeaveHint() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                isVideoPlaying() &&
                !isInPiPMode) {
            enterPiPMode();
        }

    }

    // Rest of your existing methods remain the same...
    private void setScalingMode(int newMode) {
        if (currentResizeMode != newMode && !isInPiPMode) {
            currentResizeMode = newMode;
            playerView.setResizeMode(currentResizeMode);

            String msg = (newMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
                    ? "Zoom mode"
                    : "Fit mode";
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLiveTvChannels(boolean isFirstLoad) {
        isLoading = true;

        if (isFirstLoad) {
            shimmerRecyclerView.setVisibility(View.VISIBLE);
            channelRecyclerView.setVisibility(GONE);  // Changed from recyclerView to channelRecyclerView
            paginationProgressBar.setVisibility(GONE);
        } else {
            // Show progress bar for pagination
            paginationProgressBar.setVisibility(View.VISIBLE);
        }

        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
        String accessToken = sp.getAccessToken();

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<LiveTvResponse> call = apiService.getLiveTvList(
                accessToken,
                String.valueOf(offset),
                String.valueOf(limit)
        );

        call.enqueue(new Callback<LiveTvResponse>() {
            @Override
            public void onResponse(Call<LiveTvResponse> call, Response<LiveTvResponse> response) {
                isLoading = false;
                paginationProgressBar.setVisibility(GONE); // Hide progress bar

                if (isFirstLoad) {
                    shimmerRecyclerView.setVisibility(GONE);
                    channelRecyclerView.setVisibility(View.VISIBLE);
                }

                if (response.isSuccessful() && response.body() != null) {
                    LiveTvResponse liveTvResponse = response.body();

                    // Check if the API call was successful
                    if (liveTvResponse.isStatus() && liveTvResponse.getResponse() != null) {
                        List<Channel> apiChannels = liveTvResponse.getResponse().getChannelList();

                        if (apiChannels != null && !apiChannels.isEmpty()) {
                            // Calculate the position where new items start
                            int currentSize = channelList.size();

                            channelList.addAll(apiChannels);

                            if (isFirstLoad) {
                                channelAdapter.notifyDataSetChanged();
                            } else {
                                // Only notify for the new items for better performance
                                channelAdapter.notifyItemRangeInserted(currentSize, apiChannels.size());
                            }

                            offset++; // next page

                            if (apiChannels.size() < limit) {
                                isLastPage = true;
                                Log.d(TAG, "Last page reached");
                            }

                            // Log success
                            Log.d(TAG, "Loaded " + apiChannels.size() + " channels. Total now: " + channelList.size());

                        } else {
                            isLastPage = true;
                            if (isFirstLoad) {
                                Toast.makeText(getContext(), "No channels available", Toast.LENGTH_SHORT).show();
                            }
                            Log.d(TAG, "No more channels available");
                        }
                    } else {
                        // API returned error status
                        String errorMsg = liveTvResponse.getMessage();
                        Toast.makeText(getContext(), errorMsg != null ? errorMsg : "Failed to load channels", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "API Error: " + errorMsg);
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorStr);
                            if (jsonObject.has("error_type") && "401".equals(jsonObject.getString("error_type"))) {
                                Toast.makeText(getContext(), "Access Denied", Toast.LENGTH_SHORT).show();
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
            public void onFailure(Call<LiveTvResponse> call, Throwable t) {
                isLoading = false;
                paginationProgressBar.setVisibility(GONE); // Hide progress bar on failure

                if (isFirstLoad) {
                    shimmerRecyclerView.setVisibility(GONE);
                }
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "API call failed: " + t.getMessage());
            }
        });
    }



    private void callLogoutApi() {
        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
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

                    Intent intent = new Intent(getContext(), SignUpActivity.class);
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

    private void setupChannelList() {
        int columnCount = getColumnCount();
        channelRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));
        channelAdapter = new ChannelAdapter(getContext(), channelList, channel -> {
            int pos = channelList.indexOf(channel);
            if (pos != -1) {
                channelAdapter.setSelectedPosition(pos);
                currentPosition = pos;

                // Update current channel name
                currentChannelName = channel.getChannelName(); // Assuming Channel model has getChannelName()

                // Update the TextView with new channel name
                if (txtChannelName != null) {
                    txtChannelName.setText(currentChannelName);
                }

                // Request audio focus before switching channels
                int result = audioManager.requestAudioFocus(
                        audioFocusChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN
                );

                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    playChannel(channel.getChannelURL());
                } else {
                    Toast.makeText(getContext(), "Cannot switch channel: Audio focus denied", Toast.LENGTH_SHORT).show();
                }
            }
        });
        channelAdapter.setSelectedPosition(currentPosition);
        channelRecyclerView.setAdapter(channelAdapter);
    }

    private void initializePlayer() {
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(getContext());
        trackSelector.setParameters(
                trackSelector.buildUponParameters()
                        .setMaxVideoBitrate(Integer.MAX_VALUE)
        );

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(8000, 30000, 1500, 3000)
                .build();

        player = new ExoPlayer.Builder(requireContext())
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .build();

        playerView.setPlayer(player);

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (player != null && player.getPlaybackState() == Player.STATE_READY) {
                    long currentPos = player.getCurrentPosition();
                    long duration = player.getDuration();
                    if (duration > 0) {
                        int progress = (int) ((currentPos * 100) / duration);
                        playbackProgressBar.setProgress(progress);
                    }
                }
                progressHandler.postDelayed(this, 500);
            }
        };
        progressHandler.post(progressRunnable);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                switch (playbackState) {
                    case Player.STATE_BUFFERING:
                        progressBar.setVisibility(VISIBLE);
                        if (!isInPiPMode) {
                            playerView.hideController();
                        }
                        break;
                    case Player.STATE_READY:
                        progressBar.setVisibility(INVISIBLE);
                        break;
                    case Player.STATE_ENDED:
                        // Release audio focus when playback ends
                        if (audioManager != null) {
                            audioManager.abandonAudioFocus(audioFocusChangeListener);
                        }
                        break;
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    retryCurrentChannel();
                    return;
                }
                Log.e("ExoPlayerError", "PlaybackException occurred", error);
                Toast.makeText(getContext(), "Playback error occurred", Toast.LENGTH_SHORT).show();

                // Release audio focus on error
                if (audioManager != null) {
                    audioManager.abandonAudioFocus(audioFocusChangeListener);
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    updatePiPActions();
                }
            }
        });
    }

    private void retryCurrentChannel() {
        if (player == null || currentChannelUrl == null) {
            Toast.makeText(getContext(), "No current channel to retry", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        Log.d("VideoPlayerRetry", "Retrying current channel: " + currentChannelUrl);

        // Request audio focus before retrying
        int result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        );

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Toast.makeText(getContext(), "Cannot retry: Audio focus denied", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(GONE);
            return;
        }

        lastPlaybackPosition = player.getCurrentPosition();
        player.stop();
        player.clearMediaItems();
        player.setSeekParameters(SeekParameters.CLOSEST_SYNC);

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(currentChannelUrl));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.seekTo(lastPlaybackPosition);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    progressBar.setVisibility(GONE);
                    Log.d("VideoPlayerRetry", "Playback resumed successfully");
                    player.removeListener(this);
                } else if (playbackState == Player.STATE_BUFFERING) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e("VideoPlayerRetry", "Retry failed: " + error.getMessage());
                progressBar.setVisibility(GONE);
                Toast.makeText(getContext(), "Retry failed. Please try again.", Toast.LENGTH_SHORT).show();
                player.removeListener(this);

                // Release audio focus on error
                audioManager.abandonAudioFocus(audioFocusChangeListener);
            }
        });

        player.play();
    }

    private void playChannel(String url) {
        Log.d("VideoPlayer", "Playing URL: " + url);
        currentChannelUrl = url;

        if (player != null) {
            // Request audio focus before playing
            int result = audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
            );

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
                player.setMediaItem(mediaItem);
                player.prepare();
                player.play();
                shouldResumeOnAudioFocusGain = false;
            } else {
                Toast.makeText(getContext(), "Cannot play channel: Audio focus denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //start

    @OptIn(markerClass = UnstableApi.class)
    public void toggleFullscreen() {
        if (isInPiPMode) return;

        View playerContainer = getView().findViewById(R.id.playerContainer);
        RecyclerView recyclerView = getView().findViewById(R.id.channelRecyclerView);

        boolean isTablet = isTablet();

        if (isFullscreen) {
            // Exit fullscreen logic
            if (isTablet) {
                // For tablets: Don't allow exiting fullscreen from landscape
                // Show appropriate message
                boolean isCurrentlyLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

                if (isCurrentlyLandscape) {
                    Toast.makeText(getContext(), "On tablets, rotate to portrait to exit fullscreen", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                // For mobile: Check if we're in landscape
                boolean isCurrentlyLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

                if (isCurrentlyLandscape) {
                    // Mobile in landscape - allow exiting to half-screen
                    requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }

            // Common exit fullscreen code
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

        } else {
            // Enter fullscreen logic - same for all devices
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
        }
    }

    private boolean isTablet() {
        if (getContext() == null) return false;

        // Method 1: Check screen size
        int screenSize = getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK;

        // Method 2: Check smallest screen width (more reliable)
        int smallestScreenWidthDp = getResources().getConfiguration().smallestScreenWidthDp;

        // A tablet typically has smallestScreenWidthDp >= 600
        return smallestScreenWidthDp >= 600;
    }


    private void resetOrientation() {
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void hideSystemUI() {
        View decorView = requireActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    private void showSystemUI() {
        View decorView = requireActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void toggleScalingMode() {
        if (!isFullscreen || isInPiPMode) {
            Toast.makeText(getContext(), "Switch to fullscreen to change scaling", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
            currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
        } else {
            currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
        }

        playerView.setResizeMode(currentResizeMode);
        String msg = (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) ? "Zoom mode" : "Fit mode";
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private int getColumnCount() {
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            return 3;
        }
        return 2;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Reset orientation when fragment is destroyed
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        // Abandon audio focus
        if (audioManager != null) {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        }

        if (videoPlayerListener != null) {
            videoPlayerListener.onVideoPlayerFinished();
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
        if (closeReceiver != null) {
            requireContext().unregisterReceiver(closeReceiver);
        }
        if (pipActionReceiver != null) {
            requireContext().unregisterReceiver(pipActionReceiver);
        }
    }

    public boolean handleBackPress() {
        if (isInPiPMode) {
            exitPiPMode();
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



    // Add this method to check if video is playing
    public boolean isVideoPlaying() {
        return player != null && player.isPlaying();
    }

    // Add this method to check PiP mode
    public boolean isInPiPMode() {
        return isInPiPMode;
    }

    private void sendNavigationBarVisibility(boolean showBars) {
        try {
            Intent intent = new Intent("NAVIGATION_BARS_VISIBILITY");
            intent.putExtra("show_bars", showBars);
            requireContext().sendBroadcast(intent);
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error sending navigation bar visibility", e);
        }
    }

    private void handleBackNavigation() {
        // First check if we're in PiP mode
        if (isInPiPMode) {
            exitPiPMode();
            return;
        }

        // Then check if we're in fullscreen mode
        if (isFullscreen) {
            // Exit fullscreen first (will switch to portrait)
            toggleFullscreen();
            return;
        }

        // If neither, go back to previous fragment
        goBackToPreviousFragment();
    }

    private void goBackToPreviousFragment() {
        // Reset orientation before going back
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        // Notify that video player is finishing
        if (videoPlayerListener != null) {
            videoPlayerListener.onVideoPlayerFinished();
        } else {
            sendNavigationBarVisibility(true);
        }

        // Remove this fragment from back stack
        requireActivity().getSupportFragmentManager().popBackStack();
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
            ImageButton playButton = getView().findViewById(R.id.exo_play);
            ImageButton pauseButton = getView().findViewById(R.id.exo_pause);
            if (playButton != null && pauseButton != null) {
                playButton.setVisibility(GONE);
                pauseButton.setVisibility(View.VISIBLE);
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
            ImageButton playButton = getView().findViewById(R.id.exo_play);
            ImageButton pauseButton = getView().findViewById(R.id.exo_pause);
            if (playButton != null && pauseButton != null) {
                playButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(GONE);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // Enter PiP mode when going to background in fullscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                isVideoPlaying() &&
                !isInPiPMode &&
                isFullscreen) {
            enterPiPMode();
        }
    }




}