package com.Saalai.SalaiMusicApp.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.AudioAdapter;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;

import java.util.ArrayList;
import java.util.Locale;

public class SearchFragment extends Fragment {

    private static final String TAG = "SearchFragment";
    private static final String ARG_PLAYLIST_SONGS = "playlist_songs";
    private static final String ARG_PLAYLIST_NAME = "playlist_name";
    private static final String ARG_PLAYLIST_IMAGE = "playlist_image";

    private static final String ACTION_PREPARE_NEXT = "PREPARE_NEXT";
    private static final String ACTION_PREPARE_PREVIOUS = "PREPARE_PREVIOUS";

    private EditText searchEditText;
    private ImageView btnBack;
    private ImageView tvCancel;
    private RecyclerView searchRecyclerView;
    private LinearLayout emptyStateLayout;
    private LinearLayout noResultsLayout;
    private TextView tvSongCount;

    private ArrayList<AudioModel> originalSongsList;
    private ArrayList<AudioModel> filteredSongsList;
    private AudioAdapter searchAdapter;
    private String playlistName;
    private String playlistImage;

    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    // Debounce timers for prepare receiver
    private long lastNextTime = 0;
    private long lastPrevTime = 0;
    private static final long DEBOUNCE_DELAY = 2000; // 2 seconds
    private int lastNextIndex = -1;
    private int lastPrevIndex = -1;

    private final BroadcastReceiver prepareReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (searchAdapter == null || getActivity() == null) return;

            String action = intent.getAction();
            long currentTime = System.currentTimeMillis();

