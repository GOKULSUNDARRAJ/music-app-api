package com.Saalai.SalaiMusicApp.Fragments;

import static android.content.ContentValues.TAG;

import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.DownloadedSongsAdapter;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Models.PlaylistModel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Utils.DownloadPlaylistManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class DownloadPlaylistSongsFragment extends Fragment {

    private static final String ARG_PLAYLIST_ID = "playlist_id";
    private static final String ARG_PLAYLIST_IMAGE_URL = "playlist_image_url";
    private static final String ARG_PLAYLIST_NAME = "playlist_name";

    private RecyclerView recyclerView;
    private DownloadedSongsAdapter songsAdapter;
    private DownloadPlaylistManager playlistManager;
    private TextView tvTitle, tvEmptyState, tvSongCount;
    private ImageView btnBack, ivPlaylistBackground, btnFullPlayPause;

    // Collapsible layout views
    private NestedScrollView nestedScrollView;
    private CoordinatorLayout mainlayout;

    private FrameLayout collapsibleImageContainer;

    private boolean isImageVisible = true; // Track if image is visible
    private int imageGradientColor = -1; // Store the image gradient color
    private int toolbarGradientColor = -1; // Store the toolbar gradient color

    private ConstraintLayout linearLayout3;
    private FloatingActionButton toolbarPlayPause;
    private LinearLayout toolbar;
    private ImageView toolbarAlbumArt, backtool;
    private TextView toolbarSongName, toolbarSongArtist;
    private View toolbarLayout;
    private TextView btnSearch;
    // Collapsible layout variables
    private int imageHeight = 0;
    private int maxImageSize = 0;
    private int minImageSize = 0;
    private boolean isToolbarVisible = false;
    private static final float HIDE_THRESHOLD = 0.95f;
    private boolean isAnimating = false;
    private ValueAnimator currentAnimator;
    private static final int SCROLL_THRESHOLD = 5;

    private String playlistId;
    private String playlistImageUrl;
    private String playlistName;
    private PlaylistModel playlist;
    private ArrayList<AudioModel> songsList = new ArrayList<>();
    private static final String TAG = "DownloadPlaylistSongsFragment";
    // Broadcast receiver for download updates


    private static final String ACTION_PREPARE_NEXT = "PREPARE_NEXT";
    private static final String ACTION_PREPARE_PREVIOUS = "PREPARE_PREVIOUS";


    private final BroadcastReceiver prepareReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (songsAdapter == null || getActivity() == null) return;

            String action = intent.getAction();
            getActivity().runOnUiThread(() -> {
                if (ACTION_PREPARE_NEXT.equals(action)) {
                    int nextIndex = intent.getIntExtra("nextIndex", -1);
                    if (nextIndex != -1 && nextIndex < songsList.size()) {
                        // Show progress on next item
                        songsAdapter.showNextItemProgress(nextIndex);

                        // Scroll to make the next item visible
                        if (recyclerView != null) {
                            recyclerView.smoothScrollToPosition(nextIndex);
                        }
                        Log.d(TAG, "Received prepare next for index: " + nextIndex);
                    }
                }
                else if (ACTION_PREPARE_PREVIOUS.equals(action)) {
                    int prevIndex = intent.getIntExtra("prevIndex", -1);
                    if (prevIndex != -1 && prevIndex < songsList.size()) {
                        // Show progress on previous item
                        songsAdapter.showPreviousItemProgress(prevIndex);

                        // Scroll to make the previous item visible
                        if (recyclerView != null) {
                            recyclerView.smoothScrollToPosition(prevIndex);
                        }
                        Log.d(TAG, "Received prepare previous for index: " + prevIndex);
                    }
                }
            });
        }
    };




    private final BroadcastReceiver downloadUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Broadcast received - Action: " + action);

            if ("DOWNLOAD_UPDATE".equals(action) || "PLAYLIST_UPDATE".equals(action)) {
                Log.d(TAG, "Reloading playlist songs due to broadcast update");
                loadPlaylistSongs();
            }
        }
    };

    // Broadcast receiver for player updates
    private BroadcastReceiver playerUpdateReceiver;
    private boolean isReceiverRegistered = false;


    // Updated newInstance method
    public static DownloadPlaylistSongsFragment newInstance(String playlistId, String playlistImageUrl, String playlistName) {
        DownloadPlaylistSongsFragment fragment = new DownloadPlaylistSongsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PLAYLIST_ID, playlistId);
        args.putString(ARG_PLAYLIST_IMAGE_URL, playlistImageUrl);
        args.putString(ARG_PLAYLIST_NAME, playlistName);
        fragment.setArguments(args);
        Log.d("DownloadPlaylistSongs", "newInstance called with playlistId: " + playlistId +
                ", imageUrl: " + playlistImageUrl + ", name: " + playlistName);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        playlistManager = DownloadPlaylistManager.getInstance(requireContext());

        if (getArguments() != null) {
            playlistId = getArguments().getString(ARG_PLAYLIST_ID);
            playlistImageUrl = getArguments().getString(ARG_PLAYLIST_IMAGE_URL);
            playlistName = getArguments().getString(ARG_PLAYLIST_NAME);
            Log.d(TAG, "Arguments loaded - ID: " + playlistId +
                    ", Image URL: " + playlistImageUrl +
                    ", Name: " + playlistName);
        } else {
            Log.w(TAG, "No arguments found!");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");

        View view = inflater.inflate(R.layout.fragment_download_playlist_songs, container, false);

        // Initialize PlayerManager
        PlayerManager.init(getContext());

        // Initialize views
        recyclerView = view.findViewById(R.id.audioRecyclerView);
        tvTitle = view.findViewById(R.id.tvFullPlayerSongName);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        tvSongCount = view.findViewById(R.id.tvFullPlayerSongArtist);
        btnBack = view.findViewById(R.id.back);
        ivPlaylistBackground = view.findViewById(R.id.ivPlaylistBackground);
        btnFullPlayPause = view.findViewById(R.id.btnFullPlayPause);

        // Initialize collapsible layout views
        nestedScrollView = view.findViewById(R.id.nestedScrollView);
        mainlayout= view.findViewById(R.id.mainlayout);
        collapsibleImageContainer = view.findViewById(R.id.collapsibleImageContainer);
        linearLayout3 = view.findViewById(R.id.linearLayout3);




        toolbarLayout = view.findViewById(R.id.appBarLayout);
        toolbar = view.findViewById(R.id.toolbar);
        toolbarAlbumArt = view.findViewById(R.id.toolbarAlbumArt);
        toolbarSongName = view.findViewById(R.id.toolbarSongName);
        toolbarSongArtist = view.findViewById(R.id.toolbarSongArtist);
        toolbarPlayPause = view.findViewById(R.id.toolbarPlayPause);
        backtool = view.findViewById(R.id.backtool);

        // Hide toolbar initially
        if (toolbarLayout != null) {
            toolbarLayout.setVisibility(View.INVISIBLE);
        }

        Log.d(TAG, "Views initialized - PlaylistBackground: " + (ivPlaylistBackground != null) +
                ", RecyclerView: " + (recyclerView != null) +
                ", Title: " + (tvTitle != null) + ", EmptyState: " + (tvEmptyState != null) +
                ", PlayPause: " + (btnFullPlayPause != null) +
                ", Toolbar: " + (toolbarLayout != null));

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup adapter
        songsAdapter = new DownloadedSongsAdapter(songsList, getContext(), new DownloadedSongsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AudioModel audioModel, android.widget.ProgressBar progressBar) {
                Log.d(TAG, "Song clicked: " + audioModel.getAudioName() +
                        ", Downloaded: " + audioModel.isDownloaded());
                playSong(audioModel, progressBar);
            }
        });

        songsAdapter.setRecyclerView(recyclerView);

        // Set song deleted listener
        songsAdapter.setOnSongDeletedListener(new DownloadedSongsAdapter.OnSongDeletedListener() {
            @Override
            public void onSongDeleted(AudioModel deletedAudio, int position) {
                Log.d(TAG, "Song delete requested: " + deletedAudio.getAudioName());
                removeSongFromPlaylist(deletedAudio, position);
            }
        });

        recyclerView.setAdapter(songsAdapter);
        Log.d(TAG, "Adapter set with " + songsList.size() + " songs");

        // Setup toolbar
        setupToolbar();

        // Setup toolbar play/pause button
        // Setup toolbar play/pause button
        if (toolbarPlayPause != null) {
            toolbarPlayPause.setOnClickListener(v -> {
                // Check if this playlist is playing
                boolean isPlaylistPlaying = isAnySongFromThisPlaylistPlaying();

                if (isPlaylistPlaying) {
                    // This playlist is playing - toggle play/pause
                    if (PlayerManager.isPlaying()) {
                        PlayerManager.pausePlayback();
                        toolbarPlayPause.setImageResource(R.drawable.play_player);
                        if (btnFullPlayPause != null) {
                            btnFullPlayPause.setImageResource(R.drawable.play_player);
                        }
                        Log.d(TAG, "Playlist paused from toolbar");
                    } else {
                        PlayerManager.resumePlayback();
                        toolbarPlayPause.setImageResource(R.drawable.pause);
                        if (btnFullPlayPause != null) {
                            btnFullPlayPause.setImageResource(R.drawable.pause);
                        }
                        Log.d(TAG, "Playlist resumed from toolbar");
                    }
                } else {
                    // Different playlist is playing or nothing playing - play first song from this playlist
                    Log.d(TAG, "No matching playlist playing, playing first song from this playlist");
                    playFirstSongFromPlaylist();
                }
                broadcastSongChanged();
            });
        }

        // Back button click for top back button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Log.d(TAG, "Top back button clicked");
                navigateBack();
            });
        }

        // Back button click for toolbar back button
        if (backtool != null) {
            backtool.setOnClickListener(v -> {
                Log.d(TAG, "Toolbar back button clicked");
                navigateBack();
            });
        }

        // Setup scroll listener
        setupScrollListener();

        // Setup full play/pause button
        setupFullPlayPauseButton();

        // Get image height after layout is drawn
        collapsibleImageContainer.post(new Runnable() {
            @Override
            public void run() {
                imageHeight = collapsibleImageContainer.getHeight();
            }
        });

        // Load playlist image
        loadPlaylistImage();

        // Load playlist songs
        loadPlaylistSongs();

        // Update toolbar content if something is playing
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio != null) {
            updateToolbarContent();
            updatePlayPauseButtonUI();
        }



