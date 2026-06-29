package com.Saalai.SalaiMusicApp.Fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.PictureInPictureParams;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.LruCache;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
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
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.Saalai.SalaiMusicApp.Adapters.RelatedMoviesAdapter;
import com.Saalai.SalaiMusicApp.Adapters.RelatedMoviesAdapter2;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.MovieRelatedChannel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.RelatedMoviesShimmerAdapter;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import wseemann.media.FFmpegMediaMetadataRetriever;

public class MovieVideoPlayerFragment extends Fragment {

    private PlayerView playerView;
    private ExoPlayer player;
    private ProgressBar progressBar;
    private RecyclerView relatedRecyclerView,relatedRecyclerView2;
    private ScaleGestureDetector scaleGestureDetector;

    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private ImageView backbtn;

    private TextView title, subtitle, description;
    private ShimmerFrameLayout shimmerTitle, shimmerSubtitle, shimmerDescription;
    private LinearLayout ll_share1;
    private TextView morechannelstext;
    private boolean isFullscreen = false;

    private SeekBar playerSeekBar;
    private boolean isSeeking = false;

    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;

    private TextView tvCurrentTime, tvTotalTime;

    private ImageButton exoPlay, exoPause;
    private final int SEEK_INTERVAL = 10000; // 10 seconds

    private String currentMovieId;
    private int contentType = 1; // 1-Movies, 2-TvShow, 3-Popular, 4-Catchup




    private AudioManager audioManager;
    private boolean isInPiPMode = false;
    // Factory method to create new instance

    private GestureDetector gestureDetector;
    private boolean isSwipeEnabled = true;

    // Drawer related variables
    private LinearLayout rightDrawerContainer;
    private RecyclerView drawerRecyclerView;
    private ImageView btnCloseDrawer;
    private View drawerOverlay;
    private boolean isDrawerOpen = false;
    private ValueAnimator drawerAnimator;
    private static final long DRAWER_ANIMATION_DURATION = 300;
    private ImageView btnOpenDrawer;

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    private boolean shouldResumeOnAudioFocusGain = false;


    // Thumbnail preview views
    private FrameLayout thumbnailPreviewContainer;
    private ImageView thumbnailPreview;
    private TextView thumbnailTime;

    // For thumbnail extraction
    private String videoUrl;
    private long lastPreviewTime = 0;
    private Handler thumbnailHandler = new Handler();
    private Runnable hideThumbnailRunnable;


    private FFmpegMediaMetadataRetriever ffmpegRetriever;
    private boolean isFFmpegInitialized = false;

    // Add thumbnail caching for better performance
    private LruCache<Long, Bitmap> thumbnailCache;

    public static MovieVideoPlayerFragment newInstance(String movieId) {
        MovieVideoPlayerFragment fragment = new MovieVideoPlayerFragment();
        Bundle args = new Bundle();
        args.putString("MOVIE_ID", movieId);
        fragment.setArguments(args);
        return fragment;
    }

    public interface MoviePlayerListener {
        void onMoviePlayerStarted();
        void onMoviePlayerFinished();
    }

