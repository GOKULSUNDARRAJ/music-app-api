package com.Saalai.SalaiMusicApp.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.DownloadedSongsAdapter;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Utils.DownloadPlaylistManager;

import java.util.ArrayList;
import java.util.Locale;

public class SearchDownloadedFragment extends Fragment {

    private static final String TAG = "SearchDownloadedFragment";
    private static final String ARG_PLAYLIST_ID = "playlist_id";

    // Intent actions from PlayerManager
    private static final String ACTION_PREPARE_NEXT = "PREPARE_NEXT";
    private static final String ACTION_PREPARE_PREVIOUS = "PREPARE_PREVIOUS";

    // UI Components
    private EditText searchEditText;
    private ImageView btnBackSearch, tvCancel;
    private TextView tvSongCount;
    private RecyclerView searchRecyclerView;
    private LinearLayout emptyStateLayout, noResultsLayout;

    // Adapter and data
    private DownloadedSongsAdapter searchAdapter;
    private ArrayList<AudioModel> allSongsList = new ArrayList<>();
    private ArrayList<AudioModel> filteredSongsList = new ArrayList<>();
    private DownloadPlaylistManager playlistManager;

    // Search debounce handler
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private static final long SEARCH_DELAY_MS = 300;

    // Broadcast receiver for player updates
    private BroadcastReceiver playerUpdateReceiver;
    private boolean isReceiverRegistered = false;

    // Broadcast receiver for prepare actions (next/previous)
    private final BroadcastReceiver prepareReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (searchAdapter == null || getActivity() == null) return;

