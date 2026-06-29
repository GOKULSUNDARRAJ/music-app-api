package com.Saalai.SalaiMusicApp.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.Saalai.SalaiMusicApp.Adapters.TopTabAdapterMusic;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Models.NavigationDataManager;
import com.Saalai.SalaiMusicApp.Models.TopNavItem;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Fragments.TopMenuViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    // Views
    private FrameLayout fragmentContainer;
    private RecyclerView topTabsRecycler;

    // Data
    private List<TopNavItem> topNavItems = new ArrayList<>();
    private int selectedTab = 0;

    // Adapters
    private TopTabAdapterMusic topTabAdapter;

    // ViewModel
    private TopMenuViewModel viewModel;

    // Current fragment
    private Fragment currentFragment;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(TopMenuViewModel.class);

        initializeViews(view);
        setupTopTabsRecycler();

        // Observe ViewModel for top menu changes
        observeTopMenuViewModel();

        // Load from SharedPreferences as fallback
        loadTopMenuFromSharedPrefs();

        return view;
    }

    private void initializeViews(View view) {
        topTabsRecycler = view.findViewById(R.id.top_tabs_recycler);
        fragmentContainer = view.findViewById(R.id.fragment_container);
    }

    private void setupTopTabsRecycler() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                getContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        );

        topTabsRecycler.setLayoutManager(layoutManager);
        topTabsRecycler.setHasFixedSize(false);

        topTabAdapter = new TopTabAdapterMusic(
                requireContext(),
                topNavItems,
                new TopTabAdapterMusic.OnTabClickListener() {
                    @Override
                    public void onTabClick(int position) {
                        Log.d("HomeFragment", "Top tab clicked at: " + position);
                        selectTab(position);
                    }
                }
        );

        topTabsRecycler.setAdapter(topTabAdapter);
    }

    private void observeTopMenuViewModel() {
        viewModel.getTopMenuLiveData().observe(getViewLifecycleOwner(), new Observer<List<TopNavItem>>() {
            @Override
            public void onChanged(List<TopNavItem> items) {
                if (items != null && !items.isEmpty()) {
                    Log.d("HomeFragment", "Received top menu from ViewModel: " + items.size() + " items");
                    updateTopMenu(items);
                }
            }
        });
    }

    private void loadTopMenuFromSharedPrefs() {
        Log.d("HomeFragment", "Loading top menu from SharedPreferences...");

        NavigationDataManager navManager = NavigationDataManager.getInstance(requireContext());

        if (navManager.isNavigationLoaded()) {
            List<TopNavItem> topItems = navManager.getTopNavigation();
            if (!topItems.isEmpty()) {
                Log.d("HomeFragment", "Found top nav in SharedPreferences: " + topItems.size() + " items");
                updateTopMenu(topItems);
            }
        }
    }

    private void updateTopMenu(List<TopNavItem> items) {
        this.topNavItems.clear();
        this.topNavItems.addAll(items);

        if (topTabAdapter != null) {
            topTabAdapter.notifyDataSetChanged();
            if (!items.isEmpty()) {
                selectTab(0);
            }
        }
    }

    private void selectTab(int position) {
        if (position < 0 || position >= topNavItems.size()) {
            Log.e("HomeFragment", "Invalid tab position: " + position);
            return;
        }

        Log.d("HomeFragment", "selectTab: " + position);
        selectedTab = position;

        if (topTabAdapter != null) {
            topTabAdapter.setSelectedPosition(position);
        }

        loadFragmentForTab(position);
        scrollToPosition(position);
    }

    private void loadFragmentForTab(int position) {
        Log.d("HomeFragment", "loadFragmentForTab called with position: " + position);

        if (position >= 0 && position < topNavItems.size()) {
            TopNavItem item = topNavItems.get(position);
            String tabName = item.getTopmenuName();
            String tabNameLower = tabName.toLowerCase();

            Log.d("HomeFragment", "Tab position: " + position);
            Log.d("HomeFragment", "Tab name: " + tabName);
            Log.d("HomeFragment", "Tab name lowercase: " + tabNameLower);
            Log.d("HomeFragment", "TopNavItems size: " + topNavItems.size());

            Fragment fragment = null;

            switch (tabNameLower) {
                case "all":
                    Log.d("HomeFragment", "Creating AudioHomeFragment for 'all'");
                    fragment = new AudioHomeFragment();
                    break;
                case "artist":
                    Log.d("HomeFragment", "Creating ArtistFragment for 'artist'");
                    fragment = new ArtistFragment();
                    break;
                case "divotional":
                    Log.d("HomeFragment", "Creating DivotionalFragment for 'divotional'");
                    fragment = new DivotionalFragment();
                    break;
                default:
                    Log.d("HomeFragment", "No match found for: '" + tabNameLower + "', using default AudioHomeFragment");
                    fragment = new AudioHomeFragment();
                    break;
            }

            Log.d("HomeFragment", "Fragment created: " + (fragment != null ? fragment.getClass().getSimpleName() : "null"));
            Log.d("HomeFragment", "fragmentContainer is: " + (fragmentContainer != null ? "not null" : "null"));

            if (fragment != null && fragmentContainer != null) {
                currentFragment = fragment;
                Log.d("HomeFragment", "Starting FragmentTransaction to replace fragment_container");

                FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
                transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
                transaction.replace(R.id.fragment_container, fragment);
                transaction.commit();

                Log.d("HomeFragment", "FragmentTransaction committed for tab: " + tabName);
            } else {
                Log.e("HomeFragment", "Failed to load fragment - fragment is " + (fragment == null ? "null" : "ok") +
                        ", fragmentContainer is " + (fragmentContainer == null ? "null" : "ok"));
            }
        } else {
            Log.e("HomeFragment", "Invalid position: " + position + ", topNavItems size: " + topNavItems.size());
        }
    }

    private void scrollToPosition(int position) {
        if (topTabsRecycler != null && topTabAdapter != null) {
            topTabsRecycler.smoothScrollToPosition(position);
        }
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
            Log.d("HomeFragment", "Broadcasting mini player update");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAndPlaySavedAudio();
        broadcastMiniPlayerUpdate();
    }

    // Helper method to determine if a playlist is offline
    private boolean isOfflinePlaylist(ArrayList<AudioModel> playlist) {
        if (playlist == null || playlist.isEmpty()) return false;

        // Check if any song in the playlist is downloaded or has a local path
        for (AudioModel song : playlist) {
            // Check if URL is a local file path
            if (song.getAudioUrl() != null &&
                    (song.getAudioUrl().contains("/files/") ||
                            song.getAudioUrl().startsWith("/data/") ||
                            song.getAudioUrl().contains("/storage/"))) {
                return true;
            }

            // Check if download path exists
            if (song.getDownloadPath() != null && !song.getDownloadPath().isEmpty()) {
                return true;
            }

            // Check if downloaded flag is true
            if (song.isDownloaded()) {
                return true;
            }
        }
        return false;
    }

    private void checkAndPlaySavedAudio() {
        if (PlayerManager.getCurrentAudio() == null) {
            AudioModel savedAudio = loadSavedAudio();
            ArrayList<AudioModel> savedPlaylist = loadFullPlaylist();

            if (savedAudio != null && !savedPlaylist.isEmpty()) {
                // FIX: Determine the correct playlist type
                boolean isOffline = isOfflinePlaylist(savedPlaylist);
                PlayerManager.PlaylistType playlistType = isOffline ?
                        PlayerManager.PlaylistType.OFFLINE :
                        PlayerManager.PlaylistType.ONLINE;

                Log.d("HomeFragment", "Setting playlist type to: " + playlistType +
                        " (isOffline: " + isOffline + ")");

                // Set the audio list with the CORRECT type
                PlayerManager.setAudioList(savedPlaylist, playlistType);

                AudioModel audioToPlay = findAudioInPlaylist(savedAudio, savedPlaylist);

                if (audioToPlay != null) {
                    Log.d("HomeFragment", "Playing audio: " + audioToPlay.getAudioName() +
                            " with type: " + playlistType);

                    PlayerManager.playAudio(audioToPlay, () -> {
                        Log.d("HomeFragment", "Saved audio started playing: " + audioToPlay.getAudioName());
                        if (PlayerManager.isPlaying()) {
                            PlayerManager.pausePlayback();
                            Log.d("HomeFragment", "Paused any ongoing playback");
                        }

                        broadcastMiniPlayerUpdate();
                    });

                    Log.d("HomeFragment", "Playing saved audio from playlist: " + audioToPlay.getAudioName());
                }
            }
        } else {
            Log.d("HomeFragment", "Audio is already playing: " + PlayerManager.getCurrentAudio().getAudioName() +
                    " with type: " + PlayerManager.getCurrentPlaylistType());
        }
    }

    // In HomeFragment.java, update the loadSavedAudio() method:

    private AudioModel loadSavedAudio() {
        SharedPreferences prefs = requireContext().getSharedPreferences("SavedAudio", Context.MODE_PRIVATE);
        String name = prefs.getString("audioName", null);
        String url = prefs.getString("audioUrl", null);
        String image = prefs.getString("imageUrl", null);
        String artist = prefs.getString("audioArtist", null);
        String downloadPath = prefs.getString("downloadPath", null);
        String categoryId = prefs.getString("audioCategoryId", null);
        String songId = prefs.getString("songId", null);

        Log.d("SavedAudio", "Loading saved audio - Name: " + name +
                ", URL: " + url +
                ", Artist: " + artist +
                ", DownloadPath: " + downloadPath +
                ", CategoryId: " + categoryId +
                ", SongId: " + songId);

        if (name != null && url != null) {
            AudioModel savedAudio = new AudioModel(name, url, image);
            savedAudio.setcategoryName(artist != null ? artist : "Unknown Artist");

            // Set the song ID if available
            if (songId != null) {
                savedAudio.setSongId(songId);
            }

            // 🔴 CRITICAL: Set download info exactly as saved
            if (downloadPath != null && !downloadPath.isEmpty()) {
                savedAudio.setDownloadPath(downloadPath);
                savedAudio.setDownloaded(true);
                Log.d("SavedAudio", "Restored download path: " + downloadPath);
            }

            // 🔴 CRITICAL: Set category ID exactly as saved - NO MODIFICATIONS
            if (categoryId != null && !categoryId.isEmpty()) {
                savedAudio.setCategoryId(categoryId);
                Log.d("SavedAudio", "Restored category ID: " + categoryId);
            }

            Log.d("SavedAudio", "Successfully loaded: " + savedAudio.getAudioName() +
                    " | Downloaded: " + savedAudio.isDownloaded() +
                    " | Path: " + savedAudio.getDownloadPath() +
                    " | Category ID: " + savedAudio.getCategoryId());

            return savedAudio;
        } else {
            Log.d("SavedAudio", "No saved audio found or incomplete data");
            return null;
        }
    }

    private ArrayList<AudioModel> loadFullPlaylist() {
        ArrayList<AudioModel> playlist = new ArrayList<>();
        SharedPreferences prefs = requireContext().getSharedPreferences("SavedPlaylist", Context.MODE_PRIVATE);

        int playlistSize = prefs.getInt("playlistSize", 0);
        String playlistType = prefs.getString("playlistType", "ONLINE");

        Log.d("HomeFragment", "Loading playlist of type: " + playlistType + ", size: " + playlistSize);

        if (playlistSize > 0) {
            for (int i = 0; i < playlistSize; i++) {
                String name = prefs.getString("songName_" + i, null);
                String url = prefs.getString("songUrl_" + i, null);
                String image = prefs.getString("songImage_" + i, null);
                String artist = prefs.getString("songArtist_" + i, null);
                String downloadPath = prefs.getString("songDownloadPath_" + i, null);
                String categoryId = prefs.getString("songCategoryId_" + i, null);
                boolean isDownloaded = prefs.getBoolean("songDownloaded_" + i, false);

                if (name != null && url != null) {
                    AudioModel song = new AudioModel(name, url, image);
                    song.setcategoryName(artist != null ? artist : "Unknown Artist");

                    // Set category ID if available
                    if (categoryId != null && !categoryId.isEmpty()) {
                        song.setCategoryId(categoryId);
                    }

                    // Set download info if available
                    if (downloadPath != null) {
                        song.setDownloadPath(downloadPath);
                        song.setDownloaded(true);
                    } else if (isDownloaded) {
                        song.setDownloaded(true);
                    } else if (url != null && (url.contains("/files/") || url.startsWith("/data/") || url.contains("/storage/"))) {
                        song.setDownloadPath(url);
                        song.setDownloaded(true);
                    }

                    playlist.add(song);
                }
            }
            Log.d("HomeFragment", "Loaded full playlist with " + playlist.size() + " songs");
        } else {
            Log.d("HomeFragment", "No saved playlist found");
        }

        return playlist;
    }

    private AudioModel findAudioInPlaylist(AudioModel savedAudio, ArrayList<AudioModel> playlist) {
        // First try by URL
        for (AudioModel song : playlist) {
            if (song.getAudioUrl() != null && song.getAudioUrl().equals(savedAudio.getAudioUrl())) {
                return song;
            }
        }

        // Then try by download path
        if (savedAudio.getDownloadPath() != null) {
            for (AudioModel song : playlist) {
                if (song.getDownloadPath() != null && song.getDownloadPath().equals(savedAudio.getDownloadPath())) {
                    return song;
                }
            }
        }

        // Then try by name
        for (AudioModel song : playlist) {
            if (song.getAudioName() != null && song.getAudioName().equals(savedAudio.getAudioName())) {
                return song;
            }
        }

        return savedAudio;
    }
}