package com.Saalai.SalaiMusicApp.Activity;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Rational;
import android.view.ScaleGestureDetector;
import android.view.View;
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
import androidx.media3.common.util.UnstableApi;
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

public class TvShowEpisodeActivity extends AppCompatActivity {

    private static final String TAG = "TvShowEpisodeActivity";

    private String channelId;
    private int offset = 0;
    private final int count = 30;
    private boolean isLoading = false, isLastPage = false;
    private ImageView backbtn;
    private RecyclerView recyclerView;
    private TvShowEpisodeAdapter adapter;
    private final List<TvShowEpisode> episodeList = new ArrayList<>();
    private ProgressBar progressBar;

    private TextView title, subtitle, description;
    private ShimmerFrameLayout shimmerTitle, shimmerSubtitle, shimmerDescription;
    private ScaleGestureDetector scaleGestureDetector;
    private PlayerView playerView;
    private ExoPlayer player;
    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tv_show_episode);

        PlayerManager.pausePlayback();

        View rootView = findViewById(R.id.main);

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.black)); // replace with your color
        }

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


        backbtn = findViewById(R.id.backbtn);
        backbtn.setOnClickListener(v -> onBackPressed());


        morechannelstext = findViewById(R.id.more_channels_text);
        ll_share1 = findViewById(R.id.ll_share1);

        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);


        channelId = getIntent().getStringExtra("CHANNEL_ID");

        Log.d("TvShowEpisodeActivity", "CHANNEL_ID: " + channelId);
        // UI
        recyclerView = findViewById(R.id.channelRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        progressBar = findViewById(R.id.progressBar);

        title = findViewById(R.id.movieTitle);
        subtitle = findViewById(R.id.movieSubtitle);
        description = findViewById(R.id.movieDescription);

        shimmerTitle = findViewById(R.id.shimmer_movieTitle);
        shimmerSubtitle = findViewById(R.id.shimmer_movieSubtitle);
        shimmerDescription = findViewById(R.id.shimmer_movieDescription);

        // Shimmer adapter
        ShimmerTvShowEpisodeAdapter shimmerAdapter = new ShimmerTvShowEpisodeAdapter(10);
        recyclerView.setAdapter(shimmerAdapter);

        // Player
        playerView = findViewById(R.id.player_view);
        playerSeekBar = findViewById(R.id.playerSeekBar);
        exoPlay = findViewById(R.id.exo_play);
        exoPause = findViewById(R.id.exo_pause);
        fullscreenButton = findViewById(R.id.btnFullscreen); // add in layout

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

        fullscreenButton.setOnClickListener(v -> toggleFullscreen());

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

        showShimmer();
        if (channelId != null) fetchTvShowEpisodes();


        initializePlayer();

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (player != null && player.getPlaybackState() == Player.STATE_READY && !isSeeking) {
                    long pos = player.getCurrentPosition();
                    long dur = player.getDuration();
                    if (dur > 0) {
                        int progress = (int) ((pos * 100) / dur);
                        playerSeekBar.setProgress(progress);

                        // Update time TextViews
                        tvCurrentTime.setText(formatTime(pos));
                        tvTotalTime.setText(formatTime(dur));
                    }
                }
                progressHandler.postDelayed(this, 500);
            }
        };
        progressHandler.post(progressRunnable);


        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    long dur = player.getDuration();
                    long newPos = (dur * progress) / 100;
                    player.seekTo(newPos);
                    tvCurrentTime.setText(formatTime(newPos)); // update current time instantly
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


        playerView.setOnTouchListener((v, event) -> {
            // Only process scale gestures in fullscreen mode
            if (isFullscreen) {
                scaleGestureDetector.onTouchEvent(event);
            }
            return false;
        });


        ImageButton btnRewind = findViewById(R.id.btnRewind);
        ImageButton btnFastForward = findViewById(R.id.btnForward);


        btnRewind.setOnClickListener(v -> {
            if (player != null) {
                long newPos = player.getCurrentPosition() - SEEK_INTERVAL;
                if (newPos < 0) newPos = 0;
                player.seekTo(newPos);
                tvCurrentTime.setText(formatTime(newPos));
                updateSeekBar(); // optional to refresh SeekBar immediately
                animateButton(btnRewind, true); // forward animation
            }
        });

        btnFastForward.setOnClickListener(v -> {
            if (player != null) {
                long newPos = player.getCurrentPosition() + SEEK_INTERVAL;
                long duration = player.getDuration();
                if (newPos > duration) newPos = duration;
                player.seekTo(newPos);
                tvCurrentTime.setText(formatTime(newPos));
                updateSeekBar(); // optional to refresh SeekBar immediately

                animateButton(btnFastForward, true); // forward animation
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private static final float SCALE_THRESHOLD = 0.1f;
            private float scaleFactor = 1.0f;

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                // Only allow zooming in fullscreen mode
                if (!isFullscreen) {
                    return false; // Ignore zoom gestures in non-fullscreen mode
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


        ImageView backbtn = findViewById(R.id.backbtn);

        backbtn.setOnClickListener(v -> onBackPressed());
    }


    private void setScalingMode(int newMode) {
        if (currentResizeMode != newMode) {
            currentResizeMode = newMode;
            playerView.setResizeMode(newMode);

        }
    }


    @OptIn(markerClass = UnstableApi.class)
    private void toggleFullscreen() {
        AspectRatioFrameLayout playerContainer = findViewById(R.id.playerContainer);
        RecyclerView recyclerView = findViewById(R.id.channelRecyclerView);

        if (isFullscreen) {
            // Exit fullscreen → lock to portrait
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            recyclerView.setVisibility(View.VISIBLE);

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

            recyclerView.setVisibility(View.GONE);

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
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
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
        String token = SharedPrefManager.getInstance(this).getAccessToken();

        // API call with current offset (0,1,2,3...) and count=10
        apiService.getTvShowEpisodeList(token, channelId, "", String.valueOf(offset), String.valueOf(count))
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String json = response.body().string();
                                JSONObject obj = new JSONObject(json);
                                String topLevelEpisodeUrl = obj.optString("episodeURL");
                                JSONArray array = obj.getJSONArray("tvShowEpisodeList");

                                if (array.length() > 0) {
                                    int startPos = episodeList.size();

                                    // Add new episodes to list
                                    for (int i = 0; i < array.length(); i++) {
                                        JSONObject item = array.getJSONObject(i);
                                        TvShowEpisode episode = new TvShowEpisode();
                                        episode.setEpisodeId(item.getInt("episodeId"));
                                        episode.setEpisodeName(item.getString("episodeName"));
                                        episode.setEpisodeLogo(item.getString("episodeLogo"));
                                        episode.setEpisodeDate(item.getString("episodeDate"));
                                        episode.setChannelId(item.getString("channelId"));
                                        episode.setEpisodeUrl(topLevelEpisodeUrl);
                                        episodeList.add(episode);
                                    }

                                    // For first load, show latest episode in player
                                    if (offset == 0 && !episodeList.isEmpty()) {
                                        TvShowEpisode latest = episodeList.get(0);
                                        title.setText(latest.getEpisodeName());
                                        subtitle.setText(latest.getEpisodeDate());
                                        description.setText("Latest episode description");
                                        hideShimmer();

                                        if (!topLevelEpisodeUrl.isEmpty()) {
                                            playVideo(topLevelEpisodeUrl);
                                        }
                                    }

                                    // Update adapter
                                    if (recyclerView.getAdapter() instanceof ShimmerTvShowEpisodeAdapter) {
                                        adapter = new TvShowEpisodeAdapter(TvShowEpisodeActivity.this, episodeList, episode -> {
                                            fetchEpisodeUrlAndPlay(episode.getEpisodeId(), episode);
                                        });
                                        recyclerView.setAdapter(adapter);
                                    } else {
                                        // Notify adapter of new items
                                        adapter.notifyItemRangeInserted(startPos, array.length());
                                    }

                                    // ✅ Increment offset by 1 for next page (0→1→2→3...)
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
                                        Toast.makeText(TvShowEpisodeActivity.this, "No episodes found", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(TvShowEpisodeActivity.this, "No more episodes", Toast.LENGTH_SHORT).show();
                                    }
                                }

                            } catch (Exception e) {
                                Log.e(TAG, "Parsing error", e);
                                hideShimmer();
                            }
                        } else {
                            hideShimmer();

                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        hideShimmer();
                        Log.e(TAG, "API failed", t);
                        Toast.makeText(TvShowEpisodeActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void callLogoutApi() {
        SharedPrefManager sp = SharedPrefManager.getInstance(TvShowEpisodeActivity.this);
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

                    Intent intent = new Intent(TvShowEpisodeActivity.this, SignUpActivity.class);
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


    private void fetchEpisodeUrlAndPlay(int episodeId, TvShowEpisode episode) {

        Log.d("TvShowEpisodeActivity", "Episode: " + episodeId);


        progressBar.setVisibility(View.VISIBLE);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String token = SharedPrefManager.getInstance(this).getAccessToken();

        apiService.getTvShowEpisodeList(token, channelId, String.valueOf(episodeId), "0", "1")
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String json = response.body().string();
                                JSONObject obj = new JSONObject(json);
                                String latestUrl = obj.optString("episodeURL");

                                if (!latestUrl.isEmpty()) {
                                    // Update UI
                                    title.setText(episode.getEpisodeName());
                                    subtitle.setText(episode.getEpisodeDate());
                                    description.setText("Some default description...");
                                    // Play video
                                    playVideo(latestUrl);
                                } else {
                                    Toast.makeText(TvShowEpisodeActivity.this, "Video URL not available", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Parsing error", e);
                            }
                        }else {
                            hideShimmer();
                            try {
                                if (response.errorBody() != null) {
                                    String errorStr = response.errorBody().string();
                                    JSONObject jsonObject = new JSONObject(errorStr);
                                    if (jsonObject.has("error") && "access_denied".equals(jsonObject.getString("error"))) {
                                        Toast.makeText(TvShowEpisodeActivity.this, "Access Denied", Toast.LENGTH_SHORT).show();
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
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TvShowEpisodeActivity.this, "Failed to fetch video", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "API failed", t);
                    }
                });
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).setTrackSelector(new DefaultTrackSelector(this)).build();
        playerView.setPlayer(player);

        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean isSeeking = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    long pos = (player.getDuration() * progress) / 100;
                    player.seekTo(pos);
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
                if (player != null && player.getPlaybackState() == Player.STATE_READY) {
                    long pos = player.getCurrentPosition();
                    long dur = player.getDuration();
                    if (dur > 0) playerSeekBar.setProgress((int) (pos * 100 / dur));
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
                        exoPlay.setVisibility(View.VISIBLE);
                        exoPause.setVisibility(View.INVISIBLE);
                        break;
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                progressBar.setVisibility(View.VISIBLE); // show loading on error
                Toast.makeText(TvShowEpisodeActivity.this, "Playback error occurred", Toast.LENGTH_SHORT).show();

                // Retry after 2 seconds if network available
                if (isNetworkAvailable()) {
                    new Handler().postDelayed(() -> player.prepare(), 2000);
                }
            }
        });
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        return cm.getActiveNetwork() != null;
    }


    private void playVideo(String url) {
        if (player != null) {
            MediaItem item = MediaItem.fromUri(Uri.parse(url));
            player.setMediaItem(item);
            player.prepare();
            player.play();
            exoPlay.setVisibility(View.INVISIBLE);
            exoPause.setVisibility(View.VISIBLE);
        }
    }

    private void showShimmer() {
        shimmerTitle.setVisibility(View.VISIBLE);
        shimmerSubtitle.setVisibility(View.VISIBLE);
        shimmerDescription.setVisibility(View.VISIBLE);
        title.setVisibility(View.GONE);
        subtitle.setVisibility(View.GONE);
        description.setVisibility(View.GONE);
    }

    private void hideShimmer() {
        shimmerTitle.setVisibility(View.GONE);
        shimmerSubtitle.setVisibility(View.GONE);
        shimmerDescription.setVisibility(View.GONE);
        title.setVisibility(View.VISIBLE);
        subtitle.setVisibility(View.VISIBLE);
        description.setVisibility(View.VISIBLE);
    }


    @Override
    protected void onDestroy() {
        // Update stream time when activity is destroyed
        updateStreamTime();

        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        progressHandler.removeCallbacks(progressRunnable);
    }

    @Override
    public void onBackPressed() {
        if (isFullscreen) {
            toggleFullscreen();
        } else {
            // Update stream time before going back
            updateStreamTime();
            super.onBackPressed();
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


    private void animateButton(View button, boolean forward) {
        button.setAlpha(1f);

        float fromX = forward ? 0 : 0;  // no horizontal move, just a scale or pulse
        float toX = forward ? 50 : -50; // move right for forward, left for rewind

        // Move animation
        TranslateAnimation translate = new TranslateAnimation(fromX, toX, 0, 0);
        translate.setDuration(300);
        translate.setRepeatCount(0);

        // Fade out animation
        AlphaAnimation alpha = new AlphaAnimation(1f, 0f);
        alpha.setDuration(300);
        alpha.setRepeatCount(0);

        // Combine
        AnimationSet set = new AnimationSet(true);
        set.addAnimation(translate);
        set.addAnimation(alpha);

        button.startAnimation(set);
    }


    private void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Rational aspectRatio;

            if (player != null && player.getVideoSize() != null) {
                int videoWidth = player.getVideoSize().width;
                int videoHeight = player.getVideoSize().height;

                if (videoWidth > 0 && videoHeight > 0) {
                    aspectRatio = new Rational(videoWidth, videoHeight); // ✅ correct
                } else {
                    aspectRatio = new Rational(playerView.getWidth(), playerView.getHeight());
                }
            } else {
                aspectRatio = new Rational(playerView.getWidth(), playerView.getHeight());
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

        closeMainActivity();
        closeViewMoreMovieActivity();
        closeViewMoreLatestTvShowListActivity();
        enterPipMode();

    }

    private void closeMainActivity() {
        // Send broadcast to close MainActivity
        Intent closeIntent = new Intent("CLOSE_MAIN_ACTIVITY");
        sendBroadcast(closeIntent);
    }


    private void closeViewMoreMovieActivity() {
        // Send broadcast to close MainActivity
        Intent closeIntent = new Intent("CLOSE_ViewMoreTvShowActivity");
        sendBroadcast(closeIntent);
    }

    private void closeViewMoreLatestTvShowListActivity() {
        // Send broadcast to close MainActivity
        Intent closeIntent = new Intent("CLOSE_MoreLatestTvShowListActivity");
        sendBroadcast(closeIntent);
    }


    @Override
    public void onPictureInPictureModeChanged(boolean isInPiPMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPiPMode, newConfig);


        if (isInPiPMode) {
            // Hide channel list and other controls
            recyclerView.setVisibility(View.GONE);
            backbtn.setVisibility(View.GONE);
            morechannelstext.setVisibility(View.GONE);
            title.setVisibility(View.GONE);
            subtitle.setVisibility(View.GONE);
            description.setVisibility(View.GONE);
            playerView.hideController();


            closeMainActivity();
            closeViewMoreMovieActivity();
        } else {
            // Restore UI when back to full screen
            recyclerView.setVisibility(View.VISIBLE);
            backbtn.setVisibility(View.VISIBLE);
            morechannelstext.setVisibility(View.VISIBLE);
            title.setVisibility(View.VISIBLE);
            subtitle.setVisibility(View.VISIBLE);
            description.setVisibility(View.VISIBLE);
            playerView.showController();


        }
    }

    private void updateStreamTime() {
        if (player == null || channelId == null) {
            Log.d("StreamTime", "Player or channelId is null, skipping update");
            return;
        }

        long currentPosition = player.getCurrentPosition();
        String formattedTime = formatTimeForApi(currentPosition);

        SharedPrefManager sp = SharedPrefManager.getInstance(this);
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

                            // Handle access denied
                            JSONObject jsonObject = new JSONObject(errorStr);
                            if (jsonObject.has("error") && "access_denied".equals(jsonObject.getString("error"))) {
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


}