    private MoviePlayerListener moviePlayerListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MoviePlayerListener) {
            moviePlayerListener = (MoviePlayerListener) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity__movie_video_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initFFmpegRetriever();

        // Initialize cache (store up to 20 thumbnails)
        thumbnailCache = new LruCache<Long, Bitmap>(20) {
            @Override
            protected int sizeOf(Long key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };




        // Notify listener
        if (moviePlayerListener != null) {
            moviePlayerListener.onMoviePlayerStarted();
        } else {
            sendNavigationBarVisibility(false);
        }

        PlayerManager.pausePlayback();
        initViews(view);
        setupPlayer();
        setupListeners();
        loadMovieDetails();

        // Check if we're in landscape mode on fragment creation
        if (isLandscape() && !isFullscreen && !isInPiPMode) {
            // Small delay to ensure views are properly initialized
            new Handler().postDelayed(() -> {
                if (getView() != null && !isFullscreen && !isInPiPMode) {
                    toggleFullscreen();
                }
            }, 300);
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

        playerSeekBar = view.findViewById(R.id.playerSeekBar);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        morechannelstext = view.findViewById(R.id.more_channels_text);
        ll_share1 = view.findViewById(R.id.ll_share1);

        title = view.findViewById(R.id.movieTitle);
        subtitle = view.findViewById(R.id.movieSubtitle);
        description = view.findViewById(R.id.movieDescription);

        shimmerTitle = view.findViewById(R.id.shimmer_movieTitle);
        shimmerSubtitle = view.findViewById(R.id.shimmer_movieSubtitle);
        shimmerDescription = view.findViewById(R.id.shimmer_movieDescription);

        playerView = view.findViewById(R.id.player_view);
        progressBar = view.findViewById(R.id.progressBar);
        relatedRecyclerView = view.findViewById(R.id.channelRecyclerView);
        relatedRecyclerView2= view.findViewById(R.id.drawerRecyclerView);
        backbtn = view.findViewById(R.id.backbtn);

        exoPlay = view.findViewById(R.id.exo_play);
        exoPause = view.findViewById(R.id.exo_pause);


        btnOpenDrawer = view.findViewById(R.id.btnOpenDrawer);

        // Setup shimmer
        RelatedMoviesShimmerAdapter shimmerAdapter = new RelatedMoviesShimmerAdapter(requireContext(), 10);
        relatedRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3, GridLayoutManager.VERTICAL, false));
        relatedRecyclerView.setAdapter(shimmerAdapter);

        showShimmer();

        thumbnailPreviewContainer = view.findViewById(R.id.thumbnailPreviewContainer);
        thumbnailPreview = view.findViewById(R.id.thumbnailPreview);
        thumbnailTime = view.findViewById(R.id.thumbnailTime);

    }

    private void setupPlayer() {
        initializePlayer();

        initializeAudioFocus(); // Add this line
    }





    private void setupListeners() {
        backbtn.setOnClickListener(v -> handleBackNavigation());

        setupSwipeGestures();

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

        // Fullscreen button
        ImageView fullscreenButton = getView().findViewById(R.id.btnFullscreen);
        if (fullscreenButton != null) {
            fullscreenButton.setOnClickListener(v -> toggleFullscreen());
        }

        // Play/Pause buttons
        if (exoPlay != null && exoPause != null) {
            exoPlay.setVisibility(View.INVISIBLE);
            exoPause.setVisibility(View.VISIBLE);

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
        }

        // Seek buttons
        ImageButton btnRewind = getView().findViewById(R.id.btnRewind);
        ImageButton btnForward = getView().findViewById(R.id.btnForward);

        if (btnForward != null) {
            btnForward.setOnClickListener(v -> {
                if (player != null) {
                    long newPos = player.getCurrentPosition() + SEEK_INTERVAL;
                    if (newPos > player.getDuration()) newPos = player.getDuration();
                    player.seekTo(newPos);
                    updateSeekBar(newPos);
                    animateButton(btnForward, true);
                }
            });
        }

        if (btnRewind != null) {
            btnRewind.setOnClickListener(v -> {
                if (player != null) {
                    long newPos = player.getCurrentPosition() - SEEK_INTERVAL;
                    if (newPos < 0) newPos = 0;
                    player.seekTo(newPos);
                    updateSeekBar(newPos);
                    animateButton(btnRewind, false);
                }
            });
        }

        // Network monitoring
        setupNetworkMonitoring();

        btnCloseDrawer.setOnClickListener(v -> closeDrawer());
        // Setup swipe gesture for drawer
        setupDrawerSwipeGestures();



        if (btnOpenDrawer != null) {
            btnOpenDrawer.setOnClickListener(v -> openDrawer());
        }
    }



    private void initFFmpegRetriever() {
        try {
            ffmpegRetriever = new FFmpegMediaMetadataRetriever();
            isFFmpegInitialized = true;
            Log.d("Thumbnail", "FFmpeg retriever initialized");
        } catch (Exception e) {
            Log.e("Thumbnail", "Failed to initialize FFmpeg retriever", e);
            isFFmpegInitialized = false;
        }
    }



    private Bitmap extractFrameWithFFmpeg(long timeMs) {
        if (!isFFmpegInitialized || videoUrl == null || videoUrl.isEmpty()) {
            return null;
        }

        try {
            // Set data source (FFmpegMediaMetadataRetriever works with HLS streams)
            ffmpegRetriever.setDataSource(videoUrl, new HashMap<String, String>());

            // Extract frame at specific time (in microseconds)
            Bitmap frame = ffmpegRetriever.getFrameAtTime(
                    timeMs * 1000, // microseconds
                    FFmpegMediaMetadataRetriever.OPTION_CLOSEST_SYNC
            );

            if (frame != null) {
                // Resize to appropriate dimensions
                int targetWidth = getThumbnailWidth();
                int targetHeight = getThumbnailHeight();

                Bitmap resized = Bitmap.createScaledBitmap(frame, targetWidth, targetHeight, true);

                // Add time overlay
                Bitmap finalBitmap = addTimeOverlay(resized, timeMs);

                Log.d("Thumbnail", "Extracted frame successfully at " + formatTime(timeMs));
                return finalBitmap;
            }

        } catch (Exception e) {
            Log.e("Thumbnail", "FFmpeg extraction error at " + timeMs + "ms: " + e.getMessage());

            // Try to reinitialize FFmpeg on failure
            try {
                if (ffmpegRetriever != null) {
                    ffmpegRetriever.release();
                }
                initFFmpegRetriever();
            } catch (Exception ex) {
                Log.e("Thumbnail", "Failed to reinitialize FFmpeg", ex);
            }
        }

        return null;
    }

    private int getThumbnailWidth() {
        if (thumbnailPreview != null) {
            int width = thumbnailPreview.getWidth();
            if (width > 0) return width;
        }
        return 140; // Default width
    }

    private int getThumbnailHeight() {
        if (thumbnailPreview != null) {
            int height = thumbnailPreview.getHeight();
            if (height > 0) return height;
        }
        return 80; // Default height
    }

    private Bitmap getCachedThumbnail(long timeMs) {
        // Round time to nearest 5 seconds for better cache hits
        long roundedTime = (timeMs / 5000) * 5000;
        return thumbnailCache.get(roundedTime);
    }

    private void cacheThumbnail(long timeMs, Bitmap bitmap) {
        if (bitmap != null) {
            long roundedTime = (timeMs / 5000) * 5000;
            thumbnailCache.put(roundedTime, bitmap);
        }
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




    private void setupNetworkMonitoring() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
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

    private void loadMovieDetails() {
        Bundle args = getArguments();
        if (args != null) {
            String movieId = args.getString("MOVIE_ID");
            if (movieId != null) {
                fetchMovieDetails(movieId);
            }
        }
    }

    private void fetchMovieDetails(String movieId) {
        this.currentMovieId = movieId;
        showShimmer();

        SharedPrefManager sp = SharedPrefManager.getInstance(requireContext());
        String accessToken = sp.getAccessToken();

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.getMovieDetails(accessToken, movieId);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                hideShimmer();

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONObject obj = new JSONObject(json);

                        String name = obj.optString("channelName");
                        String year = obj.optString("channelReleaseYear");
                        String cast = obj.optString("channelCast");
                        String director = obj.optString("channelDirector");
                        String music = obj.optString("channelMusic");
                        String desc = obj.optString("channelDescription");
                        String videoUrl = obj.optString("channelUrl"); // Local variable
                        String playedTime = obj.optString("channelPlayedTime", "00:00:00");
                        String channelDuration = obj.optString("channelDuration");
                        String channelCategory = obj.optString("channelCategory");

                        List<MovieRelatedChannel> relatedMovies = new ArrayList<>();
                        if (obj.has("channelList")) {
                            for (int i = 0; i < obj.getJSONArray("channelList").length(); i++) {
                                JSONObject relObj = obj.getJSONArray("channelList").getJSONObject(i);
                                MovieRelatedChannel ch = new MovieRelatedChannel();
                                ch.setChannelId(relObj.optString("channelId"));
                                ch.setChannelName(relObj.optString("channelName"));
                                ch.setChannelLogo(relObj.optString("channelLogo"));
                                relatedMovies.add(ch);
                            }
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                title.setText(name);
                                subtitle.setText(year + " • " + channelDuration + " • " + channelCategory);
                                description.setText("Cast: " + cast + "," + director + "," + music + "\n\n" + desc);

                                if (videoUrl != null && !videoUrl.isEmpty()) {
                                    // Store the video URL in class variable
                                    MovieVideoPlayerFragment.this.videoUrl = videoUrl;
                                    playVideo(videoUrl, playedTime);
                                }

                                setupRelatedMovies(relatedMovies);
                            });
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                hideShimmer();
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleErrorResponse(Response<ResponseBody> response) {
        try {
            if (response.errorBody() != null) {
                String errorStr = response.errorBody().string();
                JSONObject jsonObject = new JSONObject(errorStr);
                if (jsonObject.has("error_type") && "401".equals(jsonObject.getString("error_type"))) {
                    Toast.makeText(requireContext(),"Access Denied", Toast.LENGTH_SHORT).show();
                    callLogoutApi();
                } else {
                    Log.e("MoviePlayer", "Server Error Code: " + response.code() + ", Error: " + errorStr);
                }
            }
        } catch (Exception e) {
            Log.e("MoviePlayer", "Error parsing errorBody", e);
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
                    // Navigate to login - you'll need to handle this in your activity
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
            Log.e("MoviePlayer", "Error sending logout intent", e);
        }
    }

    private void setupRelatedMovies(List<MovieRelatedChannel> relatedMovies) {
        // Store context locally to avoid requiring it later
        Context context = getContext();
        if (context == null) return;

        int columnCount = 3;
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            columnCount = 6;
        }


        RelatedMoviesShimmerAdapter shimmerAdapter = new RelatedMoviesShimmerAdapter(context, 10);
        relatedRecyclerView.setLayoutManager(new GridLayoutManager(context, columnCount, GridLayoutManager.VERTICAL, false));
        relatedRecyclerView.setAdapter(shimmerAdapter);

        relatedRecyclerView.postDelayed(() -> {
            // Check if fragment is still attached
            if (!isAdded() || getContext() == null) {
                return;
            }

            RelatedMoviesAdapter adapter = new RelatedMoviesAdapter(requireContext(), relatedMovies, channelId -> {
                if (isAdded()) {
                    fetchAndDisplayMovie(channelId);
                }
            });
            relatedRecyclerView.setAdapter(adapter);
        }, 1000);



        relatedRecyclerView2.setLayoutManager(new GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false));
        relatedRecyclerView2.postDelayed(() -> {
            // Check if fragment is still attached
            if (!isAdded() || getContext() == null) {
                return;
            }

            RelatedMoviesAdapter2 adapter = new RelatedMoviesAdapter2(requireContext(), relatedMovies, channelId -> {
                if (isAdded()) {
                    fetchAndDisplayMovie(channelId);
                }
            });
            relatedRecyclerView2.setAdapter(adapter);
        }, 1000);


    }

    private void fetchAndDisplayMovie(String channelId) {
        this.currentMovieId = channelId;
        SharedPrefManager sp = SharedPrefManager.getInstance(requireContext());
        String accessToken = sp.getAccessToken();

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.getMovieDetails(accessToken, channelId);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONObject obj = new JSONObject(json);

                        String name = obj.optString("channelName");
                        String year = obj.optString("channelReleaseYear");
                        String cast = obj.optString("channelCast");
                        String director = obj.optString("channelDirector");
                        String music = obj.optString("channelMusic");
                        String desc = obj.optString("channelDescription");
                        String videoUrl = obj.optString("channelUrl");
                        String playedTime = obj.optString("channelPlayedTime", "00:00:00");
                        String channelDuration = obj.optString("channelDuration");
                        String channelCategory = obj.optString("channelCategory");

                        List<MovieRelatedChannel> relatedMovies = new ArrayList<>();
                        if (obj.has("channelList")) {
                            for (int i = 0; i < obj.getJSONArray("channelList").length(); i++) {
                                JSONObject relObj = obj.getJSONArray("channelList").getJSONObject(i);
                                MovieRelatedChannel ch = new MovieRelatedChannel();
                                ch.setChannelId(relObj.optString("channelId"));
                                ch.setChannelName(relObj.optString("channelName"));
                                ch.setChannelLogo(relObj.optString("channelLogo"));
                                relatedMovies.add(ch);
                            }
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                title.setText(name);
                                subtitle.setText(year + " • " + channelDuration + " • " + channelCategory);
                                description.setText("Cast: " + cast + "," + director + "," + music + "\n\n" + desc);

                                if (videoUrl != null && !videoUrl.isEmpty()) {
                                    // Store the video URL in class variable
                                    MovieVideoPlayerFragment.this.videoUrl = videoUrl;
                                    playVideo(videoUrl, playedTime);
                                }

                                setupRelatedMovies(relatedMovies);
                            });
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializePlayer() {
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

        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
                showThumbnailPreview();
                cancelHideThumbnail();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || player == null) return;

                long duration = player.getDuration();
                if (duration <= 0) return;

                long previewTime = (duration * progress) / 100;
                updateThumbnailPreview(previewTime, seekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeeking = false;
                hideThumbnailPreviewWithDelay();

                long duration = player.getDuration();
                long seekTo = (duration * seekBar.getProgress()) / 100;
                player.seekTo(seekTo);
            }
        });

        progressHandler = new Handler();
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
                        tvTotalTime.setText(formatTime(duration));
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
                        progressBar.setVisibility(View.VISIBLE);
                        playerView.hideController();
                        break;
                    case Player.STATE_READY:
                        progressBar.setVisibility(View.GONE);
                        updateSeekBar(player.getCurrentPosition());
                        break;
                    case Player.STATE_ENDED:
                        progressBar.setVisibility(View.GONE);
                        if (exoPlay != null) exoPlay.setVisibility(View.VISIBLE);
                        if (exoPause != null) exoPause.setVisibility(View.INVISIBLE);
                        break;
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                progressBar.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), "Playback error occurred", Toast.LENGTH_SHORT).show();

                if (isNetworkAvailable()) {
                    new Handler().postDelayed(() -> {
                        if (player != null) player.prepare();
                    }, 2000);
                }
            }
        });
    }


    // Thumbnail Preview Methods
    private void showThumbnailPreview() {
        if (thumbnailPreviewContainer != null) {
            thumbnailPreviewContainer.setVisibility(View.VISIBLE);
        }
    }

    private void hideThumbnailPreview() {
        if (thumbnailPreviewContainer != null) {
            thumbnailPreviewContainer.setVisibility(View.GONE);
        }
    }

    private void hideThumbnailPreviewWithDelay() {
        if (thumbnailHandler != null) {
            thumbnailHandler.postDelayed(() -> hideThumbnailPreview(), 500);
        }
    }

    private void cancelHideThumbnail() {
        if (thumbnailHandler != null) {
            thumbnailHandler.removeCallbacksAndMessages(null);
        }
    }

    private void updateThumbnailPreview(long timeMs, SeekBar seekBar) {
        // Better throttling - only update every 300ms
        if (Math.abs(timeMs - lastPreviewTime) < 300) return;
        lastPreviewTime = timeMs;

        // Update time text immediately
        if (thumbnailTime != null) {
            thumbnailTime.setText(formatTime(timeMs));
        }

        // Position the preview above the SeekBar thumb
        positionThumbnailPreview(seekBar);

        // Load thumbnail immediately
        loadThumbnailAsync(timeMs);
    }

    private void positionThumbnailPreview(SeekBar seekBar) {
        if (thumbnailPreviewContainer == null || seekBar == null) return;

        // Get SeekBar location on screen
        int[] seekBarLocation = new int[2];
        seekBar.getLocationOnScreen(seekBarLocation);

        // Get container location for accurate positioning
        int[] containerLocation = new int[2];
        thumbnailPreviewContainer.getLocationOnScreen(containerLocation);

        // Calculate thumb position relative to SeekBar
        float thumbPosition = (float) seekBar.getProgress() / seekBar.getMax();
        float thumbX = seekBar.getWidth() * thumbPosition;

        // Position thumbnail centered above the thumb
        float previewX = thumbX - (thumbnailPreviewContainer.getWidth() / 2f);

        // Ensure preview stays within bounds
        float minX = 0;
        float maxX = seekBar.getWidth() - thumbnailPreviewContainer.getWidth();
        previewX = Math.max(minX, Math.min(previewX, maxX));

        // Set the position
        thumbnailPreviewContainer.setTranslationX(previewX);
    }

    private void loadThumbnailAsync(long timeMs) {
        if (thumbnailPreview == null || getActivity() == null || getActivity().isFinishing()) {
            return;
        }

        // Check if we already have this thumbnail cached
        Bitmap cached = getCachedThumbnail(timeMs);
        if (cached != null) {
            displayThumbnail(cached);
            return;
        }

        // Extract thumbnail in background thread with proper threading
        Executors.newSingleThreadExecutor().execute(() -> {
            // Throttle thumbnail extraction
            if (Math.abs(timeMs - lastPreviewTime) < 200) {
                return;
            }

            Bitmap thumbnail = createColorfulThumbnail(timeMs);

            if (thumbnail != null && getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> {
                    if (thumbnailPreview != null) {
                        thumbnailPreview.setImageBitmap(thumbnail);
                        lastPreviewTime = timeMs;
                    }
                });
            }
        });
    }

    private void displayThumbnail(Bitmap bitmap) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            getActivity().runOnUiThread(() -> {
                if (thumbnailPreview != null) {
                    thumbnailPreview.setImageBitmap(bitmap);
                }
            });
        }
    }


    private Bitmap extractVideoFrame(long timeMs) {
        // Skip MediaMetadataRetriever completely - it's failing with your video URL
        // Use the alternative method which creates beautiful colored thumbnails

        return createColorfulThumbnail(timeMs);
    }

    private Bitmap createColorfulThumbnail(long timeMs) {
        // First try to extract actual video frame
        Bitmap videoFrame = extractVideoFrameUsingMediaMetadataRetriever(timeMs);

        if (videoFrame != null) {
            return videoFrame;
        }

        // If extraction fails, create a colorful fallback thumbnail
        return createFallbackThumbnail(timeMs);
    }

    private Bitmap extractVideoFrameUsingMediaMetadataRetriever(long timeMs) {
        // Only use MediaMetadataRetriever for local files, not HLS streams
        if (videoUrl == null || videoUrl.isEmpty() || videoUrl.startsWith("http")) {
            return null;
        }

        MediaMetadataRetriever retriever = null;
        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoUrl);

            Bitmap frame = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);

            if (frame != null) {
                int targetWidth = getThumbnailWidth();
                int targetHeight = getThumbnailHeight();

                // Resize frame
                if (frame.getWidth() > targetWidth || frame.getHeight() > targetHeight) {
                    Bitmap resizedFrame = Bitmap.createScaledBitmap(frame, targetWidth, targetHeight, true);
                    frame.recycle();
                    frame = resizedFrame;
                }

                return addTimeOverlay(frame, timeMs);
            }

        } catch (Exception e) {
            Log.e("Thumbnail", "MediaMetadataRetriever error: " + e.getMessage());
        } finally {
            if (retriever != null) {
                try {
                    retriever.release();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }

        return null;
    }

    private Bitmap addTimeOverlay(Bitmap original, long timeMs) {
        Bitmap overlayBitmap = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(overlayBitmap);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(Math.min(overlayBitmap.getWidth(), overlayBitmap.getHeight()) * 0.2f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setAntiAlias(true);

        // Add subtle shadow for better visibility
        paint.setShadowLayer(2f, 1f, 1f, Color.BLACK);

        String timeText = formatTime(timeMs);
        canvas.drawText(timeText,
                overlayBitmap.getWidth() / 2f,
                overlayBitmap.getHeight() / 2f,
                paint);

        // Remove shadow for border
        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.WHITE);

        // Add border
        canvas.drawRect(0, 0, overlayBitmap.getWidth(), overlayBitmap.getHeight(), paint);

        return overlayBitmap;
    }

    private Bitmap createFallbackThumbnail(long timeMs) {
        try {
            // Get thumbnail dimensions
            int width = 140;
            int height = 80;

            if (thumbnailPreview != null) {
                width = thumbnailPreview.getWidth();
                height = thumbnailPreview.getHeight();

                if (width <= 0) width = 140;
                if (height <= 0) height = 80;
            }

            // Create a gradient based on time
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            // Calculate colors based on time
            float hue = ((timeMs / 60000f) % 360f) / 360f;

            int startColor = Color.HSVToColor(new float[]{hue * 360f, 0.8f, 0.6f});
            int endColor = Color.HSVToColor(new float[]{((hue * 360f) + 30) % 360f, 0.7f, 0.8f});

            // Create gradient
            for (int x = 0; x < width; x++) {
                float xRatio = (float) x / width;

                for (int y = 0; y < height; y++) {
                    float yRatio = (float) y / height;

                    int red = interpolateColor(Color.red(startColor), Color.red(endColor), yRatio);
                    int green = interpolateColor(Color.green(startColor), Color.green(endColor), yRatio);
                    int blue = interpolateColor(Color.blue(startColor), Color.blue(endColor), yRatio);

                    // Add some pattern
                    if ((x / 20 + y / 20) % 2 == 0) {
                        red = clamp(red + 10, 0, 255);
                        green = clamp(green + 10, 0, 255);
                        blue = clamp(blue + 10, 0, 255);
                    }

                    bitmap.setPixel(x, y, Color.rgb(red, green, blue));
                }
            }

            // Add time overlay
            bitmap = addTimeOverlay(bitmap, timeMs);

            Log.d("Thumbnail", "Created fallback thumbnail for time: " + formatTime(timeMs));
            return bitmap;

        } catch (Exception e) {
            Log.e("Thumbnail", "Error creating fallback thumbnail: " + e.getMessage());
            return null;
        }
    }

    private int interpolateColor(int start, int end, float ratio) {
        return (int) (start + (end - start) * ratio);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }


    private Bitmap extractFrameAlternative(long timeMs) {
        // Alternative: Create a placeholder or use a default image
        // This ensures UI doesn't break even if thumbnail extraction fails

        try {
            // Create a simple colored bitmap as placeholder
            int width = thumbnailPreview.getWidth();
            int height = thumbnailPreview.getHeight();

            if (width <= 0 || height <= 0) {
                width = 140; // default width
                height = 80; // default height
            }

            // Create a gradient bitmap based on time position
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            // Fill with gradient color based on time
            int hue = (int) (timeMs % 360);
            int color = android.graphics.Color.HSVToColor(new float[]{hue, 0.7f, 0.5f});

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int pixelColor = android.graphics.Color.argb(
                            255,
                            (int) (android.graphics.Color.red(color) * (1.0 - (float)y / height)),
                            (int) (android.graphics.Color.green(color) * (1.0 - (float)y / height)),
                            (int) (android.graphics.Color.blue(color) * (1.0 - (float)y / height))
                    );
                    bitmap.setPixel(x, y, pixelColor);
                }
            }

            // Add time text overlay
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setColor(android.graphics.Color.WHITE);
            paint.setTextSize(12 * getResources().getDisplayMetrics().density);
            paint.setTextAlign(android.graphics.Paint.Align.CENTER);

            String timeText = formatTime(timeMs);
            canvas.drawText(timeText, width / 2f, height / 2f, paint);

            return bitmap;

        } catch (Exception e) {
            Log.e("Thumbnail", "Alternative frame creation failed: " + e.getMessage());
            return null;
        }
    }


    private void playVideo(String url, String playedTime) {
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

                long seekPosition = parseTimeToMillis(playedTime);
                if (seekPosition > 0) {
                    player.seekTo(seekPosition);
                }

                player.play();
                shouldResumeOnAudioFocusGain = false;

                player.addListener(new Player.Listener() {
                    @Override
                    public void onPlayerError(PlaybackException error) {
                        if (!isNetworkAvailable()) return;
                        new Handler().postDelayed(() -> playVideo(url, playedTime), 2000);
                    }

                    @Override
                    public void onPlaybackStateChanged(int playbackState) {
                        if (playbackState == Player.STATE_ENDED) {
                            // Release audio focus when playback ends
                            audioManager.abandonAudioFocus(audioFocusChangeListener);
                        }
                    }
                });
            } else {
                Toast.makeText(requireContext(), "Cannot play video: Audio focus denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

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

    private long parseTimeToMillis(String timeString) {
        if (timeString == null || timeString.isEmpty() || timeString.equals("00:00:00")) {
            return 0;
        }

        try {
            String[] parts = timeString.split(":");
            if (parts.length == 3) {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);

                return (hours * 3600L + minutes * 60L + seconds) * 1000L;
            }
        } catch (NumberFormatException e) {
            Log.e("TimeParse", "Error parsing time: " + timeString, e);
        }

        return 0;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetwork() != null;
    }

    private void setScalingMode(int newMode) {
        if (currentResizeMode != newMode) {
            currentResizeMode = newMode;
            playerView.setResizeMode(newMode);
        }
    }

    private void showShimmer() {
        if (shimmerTitle != null) shimmerTitle.setVisibility(View.VISIBLE);
        if (shimmerSubtitle != null) shimmerSubtitle.setVisibility(View.VISIBLE);
        if (shimmerDescription != null) shimmerDescription.setVisibility(View.VISIBLE);

        if (title != null) title.setVisibility(View.GONE);
        if (subtitle != null) subtitle.setVisibility(View.GONE);
        if (description != null) description.setVisibility(View.GONE);
    }

    private void hideShimmer() {
        if (shimmerTitle != null) shimmerTitle.setVisibility(View.GONE);
        if (shimmerSubtitle != null) shimmerSubtitle.setVisibility(View.GONE);
        if (shimmerDescription != null) shimmerDescription.setVisibility(View.GONE);

        if (title != null) title.setVisibility(View.VISIBLE);
        if (subtitle != null) subtitle.setVisibility(View.VISIBLE);
        if (description != null) description.setVisibility(View.VISIBLE);
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

    private void updateStreamTime() {
        if (player == null || currentMovieId == null) {
            return;
        }

        long currentPosition = player.getCurrentPosition();
        String formattedTime = formatTimeForApi(currentPosition);

        SharedPrefManager sp = SharedPrefManager.getInstance(requireContext());
        String accessToken = sp.getAccessToken();

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.updateStreamTime(accessToken, currentMovieId, String.valueOf(contentType), formattedTime);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("StreamTime", "Stream time updated successfully: " + formattedTime);
                } else {
                    Log.e("StreamTime", "Failed to update stream time: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            Log.e("StreamTime", "Error response: " + errorStr);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("StreamTime", "API call failed: " + t.getMessage());
            }
        });
    }

    private String formatTimeForApi(long milliseconds) {
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) ((milliseconds % (1000 * 60 * 60)) / (1000 * 60));
        int seconds = (int) ((milliseconds % (1000 * 60)) / 1000);

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }





    private void setBrightness(float brightness) {
        android.view.WindowManager.LayoutParams layoutParams = requireActivity().getWindow().getAttributes();
        layoutParams.screenBrightness = brightness;
        requireActivity().getWindow().setAttributes(layoutParams);
    }






    private final SeekBar.OnSeekBarChangeListener volumeSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {

            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    };





    // Broadcast receiver for volume changes (if needed)
    private final BroadcastReceiver volumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) {
                new Handler().postDelayed(() -> {

                }, 100);
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (thumbnailHandler != null) {
            thumbnailHandler.removeCallbacksAndMessages(null);
        }

        if (ffmpegRetriever != null) {
            try {
                ffmpegRetriever.release();
            } catch (Exception e) {
                Log.e("Thumbnail", "Error releasing FFmpeg retriever", e);
            }
        }

        // Clear cache
        if (thumbnailCache != null) {
            thumbnailCache.evictAll();
        }

        // Reset orientation when fragment is destroyed
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
        // Update stream time when fragment is destroyed
        updateStreamTime();

        if (moviePlayerListener != null) {
            moviePlayerListener.onMoviePlayerFinished();
        } else {
            sendNavigationBarVisibility(true);
        }

        // Abandon audio focus
        if (audioManager != null) {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        }

        if (player != null) {
            player.release();
            player = null;
        }

        if (progressHandler != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    private void sendNavigationBarVisibility(boolean showBars) {
        try {
            Intent intent = new Intent("NAVIGATION_BARS_VISIBILITY");
            intent.putExtra("show_bars", showBars);
            requireContext().sendBroadcast(intent);
        } catch (Exception e) {
            Log.e("MoviePlayer", "Error sending navigation bar visibility", e);
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
        if (moviePlayerListener != null) {
            moviePlayerListener.onMoviePlayerFinished();
        } else {
            sendNavigationBarVisibility(true);
        }

        // Remove this fragment from back stack
        requireActivity().getSupportFragmentManager().popBackStack();
    }


    public boolean handleBackPress() {
        if (isInPiPMode) {
            // Exit PiP mode if in PiP
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


    public void onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                isVideoPlaying() &&
                !isInPiPMode) {
            enterPipMode();
        }
    }

    // You might also want to add this method for PiP mode changes
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
            Log.e("MoviePiP", "Error entering PiP mode", e);
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
            if (relatedRecyclerView != null) relatedRecyclerView.setVisibility(View.GONE);
            if (relatedRecyclerView2 != null) relatedRecyclerView2.setVisibility(View.GONE);
            if (backbtn != null) backbtn.setVisibility(View.GONE);
            if (morechannelstext != null) morechannelstext.setVisibility(View.GONE);
            if (title != null) title.setVisibility(View.GONE);
            if (subtitle != null) subtitle.setVisibility(View.GONE);
            if (description != null) description.setVisibility(View.GONE);
            if (playerView != null) {
                playerView.hideController();
                playerView.setUseController(false);
            }


            // Hide player controls
            View playerControls = getView().findViewById(R.id.playerControls);
            if (playerControls != null) playerControls.setVisibility(View.GONE);

        } else {
            // Restore UI elements when exiting PiP
            if (relatedRecyclerView != null) relatedRecyclerView.setVisibility(View.VISIBLE);
            if (backbtn != null) backbtn.setVisibility(View.VISIBLE);
            if (morechannelstext != null) morechannelstext.setVisibility(View.VISIBLE);
            if (title != null) title.setVisibility(View.VISIBLE);
            if (subtitle != null) subtitle.setVisibility(View.VISIBLE);
            if (description != null) description.setVisibility(View.VISIBLE);
            if (playerView != null) {
                playerView.showController();
                playerView.setUseController(true);
            }

            // Restore player controls
            View playerControls = getView().findViewById(R.id.playerControls);
            if (playerControls != null) playerControls.setVisibility(View.VISIBLE);

            // Restore recyclerView2 visibility based on current state
            if (relatedRecyclerView2 != null) {
                relatedRecyclerView2.setVisibility(View.GONE); // Default to hidden
            }
        }
    }


    private void setupSwipeGestures() {
        gestureDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (!isSwipeEnabled) return false;

                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    // Horizontal swipe - ignore or handle separately
                    return false;
                }

                // Vertical swipe
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY < 0) {
                        // Swipe up
                        showRelatedRecyclerView2();
                        return true;
                    } else {
                        // Swipe down
                        hideRelatedRecyclerView2();
                        return true;
                    }
                }
                return false;
            }
        });

        // Set touch listener on the player view or root view
        View rootView = getView();
        if (rootView != null) {
            rootView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    gestureDetector.onTouchEvent(event);
                    return true;
                }
            });
        }
    }


    private void showRelatedRecyclerView2() {
        if (relatedRecyclerView2 != null && relatedRecyclerView2.getVisibility() != View.VISIBLE) {
            relatedRecyclerView2.setVisibility(View.VISIBLE);

            // Optional: Add slide-up animation
            TranslateAnimation animation = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 1.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f
            );
            animation.setDuration(300);
            relatedRecyclerView2.startAnimation(animation);

            Toast.makeText(requireContext(), "Showing related content", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideRelatedRecyclerView2() {
        if (relatedRecyclerView2 != null && relatedRecyclerView2.getVisibility() == View.VISIBLE) {
            // Optional: Add slide-down animation
            TranslateAnimation animation = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 1.0f
            );
            animation.setDuration(300);
            relatedRecyclerView2.startAnimation(animation);

            relatedRecyclerView2.setVisibility(View.GONE);
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

        // Update stream time when pausing
        updateStreamTime();
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


}