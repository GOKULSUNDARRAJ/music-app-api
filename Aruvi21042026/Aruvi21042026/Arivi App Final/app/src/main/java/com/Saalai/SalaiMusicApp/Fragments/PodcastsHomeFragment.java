package com.Saalai.SalaiMusicApp.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.constraintlayout.widget.ConstraintLayout;
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

import com.Saalai.SalaiMusicApp.Adapters.ArtistAdapter;
import com.Saalai.SalaiMusicApp.Adapters.ArtistPodcastsAdapter;
import com.Saalai.SalaiMusicApp.Adapters.PlaylistSectionAdapter;
import com.Saalai.SalaiMusicApp.Adapters.PlaylistSectionPodcastsAdapter;
import com.Saalai.SalaiMusicApp.Custom.CustomRefreshLayout;
import com.Saalai.SalaiMusicApp.Models.ArtistCategory;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Models.PlaylistSection;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerPlaylistSectionAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PodcastsHomeFragment extends Fragment implements ArtistPodcastsAdapter.OnArtistClickListener, CustomRefreshLayout.OnRefreshListener {

    private RecyclerView sectionsRecyclerView;
    private PlaylistSectionPodcastsAdapter sectionAdapter;
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

    private ConstraintLayout createpodcastsbtn;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_podcasts_home, container, false);

        // Initialize main views
        sectionsRecyclerView = view.findViewById(R.id.sectionsRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        btnRefresh = view.findViewById(R.id.btnRefresh);
        progressBar = view.findViewById(R.id.progressBar);
        customRefreshLayout = view.findViewById(R.id.customRefreshLayout);
        createpodcastsbtn=view.findViewById(R.id.createpodcastsbtn);
        
        // Initialize refresh header views
        refreshHeader = view.findViewById(R.id.refreshHeader);
        if (refreshHeader != null) {
            refreshProgressBar = refreshHeader.findViewById(R.id.refreshProgressBar);
        }

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


        createpodcastsbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToCreateFragment();
            }
        });

        return view;
    }
    private void navigateToCreateFragment() {
        CreatepodcastFragment createFragment = new CreatepodcastFragment();

        Bundle args = new Bundle();
        createFragment.setArguments(args);

        // Navigate to the new fragment
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, createFragment)
                .addToBackStack(null) // This allows user to navigate back
                .commit();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        sectionsRecyclerView.setLayoutManager(layoutManager);
        sectionAdapter = new PlaylistSectionPodcastsAdapter(new ArrayList<PlaylistSection>(), this);
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

        // Load fresh data
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
        Log.d("AudioHomeFragment", "=== LOADING DATA FROM JSON ===");

        try {
            // Read JSON file from raw resources
            InputStream is = getResources().openRawResource(R.raw.home_data_podcasts);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            // Parse JSON using JSONObject
            JSONObject jsonObject = new JSONObject(json);
            JSONArray sectionsArray = jsonObject.getJSONArray("sections");

            List<PlaylistSection> sections = new ArrayList<>();

            for (int i = 0; i < sectionsArray.length(); i++) {
                JSONObject sectionObj = sectionsArray.getJSONObject(i);

                String sectionId = sectionObj.getString("sectionId");
                String sectionTitle = sectionObj.getString("sectionTitle");
                int layoutType = sectionObj.getInt("layoutType");
                int spanCount = sectionObj.getInt("spanCount");

                JSONArray artistCategoriesArray = sectionObj.getJSONArray("categories");
                List<ArtistCategory> artistCategories = new ArrayList<>();

                for (int j = 0; j < artistCategoriesArray.length(); j++) {
                    JSONObject categoryObj = artistCategoriesArray.getJSONObject(j);

                    String categoryId = categoryObj.getString("categoryId");
                    String categoryName = categoryObj.getString("categoryName");
                    String categoryImage = categoryObj.getString("categoryImage");
                    int adapterType = categoryObj.getInt("adapterType");

                    JSONArray songsArray = categoryObj.getJSONArray("songs");
                    List<AudioModel> songs = new ArrayList<>();

                    for (int k = 0; k < songsArray.length(); k++) {
                        JSONObject songObj = songsArray.getJSONObject(k);

                        String songId = songObj.getString("songId");

                        String audioName;
                        if (songObj.has("audioName")) {
                            audioName = songObj.getString("audioName");
                        } else {
                            audioName = songObj.getString("name");
                        }

                        String audioUrl = songObj.getString("audioUrl");

                        String category;
                        if (songObj.has("category")) {
                            category = songObj.getString("category");
                        } else {
                            category = categoryName;
                        }

                        String imageUrl = songObj.getString("imageUrl");
                        String catId = songObj.getString("categoryId");

                        AudioModel song = new AudioModel(songId, audioName, audioUrl, category, imageUrl);
                        song.setCategoryId(catId);
                        songs.add(song);
                    }

                    ArtistCategory artistCategory = new ArtistCategory(categoryId, categoryName, songs, categoryImage, adapterType);
                    artistCategories.add(artistCategory);
                }

                PlaylistSection section = new PlaylistSection(sectionId, sectionTitle, artistCategories, layoutType, spanCount);
                sections.add(section);
            }

            // Update adapter with all sections
            if (sectionAdapter != null) {
                sectionAdapter.updateData(sections);
            }
            showContent();

            Log.d("AudioHomeFragment", "=== DATA LOADED WITH " + sections.size() + " SECTIONS ===");

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("AudioHomeFragment", "Error loading JSON: " + e.getMessage());
            showEmptyState();
        }
    }

    @Override
    public void onArtistClick(String artistName, List<AudioModel> songs, String artistImageUrl) {
        Log.d("AudioHomeFragment", "Artist clicked: " + artistName);

        // Navigate to AudioFragment with the selected artist's songs
        AudioFragment fragment = new AudioFragment();
        Bundle args = new Bundle();
        args.putString("artist_name", artistName);
        args.putString("artist_image", artistImageUrl);
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