            String action = intent.getAction();
            getActivity().runOnUiThread(() -> {
                if (ACTION_PREPARE_NEXT.equals(action)) {
                    int nextIndex = intent.getIntExtra("nextIndex", -1);
                    if (nextIndex != -1) {
                        // Find the position in filtered list
                        int filteredPosition = findPositionInFilteredList(nextIndex);
                        if (filteredPosition != -1) {
                            searchAdapter.showNextItemProgress(filteredPosition);

                            if (searchRecyclerView != null) {
                                searchRecyclerView.smoothScrollToPosition(filteredPosition);
                            }
                            Log.d(TAG, "Received prepare next for original index: " + nextIndex +
                                    ", filtered position: " + filteredPosition);
                        }
                    }
                }
                else if (ACTION_PREPARE_PREVIOUS.equals(action)) {
                    int prevIndex = intent.getIntExtra("prevIndex", -1);
                    if (prevIndex != -1) {
                        // Find the position in filtered list
                        int filteredPosition = findPositionInFilteredList(prevIndex);
                        if (filteredPosition != -1) {
                            searchAdapter.showPreviousItemProgress(filteredPosition);

                            if (searchRecyclerView != null) {
                                searchRecyclerView.smoothScrollToPosition(filteredPosition);
                            }
                            Log.d(TAG, "Received prepare previous for original index: " + prevIndex +
                                    ", filtered position: " + filteredPosition);
                        }
                    }
                }
            });
        }
    };

    private String playlistId;
    private OnSongSelectedListener songSelectedListener;

    public interface OnSongSelectedListener {
        void onSongSelected(AudioModel song);
    }

    public void setOnSongSelectedListener(OnSongSelectedListener listener) {
        this.songSelectedListener = listener;
    }

    public static SearchDownloadedFragment newInstance(String playlistId) {
        SearchDownloadedFragment fragment = new SearchDownloadedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PLAYLIST_ID, playlistId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        playlistManager = DownloadPlaylistManager.getInstance(requireContext());

        if (getArguments() != null) {
            playlistId = getArguments().getString(ARG_PLAYLIST_ID);
            Log.d(TAG, "Playlist ID: " + playlistId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_downloaded, container, false);

        initViews(view);
        setupListeners();
        setupRecyclerView();
        loadPlaylistSongs();

        // Focus the search edit text and show keyboard
        searchEditText.requestFocus();
        if (getActivity() != null) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return view;
    }

    private void initViews(View view) {
        searchEditText = view.findViewById(R.id.searchEditText);
        btnBackSearch = view.findViewById(R.id.btnBackSearch);
        tvCancel = view.findViewById(R.id.tvCancel);
        tvSongCount = view.findViewById(R.id.tvSongCount);
        searchRecyclerView = view.findViewById(R.id.searchRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        noResultsLayout = view.findViewById(R.id.noResultsLayout);

        // Initially hide everything - we'll show based on data
        emptyStateLayout.setVisibility(View.GONE);
        noResultsLayout.setVisibility(View.GONE);
        searchRecyclerView.setVisibility(View.GONE);
        tvSongCount.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnBackSearch.setOnClickListener(v -> {
            hideKeyboard();
            requireActivity().onBackPressed();
        });

        tvCancel.setOnClickListener(v -> {
            searchEditText.setText("");
            tvCancel.setVisibility(View.GONE);
            showAllSongs(); // Show all songs when cancel is clicked
            hideKeyboard();
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    tvCancel.setVisibility(View.VISIBLE);
                } else {
                    tvCancel.setVisibility(View.GONE);
                }

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> performSearch(s.toString());
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchEditText.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        // Use DownloadedSongsAdapter
        searchAdapter = new DownloadedSongsAdapter(filteredSongsList, requireContext(),
                new DownloadedSongsAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(AudioModel audioModel, ProgressBar progressBar) {
                        Log.d(TAG, "Search result clicked: " + audioModel.getAudioName());

                        // Show progress bar immediately
                        if (progressBar != null) {
                            progressBar.setVisibility(View.VISIBLE);
                        }

                        // Notify listener if set
                        if (songSelectedListener != null) {
                            songSelectedListener.onSongSelected(audioModel);
                        }

                        playSelectedSong(audioModel, progressBar);
                    }
                });

        // Set the RecyclerView reference for the adapter
        searchAdapter.setRecyclerView(searchRecyclerView);

        // Set song deleted listener
        searchAdapter.setOnSongDeletedListener(new DownloadedSongsAdapter.OnSongDeletedListener() {
            @Override
            public void onSongDeleted(AudioModel deletedAudio, int position) {
                Log.d(TAG, "Song delete requested from search: " + deletedAudio.getAudioName());

                // Remove from filtered list
                filteredSongsList.remove(position);
                searchAdapter.notifyItemRemoved(position);

                // Also remove from all songs list
                allSongsList.remove(deletedAudio);

                // Update song count
                updateSongCount();

                // If filtered list becomes empty, show appropriate state
                if (filteredSongsList.isEmpty()) {
                    if (searchEditText.getText().toString().trim().isEmpty()) {
                        showEmptyState();
                    } else {
                        showNoResults();
                    }
                }

                // Notify the parent fragment about the deletion
                if (getParentFragment() instanceof DownloadPlaylistSongsFragment) {
                    ((DownloadPlaylistSongsFragment) getParentFragment()).onSongDeletedFromSearch(deletedAudio);
                }
            }
        });

        searchRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchRecyclerView.setAdapter(searchAdapter);
    }

    private void loadPlaylistSongs() {
        Log.d(TAG, "loadPlaylistSongs called - playlistId: " + playlistId);

        if (playlistId == null) {
            Log.e(TAG, "playlistId is null!");
            Toast.makeText(requireContext(), "Error loading playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        var playlist = playlistManager.getPlaylistById(playlistId);
        if (playlist == null) {
            Log.e(TAG, "Playlist not found for ID: " + playlistId);
            Toast.makeText(requireContext(), "Playlist not found", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        ArrayList<AudioModel> songs = playlist.getSongs();
        if (songs != null && !songs.isEmpty()) {
            allSongsList.clear();
            allSongsList.addAll(songs);

            // IMPORTANT: Show all songs initially by copying to filtered list
            filteredSongsList.clear();
            filteredSongsList.addAll(allSongsList);

            Log.d(TAG, "Loaded " + allSongsList.size() + " songs from playlist");

            // Show all songs
            showAllSongs();

        } else {
            Log.w(TAG, "Playlist is empty");
            showEmptyState();
            Toast.makeText(requireContext(), "No songs in this playlist", Toast.LENGTH_SHORT).show();
        }
    }

    private void performSearch(String query) {
        Log.d(TAG, "Performing search for: '" + query + "'");

        if (query == null || query.trim().isEmpty()) {
            // When search is cleared, show all songs again
            showAllSongs();
            return;
        }

        filteredSongsList.clear();
        String searchQuery = query.trim().toLowerCase(Locale.getDefault());

        for (AudioModel song : allSongsList) {
            boolean matches = false;

            if (song.getAudioName() != null &&
                    song.getAudioName().toLowerCase(Locale.getDefault()).contains(searchQuery)) {
                matches = true;
            }
            else if (song.getcategoryName() != null &&
                    song.getcategoryName().toLowerCase(Locale.getDefault()).contains(searchQuery)) {
                matches = true;
            }

            if (matches) {
                filteredSongsList.add(song);
            }
        }

        Log.d(TAG, "Search found " + filteredSongsList.size() + " results");

        requireActivity().runOnUiThread(() -> {
            if (filteredSongsList.isEmpty()) {
                showNoResults();
            } else {
                showSearchResults();
            }
            searchAdapter.notifyDataSetChanged();
        });
    }

    private void showAllSongs() {
        // Reset filtered list to show all songs
        filteredSongsList.clear();
        filteredSongsList.addAll(allSongsList);

        // Update adapter
        searchAdapter.notifyDataSetChanged();

        // Show the recycler view with all songs
        searchRecyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
        noResultsLayout.setVisibility(View.GONE);

        // Update song count
        updateSongCount();

        Log.d(TAG, "Showing all " + filteredSongsList.size() + " songs");
    }

    private void showEmptyState() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        noResultsLayout.setVisibility(View.GONE);
        searchRecyclerView.setVisibility(View.GONE);

        // Hide song count when showing empty state
        if (tvSongCount != null) {
            tvSongCount.setVisibility(View.GONE);
        }
    }

    private void showNoResults() {
        emptyStateLayout.setVisibility(View.GONE);
        noResultsLayout.setVisibility(View.VISIBLE);
        searchRecyclerView.setVisibility(View.GONE);

        // Hide song count when showing no results
        if (tvSongCount != null) {
            tvSongCount.setVisibility(View.GONE);
        }
    }

    private void showSearchResults() {
        emptyStateLayout.setVisibility(View.GONE);
        noResultsLayout.setVisibility(View.GONE);
        searchRecyclerView.setVisibility(View.VISIBLE);

        // Update song count for search results
        updateSongCount();
    }

    private void updateSongCount() {
        if (tvSongCount != null) {
            tvSongCount.setVisibility(View.VISIBLE);
            tvSongCount.setText(filteredSongsList.size() + " songs");
        }
    }

    /**
     * Helper method to find position in filtered list from original playlist index
     */
    private int findPositionInFilteredList(int originalIndex) {
        if (originalIndex < 0 || originalIndex >= allSongsList.size()) {
            return -1;
        }

        AudioModel targetSong = allSongsList.get(originalIndex);
        for (int i = 0; i < filteredSongsList.size(); i++) {
            AudioModel song = filteredSongsList.get(i);
            if (song.getAudioUrl() != null &&
                    song.getAudioUrl().equals(targetSong.getAudioUrl())) {
                return i;
            }
        }
        return -1;
    }

    private void playSelectedSong(AudioModel song, ProgressBar progressBar) {
        Log.d(TAG, "playSelectedSong: " + song.getAudioName() +
                ", Original Category ID: " + song.getCategoryId());

        if (song.isDownloaded() && song.getDownloadPath() != null) {
            java.io.File audioFile = new java.io.File(song.getDownloadPath());
            if (audioFile.exists()) {
                // Get the playlist's category ID
                String playlistCategoryId = getPlaylistCategoryId();

                // Create a copy of the song with the playlist's category ID
                AudioModel songToPlay = new AudioModel(song);
                songToPlay.setCategoryId(playlistCategoryId);
                songToPlay.setDownloadPath(song.getDownloadPath());
                songToPlay.setDownloaded(true);

                Log.d(TAG, "Playing with playlist category ID: " + playlistCategoryId +
                        " (was: " + song.getCategoryId() + ")");

                saveAudioLocally(songToPlay);

                // IMPORTANT: Use the FULL allSongsList for the playlist, not just filtered results
                // This ensures next/previous works correctly across all songs
                ArrayList<AudioModel> fullPlaylistWithCorrectIds = new ArrayList<>();
                for (AudioModel s : allSongsList) {  // Use allSongsList instead of filteredSongsList
                    AudioModel copy = new AudioModel(s);
                    copy.setCategoryId(playlistCategoryId);
                    copy.setDownloadPath(s.getDownloadPath());
                    copy.setDownloaded(s.isDownloaded());
                    fullPlaylistWithCorrectIds.add(copy);
                }

                PlayerManager.setAudioList(fullPlaylistWithCorrectIds, PlayerManager.PlaylistType.OFFLINE);

                PlayerManager.playOfflineAudio(songToPlay, () -> {
                    // Hide progress bar when playback starts
                    if (progressBar != null) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                        });
                    }

                    requireActivity().runOnUiThread(() -> {
                        // Clear preparing progress and update UI
                        if (searchAdapter != null) {
                            searchAdapter.clearPreparingProgress();
                            searchAdapter.updatePlayingIndicators();
                        }

                        // Show toast
                        Toast.makeText(requireContext(),
                                "Playing: " + songToPlay.getAudioName(),
                                Toast.LENGTH_SHORT).show();
                    });

                    // Broadcast song changed
                    broadcastSongChanged();
                });
            } else {
                handleFileMissing(song, progressBar);
            }
        } else {
            handleNotDownloaded(song, progressBar);
        }
    }

    // Helper method to get the playlist's category ID
    private String getPlaylistCategoryId() {
        if (playlistId != null) {
            var playlist = playlistManager.getPlaylistById(playlistId);
            if (playlist != null) {
                String categoryId = playlist.getOriginalCategoryId();
                Log.d(TAG, "Playlist category ID from manager: " + categoryId);
                return categoryId;
            }
        }

        // Fallback: try to get from first song if available
        if (!allSongsList.isEmpty() && allSongsList.get(0) != null) {
            String firstSongCategory = allSongsList.get(0).getCategoryId();
            Log.d(TAG, "Using first song category ID as fallback: " + firstSongCategory);
            return firstSongCategory;
        }

        Log.w(TAG, "No category ID found for playlist");
        return "";
    }

    private void handleFileMissing(AudioModel song, ProgressBar progressBar) {
        Log.e(TAG, "File missing for: " + song.getAudioName());
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        Toast.makeText(requireContext(), "Audio file missing", Toast.LENGTH_SHORT).show();

        song.setDownloaded(false);
        song.setDownloadPath(null);
        searchAdapter.notifyDataSetChanged();
    }

    private void handleNotDownloaded(AudioModel song, ProgressBar progressBar) {
        Log.e(TAG, "Song not downloaded: " + song.getAudioName());
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        Toast.makeText(requireContext(), "This song is not downloaded", Toast.LENGTH_SHORT).show();
    }

    private void saveAudioLocally(AudioModel audio) {
        if (getContext() != null) {
            android.content.SharedPreferences prefs = getContext().getSharedPreferences("SavedAudio", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putString("audioName", audio.getAudioName());
            editor.putString("audioUrl", audio.getAudioUrl());
            editor.putString("imageUrl", audio.getImageUrl());
            editor.putString("audioArtist", audio.getcategoryName());

            // Make sure to save the category ID
            String categoryId = audio.getCategoryId();
            if (categoryId != null && !categoryId.isEmpty()) {
                editor.putString("audioCategoryId", categoryId);
                Log.d(TAG, "Saved audio with category ID: " + categoryId);
            }

            editor.apply();
        }
    }

    private void broadcastSongChanged() {
        if (getContext() != null) {
            Intent songChangedIntent = new Intent("SONG_CHANGED");
            songChangedIntent.setPackage(getContext().getPackageName());
            getContext().sendBroadcast(songChangedIntent);

            Intent adapterIntent = new Intent("UPDATE_AUDIO_ADAPTER");
            adapterIntent.setPackage(getContext().getPackageName());
            getContext().sendBroadcast(adapterIntent);

            Log.d(TAG, "Broadcasts sent from SearchDownloadedFragment");
        }
    }

    // Register player update receiver to listen for song changes
    private void registerPlayerUpdateReceiver() {
        if (!isReceiverRegistered && getContext() != null) {
            playerUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Log.d(TAG, "Player broadcast received in SearchDownloadedFragment: " + action);

                    if (action != null) {
                        switch (action) {
                            case "UPDATE_AUDIO_ADAPTER":
                            case "SONG_CHANGED":
                                // Update adapter when song changes
                                requireActivity().runOnUiThread(() -> {
                                    if (searchAdapter != null) {
                                        searchAdapter.clearPreparingProgress();
                                        searchAdapter.updatePlayingIndicators();
                                        Log.d(TAG, "Search adapter updated due to player change");
                                    }
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
            Log.d(TAG, "Player broadcast receivers registered in SearchDownloadedFragment");
        }
    }

    // Unregister player update receiver
    private void unregisterPlayerUpdateReceiver() {
        if (isReceiverRegistered && playerUpdateReceiver != null && getContext() != null) {
            try {
                getContext().unregisterReceiver(playerUpdateReceiver);
                getContext().unregisterReceiver(prepareReceiver);
                isReceiverRegistered = false;
                Log.d(TAG, "Player broadcast receivers unregistered from SearchDownloadedFragment");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering player receiver: " + e.getMessage());
            }
        }
    }

    private void hideKeyboard() {
        if (getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            View view = getActivity().getCurrentFocus();
            if (view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        loadPlaylistSongs();

        // Register player update receiver
        registerPlayerUpdateReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        hideKeyboard();

        // Unregister player update receiver
        unregisterPlayerUpdateReceiver();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        searchHandler = null;
    }
}