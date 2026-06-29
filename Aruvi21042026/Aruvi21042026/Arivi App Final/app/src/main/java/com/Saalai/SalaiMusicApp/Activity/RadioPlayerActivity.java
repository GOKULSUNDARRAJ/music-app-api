package com.Saalai.SalaiMusicApp.Activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.Player;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.RadioAdapterDetail;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.RadioModel;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.RadioResponse;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerAdapterForRadioMore;
import com.squareup.picasso.Picasso;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RadioPlayerActivity extends AppCompatActivity {

    private static final String TAG = "RadioPlayerActivity";

    private RecyclerView recyclerView, shimmerRecyclerView;
    private RadioAdapterDetail radioAdapter;
    private ShimmerAdapterForRadioMore shimmerAdapter;
    private List<RadioModel> radioList = new ArrayList<>();

    private PlayerView playerView;
    private ExoPlayer player;

    private int offset = 0;
    private final int count = 15;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private String channelId;
    private ImageView ivCurrentRadio;
    LinearLayout rootLayout;
    private ImageView btnPlayPause, btnPrevious, btnNext;
    private int currentRadioIndex = 0;
    private boolean isPlaying = false;

    ImageView back;

    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean audioFocusGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio_player);

// Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.black)); // replace with your color
        }

        View rootView = findViewById(R.id.rootLayout);


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
        }
        else {
            // Pre-Android 12
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            if (rootView != null) {
                rootView.setFitsSystemWindows(true); // critical for proper layout
            }
        }

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        rootLayout = findViewById(R.id.rootLayout);
        playerView = findViewById(R.id.playerView);
        recyclerView = findViewById(R.id.recyclerView);
        shimmerRecyclerView = findViewById(R.id.shimmerRecyclerView);

        back=findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        initializePlayer();

        // Initialize player controls
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);

        // Set up click listeners
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPrevious.setOnClickListener(v -> playPrevious());
        btnNext.setOnClickListener(v -> playNext());

        // RecyclerView setup
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(gridLayoutManager);
        radioAdapter = new RadioAdapterDetail(this, radioList);
        recyclerView.setAdapter(radioAdapter);

        GridLayoutManager gridLayoutManager1 = new GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false);
        shimmerRecyclerView.setLayoutManager(gridLayoutManager1);
        shimmerAdapter = new ShimmerAdapterForRadioMore(10);
        shimmerRecyclerView.setAdapter(shimmerAdapter);

        ivCurrentRadio = findViewById(R.id.imgRadioLogo);

        // Set click listener for radio items
        radioAdapter.setOnRadioClickListener(radio -> {
            currentRadioIndex = radioList.indexOf(radio);
            playSelectedRadio(radio);
        });

        // Get the radio data from intent
        RadioModel radio = (RadioModel) getIntent().getSerializableExtra("RADIO_DATA");

        if (radio != null) {
            // Add the current radio to the list for navigation
            radioList.add(0, radio);

            // Show radio image and play immediately
            Picasso.get()
                    .load(radio.getChannelLogo())
                    .into(new com.squareup.picasso.Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            if (bitmap == null || bitmap.isRecycled()) {
                                Log.e(TAG, "Bitmap is null or recycled, cannot generate Palette");
                                return;
                            }

                            // Set the image to your ImageView
                            ivCurrentRadio.setImageBitmap(bitmap);

                            // Extract colors using Palette
                            Palette.from(bitmap).generate(palette -> {
                                int defaultColor = getResources().getColor(R.color.bgblack);
                                int darkVibrant = palette.getDarkVibrantColor(defaultColor);
                                int darkMuted = palette.getDarkMutedColor(defaultColor);

                                // Create a gradient background
                                int[] colors = {darkVibrant, darkMuted, defaultColor};
                                GradientDrawable gradientDrawable = new GradientDrawable(
                                        GradientDrawable.Orientation.TOP_BOTTOM,
                                        colors
                                );

                                // Apply gradient background safely
                                LinearLayout rootLayout = findViewById(R.id.rootLayout);
                                if (rootLayout != null) {
                                    rootLayout.setBackground(gradientDrawable);
                                }
                            });
                        }

                        @Override
                        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                            Log.e(TAG, "Picasso failed to load image: " + e.getMessage());
                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                            // Optionally show placeholder
                            ivCurrentRadio.setImageDrawable(placeHolderDrawable);
                        }
                    });


            // Play radio immediately on activity start
            if (radio.getChannelURL() != null && !radio.getChannelURL().isEmpty()) {
                playRadio(radio.getChannelURL());


                // Update UI to show pause button
                btnPlayPause.setImageResource(R.drawable.ic_pause_active);
                isPlaying = true;
            }

            // Load radio list for "More Like This" section
            loadRadioList(true);
        }

        // Endless scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) rv.getLayoutManager();
                if (layoutManager == null) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        Log.d(TAG, "End of list reached, loading next page...");
                        loadRadioList(false);
                    }
                }
            }
        });

        // Add player state listener
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                    runOnUiThread(() -> {
                        btnPlayPause.setImageResource(R.drawable.ic_play_active);
                        isPlaying = false;
                    });
                } else if (playbackState == Player.STATE_READY) {
                    runOnUiThread(() -> {
                        btnPlayPause.setImageResource(R.drawable.ic_pause_active);
                        isPlaying = true;
                    });
                }
            }
        });



        TextView seeAll = findViewById(R.id.sellall);

        seeAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Scroll the RecyclerView by a fixed amount (e.g., 400 pixels)
                recyclerView.smoothScrollBy(600, 0);
            }
        });


        initializeAudioFocus();

    }

    private void initializeAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build();
        }
    }

    private boolean requestAudioFocus() {
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            result = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            result = audioManager.requestAudioFocus(audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
        }

        audioFocusGranted = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        Log.d(TAG, "AudioFocus request result: " + result + ", granted: " + audioFocusGranted);
        return audioFocusGranted;
    }

    private void abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        }
        audioFocusGranted = false;
        Log.d(TAG, "AudioFocus abandoned");
    }


    private void setVolumeToDuck() {
        // Lower volume to 40% when ducking
        if (player != null) {
            player.setVolume(0.4f);
        }
    }

    private void setVolumeToNormal() {
        // Restore normal volume
        if (player != null) {
            player.setVolume(1.0f);
        }
    }

    private void pausePlayback() {
        if (player != null && isPlaying) {
            player.pause();
            btnPlayPause.setImageResource(R.drawable.ic_play_active);
            isPlaying = false;
        }
    }

    private void togglePlayPause() {
        if (player != null) {
            if (isPlaying) {
                pausePlayback();
            } else {
                playRadioWithAudioFocus();
            }
        }
    }

    private void playRadioWithAudioFocus() {
        if (requestAudioFocus()) {
            player.play();
            btnPlayPause.setImageResource(R.drawable.ic_pause_active);
            isPlaying = true;
        } else {
            Toast.makeText(this, "Cannot play radio - audio focus not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void playNext() {
        if (radioList.isEmpty()) return;

        currentRadioIndex = (currentRadioIndex + 1) % radioList.size();
        RadioModel nextRadio = radioList.get(currentRadioIndex);
        playSelectedRadio(nextRadio);
    }

    private void playPrevious() {
        if (radioList.isEmpty()) return;

        currentRadioIndex = (currentRadioIndex - 1 + radioList.size()) % radioList.size();
        RadioModel previousRadio = radioList.get(currentRadioIndex);
        playSelectedRadio(previousRadio);
    }

    private void playSelectedRadio(RadioModel radio) {
        Log.d(TAG, "Playing radio: " + radio.getChannelName());

        // Set the selected radio image
        if (radio.getChannelLogo() != null && !radio.getChannelLogo().isEmpty()) {
            Picasso.get()
                    .load(radio.getChannelLogo())
                    .into(new com.squareup.picasso.Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            if (bitmap == null || bitmap.isRecycled()) {
                                Log.e(TAG, "Bitmap is null or recycled, cannot generate Palette");
                                return;
                            }

                            ivCurrentRadio.setImageBitmap(bitmap);

                            Palette.from(bitmap).generate(palette -> {
                                int defaultColor = getResources().getColor(R.color.bgblack);
                                int darkVibrant = palette.getDarkVibrantColor(defaultColor);
                                int darkMuted = palette.getDarkMutedColor(defaultColor);

                                int[] colors = {darkVibrant, darkMuted, defaultColor};
                                GradientDrawable gradientDrawable = new GradientDrawable(
                                        GradientDrawable.Orientation.TOP_BOTTOM,
                                        colors
                                );

                                LinearLayout rootLayout = findViewById(R.id.rootLayout);
                                if (rootLayout != null) {
                                    rootLayout.setBackground(gradientDrawable);
                                }
                            });
                        }

                        @Override
                        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                            Log.e(TAG, "Picasso failed to load image: " + e.getMessage());
                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                            ivCurrentRadio.setImageDrawable(placeHolderDrawable);
                        }
                    });
        } else {
            LinearLayout rootLayout = findViewById(R.id.rootLayout);
            if (rootLayout != null) {
                rootLayout.setBackgroundColor(getResources().getColor(R.color.bgblack));
            }
        }

        // Play radio
        if (radio.getChannelURL() != null && !radio.getChannelURL().isEmpty()) {
            playRadio(radio.getChannelURL());
            playRadioWithAudioFocus();
            currentRadioIndex = radioList.indexOf(radio);
        } else {
            Toast.makeText(this, "Error: Radio stream URL is not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        Log.d(TAG, "ExoPlayer initialized");
    }

    private void playRadio(String url) {
        Log.d(TAG, "Playing radio URL: " + url);
        try {
            MediaItem mediaItem = MediaItem.fromUri(url);
            player.setMediaItem(mediaItem);
            player.prepare();
            // Don't auto-play here, wait for audio focus
        } catch (Exception e) {
            Log.e(TAG, "Error playing radio: " + e.getMessage());
            Toast.makeText(this, "Error playing radio", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRadioList(boolean isFirstLoad) {
        isLoading = true;
        Log.d(TAG, "Loading radio list, isFirstLoad=" + isFirstLoad + " offset=" + offset);

        if (isFirstLoad) {
            shimmerRecyclerView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }

        String accessToken = SharedPrefManager.getInstance(this).getAccessToken();
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Call<RadioResponse> call = apiService.getRadioList(
                accessToken,
                channelId,
                String.valueOf(offset),
                String.valueOf(count)
        );

        call.enqueue(new Callback<RadioResponse>() {
            @Override
            public void onResponse(Call<RadioResponse> call, Response<RadioResponse> response) {
                isLoading = false;

                if (isFirstLoad) {
                    shimmerRecyclerView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<RadioModel> apiRadios = response.body().getRadioList();
                    Log.d(TAG, "Loaded " + (apiRadios != null ? apiRadios.size() : 0) + " radios");

                    if (apiRadios != null && !apiRadios.isEmpty()) {
                        // Add new radios to the list (skip the first one if it's the current playing radio)
                        int startIndex = radioList.size() > 0 ? 1 : 0;
                        radioList.addAll(apiRadios);
                        radioAdapter.notifyDataSetChanged();

                        offset++;
                        if (apiRadios.size() < count) isLastPage = true;
                    } else {
                        isLastPage = true;
                        Log.d(TAG, "No radios received from API");
                    }

                }
                else {
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorStr);
                            if (jsonObject.has("error") && "access_denied".equals(jsonObject.getString("error"))) {
                                Toast.makeText(RadioPlayerActivity.this, "Access Denied", Toast.LENGTH_SHORT).show();
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
            public void onFailure(Call<RadioResponse> call, Throwable t) {
                isLoading = false;
                if (isFirstLoad) shimmerRecyclerView.setVisibility(View.GONE);
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(RadioPlayerActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Releasing player and abandoning audio focus");
        abandonAudioFocus();
        if (player != null) {
            player.release();
            player = null;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Pausing radio playback");
        if (player != null && isPlaying) {
            player.pause();
            btnPlayPause.setImageResource(R.drawable.ic_play_active);
            isPlaying = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Resuming activity");

        // Auto-resume playback if we have audio focus and player is prepared
        if (player != null && !isPlaying && audioFocusGranted) {
            player.play();
            btnPlayPause.setImageResource(R.drawable.ic_pause_active);
            isPlaying = true;
            Log.d(TAG, "Auto-resumed playback on resume");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Activity stopping");
        // Consider abandoning audio focus if app goes to background
        if (isFinishing()) {
            abandonAudioFocus();
        }
    }


    private void callLogoutApi() {
        SharedPrefManager sp = SharedPrefManager.getInstance(RadioPlayerActivity.this);
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

                    Intent intent = new Intent(RadioPlayerActivity.this, SignUpActivity.class);
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

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    // regained focus, can resume playback
                    Log.d(TAG, "AudioFocus: AUDIOFOCUS_GAIN");
                    if (player != null && !isPlaying) {
                        player.play();
                        btnPlayPause.setImageResource(R.drawable.ic_pause_active);
                        isPlaying = true;
                    }
                    setVolumeToNormal();
                    break;

                case AudioManager.AUDIOFOCUS_LOSS:
                    // lost focus for a long time, stop playback
                    Log.d(TAG, "AudioFocus: AUDIOFOCUS_LOSS");
                    pausePlayback();
                    abandonAudioFocus();
                    break;

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    // lost focus for a short time, pause playback
                    Log.d(TAG, "AudioFocus: AUDIOFOCUS_LOSS_TRANSIENT");
                    if (player != null && isPlaying) {
                        player.pause();
                        btnPlayPause.setImageResource(R.drawable.ic_play_active);
                        isPlaying = false;
                    }
                    break;

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    // lost focus but can play at lower volume
                    Log.d(TAG, "AudioFocus: AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                    setVolumeToDuck();
                    break;
            }
        }
    };






}