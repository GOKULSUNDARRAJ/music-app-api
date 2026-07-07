package com.Saalai.SalaiMusicApp.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.Saalai.SalaiMusicApp.Adapters.ArtistAdapter;
import com.Saalai.SalaiMusicApp.Adapters.PlaylistSectionAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Custom.CustomRefreshLayout;
import com.Saalai.SalaiMusicApp.Models.ArtistCategory;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Models.PlaylistSection;
import com.Saalai.SalaiMusicApp.Models.RecentPlayRequest;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.HomeResponse;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerPlaylistSectionAdapter;


import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AudioHomeFragment extends Fragment implements ArtistAdapter.OnArtistClickListener, CustomRefreshLayout.OnRefreshListener {

    private RecyclerView sectionsRecyclerView;
    private PlaylistSectionAdapter sectionAdapter;
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

    private static final int LOADING_DELAY = 1500;

    private ApiService apiService;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio_home, container, false);

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
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retryLoading();
            }
        });

        // Setup broadcast receiver
        setupBroadcastReceiver();

        // Start loading data
        showShimmerLoading();
        loadDataWithDelay();

        return view;
    }



    private void initializeApiService() {
        if (ApiClient.isInitialized()) {
            apiService = ApiClient.getClient().create(ApiService.class);
        } else {
            // Initialize ApiClient if not already initialized
            ApiClient.initialize(new ApiClient.ApiClientCallback() {
                @Override
                public void onUrlLoaded(String baseUrl) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            apiService = ApiClient.getClient().create(ApiService.class);
                            // Retry loading data after API is initialized
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
                            Log.e("AudioHomeFragment", "API initialization failed: " + error);
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
        sectionAdapter = new PlaylistSectionAdapter(new ArrayList<PlaylistSection>(), this);
    }

    private void setupBroadcastReceiver() {
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

        IntentFilter filter = new IntentFilter();
        filter.addAction("UPDATE_AUDIO_ADAPTER");
        filter.addAction("UPDATE_MINI_PLAYER");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(songChangeReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            requireActivity().registerReceiver(songChangeReceiver, filter);
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
    }

    @Override
    public void onRefresh() {
        Log.d("AudioHomeFragment", "Refresh triggered");

        // Simulate network delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshData();
                        }
                    });
                }
            }
        }, LOADING_DELAY);
    }

    private void refreshData() {
        // Clear existing data
        if (sectionAdapter != null) {
            sectionAdapter.updateData(new ArrayList<PlaylistSection>());
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

        // Show shimmer adapter
        if (getContext() != null) {
            shimmerAdapter = new ShimmerPlaylistSectionAdapter(getContext(), 6);
            sectionsRecyclerView.setAdapter(shimmerAdapter);
        }

        emptyStateLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        sectionsRecyclerView.setVisibility(View.VISIBLE);

        // Hide refresh header if visible
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

        // Hide refresh header
        if (refreshHeader != null) {
            refreshHeader.setVisibility(View.GONE);
        }
    }

    private void showProgressBar() {
        sectionsRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        // Hide refresh header
        if (refreshHeader != null) {
            refreshHeader.setVisibility(View.GONE);
        }
    }

    private void retryLoading() {
        showProgressBar();
        loadDataWithDelay();
    }

    private void loadDataWithDelay() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadData();
                        }
                    });
                }
            }
        }, LOADING_DELAY);
    }

    private void loadData() {
        // Removed `if (isLoading) return;` because showShimmerLoading() already sets it to true,
        // which was causing the initial data fetch to get skipped and stuck in shimmer forever.
        isLoading = true;

        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
        // Force refresh from disk
        String accessToken = sp.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.w("AudioHomeFragment", "No access token found, skipping authenticated call");
            isLoading = false;
            showEmptyState();
            return;
        }

        String authHeader = accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;
        Log.d("AudioHomeFragment", "Loading home data with token: " + authHeader.substring(0, Math.min(20, authHeader.length())) + "...");

        Call<HomeResponse> call = apiService.getHomeData(authHeader);

        call.enqueue(new Callback<HomeResponse>() {
            @Override
            public void onResponse(Call<HomeResponse> call, Response<HomeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    HomeResponse homeResponse = response.body();
                    List<PlaylistSection> sections = homeResponse.getSections();

                    Log.d("AudioHomeFragment", "=== API DATA LOADED WITH " + (sections != null ? sections.size() : 0) + " SECTIONS ===");

                    if (sections != null && !sections.isEmpty()) {
                        // Update adapter with sections from API
                        if (sectionAdapter != null) {
                            sectionAdapter.updateData(sections);
                        }
                        showContent();
                    } else {
                        Log.w("AudioHomeFragment", "No sections found in API response");
                        showEmptyState();
                        Toast.makeText(getContext(), "No data available", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Handle error response
                    Log.e("AudioHomeFragment", "API Error: " + response.code() + " - " + response.message());

                    // Try to parse error body
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e("AudioHomeFragment", "Error Body: " + errorBody);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    showEmptyState();

                    if (response.code() == 401) {
                        // Unauthorized - token expired
                        Toast.makeText(getContext(), "Session expired. Please login again.", Toast.LENGTH_LONG).show();
                        // Navigate to login screen
                    } else {
                        Toast.makeText(getContext(), "Error: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<HomeResponse> call, Throwable t) {
                Log.e("AudioHomeFragment", "Network Failure: " + t.getMessage());
                t.printStackTrace();

                showEmptyState();
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();

                // If API fails, you can optionally fallback to local JSON
                // loadDataFromLocalJson();
            }
        });
    }


    @Override
    public void onArtistClick(String artistName, List<AudioModel> songs, String artistImageUrl) {
        Log.d("AudioHomeFragment", "Artist clicked: " + artistName);

        // Find the categoryId from the artist's songs
        String categoryId = null;
        if (songs != null && !songs.isEmpty()) {
            // Get categoryId from the first song (assuming all songs in this artist have same categoryId)
            categoryId = songs.get(0).getCategoryId();
            Log.d("AudioHomeFragment", "CategoryId for artist '" + artistName + "': " + categoryId);
        }

        // Record the recent play (fire and forget - don't wait for response)
        if (categoryId != null) {
            recordRecentPlay(categoryId);
        }

        // Navigate to AudioFragment with the selected artist's songs
        AudioFragment fragment = new AudioFragment();
        Bundle args = new Bundle();
        args.putString("artist_name", artistName);
        args.putString("artist_image", artistImageUrl);
        Log.d("artist", "artist_image: " + artistImageUrl);
        args.putSerializable("songs_list", new ArrayList<>(songs));
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        // Broadcast to update mini player immediately
        broadcastMiniPlayerUpdate();
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
            Log.d("AudioHomeFragment", "Broadcasting mini player update");
        }
    }


    private void recordRecentPlay(String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) {
            Log.d("AudioHomeFragment", "Cannot record recent play: categoryId is null or empty");
            return;
        }

        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
        // Force refresh from disk

        String accessToken = sp.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.d("AudioHomeFragment", "Cannot record recent play: No access token");
            return;
        }

        // Ensure token has Bearer prefix
        String authHeader = accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;

        // Log the full URL that will be called
        String baseUrl = ApiClient.getCurrentBaseUrl();
        Log.d("AudioHomeFragment", "Recording recent play to: " + baseUrl + "user/recordRecentPlay");
        Log.d("AudioHomeFragment", "With categoryId: " + categoryId);
        Log.d("AudioHomeFragment", "With token: " + authHeader.substring(0, Math.min(20, authHeader.length())) + "...");

        RecentPlayRequest request = new RecentPlayRequest(categoryId);

        Call<Void> call = apiService.recordRecentPlay(authHeader, request);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("AudioHomeFragment", "Successfully recorded recent play for category: " + categoryId);
                } else {
                    Log.e("AudioHomeFragment", "Failed to record recent play: " + response.code() + " - " + response.message());
                    // Try to get error body for more details
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e("AudioHomeFragment", "Error body: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e("AudioHomeFragment", "Error reading error body", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("AudioHomeFragment", "Network error recording recent play: " + t.getMessage());
            }
        });
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
        // Clean up any animations if needed
    }


}