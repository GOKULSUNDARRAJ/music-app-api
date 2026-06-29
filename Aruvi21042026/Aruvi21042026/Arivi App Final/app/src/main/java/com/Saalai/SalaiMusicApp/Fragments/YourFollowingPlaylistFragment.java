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

import com.Saalai.SalaiMusicApp.Adapters.YourFollowingPlaylistSectionAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Custom.CustomRefreshLayout;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Models.PlaylistSection;
import com.Saalai.SalaiMusicApp.Models.RecentPlayRequest;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.FollowedArtistsResponse;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerPlaylistSectionAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class YourFollowingPlaylistFragment extends Fragment implements
        YourFollowingPlaylistSectionAdapter.OnCategoryClickListener,
        CustomRefreshLayout.OnRefreshListener {

    private static final String TAG = "YourFollowingPlaylistFragment";

    private RecyclerView sectionsRecyclerView;
    private YourFollowingPlaylistSectionAdapter sectionAdapter;
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
    private BroadcastReceiver followUpdateReceiver;

    private static final int LOADING_DELAY = 1500;

    private ApiService apiService;
    private String accessToken;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_your_following_playlist, container, false);

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
        accessToken = sp.getAccessToken();

        if (ApiClient.isInitialized()) {
            apiService = ApiClient.getClient().create(ApiService.class);
            Log.d(TAG, "ApiService already initialized");
        } else {
            ApiClient.initialize(new ApiClient.ApiClientCallback() {
                @Override
                public void onUrlLoaded(String baseUrl) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            apiService = ApiClient.getClient().create(ApiService.class);
                            Log.d(TAG, "API URL loaded: " + baseUrl);
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
        sectionAdapter = new YourFollowingPlaylistSectionAdapter(new ArrayList<>(), this);
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

        // Receiver for follow updates to refresh the list
        followUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("FOLLOW_UPDATED".equals(intent.getAction())) {
                    Log.d(TAG, "Follow updated, refreshing data");
                    refreshData();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("UPDATE_AUDIO_ADAPTER");
        filter.addAction("UPDATE_MINI_PLAYER");

        IntentFilter followFilter = new IntentFilter();
        followFilter.addAction("FOLLOW_UPDATED");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(songChangeReceiver, filter, Context.RECEIVER_EXPORTED);
            requireActivity().registerReceiver(followUpdateReceiver, followFilter, Context.RECEIVER_EXPORTED);
        } else {
            requireActivity().registerReceiver(songChangeReceiver, filter);
            requireActivity().registerReceiver(followUpdateReceiver, followFilter);
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
        if (followUpdateReceiver != null) {
            try {
                requireActivity().unregisterReceiver(followUpdateReceiver);
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
        Log.d(TAG, "refreshData: Refreshing following list");

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
        Log.d(TAG, "showContent: Showing content");
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
        Log.d(TAG, "showEmptyState: No following artists found");
        hideShimmerLoading();
        sectionsRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        isDataLoaded = false;

        // Set empty state message
        TextView emptyText = emptyStateLayout.findViewById(R.id.emptyStateText);
        if (emptyText != null) {
            emptyText.setText("You aren't following any artists yet.\n\nTap the Follow button on any artist to add them here.");
        }

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
        Log.d(TAG, "loadData: Loading followed artists");

        if (apiService == null) {
            Log.e(TAG, "loadData: API service not initialized");
            showEmptyState();
            return;
        }

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "loadData: No access token available");
            SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
            if (sp != null) {
                accessToken = sp.getAccessToken();
                Log.d(TAG, "loadData: Retrieved access token");
            }
            if (accessToken == null || accessToken.isEmpty()) {
                showEmptyState();
                Toast.makeText(getContext(), "Please login to see your followed artists", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String authHeader = accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;
        Log.d(TAG, "loadData: Making API call to get followed artists");

        Call<FollowedArtistsResponse> call = apiService.getFollowedArtists(authHeader);
        Log.d(TAG, "loadData: URL = " + call.request().url());

        call.enqueue(new Callback<FollowedArtistsResponse>() {
            @Override
            public void onResponse(Call<FollowedArtistsResponse> call, Response<FollowedArtistsResponse> response) {
                Log.d(TAG, "loadData - onResponse: Response code = " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    FollowedArtistsResponse followedResponse = response.body();
                    List<PlaylistSection> sections = followedResponse.getSections();

                    Log.d(TAG, "=== FOLLOWED ARTISTS LOADED WITH " + (sections != null ? sections.size() : 0) + " SECTIONS ===");

                    if (sections != null && !sections.isEmpty()) {
                        // Check if there are any artists in the sections
                        boolean hasArtists = false;
                        for (PlaylistSection section : sections) {
                            if (section.getArtists() != null && !section.getArtists().isEmpty()) {
                                hasArtists = true;
                                Log.d(TAG, "Section '" + section.getSectionName() + "' has " + section.getArtists().size() + " artists");
                                break;
                            }
                        }

                        if (hasArtists) {
                            if (sectionAdapter != null) {
                                sectionAdapter.updateData(sections);
                            }
                            showContent();
                            Log.d(TAG, "loadData: Successfully loaded followed artists");
                        } else {
                            Log.w(TAG, "No artists found in sections");
                            showEmptyState();
                        }
                    } else {
                        Log.w(TAG, "No sections found in response");
                        showEmptyState();
                    }
                } else {
                    Log.e(TAG, "API Error: " + response.code() + " - " + response.message());
                    showEmptyState();

                    if (response.code() == 401) {
                        Toast.makeText(getContext(), "Session expired. Please login again.", Toast.LENGTH_LONG).show();
                    } else if (response.code() == 404) {
                        // No following artists yet - this is not an error
                        Log.d(TAG, "No followed artists found (404)");
                        showEmptyState();
                    } else {
                        Toast.makeText(getContext(), "Error: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<FollowedArtistsResponse> call, Throwable t) {
                Log.e(TAG, "Network Failure: " + t.getMessage());
                t.printStackTrace();
                showEmptyState();
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
        args.putBoolean("from_artist", true); // Mark as from artist to show follow button
        args.putString("artist_id", categoryId); // Pass the artist ID for follow functionality
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
        String token = sp.getAccessToken();

        if (token == null || token.isEmpty()) {
            return;
        }

        RecentPlayRequest request = new RecentPlayRequest(categoryId);
        String authHeader = "Bearer " + token;

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
        Log.d(TAG, "onResume called - isDataLoaded: " + isDataLoaded + ", isLoading: " + isLoading);

        refreshAllAdapters();
        broadcastMiniPlayerUpdate();

        // Refresh data to get latest follow status
        if (isDataLoaded) {
            refreshData();
        } else if (!isDataLoaded && !isLoading) {
            showShimmerLoading();
            loadDataWithDelay();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }
}