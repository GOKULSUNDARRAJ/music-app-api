package com.Saalai.SalaiMusicApp.Fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.Uri;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.Saalai.SalaiMusicApp.Adapters.TvShowEpisodeAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.TvShowEpisode;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerTvShowEpisodeAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TvShowEpisodeFragment extends Fragment {

    private static final String TAG = "TvShowEpisodeFragment";

    private String channelId;
    private int offset = 0;
    private final int count = 30;
    private boolean isLoading = false, isLastPage = false;
    private ImageView backbtn;
    private RecyclerView recyclerView;
    private TvShowEpisodeAdapter adapter;
    private TvShowEpisodeAdapter drawerAdapter;
    private final List<TvShowEpisode> episodeList = new ArrayList<>();
    private ProgressBar progressBar;

    private TextView title, subtitle, description;
    private ShimmerFrameLayout shimmerTitle, shimmerSubtitle, shimmerDescription;
    private ScaleGestureDetector scaleGestureDetector;
    private PlayerView playerView;
    private ExoPlayer player;
    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;

    private LinearLayout rightDrawerContainer;
    private RecyclerView drawerRecyclerView;
    private ImageView btnCloseDrawer;
    private View drawerOverlay;
    private boolean isDrawerOpen = false;
    private ValueAnimator drawerAnimator;
    private static final long DRAWER_ANIMATION_DURATION = 300;
    private ImageView btnOpenDrawer;


    private SeekBar playerSeekBar;
    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    private ImageButton exoPlay, exoPause;
    private ImageView fullscreenButton;
    private boolean isFullscreen = false;

    private TextView tvCurrentTime, tvTotalTime;
    private boolean isSeeking = false;
    final long SEEK_INTERVAL = 10000; // 10 seconds in milliseconds

    LinearLayout ll_share1;
    TextView morechannelstext;

    private int contentType = 2; // 1-Movies, 2-TvShow, 3-Popular, 4-Catchup

    // PiP variables
    private boolean isInPiPMode = false;

    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    private boolean shouldResumeOnAudioFocusGain = false;

    // Factory method to create new instance
    public static TvShowEpisodeFragment newInstance(String channelId) {
        TvShowEpisodeFragment fragment = new TvShowEpisodeFragment();
        Bundle args = new Bundle();
        args.putString("CHANNEL_ID", channelId);
        fragment.setArguments(args);
        return fragment;
    }

    public interface TvShowPlayerListener {
        void onTvShowPlayerStarted();
        void onTvShowPlayerFinished();
    }

    private TvShowPlayerListener tvShowPlayerListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof TvShowPlayerListener) {
            tvShowPlayerListener = (TvShowPlayerListener) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_tv_show_episode, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Notify listener
        if (tvShowPlayerListener != null) {
            tvShowPlayerListener.onTvShowPlayerStarted();
        } else {
            sendNavigationBarVisibility(false);
        }

        PlayerManager.pausePlayback();
        initViews(view);
        setupPlayer();
        setupListeners();
        loadTvShowEpisodes();


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


        backbtn = view.findViewById(R.id.backbtn);
        morechannelstext = view.findViewById(R.id.more_channels_text);
        ll_share1 = view.findViewById(R.id.ll_share1);

        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);

        // Get channelId from arguments
        Bundle args = getArguments();
        if (args != null) {
            channelId = args.getString("CHANNEL_ID");
        }

        Log.d("TvShowEpisodeFragment", "CHANNEL_ID: " + channelId);

        // UI
        recyclerView = view.findViewById(R.id.channelRecyclerView);
        drawerRecyclerView = view.findViewById(R.id.drawerRecyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        drawerRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));


        progressBar = view.findViewById(R.id.progressBar);

        title = view.findViewById(R.id.movieTitle);
        subtitle = view.findViewById(R.id.movieSubtitle);
        description = view.findViewById(R.id.movieDescription);

        shimmerTitle = view.findViewById(R.id.shimmer_movieTitle);
        shimmerSubtitle = view.findViewById(R.id.shimmer_movieSubtitle);
        shimmerDescription = view.findViewById(R.id.shimmer_movieDescription);

        // Player
        playerView = view.findViewById(R.id.player_view);
        playerSeekBar = view.findViewById(R.id.playerSeekBar);
        exoPlay = view.findViewById(R.id.exo_play);
        exoPause = view.findViewById(R.id.exo_pause);
        fullscreenButton = view.findViewById(R.id.btnFullscreen);

        // Shimmer adapter
        ShimmerTvShowEpisodeAdapter shimmerAdapter = new ShimmerTvShowEpisodeAdapter(10);
        recyclerView.setAdapter(shimmerAdapter);

        showShimmer();
    }

    private void setupPlayer() {
        initializePlayer();
        initializeAudioFocus();
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

    private void setupListeners() {
        backbtn.setOnClickListener(v -> handleBackNavigation());

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

        if (fullscreenButton != null) {
            fullscreenButton.setOnClickListener(v -> toggleFullscreen());
        }

        // Endless scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (!isLoading && !isLastPage && lm != null && lm.getItemCount() > 0) {

                    int visibleItemCount = lm.getChildCount();
                    int totalItemCount = lm.getItemCount();
                    int firstVisibleItemPosition = lm.findFirstVisibleItemPosition();

                    // Trigger when user scrolls to the last 3 items
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3
                            && firstVisibleItemPosition >= 0) {
                        fetchTvShowEpisodes();
                    }
                }
            }
        });


        drawerRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (!isLoading && !isLastPage && lm != null && lm.getItemCount() > 0) {

                    int visibleItemCount = lm.getChildCount();
                    int totalItemCount = lm.getItemCount();
                    int firstVisibleItemPosition = lm.findFirstVisibleItemPosition();

                    // Trigger when user scrolls to the last 3 items
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3
                            && firstVisibleItemPosition >= 0) {
                        fetchTvShowEpisodes();
                    }
                }
            }
        });


        // Seek buttons
        ImageButton btnRewind = getView().findViewById(R.id.btnRewind);
        ImageButton btnFastForward = getView().findViewById(R.id.btnForward);

        if (btnRewind != null) {
            btnRewind.setOnClickListener(v -> {
                if (player != null) {
                    long newPos = player.getCurrentPosition() - SEEK_INTERVAL;
                    if (newPos < 0) newPos = 0;
                    player.seekTo(newPos);
                    tvCurrentTime.setText(formatTime(newPos));
                    updateSeekBar();
                    animateButton(btnRewind, false);
                }
            });
        }

        if (btnFastForward != null) {
            btnFastForward.setOnClickListener(v -> {
                if (player != null) {
                    long newPos = player.getCurrentPosition() + SEEK_INTERVAL;
                    long duration = player.getDuration();
                    if (newPos > duration) newPos = duration;
                    player.seekTo(newPos);
                    tvCurrentTime.setText(formatTime(newPos));
                    updateSeekBar();
                    animateButton(btnFastForward, true);
                }
            });
        }

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

        // Network monitoring
        setupNetworkMonitoring();




        btnCloseDrawer.setOnClickListener(v -> closeDrawer());

        // Drawer overlay click to close
        drawerOverlay.setOnClickListener(v -> closeDrawer());

        // Setup swipe gesture for drawer
        setupDrawerSwipeGestures();



        if (btnOpenDrawer != null) {
            btnOpenDrawer.setOnClickListener(v -> openDrawer());
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
                        if (player != null && !player.isPlaying()) {
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
                            Toast.makeText(requireContext(), "Network lost. Paused video.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void loadTvShowEpisodes() {
        if (channelId != null) {
            fetchTvShowEpisodes();
        }
    }

    private void fetchTvShowEpisodes() {
        if (isLoading || isLastPage) return;

        isLoading = true;

        // Show loading indicators
        if (offset == 0) {
            showShimmer();
        } else {
            progressBar.setVisibility(View.VISIBLE);
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String token = SharedPrefManager.getInstance(requireContext()).getAccessToken();

        // If offset is 0 and we don't have episodes yet, fetch without episodeId to get latest
        String episodeIdParam = (offset == 0 && episodeList.isEmpty()) ? "" : "";

        apiService.getTvShowEpisodeList(token, channelId, episodeIdParam, String.valueOf(offset), String.valueOf(count))
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String json = response.body().string();
                                JSONObject obj = new JSONObject(json);

                                // Check if API call was successful
                                boolean status = obj.getBoolean("status");
                                String message = obj.getString("message");

                                if (status) {
                                    // Get the response object
                                    JSONObject responseObj = obj.getJSONObject("response");

                                    // Get episode details for the player (first episode when episodeId is empty)
                                    JSONObject episodeDetails = responseObj.optJSONObject("episodeDetails");

                                    if (episodeDetails != null && offset == 0) {
                                        // This is the first load, play the first episode
                                        String episodeUrl = episodeDetails.optString("episodeURL", "");
                                        String episodeName = episodeDetails.optString("episodeName", "");
                                        String episodeDate = episodeDetails.optString("episodeDate", "");

                                        if (!episodeUrl.isEmpty()) {
                                            title.setText(episodeName);
                                            subtitle.setText(episodeDate);
                                            description.setText("Latest episode");
                                            hideShimmer();

                                            // Request audio focus and play video
                                            int result = audioManager.requestAudioFocus(
                                                    audioFocusChangeListener,
                                                    AudioManager.STREAM_MUSIC,
                                                    AudioManager.AUDIOFOCUS_GAIN
                                            );

                                            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                                                playVideo(episodeUrl);
                                            } else {
                                                Toast.makeText(requireContext(), "Cannot play video: Audio focus denied", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }

                                    // Get the episode list
                                    JSONArray array = responseObj.getJSONArray("tvShowEpisodeList");

                                    if (array.length() > 0) {
                                        int startPos = episodeList.size();

                                        // Add new episodes to list
                                        for (int i = 0; i < array.length(); i++) {
                                            JSONObject item = array.getJSONObject(i);
                                            TvShowEpisode episode = new TvShowEpisode();
                                            episode.setEpisodeId(item.getInt("episodeId"));
                                            episode.setEpisodeName(item.getString("episodeName"));
                                            episode.setEpisodeLogo(item.optString("episodeLogo", ""));
                                            episode.setEpisodeDate(item.getString("episodeDate"));
                                            episode.setChannelId(item.getString("channelId"));
                                            // Note: episodeURL is not in the list items, only in episodeDetails
                                            episode.setEpisodeUrl("");
                                            episodeList.add(episode);
                                        }

                                        // Update MAIN recyclerView adapter
                                        if (recyclerView.getAdapter() instanceof ShimmerTvShowEpisodeAdapter) {
                                            // Create adapter for main RecyclerView

                                            adapter = new TvShowEpisodeAdapter(requireContext(), episodeList, episode -> {
                                                // When an episode is clicked, fetch its details to play
                                                fetchEpisodeUrlAndPlay(episode.getEpisodeId(), episode);
                                            });
                                            recyclerView.setAdapter(adapter);

                                            // Create SEPARATE adapter for drawer RecyclerView
                                            drawerAdapter = new TvShowEpisodeAdapter(requireContext(), episodeList, episode -> {
                                                // When an episode is clicked, fetch its details to play
                                                fetchEpisodeUrlAndPlay(episode.getEpisodeId(), episode);
                                            });
                                            drawerRecyclerView.setAdapter(drawerAdapter);
                                        } else {
                                            // Notify both adapters of new items
                                            adapter.notifyItemRangeInserted(startPos, array.length());
                                            drawerAdapter.notifyItemRangeInserted(startPos, array.length());
                                        }

                                        // Increment offset by 1 for next page (0→1→2→3...)
                                        offset += 1;

                                        // Check if it's the last page
                                        if (array.length() < count) {
                                            isLastPage = true;
                                        }

                                        Log.d(TAG, "Loaded " + array.length() + " items. Next offset: " + offset);

                                    } else {
                                        // No more data
                                        isLastPage = true;
                                        if (offset == 0) {
                                            // No episodes found
                                            hideShimmer();
                                            Toast.makeText(requireContext(), "No episodes found", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(requireContext(), "No more episodes", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                } else {
                                    // API returned error status
                                    hideShimmer();
                                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "API Error: " + message);
                                }

                            } catch (Exception e) {
                                Log.e(TAG, "Parsing error", e);
                                hideShimmer();
                                Toast.makeText(requireContext(), "Error parsing response", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            hideShimmer();
                            handleErrorResponse(response);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        hideShimmer();
                        Log.e(TAG, "API failed", t);
                        Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleErrorResponse(Response<ResponseBody> response) {
        try {
            if (response.errorBody() != null) {
                String errorStr = response.errorBody().string();
                JSONObject jsonObject = new JSONObject(errorStr);
                if (jsonObject.has("error_type") && "401".equals(jsonObject.getString("error_type"))) {
                    Toast.makeText(requireContext(), "Access Denied", Toast.LENGTH_SHORT).show();
                    callLogoutApi();
                } else {
                    Log.e("TvShowEpisode", "Server Error Code: " + response.code() + ", Error: " + errorStr);
                }
            }
        } catch (Exception e) {
            Log.e("TvShowEpisode", "Error parsing errorBody", e);
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
            Log.e("TvShowEpisode", "Error sending logout intent", e);
        }
    }

    private void fetchEpisodeUrlAndPlay(int episodeId, TvShowEpisode episode) {
        Log.d("TvShowEpisodeFragment", "Fetching episode: " + episodeId);

        progressBar.setVisibility(View.VISIBLE);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String token = SharedPrefManager.getInstance(requireContext()).getAccessToken();

        // Fetch with specific episodeId to get its URL
        apiService.getTvShowEpisodeList(token, channelId, String.valueOf(episodeId), "0", "1")
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String json = response.body().string();
                                JSONObject obj = new JSONObject(json);

                                if (obj.getBoolean("status")) {
                                    JSONObject responseObj = obj.getJSONObject("response");
                                    JSONObject episodeDetails = responseObj.getJSONObject("episodeDetails");

                                    String episodeUrl = episodeDetails.optString("episodeURL", "");
                                    String episodeName = episodeDetails.optString("episodeName", "");
                                    String episodeDate = episodeDetails.optString("episodeDate", "");

                                    if (!episodeUrl.isEmpty()) {
                                        // Update UI
                                        title.setText(episodeName.isEmpty() ? episode.getEpisodeName() : episodeName);
                                        subtitle.setText(episodeDate.isEmpty() ? episode.getEpisodeDate() : episodeDate);
                                        description.setText("Episode description");

                                        // Request audio focus and play video
                                        int result = audioManager.requestAudioFocus(
                                                audioFocusChangeListener,
                                                AudioManager.STREAM_MUSIC,
                                                AudioManager.AUDIOFOCUS_GAIN
                                        );

                                        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                                            playVideo(episodeUrl);
                                        } else {
                                            Toast.makeText(requireContext(), "Cannot play video: Audio focus denied", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(requireContext(), "Video URL not available", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    String message = obj.getString("message");
                                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Parsing error", e);
                                Toast.makeText(requireContext(), "Error parsing response", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            handleErrorResponse(response);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Failed to fetch video", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "API failed", t);
                    }
                });
    }

    private void initializePlayer() {
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(requireContext());
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
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    long dur = player.getDuration();
                    long newPos = (dur * progress) / 100;
                    player.seekTo(newPos);
                    tvCurrentTime.setText(formatTime(newPos));
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

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (player != null && player.getPlaybackState() == Player.STATE_READY && !isSeeking) {
                    long pos = player.getCurrentPosition();
                    long dur = player.getDuration();
                    if (dur > 0) {
                        int progress = (int) ((pos * 100) / dur);
                        playerSeekBar.setProgress(progress);

                        tvCurrentTime.setText(formatTime(pos));
                        tvTotalTime.setText(formatTime(dur));
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
                        break;
                    case Player.STATE_ENDED:
                        progressBar.setVisibility(View.GONE);
                        if (exoPlay != null) exoPlay.setVisibility(View.VISIBLE);
                        if (exoPause != null) exoPause.setVisibility(View.INVISIBLE);

                        // Release audio focus when playback ends
                        audioManager.abandonAudioFocus(audioFocusChangeListener);
                        break;
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                progressBar.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), "Playback error occurred", Toast.LENGTH_SHORT).show();

                // Release audio focus on error
                audioManager.abandonAudioFocus(audioFocusChangeListener);

                if (isNetworkAvailable()) {
                    new Handler().postDelayed(() -> {
                        if (player != null) player.prepare();
                    }, 2000);
                }
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetwork() != null;
    }

    private void playVideo(String url) {
        if (player != null) {
            // Request audio focus before playing
            int result = audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
            );

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                MediaItem item = MediaItem.fromUri(Uri.parse(url));
                player.setMediaItem(item);
                player.prepare();
                player.play();

                if (exoPlay != null) exoPlay.setVisibility(View.INVISIBLE);
                if (exoPause != null) exoPause.setVisibility(View.VISIBLE);
                shouldResumeOnAudioFocusGain = false;
            } else {
                Toast.makeText(requireContext(), "Cannot play video: Audio focus denied", Toast.LENGTH_SHORT).show();
            }
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

    // Rest of the methods (setScalingMode, toggleFullscreen, hideSystemUI, showSystemUI,
    // animateButton, formatTime, updateSeekBar, updateStreamTime, formatTimeForApi,
    // enterPipMode, onUserLeaveHint, onPictureInPictureMo
    // deChanged, etc.)

    private void setScalingMode(int newMode) {
        if (currentResizeMode != newMode) {
            currentResizeMode = newMode;
            playerView.setResizeMode(newMode);
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

    private void updateSeekBar() {
        if (player != null) {
            long pos = player.getCurrentPosition();
            long dur = player.getDuration();
            if (dur > 0) {
                int progress = (int) ((pos * 100) / dur);
                playerSeekBar.setProgress(progress);
            }
        }
    }

    private void updateStreamTime() {
        if (player == null || channelId == null) {
            Log.d("StreamTime", "Player or channelId is null, skipping update");
            return;
        }

        long currentPosition = player.getCurrentPosition();
        String formattedTime = formatTimeForApi(currentPosition);

        SharedPrefManager sp = SharedPrefManager.getInstance(requireContext());
        String accessToken = sp.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e("StreamTime", "Access token is null or empty");
            return;
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.updateStreamTime(accessToken, channelId, String.valueOf(contentType), formattedTime);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("StreamTime", "Stream time updated successfully: " + formattedTime + " for channel: " + channelId);
                } else {
                    Log.e("StreamTime", "Failed to update stream time: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            Log.e("StreamTime", "Error response: " + errorStr);

                            JSONObject jsonObject = new JSONObject(errorStr);
                            if (jsonObject.has("error_type") && "401".equals(jsonObject.getString("error_type"))) {
                                callLogoutApi();
                            }
                        }
                    } catch (Exception e) {
                        Log.e("StreamTime", "Error parsing error body", e);
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
            Log.e("TvShowPiP", "Error entering PiP mode", e);
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
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
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
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
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
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Reset orientation when fragment is destroyed
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        // Update stream time when fragment is destroyed
        updateStreamTime();

        // Abandon audio focus
        if (audioManager != null) {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        }

        if (tvShowPlayerListener != null) {
            tvShowPlayerListener.onTvShowPlayerFinished();
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

    private void sendNavigationBarVisibility(boolean showBars) {
        try {
            Intent intent = new Intent("NAVIGATION_BARS_VISIBILITY");
            intent.putExtra("show_bars", showBars);
            requireContext().sendBroadcast(intent);
        } catch (Exception e) {
            Log.e("TvShowEpisode", "Error sending navigation bar visibility", e);
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


    private boolean isTablet() {
        if (getContext() == null) return false;

        // Check smallest screen width (most reliable method)
        int smallestScreenWidthDp = getResources().getConfiguration().smallestScreenWidthDp;

        // A tablet typically has smallestScreenWidthDp >= 600
        return smallestScreenWidthDp >= 600;
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

    private void goBackToPreviousFragment() {
        // Reset orientation before going back
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        // Notify that video player is finishing
        if (tvShowPlayerListener != null) {
            tvShowPlayerListener.onTvShowPlayerFinished();
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

}