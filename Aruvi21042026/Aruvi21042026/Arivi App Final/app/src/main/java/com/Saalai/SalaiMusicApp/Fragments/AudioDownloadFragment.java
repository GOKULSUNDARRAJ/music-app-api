package com.Saalai.SalaiMusicApp.Fragments;

import static android.content.ContentValues.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.AudioAdapterfordownload;
import com.Saalai.SalaiMusicApp.AudioDownloadManager;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AudioDownloadFragment extends Fragment {
    private SaalaiFragment.DrawerToggleListener drawerToggleListener;
    private static final String TAG = "AudioDownloadFragment";
    private ImageView backbtn, btnFullPlayPause;
    private RecyclerView recyclerViewDownloads;
    private TextView noDownloadsText;
    private ArrayList<AudioModel> offlineSongsList;
    private AudioAdapterfordownload audioAdapter;
    private AudioDownloadManager downloadManager;
    private boolean isFullPlayPauseSetup = false;

    // SharedPreferences for saving playlist order
    private SharedPreferences orderPrefs;
    private static final String ORDER_PREFS_NAME = "DownloadPlaylistOrder";
    private static final String ORDER_KEY = "song_order_paths"; // Changed key name

    // ItemTouchHelper for drag and drop
    private ItemTouchHelper itemTouchHelper;

    // Broadcast Receiver for song changes
    private BroadcastReceiver songChangeReceiver;
    private boolean isReceiverRegistered = false;

    public interface DrawerToggleListener {
        void onToggleDrawer();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            drawerToggleListener = (SaalaiFragment.DrawerToggleListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement DrawerToggleListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_audio_download, container, false);

        backbtn = view.findViewById(R.id.backbtn);
        btnFullPlayPause = view.findViewById(R.id.btnFullPlayPause);
        recyclerViewDownloads = view.findViewById(R.id.recyclerViewDownloads);
        noDownloadsText = view.findViewById(R.id.noDownloadsText);

        // Initialize download manager and order preferences
        downloadManager = new AudioDownloadManager(requireContext());
        orderPrefs = requireContext().getSharedPreferences(ORDER_PREFS_NAME, Context.MODE_PRIVATE);

        // Initialize the offline songs list
        offlineSongsList = new ArrayList<>();

        // Setup RecyclerView FIRST (without setting playlist)
        setupRecyclerView();

        // Setup swipe to delete and drag to reorder
        setupItemTouchHelper();

        // Setup full play/pause button
        setupFullPlayPauseButton();

        // Load downloaded songs
        loadDownloadedSongs();

        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload downloaded songs when fragment resumes
        loadDownloadedSongs();

        // Debug the offline songs list
        debugOfflineSongsList();

        // Register broadcast receiver for song changes
        registerSongChangeReceiver();

        // Update full play/pause button
        if (isFullPlayPauseSetup) {
            updateFullPlayPauseButtonUI();
        }
    }

    private void debugOfflineSongsList() {
        Log.d(TAG, "=== OFFLINE SONGS LIST DEBUG ===");
        Log.d(TAG, "List size: " + offlineSongsList.size());

        for (int i = 0; i < offlineSongsList.size(); i++) {
            AudioModel song = offlineSongsList.get(i);
            Log.d(TAG, "[" + i + "] " + song.getAudioName() +
                    " | Path: " + song.getDownloadPath() +
                    " | SongId: " + song.getSongId());
        }
        Log.d(TAG, "=== END DEBUG ===");
    }

    @Override
    public void onPause() {
        super.onPause();
        // Save the current order before pausing
        savePlaylistOrder();
        // Unregister broadcast receiver
        unregisterSongChangeReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Save the current order before destroying
        savePlaylistOrder();
        // Ensure receiver is unregistered
        unregisterSongChangeReceiver();
    }

    /**
     * Save the current playlist order to SharedPreferences using download paths as unique identifiers
     */
    private void savePlaylistOrder() {
        if (offlineSongsList != null && !offlineSongsList.isEmpty()) {
            List<String> downloadPaths = new ArrayList<>();
            Map<String, String> pathToNameMap = new HashMap<>(); // For debugging

            for (AudioModel song : offlineSongsList) {
                // Use download path as unique identifier (from logs, these are unique)
                if (song.getDownloadPath() != null && !song.getDownloadPath().isEmpty()) {
                    downloadPaths.add(song.getDownloadPath());
                    pathToNameMap.put(song.getDownloadPath(), song.getAudioName());
                }
            }

            // Save the ordered list of download paths
            SharedPreferences.Editor editor = orderPrefs.edit();
            Gson gson = new Gson();
            String json = gson.toJson(downloadPaths);
            editor.putString(ORDER_KEY, json);
            editor.apply();

            Log.d(TAG, "Saved playlist order with " + downloadPaths.size() + " songs:");
            for (int i = 0; i < downloadPaths.size(); i++) {
                String path = downloadPaths.get(i);
                Log.d(TAG, "  [" + i + "] " + pathToNameMap.get(path) + " -> " + path);
            }
        }
    }

    /**
     * Load the saved playlist order and reorder the offlineSongsList accordingly
     */
    private void applySavedOrder() {
        if (offlineSongsList == null || offlineSongsList.isEmpty()) {
            return;
        }

        // Load saved order from SharedPreferences
        String json = orderPrefs.getString(ORDER_KEY, "");
        if (json.isEmpty()) {
            Log.d(TAG, "No saved order found");
            return;
        }

        Gson gson = new Gson();
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> savedOrder = gson.fromJson(json, type);

        if (savedOrder == null || savedOrder.isEmpty()) {
            return;
        }

        Log.d(TAG, "Applying saved order with " + savedOrder.size() + " songs");

        // Create a map of download path to song for quick lookup
        Map<String, AudioModel> pathToSongMap = new HashMap<>();
        for (AudioModel song : offlineSongsList) {
            if (song.getDownloadPath() != null) {
                pathToSongMap.put(song.getDownloadPath(), song);
            }
        }

        // Create a new ordered list
        ArrayList<AudioModel> orderedList = new ArrayList<>();
        List<String> notFoundPaths = new ArrayList<>();

        // First, add songs in the saved order
        for (String path : savedOrder) {
            if (pathToSongMap.containsKey(path)) {
                orderedList.add(pathToSongMap.get(path));
                pathToSongMap.remove(path); // Remove to track what's left
            } else {
                notFoundPaths.add(path);
                Log.w(TAG, "Saved path not found in current downloads: " + path);
            }
        }

        // Add any remaining songs that weren't in the saved order (new downloads)
        if (!pathToSongMap.isEmpty()) {
            Log.d(TAG, "Adding " + pathToSongMap.size() + " new songs not in saved order");
            orderedList.addAll(pathToSongMap.values());
        }

        // Update the offlineSongsList with the ordered list
        offlineSongsList.clear();
        offlineSongsList.addAll(orderedList);

        Log.d(TAG, "Applied saved order. Final list size: " + offlineSongsList.size());

        // Log the final order for debugging
        for (int i = 0; i < offlineSongsList.size(); i++) {
            AudioModel song = offlineSongsList.get(i);
            Log.d(TAG, "  Final [" + i + "]: " + song.getAudioName() + " - " + song.getDownloadPath());
        }
    }

    /**
     * Setup ItemTouchHelper for swipe to delete and drag to reorder
     */
    private void setupItemTouchHelper() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, // Drag directions
                ItemTouchHelper.LEFT // Swipe directions (LEFT for delete)
        ) {
            private boolean isDragging = false;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                isDragging = true;

                // Get positions
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                // Move item in the list
                if (fromPosition < toPosition) {
                    for (int i = fromPosition; i < toPosition; i++) {
                        Collections.swap(offlineSongsList, i, i + 1);
                    }
                } else {
                    for (int i = fromPosition; i > toPosition; i--) {
                        Collections.swap(offlineSongsList, i, i - 1);
                    }
                }

                // Notify adapter of the move
                audioAdapter.notifyItemMoved(fromPosition, toPosition);

                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Handle swipe to delete (left swipe)
                int position = viewHolder.getAdapterPosition();
                AudioModel deletedSong = offlineSongsList.get(position);

                // Delete the song
                deleteDownloadedSong(deletedSong, position);
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);

                // When starting to drag, provide visual feedback
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    isDragging = true;
                    if (viewHolder != null) {
                        viewHolder.itemView.setAlpha(0.7f);
                        viewHolder.itemView.setScaleX(1.05f);
                        viewHolder.itemView.setScaleY(1.05f);
                    }
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                // Reset visual feedback after drag completes
                viewHolder.itemView.setAlpha(1.0f);
                viewHolder.itemView.setScaleX(1.0f);
                viewHolder.itemView.setScaleY(1.0f);

                // If we were dragging, save the new order
                if (isDragging) {
                    isDragging = false;

                    // Save the new playlist order
                    saveFullPlaylistWithoutCategoryIds();

                    // 🔴 SAVE THE ORDER TO SHAREDPREFERENCES
                    savePlaylistOrder();

                    // Update PlayerManager with reordered list
                    PlayerManager.setAudioList(offlineSongsList, PlayerManager.PlaylistType.OFFLINE);

                    // Show feedback
                    Snackbar.make(recyclerView, "Playlist reordered", Snackbar.LENGTH_SHORT).show();

                    // Broadcast that playlist order changed
                    broadcastPlaylistOrderChanged();

                    Log.d(TAG, "Playlist reordered and saved");
                }
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas canvas,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                // Handle swipe delete background (only for left swipe)
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    View itemView = viewHolder.itemView;

                    // Draw red background
                    android.graphics.drawable.Drawable background = new android.graphics.drawable.ColorDrawable(
                            getResources().getColor(R.color.red));
                    background.setBounds(itemView.getRight() + (int) dX,
                            itemView.getTop(),
                            itemView.getRight(),
                            itemView.getBottom());
                    background.draw(canvas);

                    // Draw delete icon
                    try {
                        android.graphics.drawable.Drawable deleteIcon = getResources().getDrawable(R.drawable.baseline_auto_delete_24);
                        int deleteIconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int deleteIconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int deleteIconBottom = deleteIconTop + deleteIcon.getIntrinsicHeight();
                        int deleteIconLeft = itemView.getRight() - deleteIconMargin - deleteIcon.getIntrinsicWidth();
                        int deleteIconRight = itemView.getRight() - deleteIconMargin;

                        deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
                        deleteIcon.draw(canvas);
                    } catch (Exception e) {
                        Log.e(TAG, "Error drawing delete icon: " + e.getMessage());
                    }
                }

                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public boolean isLongPressDragEnabled() {
                // Enable long press drag
                return true;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                // Enable swipe to delete
                return true;
            }
        };

        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerViewDownloads);
    }

    /**
     * Setup the full play/pause button click listener for downloaded songs
     */
    private void setupFullPlayPauseButton() {
        if (btnFullPlayPause == null) {
            Log.e(TAG, "btnFullPlayPause is null, cannot setup");
            return;
        }

        btnFullPlayPause.setOnClickListener(v -> {
            boolean isSongFromThisPlaylistPlaying = isSongFromThisPlaylistPlaying();

            if (isSongFromThisPlaylistPlaying) {
                // A song from this playlist is playing - toggle play/pause
                if (PlayerManager.isPlaying()) {
                    PlayerManager.pausePlayback();
                    btnFullPlayPause.setImageResource(R.drawable.play_player);
                    Log.d(TAG, "This playlist playback paused");
                } else {
                    PlayerManager.resumePlayback();
                    btnFullPlayPause.setImageResource(R.drawable.pause);
                    Log.d(TAG, "This playlist playback resumed");
                }
            } else {
                // Force play first song from this playlist regardless of what's playing
                Log.d(TAG, "Different playlist or nothing playing - forcing play first song from this playlist");

                // Stop any current playback
                if (PlayerManager.isPlaying()) {
                    PlayerManager.pausePlayback();
                }

                // Play first song from this playlist
                playFirstValidSong();
            }
            broadcastSongChanged();
        });

        // Initial update of button state
        updateFullPlayPauseButtonUI();
        isFullPlayPauseSetup = true;
    }

    /**
     * Play the first downloaded song from this playlist (index 0)
     */
    private void playFirstValidSong() {
        if (offlineSongsList == null || offlineSongsList.isEmpty()) {
            Log.d(TAG, "Cannot play - no downloaded songs");
            Toast.makeText(requireContext(), "No downloaded songs available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the first song in the list (index 0)
        AudioModel firstSong = offlineSongsList.get(0);

        Log.d(TAG, "Attempting to play first song at index 0: " + firstSong.getAudioName() +
                ", Path: " + firstSong.getDownloadPath());

        // Check if the first song is valid
        if (!firstSong.isDownloaded()) {
            Log.e(TAG, "First song is not downloaded: " + firstSong.getAudioName());
            Toast.makeText(requireContext(), "First song is not downloaded", Toast.LENGTH_SHORT).show();
            return;
        }

        if (firstSong.getDownloadPath() == null || firstSong.getDownloadPath().isEmpty()) {
            Log.e(TAG, "First song has no download path: " + firstSong.getAudioName());
            Toast.makeText(requireContext(), "First song file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if file exists
        java.io.File audioFile = new java.io.File(firstSong.getDownloadPath());
        if (!audioFile.exists()) {
            Log.e(TAG, "First song file doesn't exist: " + firstSong.getDownloadPath());
            Toast.makeText(requireContext(), "First song file missing", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "First song file exists, size: " + audioFile.length() + " bytes");

        // Stop any current playback
        if (PlayerManager.isPlaying()) {
            PlayerManager.stopPlayback();
        }

        // Update button UI immediately
        btnFullPlayPause.setImageResource(R.drawable.pause);

        // Clear any existing category ID (offline songs shouldn't have category IDs)
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("SavedAudio", Context.MODE_PRIVATE);
        prefs.edit().remove("audioCategoryId").apply();

        // Save the audio and playlist without category IDs
        saveAudioLocallyWithoutCategoryId(firstSong);
        saveFullPlaylistWithoutCategoryIds();

        // Set the offline playlist
        PlayerManager.setAudioList(offlineSongsList, PlayerManager.PlaylistType.OFFLINE);

        // Play the first song
        Log.d(TAG, "Playing first downloaded song at index 0: " + firstSong.getAudioName());
        AudioModel finalFirstSong = firstSong;

        PlayerManager.playOfflineAudio(firstSong, new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "First downloaded song started playing successfully: " + finalFirstSong.getAudioName());

                // Update adapter to highlight playing song
                if (audioAdapter != null) {
                    getActivity().runOnUiThread(() -> {
                        audioAdapter.notifyDataSetChanged();
                    });
                }

                // Update full button UI
                updateFullPlayPauseButtonUI();

                // Broadcast song changed
                broadcastSongChanged();
            }
        });
    }

    private boolean isSongFromThisPlaylistPlaying() {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) {
            Log.d(TAG, "No current audio playing");
            return false;
        }

        // Check if current playlist type is OFFLINE
        boolean isOfflinePlaylist = PlayerManager.getCurrentPlaylistType() == PlayerManager.PlaylistType.OFFLINE;

        if (!isOfflinePlaylist) {
            Log.d(TAG, "Current playing is from ONLINE playlist, not this fragment");
            return false;
        }

        // Check if the current song exists in our offlineSongsList by comparing download paths
        boolean isInThisList = false;
        for (AudioModel song : offlineSongsList) {
            // Compare by download path (most reliable for offline songs)
            if (song.getDownloadPath() != null &&
                    currentAudio.getDownloadPath() != null &&
                    song.getDownloadPath().equals(currentAudio.getDownloadPath())) {
                isInThisList = true;
                break;
            }
            // Fallback to URL comparison
            if (song.getAudioUrl() != null &&
                    currentAudio.getAudioUrl() != null &&
                    song.getAudioUrl().equals(currentAudio.getAudioUrl())) {
                isInThisList = true;
                break;
            }
        }

        Log.d(TAG, "Current playing song: " + currentAudio.getAudioName() +
                ", Is from this playlist: " + isInThisList);

        return isInThisList;
    }

    /**
     * Update the full play/pause button UI based on current playback state
     */
    public void updateFullPlayPauseButtonUI() {
        if (btnFullPlayPause == null || !isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            boolean isSongFromThisPlaylistPlaying = isSongFromThisPlaylistPlaying();

            if (isSongFromThisPlaylistPlaying) {
                // A song from this playlist is playing
                btnFullPlayPause.setVisibility(View.VISIBLE);
                if (PlayerManager.isPlaying()) {
                    btnFullPlayPause.setImageResource(R.drawable.pause);
                    Log.d(TAG, "Full button: Show PAUSE (this playlist playing)");
                } else {
                    btnFullPlayPause.setImageResource(R.drawable.play_player);
                    Log.d(TAG, "Full button: Show PLAY (this playlist paused)");
                }
            } else {
                // No song from this playlist is playing
                btnFullPlayPause.setVisibility(View.VISIBLE);
                btnFullPlayPause.setImageResource(R.drawable.play_player);
                Log.d(TAG, "Full button: Show PLAY (no playback from this playlist)");
            }
        });
    }

    private void registerSongChangeReceiver() {
        if (!isReceiverRegistered && getContext() != null) {
            songChangeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Log.d("AudioDownloadFragment", "Broadcast received: " + action);

                    if (action != null) {
                        switch (action) {
                            case "UPDATE_AUDIO_ADAPTER":
                                updateAdapterPlayingState();
                                if (isFullPlayPauseSetup) {
                                    updateFullPlayPauseButtonUI();
                                }
                                break;
                            case "DOWNLOAD_UPDATE":
                                // When download is updated (added or deleted), reload the songs
                                Log.d("AudioDownloadFragment", "Reloading downloaded songs after DOWNLOAD_UPDATE");
                                loadDownloadedSongs();
                                if (isFullPlayPauseSetup) {
                                    updateFullPlayPauseButtonUI();
                                }
                                break;
                            case "SONG_CHANGED":
                                handleSongChanged();
                                if (isFullPlayPauseSetup) {
                                    updateFullPlayPauseButtonUI();
                                }
                                break;
                            case "PLAYLIST_ORDER_CHANGED":
                                // Refresh adapter to show any changes
                                if (audioAdapter != null) {
                                    audioAdapter.notifyDataSetChanged();
                                }
                                break;
                        }
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction("UPDATE_AUDIO_ADAPTER");
            filter.addAction("DOWNLOAD_UPDATE");
            filter.addAction("SONG_CHANGED");
            filter.addAction("PLAYLIST_ORDER_CHANGED");

            androidx.core.content.ContextCompat.registerReceiver(
                    getContext(),
                    songChangeReceiver,
                    filter,
                    androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            );

            isReceiverRegistered = true;
            Log.d("AudioDownloadFragment", "Broadcast receiver registered");
        }
    }

    private void unregisterSongChangeReceiver() {
        if (isReceiverRegistered && songChangeReceiver != null && getContext() != null) {
            try {
                getContext().unregisterReceiver(songChangeReceiver);
                isReceiverRegistered = false;
                Log.d("AudioDownloadFragment", "Broadcast receiver unregistered");
            } catch (Exception e) {
                Log.e("AudioDownloadFragment", "Error unregistering receiver: " + e.getMessage());
            }
        }
    }

    private void updateAdapterPlayingState() {
        if (audioAdapter != null) {
            getActivity().runOnUiThread(() -> {
                audioAdapter.notifyDataSetChanged();
                Log.d("AudioDownloadFragment", "Adapter updated for playing state change");
            });
        }
    }

    private void handleSongChanged() {
        // Refresh the list to ensure current playing song is highlighted
        if (audioAdapter != null) {
            getActivity().runOnUiThread(() -> {
                audioAdapter.notifyDataSetChanged();
                Log.d("AudioDownloadFragment", "Adapter updated for song change");
            });
        }
    }

    private void setupRecyclerView() {
        audioAdapter = new AudioAdapterfordownload(offlineSongsList, getContext(), new AudioAdapterfordownload.OnItemClickListener() {
            @Override
            public void onItemClick(AudioModel audioModel, android.widget.ProgressBar progressBar) {
                // Play song from local file path
                playOfflineSong(audioModel, progressBar);
            }
        });

        // Set the RecyclerView reference to the adapter
        audioAdapter.setRecyclerView(recyclerViewDownloads);

        // Set song deleted listener
        audioAdapter.setOnSongDeletedListener(new AudioAdapterfordownload.OnSongDeletedListener() {
            @Override
            public void onSongDeleted(AudioModel deletedAudio, int position) {
                // Handle song deletion from popup menu
                deleteDownloadedSong(deletedAudio, position);
            }
        });

        // Set drag handle listener
        audioAdapter.setOnDragHandleListener(new AudioAdapterfordownload.OnDragHandleListener() {
            @Override
            public void onDragHandleClick(RecyclerView.ViewHolder viewHolder) {
                // Start drag when drag handle is clicked
                if (itemTouchHelper != null) {
                    itemTouchHelper.startDrag(viewHolder);
                }
            }
        });

        recyclerViewDownloads.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewDownloads.setAdapter(audioAdapter);
    }

    private void loadDownloadedSongs() {
        // Check if fragment is attached to activity
        if (getActivity() == null || !isAdded()) {
            Log.e("AudioDownloadFragment", "Fragment not attached to activity, skipping loadDownloadedSongs");
            return;
        }

        getActivity().runOnUiThread(() -> {
            // Get downloaded songs from AudioDownloadManager
            List<AudioModel> downloadedSongs = downloadManager.getDownloadedSongs();

            offlineSongsList.clear();

            if (downloadedSongs != null && !downloadedSongs.isEmpty()) {
                // Ensure each song has proper download status and path
                for (AudioModel song : downloadedSongs) {
                    String audioUrl = song.getAudioUrl();

                    // If audioUrl looks like a local file path, set download info
                    if (audioUrl != null && !audioUrl.startsWith("http")) {
                        song.setDownloaded(true);
                        song.setDownloadPath(audioUrl);
                        Log.d(TAG, "Loaded song: " + song.getAudioName() +
                                " | Path: " + audioUrl);
                    }

                    offlineSongsList.add(song);
                }

                // 🔴 APPLY THE SAVED ORDER TO THE LIST (using download paths)
                applySavedOrder();

                showDownloadsList();
                Log.d("AudioDownloadFragment", "Loaded " + downloadedSongs.size() + " downloaded songs");
            } else {
                showNoDownloads();
                Log.d("AudioDownloadFragment", "No downloaded songs found");
            }

            // Debug the offline songs list
            debugOfflineSongsList();

            // Notify adapter
            if (audioAdapter != null) {
                audioAdapter.notifyDataSetChanged();
                Log.d("AudioDownloadFragment", "Adapter notified of data change");
            } else {
                Log.w("AudioDownloadFragment", "Adapter is null, cannot notify");
            }

            // Update full play/pause button
            if (isFullPlayPauseSetup) {
                updateFullPlayPauseButtonUI();
            }
        });
    }

    private void showDownloadsList() {
        recyclerViewDownloads.setVisibility(View.VISIBLE);
        noDownloadsText.setVisibility(View.GONE);
    }

    private void showNoDownloads() {
        recyclerViewDownloads.setVisibility(View.GONE);
        noDownloadsText.setVisibility(View.VISIBLE);
        noDownloadsText.setText("No downloaded songs\n\nDownload songs to listen offline");
    }

    private void playOfflineSong(AudioModel audioModel, android.widget.ProgressBar progressBar) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        Log.d("AudioDownloadFragment", "Playing OFFLINE song: " + audioModel.getAudioName() +
                ", Total downloaded songs in list: " + offlineSongsList.size());

        // Check if file exists
        if (audioModel.getDownloadPath() != null && !audioModel.getDownloadPath().isEmpty()) {
            java.io.File audioFile = new java.io.File(audioModel.getDownloadPath());
            if (!audioFile.exists()) {
                Log.e(TAG, "File doesn't exist: " + audioModel.getDownloadPath());
                Toast.makeText(requireContext(), "Audio file missing", Toast.LENGTH_SHORT).show();
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                return;
            }
        }

        // Remove any existing category ID from SharedPreferences
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("SavedAudio", Context.MODE_PRIVATE);
        prefs.edit().remove("audioCategoryId").apply();

        // Save audio locally WITHOUT category ID
        saveAudioLocallyWithoutCategoryId(audioModel);

        // Save full playlist WITHOUT category IDs
        saveFullPlaylistWithoutCategoryIds();

        PlayerManager.setAudioList(offlineSongsList, PlayerManager.PlaylistType.OFFLINE);

        // Play the selected song with callback
        PlayerManager.playOfflineAudio(audioModel, () -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            if (audioAdapter != null) {
                getActivity().runOnUiThread(() -> {
                    audioAdapter.notifyDataSetChanged();
                });
            }

            updateFullPlayPauseButtonUI();
            broadcastSongChanged();
        });

        Log.d("AudioDownloadFragment", "Started playing OFFLINE song from playlist of " + offlineSongsList.size() + " songs");
    }

    private void saveAudioLocallyWithoutCategoryId(AudioModel audio) {
        if (getContext() != null) {
            android.content.SharedPreferences prefs = getContext().getSharedPreferences("SavedAudio", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putString("audioName", audio.getAudioName());
            editor.putString("audioUrl", audio.getAudioUrl());
            editor.putString("imageUrl", audio.getImageUrl());
            editor.putString("audioArtist", audio.getcategoryName());

            if (audio.getDownloadPath() != null && !audio.getDownloadPath().isEmpty()) {
                editor.putString("downloadPath", audio.getDownloadPath());
            }

            editor.remove("audioCategoryId");
            editor.apply();

            Log.d("AudioDownloadFragment", "Saved OFFLINE audio without category ID: " + audio.getAudioName() +
                    ", Path: " + audio.getDownloadPath());
        }
    }

    private void saveFullPlaylistWithoutCategoryIds() {
        if (getContext() != null && offlineSongsList != null && !offlineSongsList.isEmpty()) {
            android.content.SharedPreferences prefs = getContext().getSharedPreferences("SavedPlaylist", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();

            // Clear existing data
            editor.clear();

            // Save playlist size
            editor.putInt("playlistSize", offlineSongsList.size());
            editor.putString("playlistType", PlayerManager.PlaylistType.OFFLINE.name());

            // Save each song in the playlist WITHOUT category IDs
            for (int i = 0; i < offlineSongsList.size(); i++) {
                AudioModel song = offlineSongsList.get(i);
                editor.putString("songName_" + i, song.getAudioName());
                editor.putString("songUrl_" + i, song.getAudioUrl());
                editor.putString("songImage_" + i, song.getImageUrl());
                editor.putString("songArtist_" + i, song.getcategoryName());
                editor.putString("songId_" + i, song.getSongId());

                // Save download info
                if (song.getDownloadPath() != null && !song.getDownloadPath().isEmpty()) {
                    editor.putString("songDownloadPath_" + i, song.getDownloadPath());
                    editor.putBoolean("songDownloaded_" + i, true);
                }

                editor.remove("songCategoryId_" + i);
            }

            editor.apply();
            Log.d("AudioDownloadFragment", "Saved OFFLINE playlist without category IDs: " + offlineSongsList.size() + " songs");
        }
    }

    // Method to delete a downloaded song
    private void deleteDownloadedSong(AudioModel audioModel, int position) {
        boolean isDeleted = downloadManager.deleteDownloadedSong(audioModel);

        if (isDeleted) {
            // Remove from list
            offlineSongsList.remove(position);
            audioAdapter.notifyItemRemoved(position);

            // Show snackbar with undo option
            showUndoSnackbar(audioModel, position);

            // Update UI if no downloads left
            if (offlineSongsList.isEmpty()) {
                showNoDownloads();
            }

            Toast.makeText(getContext(), "Song deleted", Toast.LENGTH_SHORT).show();

            // Broadcast download update to refresh other parts of the app
            broadcastDownloadUpdate();

            // Also broadcast song change in case the deleted song was playing
            broadcastSongChanged();

            // Update full play/pause button
            updateFullPlayPauseButtonUI();

            // 🔴 SAVE THE UPDATED ORDER AFTER DELETION
            savePlaylistOrder();
        } else {
            Toast.makeText(getContext(), "Failed to delete song", Toast.LENGTH_SHORT).show();
            audioAdapter.notifyItemChanged(position);
        }
    }

    private void broadcastDownloadUpdate() {
        if (getContext() != null) {
            Intent downloadUpdateIntent = new Intent("DOWNLOAD_UPDATE");
            downloadUpdateIntent.setPackage(getContext().getPackageName());
            getContext().sendBroadcast(downloadUpdateIntent);
            Log.d("AudioDownloadFragment", "DOWNLOAD_UPDATE broadcast sent");
        }
    }

    private void broadcastSongChanged() {
        if (getContext() != null) {
            Intent songChangedIntent = new Intent("SONG_CHANGED");
            songChangedIntent.setPackage(getContext().getPackageName());
            getContext().sendBroadcast(songChangedIntent);
            Log.d("AudioDownloadFragment", "SONG_CHANGED broadcast sent");
        }
    }

    private void broadcastPlaylistOrderChanged() {
        if (getContext() != null) {
            Intent orderChangedIntent = new Intent("PLAYLIST_ORDER_CHANGED");
            orderChangedIntent.setPackage(getContext().getPackageName());
            getContext().sendBroadcast(orderChangedIntent);
            Log.d("AudioDownloadFragment", "PLAYLIST_ORDER_CHANGED broadcast sent");
        }
    }

    private void showUndoSnackbar(AudioModel deletedSong, int position) {
        Snackbar snackbar = Snackbar.make(recyclerViewDownloads, "Song deleted", Snackbar.LENGTH_LONG);
        snackbar.setAction("UNDO", v -> {
            // Restore the song
            boolean isRestored = downloadManager.restoreDownloadedSong(deletedSong);
            if (isRestored) {
                offlineSongsList.add(position, deletedSong);
                audioAdapter.notifyItemInserted(position);
                if (offlineSongsList.size() == 1) {
                    showDownloadsList();
                }

                // Update PlayerManager with restored list
                PlayerManager.setAudioList(offlineSongsList, PlayerManager.PlaylistType.OFFLINE);

                // Broadcast updates
                broadcastDownloadUpdate();
                broadcastSongChanged();

                // Update full play/pause button
                updateFullPlayPauseButtonUI();

                // 🔴 SAVE THE RESTORED ORDER
                savePlaylistOrder();
            } else {
                Toast.makeText(getContext(), "Failed to restore song", Toast.LENGTH_SHORT).show();
            }
        });

        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    // Song permanently deleted, update full button
                    updateFullPlayPauseButtonUI();
                }
            }
        });

        snackbar.show();
    }
}