            getActivity().runOnUiThread(() -> {
                if (ACTION_PREPARE_NEXT.equals(action)) {
                    int nextIndex = intent.getIntExtra("nextIndex", -1);

                    // Stricter debouncing: check time AND index
                    if (currentTime - lastNextTime < DEBOUNCE_DELAY && nextIndex == lastNextIndex) {
                        Log.d(TAG, "Ignoring duplicate next broadcast for index: " + nextIndex);
                        return;
                    }

                    lastNextTime = currentTime;
                    lastNextIndex = nextIndex;

                    if (nextIndex != -1 && nextIndex < filteredSongsList.size()) {
                        // Show progress on next item
                        searchAdapter.showNextItemProgress(nextIndex);
                        Log.d(TAG, "Received prepare next for index: " + nextIndex);
                    }
                }
                else if (ACTION_PREPARE_PREVIOUS.equals(action)) {
                    int prevIndex = intent.getIntExtra("prevIndex", -1);

                    // Stricter debouncing: check time AND index
                    if (currentTime - lastPrevTime < DEBOUNCE_DELAY && prevIndex == lastPrevIndex) {
                        Log.d(TAG, "Ignoring duplicate previous broadcast for index: " + prevIndex);
                        return;
                    }

                    lastPrevTime = currentTime;
                    lastPrevIndex = prevIndex;

                    if (prevIndex != -1 && prevIndex < filteredSongsList.size()) {
                        // Show progress on previous item
                        searchAdapter.showPreviousItemProgress(prevIndex);
                        Log.d(TAG, "Received prepare previous for index: " + prevIndex);
                    }
                }
            });
        }
    };

    private final BroadcastReceiver playerUpdateReceiver = new BroadcastReceiver() {
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
                            if (searchAdapter != null) {
                                searchAdapter.clearPreparingProgress();
                                searchAdapter.notifyDataSetChanged();
                            }
                            Log.d(TAG, "Search UI updated due to player change");
                        });
                        break;
                }
            }
        }
    };

    private boolean isReceiverRegistered = false;

    public static SearchFragment newInstance(ArrayList<AudioModel> songsList, String playlistName, String playlistImage) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PLAYLIST_SONGS, songsList);
        args.putString(ARG_PLAYLIST_NAME, playlistName);
        args.putString(ARG_PLAYLIST_IMAGE, playlistImage);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            originalSongsList = (ArrayList<AudioModel>) getArguments().getSerializable(ARG_PLAYLIST_SONGS);
            playlistName = getArguments().getString(ARG_PLAYLIST_NAME);
            playlistImage = getArguments().getString(ARG_PLAYLIST_IMAGE);

            if (originalSongsList == null) {
                originalSongsList = new ArrayList<>();
            }

            filteredSongsList = new ArrayList<>(originalSongsList);

            Log.d(TAG, "Received " + originalSongsList.size() + " songs for search");
        } else {
            originalSongsList = new ArrayList<>();
            filteredSongsList = new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        initViews(view);
        setupRecyclerView();
        setupSearchListener();
        setupClickListeners();

        // Show all songs initially instead of empty state
        showAllSongs();

        // Focus the search edit text and show keyboard
        searchEditText.requestFocus();
        if (getActivity() != null) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return view;
    }

    private void initViews(View view) {
        searchEditText = view.findViewById(R.id.searchEditText);
        btnBack = view.findViewById(R.id.btnBackSearch);
        tvCancel = view.findViewById(R.id.tvCancel);
        searchRecyclerView = view.findViewById(R.id.searchRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        noResultsLayout = view.findViewById(R.id.noResultsLayout);
        tvSongCount = view.findViewById(R.id.tvSongCount);
    }

    private void setupRecyclerView() {
        searchRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        searchAdapter = new AudioAdapter(filteredSongsList, getContext(), (audioModel, progressBar) -> {
            // Show progress on clicked item
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }

            // Play the selected song from search results
            PlayerManager.setAudioList(originalSongsList, PlayerManager.PlaylistType.ONLINE);

            PlayerManager.playAudio(audioModel, () -> {
                // This callback runs when audio starts playing
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                // Clear any preparing states in the adapter
                if (searchAdapter != null) {
                    searchAdapter.clearPreparingProgress();
                    searchAdapter.notifyDataSetChanged();
                }

                Toast.makeText(getContext(), "Playing: " + audioModel.getAudioName(), Toast.LENGTH_SHORT).show();

                Log.d(TAG, "Audio started playing, progress hidden for: " + audioModel.getAudioName());
            });

            // Update full player UI if needed
            if (getActivity() != null) {
                getActivity().sendBroadcast(new Intent("UPDATE_MINI_PLAYER"));
                getActivity().sendBroadcast(new Intent("SONG_CHANGED"));
            }
        });

        // Set RecyclerView reference for scrolling to preparing items
        searchAdapter.setRecyclerView(searchRecyclerView);

        searchRecyclerView.setAdapter(searchAdapter);
    }

    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Show cancel button when there's text
                if (s.length() > 0) {
                    tvCancel.setVisibility(View.VISIBLE);
                } else {
                    tvCancel.setVisibility(View.GONE);
                }

                // Debounce search to avoid too many updates
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> performSearch(s.toString());
                searchHandler.postDelayed(searchRunnable, 300); // 300ms delay
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Handle search action on keyboard
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchEditText.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            hideKeyboard();
            getParentFragmentManager().popBackStack();
        });

        tvCancel.setOnClickListener(v -> {
            searchEditText.setText("");
            tvCancel.setVisibility(View.GONE);
            showAllSongs();
            hideKeyboard();
        });
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            // When search is cleared, show all songs again
            showAllSongs();
            return;
        }

        String searchQuery = query.toLowerCase(Locale.getDefault()).trim();
        Log.d(TAG, "Searching for: " + searchQuery);

        filteredSongsList.clear();

        for (AudioModel song : originalSongsList) {
            boolean matches = false;

            // Search in song name
            if (song.getAudioName() != null &&
                    song.getAudioName().toLowerCase(Locale.getDefault()).contains(searchQuery)) {
                matches = true;
            }

            // Search in artist name
            if (!matches && song.getcategoryName() != null &&
                    song.getcategoryName().toLowerCase(Locale.getDefault()).contains(searchQuery)) {
                matches = true;
            }

            if (matches) {
                filteredSongsList.add(song);
            }
        }

        Log.d(TAG, "Found " + filteredSongsList.size() + " matches");

        // Update UI based on search results
        requireActivity().runOnUiThread(() -> {
            searchAdapter.updatePlaylist(filteredSongsList); // Use updatePlaylist method

            if (filteredSongsList.isEmpty()) {
                showNoResults();
            } else {
                showSearchResults();
            }
        });
    }

    private void showAllSongs() {
        // Reset filtered list to show all songs
        filteredSongsList.clear();
        filteredSongsList.addAll(originalSongsList);

        // Update adapter
        searchAdapter.updatePlaylist(filteredSongsList);

        // Show the recycler view with all songs
        searchRecyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
        noResultsLayout.setVisibility(View.GONE);

        // Show song count
        if (tvSongCount != null) {
            tvSongCount.setVisibility(View.VISIBLE);
            tvSongCount.setText(filteredSongsList.size() + " songs");
        }

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
        if (tvSongCount != null) {
            tvSongCount.setText(filteredSongsList.size() + " songs");
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

        // Create a single filter with ALL actions
        IntentFilter filter = new IntentFilter();
        filter.addAction("UPDATE_AUDIO_ADAPTER");
        filter.addAction("SONG_CHANGED");
        filter.addAction(ACTION_PREPARE_NEXT);
        filter.addAction(ACTION_PREPARE_PREVIOUS);

        // Register receivers with the comprehensive filter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(prepareReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            requireContext().registerReceiver(playerUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(prepareReceiver, filter);
            requireContext().registerReceiver(playerUpdateReceiver, filter);
        }

        isReceiverRegistered = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            // Unregister receivers
            requireContext().unregisterReceiver(prepareReceiver);
            requireContext().unregisterReceiver(playerUpdateReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered: " + e.getMessage());
        }
        isReceiverRegistered = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}