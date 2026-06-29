package com.Saalai.SalaiMusicApp.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.YourCreatedPlaylistSectionAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Custom.CustomRefreshLayout;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Models.ArtistCategory;

import com.Saalai.SalaiMusicApp.Models.PlaylistSection;
import com.Saalai.SalaiMusicApp.Models.RecentPlayRequest;
import com.Saalai.SalaiMusicApp.Models.SongItem;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.LibraryCategory;
import com.Saalai.SalaiMusicApp.Response.LibraryResponse;
import com.Saalai.SalaiMusicApp.Response.LibrarySection;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerPlaylistSectionAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class YourCreatedPlaylistFragment extends Fragment implements
        YourCreatedPlaylistSectionAdapter.OnCategoryClickListener,
        CustomRefreshLayout.OnRefreshListener {

    private static final String TAG = "YourCreatedPlaylistFragment";

    private RecyclerView sectionsRecyclerView;
    private YourCreatedPlaylistSectionAdapter sectionAdapter;
    private ShimmerPlaylistSectionAdapter shimmerAdapter;
    private LinearLayout emptyStateLayout;
    private Button btnRefresh;
    private ProgressBar progressBar;
    private CustomRefreshLayout customRefreshLayout;

    private View refreshHeader;
    private ProgressBar refreshProgressBar;

    private boolean isLoading = false;
    private boolean isDataLoaded = false;

    private BroadcastReceiver songChangeReceiver;
    private BroadcastReceiver likesUpdateReceiver;
    private BroadcastReceiver playlistUpdateReceiver;

    private static final int LOADING_DELAY = 1500;

    private ApiService apiService;
    private String accessToken;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_your_album_playlist, container, false);

        // Initialize main views
        sectionsRecyclerView = view.findViewById(R.id.sectionsRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        btnRefresh = view.findViewById(R.id.btnRefresh);
        progressBar = view.findViewById(R.id.progressBar);
        customRefreshLayout = view.findViewById(R.id.customRefreshLayout);

        // Initialize refresh header views
        refreshHeader = view.findViewById(R.id.refreshHeader);
        if (refreshHeader != null) {
            refreshProgressBar = refreshHeader.findViewById(R.id.refreshProgressBar);
        }

        // Initialize API Service
        initializeApiService();

        // Setup custom refresh layout
        if (customRefreshLayout != null) {
            customRefreshLayout.setOnRefreshListener(this);
        }

        // Setup recycler view
        setupRecyclerView();

        // Setup click listeners
        btnRefresh.setOnClickListener(v -> retryLoading());

        // Setup broadcast receivers
        setupBroadcastReceivers();

        // Start loading data
        showShimmerLoading();
        loadDataWithDelay();

        return view;
    }

    private void initializeApiService() {
        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
        // Force refresh from disk
        accessToken = sp.getAccessToken();

        if (ApiClient.isInitialized()) {
            apiService = ApiClient.getClient().create(ApiService.class);
        } else {
            ApiClient.initialize(new ApiClient.ApiClientCallback() {
                @Override
                public void onUrlLoaded(String baseUrl) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            apiService = ApiClient.getClient().create(ApiService.class);
                            if (!isDataLoaded && !isLoading) {
                                loadData();
                            }
                        });
                    }
                }

                @Override
                public void onAllUrlsFailed(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.e(TAG, "API initialization failed: " + error);
                            showEmptyState();
                            Toast.makeText(getContext(), "Failed to connect to server", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onNoUrlsAvailable() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showEmptyState();
                            Toast.makeText(getContext(), "No server URLs available", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        sectionsRecyclerView.setLayoutManager(layoutManager);
        sectionAdapter = new YourCreatedPlaylistSectionAdapter(new ArrayList<>(), this);
    }

    private void setupBroadcastReceivers() {
        // Receiver for song changes to update playing state
        songChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals("UPDATE_AUDIO_ADAPTER") || action.equals("UPDATE_MINI_PLAYER")) {
                        refreshAllAdapters();
                    }
                }
            }
        };

        // Receiver for likes updates to refresh the list
        likesUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("LIKES_UPDATED".equals(intent.getAction())) {
                    Log.d(TAG, "Likes updated, refreshing data");
                    refreshData();
                }
            }
        };

        // Receiver for playlist updates
        playlistUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("PLAYLIST_UPDATED".equals(intent.getAction())) {
                    Log.d(TAG, "Playlist updated, refreshing data");
                    refreshData();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("UPDATE_AUDIO_ADAPTER");
        filter.addAction("UPDATE_MINI_PLAYER");

        IntentFilter likesFilter = new IntentFilter();
        likesFilter.addAction("LIKES_UPDATED");

        IntentFilter playlistFilter = new IntentFilter();
        playlistFilter.addAction("PLAYLIST_UPDATED");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(songChangeReceiver, filter, Context.RECEIVER_EXPORTED);
            requireActivity().registerReceiver(likesUpdateReceiver, likesFilter, Context.RECEIVER_EXPORTED);
            requireActivity().registerReceiver(playlistUpdateReceiver, playlistFilter, Context.RECEIVER_EXPORTED);
        } else {
            requireActivity().registerReceiver(songChangeReceiver, filter);
            requireActivity().registerReceiver(likesUpdateReceiver, likesFilter);
            requireActivity().registerReceiver(playlistUpdateReceiver, playlistFilter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (songChangeReceiver != null) {
            try {
                requireActivity().unregisterReceiver(songChangeReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        if (likesUpdateReceiver != null) {
            try {
                requireActivity().unregisterReceiver(likesUpdateReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        if (playlistUpdateReceiver != null) {
            try {
                requireActivity().unregisterReceiver(playlistUpdateReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRefresh() {
        Log.d(TAG, "Refresh triggered");
        new Handler().postDelayed(() -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::refreshData);
            }
        }, LOADING_DELAY);
    }

    private void refreshData() {
        // Clear existing data
        if (sectionAdapter != null) {
            sectionAdapter.updateData(new ArrayList<>());
        }

        // Load fresh data from API
        loadData();

        // Tell custom refresh layout to finish
        if (customRefreshLayout != null) {
            customRefreshLayout.finishRefreshing();
        }
    }

    private void refreshAllAdapters() {
        if (sectionAdapter != null && isDataLoaded) {
            sectionAdapter.refreshPlayingState();
        }
    }

    private void showShimmerLoading() {
        isLoading = true;
        isDataLoaded = false;

        if (getContext() != null) {
            shimmerAdapter = new ShimmerPlaylistSectionAdapter(getContext(), 6);
            sectionsRecyclerView.setAdapter(shimmerAdapter);
        }

        emptyStateLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        sectionsRecyclerView.setVisibility(View.VISIBLE);

        if (refreshHeader != null) {
            refreshHeader.setVisibility(View.GONE);
        }
    }

    private void hideShimmerLoading() {
        isLoading = false;
    }

    private void showContent() {
        hideShimmerLoading();
        if (sectionAdapter != null) {
            sectionsRecyclerView.setAdapter(sectionAdapter);
        }
        emptyStateLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        sectionsRecyclerView.setVisibility(View.VISIBLE);
        isDataLoaded = true;
    }

    private void showEmptyState() {
        hideShimmerLoading();
        sectionsRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        isDataLoaded = false;

        if (refreshHeader != null) {
            refreshHeader.setVisibility(View.GONE);
        }
    }

    private void showProgressBar() {
        sectionsRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        if (refreshHeader != null) {
            refreshHeader.setVisibility(View.GONE);
        }
    }

    private void retryLoading() {
        showProgressBar();
        loadDataWithDelay();
    }

    private void loadDataWithDelay() {
        new Handler().postDelayed(() -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::loadData);
            }
        }, LOADING_DELAY);
    }

    private void loadData() {
        if (apiService == null) {
            Log.e(TAG, "API service not initialized");
            showEmptyState();
            return;
        }

        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
        // Force refresh from disk
        accessToken = sp.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "No access token available");
            showEmptyState();
            Toast.makeText(getContext(), "Please login to see your playlists", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure token has Bearer prefix
        String authHeader = accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;
        Log.d(TAG, "Loading playlists with token: " + authHeader.substring(0, Math.min(20, authHeader.length())) + "...");

        // Call the user playlists endpoint
        Call<LibraryResponse> call = apiService.getUserPlaylists(authHeader);

        call.enqueue(new Callback<LibraryResponse>() {
            @Override
            public void onResponse(Call<LibraryResponse> call, Response<LibraryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LibraryResponse libraryResponse = response.body();

                    if (libraryResponse.isSuccess() && libraryResponse.getSections() != null) {
                        List<LibrarySection> sections = libraryResponse.getSections();
                        Log.d(TAG, "=== USER PLAYLISTS LOADED WITH " + sections.size() + " SECTIONS ===");

                        // Convert LibraryResponse to PlaylistSection format for the adapter
                        List<PlaylistSection> playlistSections = convertToPlaylistSections(sections);

                        if (playlistSections != null && !playlistSections.isEmpty()) {
                            if (sectionAdapter != null) {
                                sectionAdapter.updateData(playlistSections);
                            }
                            showContent();
                        } else {
                            Log.w(TAG, "No playlists found");
                            showEmptyState();
                            // Set empty state message
                            TextView emptyText = emptyStateLayout.findViewById(R.id.emptyStateText);
                            if (emptyText != null) {
                                emptyText.setText("No playlists found.\n\nCreate your first playlist by tapping the + button.");
                            }
                        }
                    } else {
                        Log.w(TAG, "API returned unsuccessful or no sections");
                        showEmptyState();
                    }
                } else {
                    Log.e(TAG, "API Error: " + response.code() + " - " + response.message());
                    showEmptyState();

                    if (response.code() == 401) {
                        Toast.makeText(getContext(), "Session expired. Please login again.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Error: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<LibraryResponse> call, Throwable t) {
                Log.e(TAG, "Network Failure: " + t.getMessage());
                t.printStackTrace();
                showEmptyState();
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<PlaylistSection> convertToPlaylistSections(List<LibrarySection> librarySections) {
        List<PlaylistSection> playlistSections = new ArrayList<>();

        for (LibrarySection libSection : librarySections) {
            PlaylistSection playlistSection = new PlaylistSection();
            playlistSection.setSectionId(libSection.getSectionId());
            playlistSection.setSectionName(libSection.getSectionTitle());
            playlistSection.setLayoutType(libSection.getLayoutType());
            playlistSection.setSpanCount(libSection.getSpanCount());

            // Convert LibraryCategory to ArtistCategory
            List<ArtistCategory> artists = new ArrayList<>();
            if (libSection.getCategories() != null) {
                for (LibraryCategory category : libSection.getCategories()) {
                    // Only include playlists (isPlaylist = true)
                    if (category.isIsPlaylist()) {
                        ArtistCategory artistCategory = new ArtistCategory();
                        artistCategory.setCategoryId(category.getCategoryId());
                        artistCategory.setCategoryName(category.getCategoryName());
                        artistCategory.setArtistImageUrl(category.getCategoryImage());
                        artistCategory.setAdapterType(category.getAdapterType());

                        // Convert songs to AudioModel
                        List<AudioModel> songs = new ArrayList<>();
                        if (category.getSongs() != null) {
                            for (SongItem songItem : category.getSongs()) {
                                songs.add(songItem.toAudioModel());
                            }
                        }
                        artistCategory.setSongs(songs);
                        artists.add(artistCategory);
                    }
                }
            }

            playlistSection.setArtists(artists);
            playlistSections.add(playlistSection);
        }

        return playlistSections;
    }

    @Override
    public void onCategoryClick(String categoryName, List<AudioModel> songs, String categoryImageUrl, String categoryId) {
        Log.d(TAG, "Category clicked: " + categoryName + ", ID: " + categoryId);

        // Record the recent play
        if (categoryId != null) {
            recordRecentPlay(categoryId);
        }

        // Navigate to AudioFragment with the selected category's songs
        AudioFragment fragment = new AudioFragment();
        Bundle args = new Bundle();
        args.putString("artist_name", categoryName);
        args.putString("artist_image", categoryImageUrl);
        args.putSerializable("songs_list", new ArrayList<>(songs));
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        // Broadcast to update mini player immediately
        broadcastMiniPlayerUpdate();
    }

    private void recordRecentPlay(String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) {
            return;
        }

        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
        // Force refresh from disk
        String token = sp.getAccessToken();

        if (token == null || token.isEmpty()) {
            return;
        }

        // Ensure token has Bearer prefix
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        RecentPlayRequest request = new RecentPlayRequest(categoryId);

        Call<Void> call = apiService.recordRecentPlay(authHeader, request);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Successfully recorded recent play for category: " + categoryId);
                } else {
                    Log.e(TAG, "Failed to record recent play: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error recording recent play: " + t.getMessage());
            }
        });
    }

    private void broadcastMiniPlayerUpdate() {
        if (getContext() != null) {
            Intent intent = new Intent("UPDATE_MINI_PLAYER");
            intent.setPackage(getContext().getPackageName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getContext().sendBroadcast(intent, null);
            } else {
                getContext().sendBroadcast(intent);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAllAdapters();
        broadcastMiniPlayerUpdate();

        // Check if we need to refresh data
        if (!isDataLoaded && !isLoading) {
            showShimmerLoading();
            loadDataWithDelay();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}