// In onCreateView, after initializing other views:
        btnSearch = view.findViewById(R.id.searchEditText);

        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                openSearchFragment();
            });
        }
        return view;
    }

    // In DownloadPlaylistSongsFragment.java, update the openSearchFragment method:
    private void openSearchFragment() {
        Log.d(TAG, "Opening search fragment for playlist: " + playlistId);

        if (playlistId != null) {
            SearchDownloadedFragment searchFragment =
                    SearchDownloadedFragment.newInstance(playlistId);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, searchFragment)
                    .addToBackStack("search")
                    .commit();
        } else {
            Log.e(TAG, "Cannot open search - playlistId is null");
            Toast.makeText(requireContext(), "Cannot open search", Toast.LENGTH_SHORT).show();
        }
    }

    // Add this method to DownloadPlaylistSongsFragment.java
    public void onSongDeletedFromSearch(AudioModel deletedSong) {
        Log.d(TAG, "Song deleted from search: " + deletedSong.getAudioName());

        // Find and remove the song from the main list
        int position = -1;
        for (int i = 0; i < songsList.size(); i++) {
            if (songsList.get(i).getAudioUrl().equals(deletedSong.getAudioUrl())) {
                position = i;
                break;
            }
        }

        if (position != -1) {
            songsList.remove(position);
            songsAdapter.notifyItemRemoved(position);
            updateSongCount();
        }
    }

    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            Log.d(TAG, "Popping back stack");
            getParentFragmentManager().popBackStack();
        } else if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    private void setupToolbar() {
        if (toolbar == null) return;


        toolbar.setOnClickListener(v -> navigateBack());

        // Setup toolbar click listener
        toolbar.setOnClickListener(v -> {
            // Scroll to top when toolbar is clicked
            if (nestedScrollView != null) {
                nestedScrollView.smoothScrollTo(0, 0);
            }
        });
    }

    private void setupScrollListener() {
        if (nestedScrollView == null || collapsibleImageContainer == null) return;

        nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY,
                                       int oldScrollX, int oldScrollY) {

                // Initialize sizes if needed
                if (maxImageSize == 0 && collapsibleImageContainer.getHeight() > 0) {
                    maxImageSize = collapsibleImageContainer.getHeight();
                    minImageSize = (int) (maxImageSize * 0.4f); // Reduce to 40% of original
                }

                // Only process vertical scrolling
                if (scrollY != oldScrollY) {
                    // Calculate scroll progress based on vertical scroll
                    float scrollProgress = Math.min(1f, (float) Math.abs(scrollY) / 500);

                    // Apply smooth size reduction
                    applySmoothSizeReduction(scrollProgress, scrollY);

                    // Handle control buttons visibility based on scroll
                    handleControlButtonsVisibility(scrollY, oldScrollY);

                    // Update FAB visibility
                    updateFABVisibility();
                }
            }
        });
    }

    private void applySmoothSizeReduction(float progress, int scrollY) {
        if (collapsibleImageContainer == null || maxImageSize == 0) return;

        try {
            // Calculate new size based on scroll progress
            int newSize = (int) (maxImageSize - (progress * (maxImageSize - minImageSize)));

            // Ensure size doesn't go below minimum
            newSize = Math.max(newSize, minImageSize);

            // Get current layout parameters
            ViewGroup.LayoutParams params = collapsibleImageContainer.getLayoutParams();

            // Only update if size actually changed
            if (params.height != newSize) {
                params.height = newSize;
                params.width = newSize;
                collapsibleImageContainer.setLayoutParams(params);

                // Force centering
                collapsibleImageContainer.setTranslationX(0);
                collapsibleImageContainer.setTranslationY(0);
                collapsibleImageContainer.requestLayout();
            }

            // Handle toolbar visibility and status bar color based on progress
            handleToolbarVisibility(progress);

            // Update FAB visibility
            updateFABVisibility();

        } catch (Exception e) {
            Log.e(TAG, "Error in smooth size reduction: " + e.getMessage());
        }
    }

    private void handleControlButtonsVisibility(int scrollY, int oldScrollY) {
        if (linearLayout3 == null) return;

        // Define scroll threshold
        int scrollThreshold = 760; // Scroll 760px to hide controls

        // Scrolling down
        if (scrollY > oldScrollY && scrollY > scrollThreshold) {
            // User is scrolling down and passed threshold - hide controls
            if (linearLayout3.getVisibility() == View.VISIBLE) {
                animateControlButtons(false);
                Log.d(TAG, "Animating control buttons hidden - scrolling down");
            }
        }
        // Scrolling up
        else if (scrollY < oldScrollY && scrollY < scrollThreshold) {
            // User is scrolling up and below threshold - show controls
            if (linearLayout3.getVisibility() != View.VISIBLE) {
                animateControlButtons(true);
                Log.d(TAG, "Animating control buttons shown - scrolling up");
            }
        }

        // Also hide controls if we're at the very bottom
        View contentView = nestedScrollView.getChildAt(0);
        if (contentView != null) {
            int contentHeight = contentView.getHeight();
            int scrollViewHeight = nestedScrollView.getHeight();

            // Check if scrolled to bottom
            if (scrollY + scrollViewHeight >= contentHeight - 100) {
                if (linearLayout3.getVisibility() == View.VISIBLE) {
                    animateControlButtons(false);
                    Log.d(TAG, "Animating control buttons hidden - at bottom");
                }
            }
        }
    }

    private void animateControlButtons(boolean show) {
        if (linearLayout3 == null) return;

        // Cancel any ongoing animations
        linearLayout3.animate().cancel();

        if (show) {
            // Show immediately without animation
            linearLayout3.setVisibility(View.VISIBLE);
            linearLayout3.setAlpha(1f);
            linearLayout3.setTranslationY(0f);
            updateFABVisibility();
            Log.d(TAG, "Controls shown immediately");
        } else {
            // Hide immediately without animation
            linearLayout3.setVisibility(View.INVISIBLE);
            linearLayout3.setAlpha(0f);
            linearLayout3.setTranslationY(0f);
            updateFABVisibility();
            Log.d(TAG, "Controls hidden immediately");
        }
    }

    private void handleToolbarVisibility(float progress) {
        if (progress >= HIDE_THRESHOLD && !isToolbarVisible) {
            // Image is almost hidden - show toolbar
            showToolbar();

            // IMAGE HIDDEN - Set status bar to TOOLBAR GRADIENT COLOR
            isImageVisible = false;
            if (toolbarGradientColor != -1) {
                setStatusBarColor(toolbarGradientColor);
                Log.d(TAG, "Image HIDDEN - Setting status bar to TOOLBAR GRADIENT color: " +
                        Integer.toHexString(toolbarGradientColor));
            } else {
                // Fallback if toolbar color not set
                setStatusBarColor(getResources().getColor(R.color.dark_gray));
            }

            // Update FAB visibility
            updateFABVisibility();

        } else if (progress < HIDE_THRESHOLD && isToolbarVisible) {
            // Image is becoming visible - hide toolbar
            hideToolbar();

            // IMAGE VISIBLE - Set status bar to IMAGE GRADIENT COLOR
            isImageVisible = true;
            if (imageGradientColor != -1) {
                setStatusBarColor(imageGradientColor);
                Log.d(TAG, "Image VISIBLE - Setting status bar to IMAGE GRADIENT color: " +
                        Integer.toHexString(imageGradientColor));
            } else {
                // Fallback if image color not set
                setStatusBarColor(getResources().getColor(R.color.medium_gray));
            }

            // Hide FAB when toolbar hides
            if (toolbarPlayPause != null) {
                toolbarPlayPause.hide();
            }
        }
    }

    private void updateFABVisibility() {
        if (toolbarPlayPause == null || linearLayout3 == null) return;

        // Check if this playlist is currently playing
        boolean isPlaylistPlaying = isAnySongFromThisPlaylistPlaying();

        // Show FAB only when:
        // 1. Toolbar is visible
        // 2. Control buttons are hidden
        // 3. AND this playlist is playing (EXACTLY like btnFullPlayPause)
        if (isToolbarVisible && linearLayout3.getVisibility() != View.VISIBLE ) {
            // All conditions met - show FAB
            if (toolbarPlayPause.getVisibility() != View.VISIBLE) {
                toolbarPlayPause.show();
                Log.d(TAG, "FAB shown - playlist playing & toolbar visible");
            }
        } else {
            // Any condition not met - hide FAB
            if (toolbarPlayPause.getVisibility() == View.VISIBLE) {
                toolbarPlayPause.hide();
                Log.d(TAG, "FAB hidden - playlist playing: " + isPlaylistPlaying);
            }
        }
    }

    private void showToolbar() {
        if (toolbarLayout == null || isToolbarVisible) return;

        isToolbarVisible = true;

        // Animate toolbar appearing
        toolbarLayout.setVisibility(View.VISIBLE);
        toolbarLayout.setAlpha(0f);
        toolbarLayout.animate()
                .alpha(1f)
                .setDuration(300)
                .start();

        // Update toolbar content
        updateToolbarContent();
    }

    private void hideToolbar() {
        if (toolbarLayout == null || !isToolbarVisible) return;

        isToolbarVisible = false;

        // Animate toolbar disappearing
        toolbarLayout.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> toolbarLayout.setVisibility(View.INVISIBLE))
                .start();
    }

    private void updateToolbarContent() {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio != null && toolbarSongName != null) {
            // Set playlist name with max length of 15 characters
            String displayName = playlistName != null ? playlistName : "Playlist";
            setToolbarSongNameWithMaxLength(displayName, 20);

            toolbarSongArtist.setText(currentAudio.getcategoryName() != null ?
                    currentAudio.getcategoryName() : "New");

            // Load album art into toolbar
            if (playlistImageUrl != null && !playlistImageUrl.isEmpty()) {
                Glide.with(requireContext())
                        .load(playlistImageUrl)
                        .placeholder(R.drawable.video_placholder)
                        .into(toolbarAlbumArt);
            } else if (ivPlaylistBackground != null && ivPlaylistBackground.getDrawable() != null) {
                toolbarAlbumArt.setImageDrawable(ivPlaylistBackground.getDrawable());
            }

            // Check if this playlist is playing
            boolean isPlaylistPlaying = isAnySongFromThisPlaylistPlaying();

            // Update play/pause button state - ONLY if this playlist is playing
            if (isPlaylistPlaying) {
                if (PlayerManager.isPlaying()) {
                    toolbarPlayPause.setImageResource(R.drawable.pause);
                } else {
                    toolbarPlayPause.setImageResource(R.drawable.play_player);
                }
            } else {
                // If different playlist is playing, set to play icon (like btnFullPlayPause)
                toolbarPlayPause.setImageResource(R.drawable.play_player);
            }

            // Update visibility based on playlist playing state
            updateFABVisibility();
        }
    }

    private void setToolbarSongNameWithMaxLength(String songName, int maxLength) {
        if (toolbarSongName == null) return;

        if (songName != null && songName.length() > maxLength) {
            // Truncate and add ellipsis (3 dots)
            String truncated = songName.substring(0, maxLength - 3) + "...";
            toolbarSongName.setText(truncated);
        } else {
            toolbarSongName.setText(songName);
        }

        // Backup settings to ensure single line with ellipsis
        toolbarSongName.setMaxLines(1);
        toolbarSongName.setEllipsize(android.text.TextUtils.TruncateAt.END);
    }
    /**
     * Get the current playlist's category ID
     */
    private String getPlaylistCategoryId() {
        if (playlist != null) {
            String categoryId = playlist.getOriginalCategoryId();
            if (categoryId != null && !categoryId.isEmpty()) {
                Log.d(TAG, "Playlist category ID from playlist: " + categoryId);
                return categoryId;
            }
        }

        if (songsList != null && !songsList.isEmpty()) {
            // First try to get from first song
            AudioModel firstSong = songsList.get(0);
            if (firstSong != null && firstSong.getCategoryId() != null &&
                    !firstSong.getCategoryId().isEmpty()) {
                Log.d(TAG, "Playlist category ID from first song: " + firstSong.getCategoryId());
                return firstSong.getCategoryId();
            }

            // If first song doesn't have category ID, check all songs
            for (AudioModel song : songsList) {
                if (song.getCategoryId() != null && !song.getCategoryId().isEmpty()) {
                    Log.d(TAG, "Playlist category ID from song: " + song.getCategoryId());
                    return song.getCategoryId();
                }
            }
        }
        Log.d(TAG, "No playlist category ID found");
        return "";
    }

    /**
     * Check if ANY song from this playlist is currently playing
     * ONLY by comparing category IDs - NO FALLBACKS
     */
    private boolean isAnySongFromThisPlaylistPlaying() {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) {
            Log.d(TAG, "No current audio playing");
            return false;
        }

        // Get current playing song's category ID
        String currentCategoryId = currentAudio.getCategoryId();
        Log.d(TAG, "Current playing song: " + currentAudio.getAudioName());
        Log.d(TAG, "Current song category ID: " + currentCategoryId);

        // Get THIS playlist's category ID
        String thisPlaylistCategoryId = getPlaylistCategoryId();
        Log.d(TAG, "This playlist category ID: " + thisPlaylistCategoryId);

        // ONLY CHECK: Compare category IDs
        if (currentCategoryId != null && thisPlaylistCategoryId != null &&
                !currentCategoryId.isEmpty() && !thisPlaylistCategoryId.isEmpty()) {

            boolean match = currentCategoryId.equals(thisPlaylistCategoryId);
            Log.d(TAG, "Category ID match: " + match);

            if (match) {
                Log.d(TAG, "✓ EXACT MATCH - song belongs to this playlist");
                return true;
            } else {
                Log.d(TAG, "✗ Category IDs don't match - different playlist");
                return false;
            }
        }

        // If either category ID is missing, assume not playing from this playlist
        Log.d(TAG, "✗ Missing category IDs - cannot verify");
        return false;
    }

    /**
     * Update the play/pause button UI based on playlist match
     */
    private void updatePlayPauseButtonUI() {
        if (btnFullPlayPause == null) return;

        boolean isPlaylistPlaying = isAnySongFromThisPlaylistPlaying();

        requireActivity().runOnUiThread(() -> {
            if (isPlaylistPlaying) {
                // EXACT category ID match - current song is from this playlist
                btnFullPlayPause.setVisibility(View.VISIBLE);
                if (PlayerManager.isPlaying()) {
                    btnFullPlayPause.setImageResource(R.drawable.pause);
                    Log.d(TAG, "Button: Show PAUSE (this playlist playing - EXACT MATCH)");
                } else {
                    btnFullPlayPause.setImageResource(R.drawable.play_player);
                    Log.d(TAG, "Button: Show PLAY (this playlist paused)");
                }
            } else {
                // No EXACT category ID match - different playlist
                btnFullPlayPause.setVisibility(View.VISIBLE);
                btnFullPlayPause.setImageResource(R.drawable.play_player);
                Log.d(TAG, "Button: Show PLAY (start this playlist - NO MATCH)");
            }

            // Update toolbar play/pause button based on playlist match
            if (toolbarPlayPause != null) {
                if (isPlaylistPlaying) {
                    // This playlist is playing - set appropriate icon
                    if (PlayerManager.isPlaying()) {
                        toolbarPlayPause.setImageResource(R.drawable.pause);
                    } else {
                        toolbarPlayPause.setImageResource(R.drawable.play_player);
                    }
                } else {
                    // Different playlist - always show play icon
                    toolbarPlayPause.setImageResource(R.drawable.play_player);
                }

                // Update FAB visibility based on playlist playing state
                updateFABVisibility();
            }
        });
    }

    /**
     * Setup the full play/pause button click listener
     */
    private void setupFullPlayPauseButton() {
        btnFullPlayPause.setOnClickListener(v -> {
            boolean isPlaylistPlaying = isAnySongFromThisPlaylistPlaying();

            if (isPlaylistPlaying) {
                // EXACT match - toggle play/pause for current playlist song
                if (PlayerManager.isPlaying()) {
                    PlayerManager.pausePlayback();
                    btnFullPlayPause.setImageResource(R.drawable.play_player);
                    if (toolbarPlayPause != null) {
                        toolbarPlayPause.setImageResource(R.drawable.play_player);
                    }
                    Log.d(TAG, "Playlist paused");
                } else {
                    PlayerManager.resumePlayback();
                    btnFullPlayPause.setImageResource(R.drawable.pause);
                    if (toolbarPlayPause != null) {
                        toolbarPlayPause.setImageResource(R.drawable.pause);
                    }
                    Log.d(TAG, "Playlist resumed");
                }
            } else {
                // No EXACT match - play first downloaded song from this playlist
                Log.d(TAG, "No matching playlist playing, playing first song from this playlist");
                playFirstSongFromPlaylist();
            }
            broadcastSongChanged();
        });
    }

    /**
     * Play the first downloaded song from this playlist
     */
    private void playFirstSongFromPlaylist() {
        if (songsList == null || songsList.isEmpty()) {
            Log.d(TAG, "Cannot play first song - playlist is empty");
            Toast.makeText(requireContext(), "No songs in this playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find the first downloaded song with valid file
        AudioModel firstValidSong = null;
        for (AudioModel song : songsList) {
            if (song.isDownloaded() && song.getDownloadPath() != null && !song.getDownloadPath().isEmpty()) {
                java.io.File audioFile = new java.io.File(song.getDownloadPath());
                if (audioFile.exists()) {
                    firstValidSong = song;
                    Log.d(TAG, "Found first valid downloaded song: " + song.getAudioName() +
                            " at index: " + songsList.indexOf(song));
                    break;
                } else {
                    Log.w(TAG, "Song marked as downloaded but file missing: " + song.getDownloadPath());
                }
            }
        }

        if (firstValidSong == null) {
            Log.d(TAG, "No valid downloaded songs found in playlist");
            Toast.makeText(requireContext(), "No downloaded songs available", Toast.LENGTH_SHORT).show();
            btnFullPlayPause.setImageResource(R.drawable.play_player);
            return;
        }

        // Update button UI immediately
        btnFullPlayPause.setImageResource(R.drawable.pause);
        if (toolbarPlayPause != null) {
            toolbarPlayPause.setImageResource(R.drawable.pause);
        }

        // Play the song
        playSong(firstValidSong, null);
    }

    private void loadPlaylistSongs() {
        Log.d(TAG, "loadPlaylistSongs called - playlistId: " + playlistId);

        if (playlistId == null) {
            Log.e(TAG, "playlistId is null! Cannot load songs.");
            return;
        }

        // Get playlist
        playlist = playlistManager.getPlaylistById(playlistId);
        if (playlist == null) {
            Log.e(TAG, "Playlist not found for ID: " + playlistId);
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        Log.d(TAG, "Playlist found: " + playlist.getName() +
                ", ID: " + playlist.getId() +
                ", Category ID: " + playlist.getOriginalCategoryId());

        // Use passed playlist name if available, otherwise use from playlist object
        if (playlistName != null && !playlistName.isEmpty()) {
            tvTitle.setText(playlistName);
            Log.d(TAG, "Title set from arguments: " + playlistName);
        } else {
            tvTitle.setText(playlist.getName());
            Log.d(TAG, "Title set from playlist object: " + playlist.getName());
        }

        // Get songs
        songsList.clear();
        ArrayList<AudioModel> songs = playlist.getSongs();

        Log.d(TAG, "Retrieved songs from playlist - Count: " +
                (songs != null ? songs.size() : 0));

        if (songs != null && !songs.isEmpty()) {
            songsList.addAll(songs);

            // Log each song's details
            for (int i = 0; i < songsList.size(); i++) {
                AudioModel song = songsList.get(i);
                Log.d(TAG, "Song " + i + ": " + song.getAudioName() +
                        " | Artist: " + song.getcategoryName() +
                        " | Category ID: " + song.getCategoryId() +
                        " | Downloaded: " + song.isDownloaded() +
                        " | Path: " + song.getDownloadPath());
            }

            // Update song count
            updateSongCount();

            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);

            // Update adapter
            songsAdapter.notifyDataSetChanged();

            // Update play/pause button UI
            updatePlayPauseButtonUI();

            Log.d(TAG, "Successfully loaded " + songsList.size() + " songs. UI updated.");
        } else {
            Log.w(TAG, "Playlist is empty or songs list is null");
            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("No songs in this playlist");
            tvSongCount.setText("0 songs");
        }
    }

    private void loadPlaylistImage() {
        Log.d(TAG, "loadPlaylistImage called - Image URL: " + playlistImageUrl);

        String imageUrl = null;

        // Determine which image URL to use
        if (playlistImageUrl != null && !playlistImageUrl.isEmpty()) {
            imageUrl = playlistImageUrl;
            Log.d(TAG, "Using passed playlist image URL");
        } else if (playlist != null && playlist.getImageUrl() != null && !playlist.getImageUrl().isEmpty()) {
            imageUrl = playlist.getImageUrl();
            Log.d(TAG, "Using playlist object image URL");
        } else if (songsList != null && !songsList.isEmpty()) {
            AudioModel firstSong = songsList.get(0);
            if (firstSong != null && firstSong.getImageUrl() != null && !firstSong.getImageUrl().isEmpty()) {
                imageUrl = firstSong.getImageUrl();
                Log.d(TAG, "Using first song's image URL");
            }
        }

        if (imageUrl != null) {
            Log.d(TAG, "Loading image from URL: " + imageUrl);

            Glide.with(requireContext())
                    .asBitmap()
                    .load(imageUrl)
                    .placeholder(R.drawable.video_placholder)
                    .error(R.drawable.video_placholder)
                    .centerCrop()
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                            // Set the image
                            ivPlaylistBackground.setImageBitmap(bitmap);

                            // Also set toolbar album art if available
                            if (toolbarAlbumArt != null) {
                                toolbarAlbumArt.setImageBitmap(bitmap);
                            }

                            // Generate and apply dynamic background colors
                            generateDynamicBackground(bitmap);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            ivPlaylistBackground.setImageDrawable(placeholder);
                            if (toolbarAlbumArt != null) {
                                toolbarAlbumArt.setImageDrawable(placeholder);
                            }
                            setDefaultGradientBackground();
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            ivPlaylistBackground.setImageDrawable(errorDrawable);
                            if (toolbarAlbumArt != null) {
                                toolbarAlbumArt.setImageDrawable(errorDrawable);
                            }
                            setDefaultGradientBackground();
                        }
                    });
        } else {
            Log.d(TAG, "No image URL found, using default placeholder");
            ivPlaylistBackground.setImageResource(R.drawable.video_placholder);
            if (toolbarAlbumArt != null) {
                toolbarAlbumArt.setImageResource(R.drawable.video_placholder);
            }
            setDefaultGradientBackground();
        }
    }

    private void generateDynamicBackground(Bitmap bitmap) {
        if (bitmap == null || !isAdded()) return;

        Palette.from(bitmap).generate(palette -> {
            if (!isAdded() || getContext() == null) return;

            int defaultColor = getResources().getColor(R.color.bgblack);

            // Get colors from palette
            int darkVibrantColor = palette.getDarkVibrantColor(defaultColor);
            int darkMutedColor = palette.getDarkMutedColor(defaultColor);
            int mutedColor = palette.getMutedColor(defaultColor);
            int vibrantColor = palette.getVibrantColor(defaultColor);
            int lightVibrantColor = palette.getLightVibrantColor(defaultColor);
            int lightMutedColor = palette.getLightMutedColor(defaultColor);

            // Select a MID/LIGHT color for the top (preferring lighter colors)
            int topColor = getMidLightColor(new int[]{
                    lightVibrantColor,
                    lightMutedColor,
                    vibrantColor,
                    mutedColor,
                    darkVibrantColor,
                    darkMutedColor
            }, defaultColor);

            // Ensure top color is not too dark
            topColor = ensureNotTooDark(topColor, getResources().getColor(R.color.medium_gray));

            // Use BLACK or VERY DARK color for the bottom
            int bottomColor = getResources().getColor(R.color.bgblack); // Pure black

            // Alternatively, you can darken the top color significantly:
            // int bottomColor = darkenColor(topColor, 0.85f); // Darken by 85%

            // ENSURE IMAGE GRADIENT COLOR IS DARK ENOUGH FOR STATUS BAR
            imageGradientColor = ensureDarkEnough(topColor);

            // Create main gradient with LIGHT/MID top and BLACK bottom
            GradientDrawable mainGradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{topColor, bottomColor}
            );

            // Apply main gradient
            applyGradientBackground(mainGradient);

            // Create a slightly darker gradient for toolbar layouts
            int toolbarTopColor = darkenColor(topColor, 0.3f); // Darken top color by 30% for toolbar
            int toolbarBottomColor = bottomColor;

            GradientDrawable toolbarGradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{toolbarTopColor, toolbarTopColor}
            );

            // Apply toolbar gradient
            if (toolbarLayout != null) {
                toolbarLayout.setBackground(toolbarGradient);
            }

            // STORE THE TOOLBAR GRADIENT COLOR
            toolbarGradientColor = ensureDarkEnough(toolbarTopColor);

            // SET STATUS BAR COLOR - Use darkened top color when fragment opens
            if (isImageVisible) {
                setStatusBarColor(imageGradientColor);
                Log.d(TAG, "Fragment opened - Setting status bar to TOP GRADIENT color: " +
                        Integer.toHexString(imageGradientColor));
            }

            // Adjust text colors based on palette
            adjustTextColors(palette);

            Log.d(TAG, "Gradients applied. Top color: " + Integer.toHexString(topColor) +
                    " (status bar: " + Integer.toHexString(imageGradientColor) +
                    "), Bottom color: " + Integer.toHexString(bottomColor));
        });
    }

    /**
     * Select a MID/LIGHT color from the palette (not too dark, not too bright)
     */
    private int getMidLightColor(int[] colors, int defaultColor) {
        for (int color : colors) {
            // Check if color is in the mid-light range (brightness between 0.4 and 0.8)
            if (!isNearBlack(color) && !isTooBright(color)) {
                return color;
            }
        }
        // If no suitable mid-light color found, try light colors
        for (int color : colors) {
            if (!isNearBlack(color)) {
                return color;
            }
        }
        return defaultColor;
    }

    /**
     * Darken a color by a specified factor (0.0 - 1.0)
     */
    private int darkenColor(int color, float factor) {
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(color, hsv);
        hsv[2] = hsv[2] * (1 - factor);
        return android.graphics.Color.HSVToColor(hsv);
    }

    /**
     * Ensure color is not too dark (minimum brightness)
     */
    private int ensureNotTooDark(int color, int fallbackColor) {
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(color, hsv);

        // If brightness is too low (less than 0.3), increase it
        if (hsv[2] < 0.3f) {
            hsv[2] = 0.4f; // Set to minimum acceptable brightness
            return android.graphics.Color.HSVToColor(hsv);
        }
        return color;
    }

    private int ensureDarkEnough(int color) {
        // Calculate brightness
        double brightness = (0.299 * android.graphics.Color.red(color) +
                0.587 * android.graphics.Color.green(color) +
                0.114 * android.graphics.Color.blue(color)) / 255;

        // If color is too bright (brightness > 0.6), darken it
        if (brightness > 0.6) {
            float[] hsv = new float[3];
            android.graphics.Color.colorToHSV(color, hsv);
            // Reduce brightness to 40%
            hsv[2] = hsv[2] * 0.4f;
            int darkenedColor = android.graphics.Color.HSVToColor(hsv);
            Log.d(TAG, "Darkened color from " + Integer.toHexString(color) +
                    " to " + Integer.toHexString(darkenedColor));
            return darkenedColor;
        }
        return color;
    }


    private void applyGradientBackground(GradientDrawable gradient) {
        if (getView() == null) return;

        View rootView = getView();

        // Apply to fragment background
        rootView.setBackground(gradient);



        // Apply to scroll container
        if (mainlayout != null) {
            mainlayout.setBackground(gradient);
        }


        // Apply to appBarLayout
        if (toolbarLayout != null) {
            toolbarLayout.setBackground(gradient);
        }


        Log.d(TAG, "Gradient applied to toolbar layouts");
    }

    private int getNonBlackColor(int[] colors, int defaultColor) {
        for (int color : colors) {
            if (!isNearBlack(color)) {
                return color;
            }
        }
        return defaultColor;
    }

    private void setDefaultGradientBackground() {
        if (getView() == null) return;

        // Use mid-light color for top, black for bottom
        int topColor = getResources().getColor(R.color.medium_gray); // Mid gray
        int bottomColor = getResources().getColor(R.color.bgblack);  // Black

        GradientDrawable defaultGradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{topColor, bottomColor}
        );

        applyGradientBackground(defaultGradient);

        // SET DEFAULT STATUS BAR COLOR
        setStatusBarColor(topColor);

        // Create default toolbar gradient
        GradientDrawable defaultToolbarGradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{darkenColor(topColor, 0.3f), bottomColor}
        );

        if (toolbarLayout != null) {
            toolbarLayout.setBackground(defaultToolbarGradient);
        }
    }


    private void setStatusBarColor(int color) {
        if (getActivity() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Ensure color is dark enough
            color = ensureDarkEnough(color);

            getActivity().getWindow().setStatusBarColor(color);

            // Set light or dark status bar icons based on background color
            setStatusBarTextColor(color);
        }
    }

    private void setStatusBarTextColor(int backgroundColor) {
        if (getActivity() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = getActivity().getWindow().getDecorView();

            // Check if background is dark (use light icons) or light (use dark icons)
            double brightness = (0.299 * android.graphics.Color.red(backgroundColor) +
                    0.587 * android.graphics.Color.green(backgroundColor) +
                    0.114 * android.graphics.Color.blue(backgroundColor)) / 255;

            if (brightness < 0.5) {
                // Dark background - use light icons (default)
                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                // Light background - use dark icons
                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    private boolean isColorLight(int color) {
        double darkness = 1 - (0.299 * android.graphics.Color.red(color) +
                0.587 * android.graphics.Color.green(color) +
                0.114 * android.graphics.Color.blue(color)) / 255;
        return darkness < 0.5;
    }


    private void adjustTextColors(Palette palette) {
        if (tvTitle == null || tvSongCount == null) return;

        // Get a light color from palette for text
        int defaultTextColor = getResources().getColor(android.R.color.white);
        int lightVibrant = palette.getLightVibrantColor(defaultTextColor);
        int lightMuted = palette.getLightMutedColor(defaultTextColor);

        int textColor = defaultTextColor;

        // Choose a light color that's not too bright
        if (!isTooBright(lightVibrant)) {
            textColor = lightVibrant;
        } else if (!isTooBright(lightMuted)) {
            textColor = lightMuted;
        }

        // Apply text colors
        tvTitle.setTextColor(textColor);
        tvSongCount.setTextColor(textColor);

        // Optionally adjust other text views
        if (tvEmptyState != null) {
            tvEmptyState.setTextColor(textColor);
        }
    }

    private boolean isNearBlack(int color) {
        int r = android.graphics.Color.red(color);
        int g = android.graphics.Color.green(color);
        int b = android.graphics.Color.blue(color);

        double brightness = (r * 0.299 + g * 0.587 + b * 0.114) / 255;
        return brightness < 0.2;
    }

    private boolean isTooBright(int color) {
        int r = android.graphics.Color.red(color);
        int g = android.graphics.Color.green(color);
        int b = android.graphics.Color.blue(color);

        double brightness = (r * 0.299 + g * 0.587 + b * 0.114) / 255;
        return brightness > 0.8;
    }

    private void updateSongCount() {
        int totalSongs = songsList.size();
        int downloadedSongs = 0;

        Log.d(TAG, "updateSongCount - Total songs: " + totalSongs);

        for (AudioModel song : songsList) {
            if (song.isDownloaded()) {
                downloadedSongs++;
                Log.v(TAG, "Downloaded song found: " + song.getAudioName());
            }
        }

        String countText = totalSongs + " songs";
        if (downloadedSongs > 0) {
            countText += " • " + downloadedSongs + " downloaded";
        }

        tvSongCount.setText(countText);
        Log.d(TAG, "Song count updated: " + countText);
    }

    private void playSong(AudioModel song, android.widget.ProgressBar progressBar) {
        Log.d(TAG, "playSong called - Song: " + song.getAudioName() +
                ", Downloaded: " + song.isDownloaded() +
                ", ProgressBar: " + (progressBar != null) +
                ", Category ID: " + song.getCategoryId());

        // Show progress bar if available
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Save the song WITH category ID
        saveSongWithCategoryId(song);

        // Check if song is downloaded and has valid path
        if (song.isDownloaded() && song.getDownloadPath() != null && !song.getDownloadPath().isEmpty()) {
            // Verify file exists
            java.io.File audioFile = new java.io.File(song.getDownloadPath());
            if (audioFile.exists()) {
                Log.d(TAG, "Playing downloaded song from: " + song.getDownloadPath());
                playDownloadedSong(song, progressBar);
            } else {
                // File doesn't exist, update download status
                Log.e(TAG, "File marked as downloaded but doesn't exist: " + song.getDownloadPath());
                song.setDownloaded(false);
                song.setDownloadPath(null);

                // Show error
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                Toast.makeText(requireContext(), "Audio file missing", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Song is not downloaded - show error
            Log.e(TAG, "Song not downloaded locally: " + song.getAudioName());
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            Toast.makeText(requireContext(), "This song is not downloaded", Toast.LENGTH_SHORT).show();
        }
    }

    private void playDownloadedSong(AudioModel song, android.widget.ProgressBar progressBar) {
        try {
            Log.d(TAG, "playDownloadedSong - Path: " + song.getDownloadPath());
            Log.d(TAG, "Original song category ID: " + song.getCategoryId());
            Log.d(TAG, "Playlist category ID: " + (playlist != null ? playlist.getOriginalCategoryId() : "null"));

            // Validate that the song has a valid download path
            String filePath = song.getDownloadPath();
            if (filePath == null || filePath.isEmpty()) {
                Log.e(TAG, "No download path available for song: " + song.getAudioName());
                if (progressBar != null) {
                    requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                }
                Toast.makeText(requireContext(), "Song not downloaded locally", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if file exists
            java.io.File audioFile = new java.io.File(filePath);
            if (!audioFile.exists()) {
                Log.e(TAG, "File doesn't exist at path: " + filePath);
                if (progressBar != null) {
                    requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                }
                Toast.makeText(requireContext(), "Audio file not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "File exists, size: " + audioFile.length() + " bytes");

            // Create a copy of the song with the playlist's category ID
            AudioModel updatedSong = new AudioModel(song);

            // Set the playlist's category ID
            String playlistCategoryId = playlist != null ? playlist.getOriginalCategoryId() : "";
            updatedSong.setCategoryId(playlistCategoryId);

            // Make sure download path is preserved
            updatedSong.setDownloadPath(filePath);
            updatedSong.setDownloaded(true);

            Log.d(TAG, "Updated song category ID for playback: " + updatedSong.getCategoryId());

            // Create a copy of songsList for offline playback WITH UPDATED CATEGORY IDs
            ArrayList<AudioModel> offlineSongsList = new ArrayList<>();
            for (AudioModel s : songsList) {
                // Only add songs that are downloaded and have valid paths
                if (s.isDownloaded() && s.getDownloadPath() != null && !s.getDownloadPath().isEmpty()) {
                    // Check if file actually exists
                    java.io.File sFile = new java.io.File(s.getDownloadPath());
                    if (sFile.exists()) {
                        // Create copy with playlist category ID
                        AudioModel songCopy = new AudioModel(s);
                        songCopy.setCategoryId(playlistCategoryId);
                        songCopy.setDownloaded(true);
                        offlineSongsList.add(songCopy);

                        Log.d(TAG, "Added to offline list: " + songCopy.getAudioName() +
                                ", Category ID: " + songCopy.getCategoryId() +
                                ", Path: " + songCopy.getDownloadPath());
                    } else {
                        Log.w(TAG, "Song marked as downloaded but file missing: " + s.getAudioName());
                    }
                }
            }

            // If no downloaded songs, show error
            if (offlineSongsList.isEmpty()) {
                Log.e(TAG, "No valid downloaded songs in playlist");
                if (progressBar != null) {
                    requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                }
                Toast.makeText(requireContext(), "No downloaded songs available", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Created offline playlist with " + offlineSongsList.size() + " songs");

            // Find the song to play in the offline list
            AudioModel songToPlay = null;
            for (AudioModel s : offlineSongsList) {
                if (s.getAudioUrl().equals(song.getAudioUrl())) {
                    songToPlay = s;
                    break;
                }
            }

            if (songToPlay == null) {
                // If not found by URL, try by file path
                for (AudioModel s : offlineSongsList) {
                    if (s.getDownloadPath() != null && s.getDownloadPath().equals(filePath)) {
                        songToPlay = s;
                        break;
                    }
                }
            }

            if (songToPlay == null) {
                songToPlay = updatedSong;
                offlineSongsList.add(songToPlay);
            }

            // Set the OFFLINE playlist with downloaded songs
            PlayerManager.setAudioList(offlineSongsList, PlayerManager.PlaylistType.OFFLINE);

            // Save the song WITH category ID to SharedPreferences
            saveSongWithCategoryId(songToPlay);

            // Use playOfflineAudio - this will use ONLY file path, no URL
            AudioModel finalSongToPlay = songToPlay;
            PlayerManager.playOfflineAudio(songToPlay, () -> {
                // Callback when prepared
                if (progressBar != null) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                    });
                }

                // Update UI
                requireActivity().runOnUiThread(() -> {
                    songsAdapter.notifyDataSetChanged();
                    updatePlayPauseButtonUI();
                    updateToolbarContent();
                });

                // Broadcast song change
                broadcastSongChanged();

                Log.d(TAG, "Offline song playback started successfully: " + finalSongToPlay.getAudioName() +
                        ", Category ID: " + finalSongToPlay.getCategoryId() +
                        ", Path: " + finalSongToPlay.getDownloadPath() +
                        ", Offline playlist size: " + offlineSongsList.size());
            });

        } catch (Exception e) {
            Log.e(TAG, "Error playing downloaded song: " + e.getMessage(), e);

            // Hide progress bar on error
            if (progressBar != null) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                });
            }

            // Show error toast
            Toast.makeText(requireContext(), "Error playing song: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to save song with category ID
    private void saveSongWithCategoryId(AudioModel song) {
        if (getContext() != null) {
            android.content.SharedPreferences prefs = getContext().getSharedPreferences("SavedAudio", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();

            editor.putString("audioName", song.getAudioName());
            editor.putString("audioUrl", song.getAudioUrl());
            editor.putString("imageUrl", song.getImageUrl());
            editor.putString("audioArtist", song.getcategoryName());

            // Save the category ID
            String categoryId = song.getCategoryId();
            if (categoryId != null && !categoryId.isEmpty()) {
                editor.putString("audioCategoryId", categoryId);
                Log.d(TAG, "Saved song with category ID: " + categoryId);
            } else {
                // Try to get category ID from playlist
                String playlistCategoryId = playlist != null ? playlist.getOriginalCategoryId() : "";
                if (playlistCategoryId != null && !playlistCategoryId.isEmpty()) {
                    editor.putString("audioCategoryId", playlistCategoryId);
                    Log.d(TAG, "Saved song with playlist category ID: " + playlistCategoryId);
                } else {
                    editor.remove("audioCategoryId");
                    Log.d(TAG, "No category ID available for song");
                }
            }

            editor.apply();
        }
    }

    private void removeSongFromPlaylist(AudioModel song, int position) {
        Log.d(TAG, "removeSongFromPlaylist called - Position: " + position +
                ", Song: " + song.getAudioName());

        if (playlist == null) {
            Log.e(TAG, "Cannot remove song - playlist is null!");
            return;
        }

        try {
            ArrayList<AudioModel> songsToRemove = new ArrayList<>();
            songsToRemove.add(song);

            Log.d(TAG, "Attempting to remove song from playlist: " + playlist.getName());

            boolean removed = playlistManager.removeSongsFromPlaylist(playlistId, songsToRemove);
            Log.d(TAG, "Remove operation result: " + removed);

            if (removed) {
                // Remove from local list
                songsList.remove(position);
                songsAdapter.notifyItemRemoved(position);
                Log.d(TAG, "Song removed from local list at position: " + position);

                // Update song count
                updateSongCount();

                // Show message
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "Removed from playlist", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Toast shown for removal");
                }

                // Show undo snackbar
                showUndoSnackbar(song, position);

                // Send update broadcast
                broadcastPlaylistUpdate();
            } else {
                Log.e(TAG, "Failed to remove song from playlist in manager");
                Toast.makeText(getContext(), "Failed to remove song", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing song from playlist: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error removing song", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUndoSnackbar(AudioModel deletedSong, int position) {
        Snackbar snackbar = Snackbar.make(recyclerView, "Song removed from playlist", Snackbar.LENGTH_LONG);
        snackbar.setAction("UNDO", v -> {
            // Restore the song to playlist
            ArrayList<AudioModel> songsToAdd = new ArrayList<>();
            songsToAdd.add(deletedSong);

            boolean restored = playlistManager.addSongsToPlaylist(playlistId, songsToAdd);

            if (restored) {
                songsList.add(position, deletedSong);
                songsAdapter.notifyItemInserted(position);

                // Update song count
                updateSongCount();

                Toast.makeText(getContext(), "Song restored to playlist", Toast.LENGTH_SHORT).show();

                // Broadcast update
                broadcastPlaylistUpdate();
            } else {
                Toast.makeText(getContext(), "Failed to restore song", Toast.LENGTH_SHORT).show();
            }
        });

        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    // Song permanently deleted, broadcast update
                    broadcastPlaylistUpdate();
                }
            }
        });

        snackbar.show();
    }

    private void broadcastPlaylistUpdate() {
        if (getContext() != null) {
            Intent updateIntent = new Intent("PLAYLIST_UPDATE");
            updateIntent.setPackage(getContext().getPackageName());
            getContext().sendBroadcast(updateIntent);
            Log.d(TAG, "PLAYLIST_UPDATE broadcast sent");
        }
    }

    private void broadcastSongChanged() {
        if (getContext() != null) {
            Intent songChangedIntent = new Intent("SONG_CHANGED");
            songChangedIntent.setPackage(getContext().getPackageName());
            getContext().sendBroadcast(songChangedIntent);
            Log.d(TAG, "SONG_CHANGED broadcast sent");
        }
    }

    private void registerPlayerUpdateReceiver() {
        if (!isReceiverRegistered && getContext() != null) {
            playerUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Log.d(TAG, "Player broadcast received: " + action);

                    if (action != null) {
                        switch (action) {
                            case "UPDATE_AUDIO_ADAPTER":
                            case "SONG_CHANGED":
                                // Update adapter when song changes
                                requireActivity().runOnUiThread(() -> {
                                    // Clear preparing progress and update UI
                                    if (songsAdapter != null) {
                                        songsAdapter.clearPreparingProgress();
                                        songsAdapter.updatePlayingIndicators();
                                    }
                                    updatePlayPauseButtonUI();
                                    updateToolbarContent();
                                    updateFABVisibility();
                                    Log.d(TAG, "UI updated due to player change");
                                });
                                break;
                        }
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction("UPDATE_AUDIO_ADAPTER");
            filter.addAction("SONG_CHANGED");
            filter.addAction(ACTION_PREPARE_NEXT);
            filter.addAction(ACTION_PREPARE_PREVIOUS);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().registerReceiver(playerUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                requireContext().registerReceiver(prepareReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                requireContext().registerReceiver(playerUpdateReceiver, filter);
                requireContext().registerReceiver(prepareReceiver, filter);
            }

            isReceiverRegistered = true;
            Log.d(TAG, "Player broadcast receivers registered");
        }
    }

    private void unregisterPlayerUpdateReceiver() {
        if (isReceiverRegistered && playerUpdateReceiver != null && getContext() != null) {
            try {
                getContext().unregisterReceiver(playerUpdateReceiver);
                getContext().unregisterReceiver(prepareReceiver);
                isReceiverRegistered = false;
                Log.d(TAG, "Player broadcast receivers unregistered");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering player receiver: " + e.getMessage());
            }
        }
    }

    private void resetStatusBarColor() {
        if (getActivity() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Get default color from theme attributes
            TypedArray typedArray = getActivity().getTheme().obtainStyledAttributes(new int[]{
                    android.R.attr.statusBarColor
            });

            int defaultColor = typedArray.getColor(0, getResources().getColor(R.color.bgblack));
            typedArray.recycle();

            getActivity().getWindow().setStatusBarColor(defaultColor);

            // Reset status bar text color
            setStatusBarTextColor(defaultColor);

            Log.d(TAG, "Status bar color reset to default from theme");
        }
    }




    @Override
    public void onResume() {
        super.onResume();

        // Set initial state
        isImageVisible = true;

        // Load songs when fragment resumes
        loadPlaylistSongs();

        // Update UI based on current playback state
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio != null) {
            updateToolbarContent();
            updatePlayPauseButtonUI();
        }

        try {
            // Register broadcast receivers
            IntentFilter filter = new IntentFilter();
            filter.addAction("DOWNLOAD_UPDATE");
            filter.addAction("PLAYLIST_UPDATE");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().registerReceiver(downloadUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                requireContext().registerReceiver(downloadUpdateReceiver, filter);
            }

            Log.d(TAG, "Download update receiver registered");

            // Register player update receiver
            registerPlayerUpdateReceiver();

        } catch (Exception e) {
            Log.e(TAG, "Error registering broadcast receivers: " + e.getMessage(), e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");

        // Reset status bar color when fragment is pausing
        resetStatusBarColor();

        try {
            // Unregister broadcast receivers
            requireContext().unregisterReceiver(downloadUpdateReceiver);
            Log.d(TAG, "Download update receiver unregistered");

            // Unregister player update receiver
            unregisterPlayerUpdateReceiver();

        } catch (Exception e) {
            Log.e(TAG, "Error unregistering broadcast receivers: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");

        // Reset status bar color before exiting
        resetStatusBarColor();

        // Clean up any ongoing animations
        if (linearLayout3 != null) {
            linearLayout3.animate().cancel();
        }
        if (toolbarLayout != null) {
            toolbarLayout.animate().cancel();
        }

        // Cancel Glide requests
        Glide.with(this).clear(ivPlaylistBackground);
        if (toolbarAlbumArt != null) {
            Glide.with(this).clear(toolbarAlbumArt);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
    }


}