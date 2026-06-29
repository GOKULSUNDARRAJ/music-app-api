package com.Saalai.SalaiMusicApp.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.PlaylistAdapter;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Models.PlaylistModel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Utils.DownloadPlaylistManager;

import java.util.ArrayList;
import java.util.List;

public class DownloadPlaylistsFragment extends Fragment {

    private RecyclerView recyclerView;
    private PlaylistAdapter playlistAdapter;
    private DownloadPlaylistManager playlistManager;
    private TextView tvEmptyState;
    private ImageView btnBack;

    private ArrayList<PlaylistModel> playlistList = new ArrayList<>();

    // Track playing playlist by category ID
    private List<String> currentlyPlayingCategoryIds = new ArrayList<>();
    private boolean isReceiversRegistered = false;

    // Broadcast receiver for playlist updates
    private final BroadcastReceiver playlistUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("DownloadPlaylists", "Broadcast received: " + intent.getAction());
            if ("PLAYLIST_UPDATE".equals(intent.getAction()) ||
                    "DOWNLOAD_UPDATE".equals(intent.getAction())) {
                // Refresh the playlist list
                loadPlaylists();
            }
        }
    };

    // Broadcast receiver for player updates
    private final BroadcastReceiver playerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("DownloadPlaylists", "Player broadcast received: " + action);

            if ("SONG_CHANGED".equals(action) ||
                    "UPDATE_AUDIO_ADAPTER".equals(action) ||
                    "UPDATE_MINI_PLAYER".equals(action) ||
                    "PLAYLIST_SONGS_CLOSED".equals(action)) {
                // Check which playlist is currently playing BY CATEGORY ID
                updatePlayingPlaylistStatusByCategoryId();
            }
        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlistManager = DownloadPlaylistManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download_playlists, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerViewPlaylists);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        btnBack = view.findViewById(R.id.btnBack);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup adapter - now using category IDs for highlighting
        playlistAdapter = new PlaylistAdapter(playlistList, new PlaylistAdapter.PlaylistClickListener() {
            @Override
            public void onPlaylistClick(PlaylistModel playlist) {
                openPlaylist(playlist);
            }

            @Override
            public void onPlaylistDelete(PlaylistModel playlist) {
                showDeleteDialog(playlist);
            }

            @Override
            public void onPlaylistRename(PlaylistModel playlist) {
                // Handle rename if needed
            }
        });

        recyclerView.setAdapter(playlistAdapter);

        // Back button click
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initial load
        loadPlaylists();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("DownloadPlaylists", "onResume called");

        // Always refresh data when fragment becomes visible
        loadPlaylists();

        // Update playing status immediately with a short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            updatePlayingPlaylistStatusByCategoryId();
        }, 50);

        // Register broadcast receivers
        registerReceivers();
    }

    private void updatePlayingPlaylistStatusByCategoryId() {
        if (!isAdded() || getContext() == null) {
            Log.d("DownloadPlaylists", "Fragment not attached, skipping playlist status update");
            return;
        }

        AudioModel currentAudio = PlayerManager.getCurrentAudio();

        if (currentAudio == null) {
            currentlyPlayingCategoryIds.clear();
            if (playlistAdapter != null) {
                playlistAdapter.setCurrentlyPlayingPlaylists(new ArrayList<>());
                playlistAdapter.notifyDataSetChanged();
            }
            return;
        }

        String currentCategoryId = currentAudio.getCategoryId();
        if (currentCategoryId == null || currentCategoryId.isEmpty()) {
            Log.d("DownloadPlaylists", "No category ID found for current song");
            currentlyPlayingCategoryIds.clear();
            if (playlistAdapter != null) {
                playlistAdapter.setCurrentlyPlayingPlaylists(new ArrayList<>());
                playlistAdapter.notifyDataSetChanged();
            }
            return;
        }

        // 🔴 ENHANCED MATCHING: Check both with and without _download suffix
        List<String> possibleSongCategoryIds = new ArrayList<>();
        possibleSongCategoryIds.add(currentCategoryId);

        // If song has cat_001, also check cat_001_download
        if (!currentCategoryId.endsWith("_download")) {
            possibleSongCategoryIds.add(currentCategoryId + "_download");
        }

        // If song has cat_001_download, also check cat_001
        if (currentCategoryId.endsWith("_download")) {
            possibleSongCategoryIds.add(currentCategoryId.replace("_download", ""));
        }

        Log.d("DownloadPlaylists", "Current song category ID: " + currentCategoryId);
        Log.d("DownloadPlaylists", "Checking against possible IDs: " + possibleSongCategoryIds);

        List<String> matchingPlaylistCategoryIds = new ArrayList<>();
        for (PlaylistModel playlist : playlistList) {
            String playlistCategoryId = playlist.getOriginalCategoryId();
            Log.d("DownloadPlaylists", "Checking playlist: " + playlist.getName() +
                    " | Category ID: " + playlistCategoryId);

            if (playlistCategoryId != null) {
                // Check if playlist category ID matches any of the possible song category IDs
                for (String possibleId : possibleSongCategoryIds) {
                    if (possibleId.equals(playlistCategoryId)) {
                        matchingPlaylistCategoryIds.add(playlistCategoryId);
                        Log.d("DownloadPlaylists", "✅ MATCH FOUND!");
                        Log.d("DownloadPlaylists", "   Playlist: " + playlist.getName());
                        Log.d("DownloadPlaylists", "   Playlist Category ID: " + playlistCategoryId);
                        Log.d("DownloadPlaylists", "   Matched with possible ID: " + possibleId);
                        break;
                    }
                }
            }
        }

        currentlyPlayingCategoryIds = matchingPlaylistCategoryIds;
        if (playlistAdapter != null) {
            playlistAdapter.setCurrentlyPlayingPlaylists(currentlyPlayingCategoryIds);
            playlistAdapter.notifyDataSetChanged();
            Log.d("DownloadPlaylists", "Updated adapter with " + currentlyPlayingCategoryIds.size() +
                    " playing playlists: " + currentlyPlayingCategoryIds);
        }
    }

    private void loadPlaylists() {
        Log.d("DownloadPlaylists", "Loading playlists...");

        if (!isAdded() || getContext() == null) {
            Log.d("DownloadPlaylists", "Fragment not attached, skipping load playlists");
            return;
        }

        playlistList.clear();
        ArrayList<PlaylistModel> playlists = playlistManager.getAllPlaylists();

        Log.d("DownloadPlaylists", "Found " + (playlists != null ? playlists.size() : 0) + " playlists");

        if (playlists != null && !playlists.isEmpty()) {
            playlistList.addAll(playlists);

            // ✅ ADD EXTRA DEBUG FOR EACH PLAYLIST
            Log.d("DownloadPlaylists", "=== LOADED PLAYLISTS DEBUG ===");
            for (int i = 0; i < playlistList.size(); i++) {
                PlaylistModel playlist = playlistList.get(i);
                String categoryId = playlist.getOriginalCategoryId();
                String normalizedId = categoryId != null && categoryId.endsWith("_download") ?
                        categoryId.replace("_download", "") : categoryId;

                Log.d("DownloadPlaylists", "[" + i + "] " + playlist.getName() +
                        " | Original Cat ID: " + categoryId +
                        " | Normalized Cat ID: " + normalizedId +
                        " | Songs: " + (playlist.getSongs() != null ? playlist.getSongs().size() : 0));
            }
            Log.d("DownloadPlaylists", "=== END DEBUG ===");

            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.GONE);
            }

            // Update playing status after loading
            new Handler(Looper.getMainLooper()).post(() -> {
                updatePlayingPlaylistStatusByCategoryId();
            });
        } else {
            Log.d("DownloadPlaylists", "No playlists found");
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("No playlists found\nTap + to create one");
            }
        }

        if (playlistAdapter != null) {
            playlistAdapter.notifyDataSetChanged();
        }
    }

    private void openPlaylist(PlaylistModel playlist) {
        // Update playlist with download info before opening
        playlistManager.updatePlaylistWithDownloadInfo(playlist.getId(), requireContext());

        // Get the playlist image URL (either from playlist or first song)
        String playlistImageUrl = getPlaylistImageUrl(playlist);
        Log.d("DownloadPlaylists", "Passing playlist image URL: " + playlistImageUrl);

        // Create fragment instance with playlist image URL
        DownloadPlaylistSongsFragment songsFragment = DownloadPlaylistSongsFragment.newInstance(
                playlist.getId(),
                playlistImageUrl,
                playlist.getName()
        );

        // Navigate to the fragment
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, songsFragment)
                .addToBackStack("playlist_songs")
                .commit();
    }

    private String getPlaylistImageUrl(PlaylistModel playlist) {
        // First, try to get image from playlist's cover image
        if (playlist.getImageUrl() != null && !playlist.getImageUrl().isEmpty()) {
            return playlist.getImageUrl();
        }

        // If playlist doesn't have a cover image, try to get from first song
        if (playlist.getSongs() != null && !playlist.getSongs().isEmpty()) {
            AudioModel firstSong = playlist.getSongs().get(0);
            if (firstSong != null && firstSong.getImageUrl() != null && !firstSong.getImageUrl().isEmpty()) {
                return firstSong.getImageUrl();
            }
        }

        // Return null if no image found
        return null;
    }

    private void showDeleteDialog(PlaylistModel playlist) {
        // Inflate custom layout
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_delete_playlist, null);

        // Initialize views
        TextView tvPlaylistName = dialogView.findViewById(R.id.tvPlaylistName);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);

        // Set playlist name
        tvPlaylistName.setText(playlist.getName());

        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Set button click listeners
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            // Disable buttons during deletion
            btnDelete.setEnabled(false);
            btnCancel.setEnabled(false);
            btnDelete.setText("Deleting...");

            // Perform deletion
            boolean deleted = playlistManager.deletePlaylist(playlist.getId());

            if (deleted) {
                Toast.makeText(getContext(), "Playlist deleted", Toast.LENGTH_SHORT).show();
                loadPlaylists();
            } else {
                Toast.makeText(getContext(), "Failed to delete playlist", Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
        });

        // Show dialog
        dialog.show();

        // Set left and right margins
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Set width to 90% of screen and center it
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());

            // Calculate 90% of screen width
            DisplayMetrics displayMetrics = new DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            int dialogWidth = (int) (screenWidth * 0.80); // 90% of screen width

            layoutParams.width = dialogWidth;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER;

            dialog.getWindow().setAttributes(layoutParams);
        }
    }

    private void registerReceivers() {
        if (!isReceiversRegistered && getContext() != null) {
            try {
                // Register for playlist updates
                IntentFilter playlistFilter = new IntentFilter();
                playlistFilter.addAction("PLAYLIST_UPDATE");
                playlistFilter.addAction("DOWNLOAD_UPDATE");
                requireContext().registerReceiver(playlistUpdateReceiver, playlistFilter);

                // Register for player updates
                IntentFilter playerFilter = new IntentFilter();
                playerFilter.addAction("SONG_CHANGED");
                playerFilter.addAction("UPDATE_AUDIO_ADAPTER");
                playerFilter.addAction("UPDATE_MINI_PLAYER");
                playerFilter.addAction("PLAYLIST_SONGS_CLOSED");
                requireContext().registerReceiver(playerUpdateReceiver, playerFilter);

                isReceiversRegistered = true;
                Log.d("DownloadPlaylists", "Broadcast receivers registered");
            } catch (Exception e) {
                Log.e("DownloadPlaylists", "Error registering receivers", e);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("DownloadPlaylists", "onPause called");

        // Unregister broadcast receivers
        unregisterReceivers();
    }

    private void unregisterReceivers() {
        if (isReceiversRegistered && getContext() != null) {
            try {
                requireContext().unregisterReceiver(playlistUpdateReceiver);
                requireContext().unregisterReceiver(playerUpdateReceiver);
                isReceiversRegistered = false;
                Log.d("DownloadPlaylists", "Broadcast receivers unregistered");
            } catch (Exception e) {
                Log.e("DownloadPlaylists", "Error unregistering receivers", e);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("DownloadPlaylists", "onDestroyView called");

        // Clear playing status
        if (playlistAdapter != null) {
            playlistAdapter.clearPlayingStatus();
        }

        // Unregister receivers to be safe
        unregisterReceivers();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.d("DownloadPlaylists", "onHiddenChanged: " + hidden);

        if (!hidden) {
            // Fragment is now visible
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                loadPlaylists();
                updatePlayingPlaylistStatusByCategoryId();
            }, 100);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.d("DownloadPlaylists", "setUserVisibleHint: " + isVisibleToUser);

        if (isVisibleToUser && isResumed()) {
            // Fragment is visible to user
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                loadPlaylists();
                updatePlayingPlaylistStatusByCategoryId();
            }, 100);
        }
    }
}