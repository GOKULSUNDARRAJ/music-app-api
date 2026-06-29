package com.Saalai.SalaiMusicApp.Fragments;

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
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.media3.common.PlaybackException;
import com.Saalai.SalaiMusicApp.Activity.SignUpActivity;
import com.Saalai.SalaiMusicApp.Adapters.RadioAdapterDetail;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.RadioModel;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.RadioResponse;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerAdapterForRadioMore;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RadioPlayerFragment extends Fragment {

    private static final String TAG = "RadioPlayerFragment";

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

    // Add progress bar
    private ProgressBar progressBar;

    // Track if we're waiting for audio focus
    private boolean waitingForAudioFocus = false;

    // Factory method to create new instance
    public static RadioPlayerFragment newInstance(RadioModel radio) {
        RadioPlayerFragment fragment = new RadioPlayerFragment();
        Bundle args = new Bundle();
        args.putSerializable("RADIO_DATA", radio);
        fragment.setArguments(args);
        return fragment;
    }

    public interface RadioPlayerListener {
        void onRadioPlayerStarted();
        void onRadioPlayerFinished();
    }

    private RadioPlayerListener radioPlayerListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof RadioPlayerListener) {
            radioPlayerListener = (RadioPlayerListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_radio_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (radioPlayerListener != null) {
            radioPlayerListener.onRadioPlayerStarted();
        } else {
            sendNavigationBarVisibility(false);
        }

        // Set status bar color - This might need to be handled in the Activity
        if (getActivity() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.black));
        }

        initializeViews(view);
        initializePlayer();
        setupRecyclerView();
        setupClickListeners();

        // Get the radio data from arguments
        RadioModel radio = (RadioModel) getArguments().getSerializable("RADIO_DATA");

        if (radio != null) {
            setupRadioPlayer(radio);
        }

        initializeAudioFocus();
    }

    private void sendNavigationBarVisibility(boolean showBars) {
        try {
            Intent intent = new Intent("NAVIGATION_BARS_VISIBILITY");
            intent.putExtra("show_bars", showBars);
            requireContext().sendBroadcast(intent);
        } catch (Exception e) {
            Log.e("RadioPlayer", "Error sending navigation bar visibility", e);
        }
    }

    private void initializeViews(View view) {
        rootLayout = view.findViewById(R.id.rootLayout);

        rootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        
        playerView = view.findViewById(R.id.playerView);
        recyclerView = view.findViewById(R.id.recyclerView);
        shimmerRecyclerView = view.findViewById(R.id.shimmerRecyclerView);
        back = view.findViewById(R.id.back);
        ivCurrentRadio = view.findViewById(R.id.imgRadioLogo);

        // Initialize player controls
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext = view.findViewById(R.id.btnNext);

        // Initialize progress bar
        progressBar = view.findViewById(R.id.progressBar); // Make sure you have this in your layout
        if (progressBar == null) {
            // If progressBar ID is different, find it by its actual ID
            progressBar = view.findViewById(android.R.id.progress); // or use your actual ID
        }

        // Initially hide progress bar
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        back.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPrevious.setOnClickListener(v -> playPrevious());
        btnNext.setOnClickListener(v -> playNext());
    }

    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 2, GridLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(gridLayoutManager);
        radioAdapter = new RadioAdapterDetail((FragmentActivity) requireContext(), radioList);
        recyclerView.setAdapter(radioAdapter);

        GridLayoutManager shimmerLayoutManager = new GridLayoutManager(requireContext(), 2, GridLayoutManager.HORIZONTAL, false);
        shimmerRecyclerView.setLayoutManager(shimmerLayoutManager);
        shimmerAdapter = new ShimmerAdapterForRadioMore(10);
        shimmerRecyclerView.setAdapter(shimmerAdapter);

        // Set click listener for radio items
        radioAdapter.setOnRadioClickListener(radio -> {
            currentRadioIndex = radioList.indexOf(radio);
            playSelectedRadio(radio);
        });

        // Endless scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                GridLayoutManager layoutManager = (GridLayoutManager) rv.getLayoutManager();
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

        TextView seeAll = requireView().findViewById(R.id.sellall);
        seeAll.setOnClickListener(v -> {
            recyclerView.smoothScrollBy(600, 0);
        });
    }

    private void setupRadioPlayer(RadioModel radio) {
        // Add the current radio to the list for navigation
        radioList.add(0, radio);

        // Show radio image and play immediately
        if (radio.getChannelLogo() != null && !radio.getChannelLogo().isEmpty()) {
            Picasso.get()
                    .load(radio.getChannelLogo())
                    .into(new com.squareup.picasso.Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            // Check if fragment is still attached
                            if (!isAdded() || getContext() == null) {
                                return;
                            }

                            if (bitmap == null || bitmap.isRecycled()) {
                                Log.e(TAG, "Bitmap is null or recycled, cannot generate Palette");
                                return;
                            }

                            if (ivCurrentRadio != null) {
                                ivCurrentRadio.setImageBitmap(bitmap);
                            }

                            Palette.from(bitmap).generate(palette -> {
                                // Check again before using requireContext()
                                if (!isAdded() || getContext() == null) {
                                    return;
                                }

                                try {
                                    int defaultColor = ContextCompat.getColor(requireContext(), R.color.bgblack);
                                    int darkVibrant = palette.getDarkVibrantColor(defaultColor);
                                    int darkMuted = palette.getDarkMutedColor(defaultColor);

                                    int[] colors = {darkVibrant, darkMuted, defaultColor};
                                    GradientDrawable gradientDrawable = new GradientDrawable(
                                            GradientDrawable.Orientation.TOP_BOTTOM,
                                            colors
                                    );

                                    if (rootLayout != null) {
                                        rootLayout.setBackground(gradientDrawable);
                                    }
                                } catch (IllegalStateException e) {
                                    Log.e(TAG, "Fragment not attached when trying to use requireContext()", e);
                                }
                            });
                        }

                        @Override
                        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                            Log.e(TAG, "Picasso failed to load image: " + e.getMessage());
                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                            if (isAdded() && ivCurrentRadio != null) {
                                ivCurrentRadio.setImageDrawable(placeHolderDrawable);
                            }
                        }
                    });
        }

        // Play radio immediately on fragment start
        if (radio.getChannelURL() != null && !radio.getChannelURL().isEmpty()) {
            playRadio(radio.getChannelURL());

            // Add this: Actually start playback with audio focus
            new Handler().postDelayed(() -> {
                if (isAdded()) {
                    playRadioWithAudioFocus();
                }
            }, 500); // Small delay to ensure player is prepared

        } else {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Error: Radio stream URL is not available", Toast.LENGTH_SHORT).show();
            }
        }

        // Load radio list for "More Like This" section
        loadRadioList(true);
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(requireContext()).build();
        playerView.setPlayer(player);
        Log.d(TAG, "ExoPlayer initialized");

        // Add player state listener with buffering handling
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        switch (playbackState) {
                            case Player.STATE_BUFFERING:
                                // Only show progress bar if we're not waiting for audio focus
                                if (!waitingForAudioFocus) {
                                    showProgressBar(true);
                                    setPlayPauseButtonVisibility(false);
                                }
                                Log.d(TAG, "Player state: BUFFERING");
                                break;

                            case Player.STATE_READY:
                                // Hide progress bar when ready (audio focus will handle playback)
                                showProgressBar(false);
                                setPlayPauseButtonVisibility(true);
                                waitingForAudioFocus = false;

                                if (isPlaying) {
                                    btnPlayPause.setImageResource(R.drawable.ic_pause_active);
                                } else {
                                    btnPlayPause.setImageResource(R.drawable.ic_play_active);
                                }
                                Log.d(TAG, "Player state: READY");
                                break;

                            case Player.STATE_ENDED:
                            case Player.STATE_IDLE:
                                // Hide progress bar and show play button when ended/idle
                                showProgressBar(false);
                                setPlayPauseButtonVisibility(true);
                                waitingForAudioFocus = false;
                                btnPlayPause.setImageResource(R.drawable.ic_play_active);
                                isPlaying = false;
                                Log.d(TAG, "Player state: " + (playbackState == Player.STATE_ENDED ? "ENDED" : "IDLE"));
                                break;
                        }
                    });
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Player error: " + error.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgressBar(false);
                        setPlayPauseButtonVisibility(true);
                        waitingForAudioFocus = false;
                        btnPlayPause.setImageResource(R.drawable.ic_play_active);
                        Toast.makeText(requireContext(), "Playback error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void showProgressBar(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            Log.d(TAG, "Progress bar: " + (show ? "SHOW" : "HIDE"));
        }
    }

    private void setPlayPauseButtonVisibility(boolean visible) {
        if (btnPlayPause != null) {
            btnPlayPause.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void initializeAudioFocus() {
        audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);

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
        waitingForAudioFocus = false;
        Log.d(TAG, "AudioFocus abandoned");
    }

    private void setVolumeToDuck() {
        if (player != null) {
            player.setVolume(0.4f);
        }
    }

    private void setVolumeToNormal() {
        if (player != null) {
            player.setVolume(1.0f);
        }
    }

    private void pausePlayback() {
        if (player != null && isPlaying) {
            player.pause();
            btnPlayPause.setImageResource(R.drawable.ic_play_active);
            isPlaying = false;
            showProgressBar(false); // Hide progress bar when pausing
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
        Log.d(TAG, "playRadioWithAudioFocus: requesting audio focus");

        // Show progress bar immediately when user initiates playback
        showProgressBar(true);
        setPlayPauseButtonVisibility(false);
        waitingForAudioFocus = true;

        if (requestAudioFocus()) {
            // Audio focus granted immediately
            waitingForAudioFocus = false;

            // Check if player is already ready before starting playback
            if (player != null && player.getPlaybackState() == Player.STATE_READY) {
                // Player is already ready, hide progress bar and start playback
                showProgressBar(false);
                setPlayPauseButtonVisibility(true);
                startPlayback();
            } else {
                // Player is still buffering, let the state listener handle progress bar
                startPlayback();
            }
        } else {
            // Audio focus not granted immediately, keep progress bar visible
            Log.d(TAG, "Audio focus not granted immediately, waiting...");
            // Progress bar remains visible until we get audio focus or timeout
        }
    }

    private void startPlayback() {
        if (player != null) {
            player.play();
            btnPlayPause.setImageResource(R.drawable.ic_pause_active);
            isPlaying = true;
            Log.d(TAG, "Radio playback started successfully");

            // Check if we need to hide progress bar immediately
            if (player.getPlaybackState() == Player.STATE_READY) {
                showProgressBar(false);
                setPlayPauseButtonVisibility(true);
            }
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

        // Show progress bar when switching radio
        showProgressBar(true);
        setPlayPauseButtonVisibility(false);
        waitingForAudioFocus = true;

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
                                int defaultColor = ContextCompat.getColor(requireContext(), R.color.bgblack);
                                int darkVibrant = palette.getDarkVibrantColor(defaultColor);
                                int darkMuted = palette.getDarkMutedColor(defaultColor);

                                int[] colors = {darkVibrant, darkMuted, defaultColor};
                                GradientDrawable gradientDrawable = new GradientDrawable(
                                        GradientDrawable.Orientation.TOP_BOTTOM,
                                        colors
                                );

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
            if (rootLayout != null) {
                rootLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bgblack));
            }
        }

        // Play radio
        if (radio.getChannelURL() != null && !radio.getChannelURL().isEmpty()) {
            playRadio(radio.getChannelURL());

            // Request audio focus for the new radio
            if (requestAudioFocus()) {
                waitingForAudioFocus = false;
                startPlayback();
            }
            currentRadioIndex = radioList.indexOf(radio);
        } else {
            Toast.makeText(requireContext(), "Error: Radio stream URL is not available", Toast.LENGTH_SHORT).show();
            // Hide progress bar if there's an error
            showProgressBar(false);
            setPlayPauseButtonVisibility(true);
            waitingForAudioFocus = false;
        }
    }

    private void playRadio(String url) {
        Log.d(TAG, "Playing radio URL: " + url);
        try {
            MediaItem mediaItem = MediaItem.fromUri(url);
            player.setMediaItem(mediaItem);
            player.prepare();
        } catch (Exception e) {
            Log.e(TAG, "Error playing radio: " + e.getMessage());
            Toast.makeText(requireContext(), "Error playing radio", Toast.LENGTH_SHORT).show();
            // Hide progress bar if there's an error
            showProgressBar(false);
            setPlayPauseButtonVisibility(true);
            waitingForAudioFocus = false;
        }
    }

    private void loadRadioList(boolean isFirstLoad) {
        isLoading = true;
        Log.d(TAG, "Loading radio list, isFirstLoad=" + isFirstLoad + " offset=" + offset);

        if (isFirstLoad) {
            shimmerRecyclerView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }

        String accessToken = SharedPrefManager.getInstance(requireContext()).getAccessToken();
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

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isFirstLoad) {
                            shimmerRecyclerView.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }

                        if (response.isSuccessful() && response.body() != null) {
                            List<RadioModel> apiRadios = response.body().getRadioList();
                            Log.d(TAG, "Loaded " + (apiRadios != null ? apiRadios.size() : 0) + " radios");

                            if (apiRadios != null && !apiRadios.isEmpty()) {
                                int startIndex = radioList.size() > 0 ? 1 : 0;
                                radioList.addAll(apiRadios);
                                radioAdapter.notifyDataSetChanged();

                                offset++;
                                if (apiRadios.size() < count) isLastPage = true;
                            } else {
                                isLastPage = true;
                                Log.d(TAG, "No radios received from API");
                            }
                        } else {
                            try {
                                if (response.errorBody() != null) {
                                    String errorStr = response.errorBody().string();
                                    JSONObject jsonObject = new JSONObject(errorStr);
                                    if (jsonObject.has("error") && "access_denied".equals(jsonObject.getString("error"))) {
                                        Toast.makeText(requireContext(), "Access Denied", Toast.LENGTH_SHORT).show();
                                        callLogoutApi();
                                    } else {
                                        Log.e("Dashboard", "Server Error Code: " + response.code() + ", Error: " + errorStr);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("Dashboard", "Error parsing errorBody", e);
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<RadioResponse> call, Throwable t) {
                isLoading = false;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isFirstLoad) shimmerRecyclerView.setVisibility(View.GONE);
                        Log.e(TAG, "API call failed: " + t.getMessage());
                        Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Releasing player and abandoning audio focus");
        abandonAudioFocus();
        if (player != null) {
            player.release();
            player = null;
        }
        if (radioPlayerListener != null) {
            radioPlayerListener.onRadioPlayerFinished();
        } else {
            sendNavigationBarVisibility(true);
        }
    }



    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Resuming fragment");

        if (player != null && !isPlaying && audioFocusGranted) {
            // Don't auto-resume with progress bar - let user manually play
            // This prevents unwanted auto-playback with progress bar
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Fragment stopping");
        if (isRemoving()) {
            abandonAudioFocus();
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

                    Intent intent = new Intent(requireContext(), SignUpActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
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
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_GAIN:
                            Log.d(TAG, "AudioFocus: AUDIOFOCUS_GAIN");
                            waitingForAudioFocus = false;

                            if (player != null && !isPlaying) {
                                // Only start playback if we were waiting for audio focus
                                if (waitingForAudioFocus) {
                                    startPlayback();
                                }
                            }
                            setVolumeToNormal();
                            break;

                        case AudioManager.AUDIOFOCUS_LOSS:
                            Log.d(TAG, "AudioFocus: AUDIOFOCUS_LOSS");
                            pausePlayback();
                            abandonAudioFocus();
                            break;

                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            Log.d(TAG, "AudioFocus: AUDIOFOCUS_LOSS_TRANSIENT");
                            if (player != null && isPlaying) {
                                player.pause();
                                btnPlayPause.setImageResource(R.drawable.ic_play_active);
                                isPlaying = false;
                                showProgressBar(false); // Hide progress bar when losing focus
                            }
                            break;

                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            Log.d(TAG, "AudioFocus: AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                            setVolumeToDuck();
                            break;
                    }
                });
            }
        }
    };
}