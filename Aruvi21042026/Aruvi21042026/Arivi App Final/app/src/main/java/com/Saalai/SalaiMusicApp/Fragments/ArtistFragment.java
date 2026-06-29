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
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.ArtistArtistAdapter;
import com.Saalai.SalaiMusicApp.Adapters.PlaylistSectionArtistAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Custom.CustomRefreshLayout;
import com.Saalai.SalaiMusicApp.Models.ArtistCategory;
import com.Saalai.SalaiMusicApp.Models.ArtistResponse;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Models.PlaylistSection;
import com.Saalai.SalaiMusicApp.R;

import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerPlaylistSectionAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArtistFragment extends Fragment implements ArtistArtistAdapter.OnArtistClickListener, CustomRefreshLayout.OnRefreshListener {

    private static final String TAG = "ArtistFragment";

    private RecyclerView sectionsRecyclerView;
    private PlaylistSectionArtistAdapter sectionAdapter;
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
        Log.d(TAG, "onCreateView called");
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
        btnRefresh.setOnClickListener(v -> retryLoading());

        // Setup broadcast receiver
        setupBroadcastReceiver();

        // Start loading data
        showShimmerLoading();
        loadDataWithDelay();

        return view;
    }

    private void initializeApiService() {
        Log.d(TAG, "initializeApiService called");

        if (ApiClient.isInitialized()) {
            apiService = ApiClient.getClient().create(ApiService.class);
            Log.d(TAG, "ApiService already initialized");
        } else {
            ApiClient.initialize(new ApiClient.ApiClientCallback() {
                @Override
                public void onUrlLoaded(String baseUrl) {
                    Log.d(TAG, "API URL loaded: " + baseUrl);
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
                    Log.e(TAG, "API initialization failed: " + error);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showEmptyState();
                            Toast.makeText(getContext(), "Failed to connect to server", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onNoUrlsAvailable() {
                    Log.e(TAG, "No URLs available");
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
        sectionAdapter = new PlaylistSectionArtistAdapter(new ArrayList<PlaylistSection>(), this);
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
        Log.d(TAG, "Refresh triggered");
        new Handler().postDelayed(() -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::refreshData);
            }
        }, LOADING_DELAY);
    }

    private void refreshData() {
        if (sectionAdapter != null) {
            sectionAdapter.updateData(new ArrayList<PlaylistSection>());
        }
        loadData();
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
        Log.d(TAG, "showContent called");
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
        Log.d(TAG, "showEmptyState called");
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
        Log.d(TAG, "loadData called - API ONLY MODE");

        if (apiService == null) {
            Log.e(TAG, "apiService is null, re-initializing...");
            initializeApiService();
            if (apiService == null) {
                Log.e(TAG, "apiService still null, showing empty state");
                showEmptyState();
                Toast.makeText(getContext(), "API service not available", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
        String accessToken = sp.getAccessToken();
        String authHeader = accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;

        Log.d(TAG, "Loading artist data from API ONLY");

        Call<ArtistResponse> call = apiService.getArtistData(authHeader);

        call.enqueue(new Callback<ArtistResponse>() {
            @Override
            public void onResponse(Call<ArtistResponse> call, Response<ArtistResponse> response) {
                Log.d(TAG, "API Response received - Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    ArtistResponse artistResponse = response.body();
                    List<PlaylistSection> sections = artistResponse.getSections();

                    Log.d(TAG, "=== API DATA LOADED WITH " + (sections != null ? sections.size() : 0) + " SECTIONS ===");

                    if (sections != null && !sections.isEmpty()) {
                        boolean hasArtists = false;
                        for (PlaylistSection section : sections) {
                            if (section.getArtistCategories() != null && !section.getArtistCategories().isEmpty()) {
                                hasArtists = true;
                                break;
                            }
                        }

                        if (hasArtists) {
                            if (sectionAdapter != null) {
                                sectionAdapter.updateData(sections);
                            }
                            showContent();
                        } else {
                            Log.w(TAG, "No artist categories found in API response");
                            showEmptyState();
                            Toast.makeText(getContext(), "No artist data available", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "No sections found in API response");
                        showEmptyState();
                        Toast.makeText(getContext(), "No sections found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "API Error: " + response.code() + " - " + response.message());
                    showEmptyState();

                    if (response.code() == 401) {
                        Toast.makeText(getContext(), "Session expired. Please login again.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "API Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ArtistResponse> call, Throwable t) {
                Log.e(TAG, "Network Failure: " + t.getMessage());
                t.printStackTrace();
                showEmptyState();
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onArtistClick(String artistName, List<AudioModel> songs, String artistImageUrl,String categoryId) {
        Log.d(TAG, "Artist clicked: " + artistName);

        AudioFragment fragment = new AudioFragment();
        Bundle args = new Bundle();
        args.putString("artist_name", artistName);
        args.putString("artist_image", artistImageUrl);
        args.putString("artist_id",categoryId );
        args.putSerializable("songs_list", new ArrayList<>(songs));

        args.putBoolean("from_artist", true);

        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

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
            Log.d(TAG, "Broadcasting mini player update");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called - isDataLoaded: " + isDataLoaded + ", isLoading: " + isLoading);
        refreshAllAdapters();
        broadcastMiniPlayerUpdate();

        if (!isDataLoaded && !isLoading) {
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