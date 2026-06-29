package com.Saalai.SalaiMusicApp.Fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.Saalai.SalaiMusicApp.R;


public class LibraryFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        // Initialize views and set click listeners using lambda
        view.findViewById(R.id.yoursong).setOnClickListener(v -> handleYourLikesClick());
        view.findViewById(R.id.youralbum).setOnClickListener(v -> handleAlbumClick());
        view.findViewById(R.id.yourartist).setOnClickListener(v -> handleArtistClick());
        view.findViewById(R.id.addtoplaylist).setOnClickListener(v -> handleAddPlayListClick());
        view.findViewById(R.id.yourdownloaded).setOnClickListener(v -> handleDownloadedClick());
        view.findViewById(R.id.recentlyplayed).setOnClickListener(v -> handleRecentlyPlayedClick());
        view.findViewById(R.id.yourlisterning).setOnClickListener(v -> handleListeningLibraryClick());
        view.findViewById(R.id.searchicon).setOnClickListener(v -> handleSearchClick());
        view.findViewById(R.id.iv_user_info).setOnClickListener(v -> handleUserInfoClick());
        view.findViewById(R.id.boardicon).setOnClickListener(v -> handleBoardcastClick());
        view.findViewById(R.id.yoursettings).setOnClickListener(v -> handleyoursettingsClick());



        view.findViewById(R.id.searchicon).setOnClickListener(v -> handleCameraClick());



        return view;
    }

    private void handleCameraClick() {
        loadFragment(new ScannerFragment());
    }

    private void handleYourLikesClick() {
        loadFragment(new YourLikedPlaylistFragment());

    }
    private void handleAddPlayListClick() {
        loadFragment(new YourAddToPlaylistFragment());

    }

    private void handleAlbumClick() {
        loadFragment(new YourCreatedPlaylistFragment());


    }

    private void handleArtistClick() {
        showToast("Artist clicked");
        loadFragment(new YourFollowingPlaylistFragment());
    }

    private void handleDownloadedClick() {
        showToast("Downloaded clicked");
        loadFragment(new DownloadedTabFragment());
    }

    private void handleyoursettingsClick() {
        showToast("Downloaded clicked");
        loadFragment(new SettingsFragment());
    }

    private void handleRecentlyPlayedClick() {

    }

    private void handleListeningLibraryClick() {
        showToast("Listening Library clicked");
    }

    private void handleSearchClick() {
        showToast("Search clicked");
    }

    private void handleUserInfoClick() {
        showToast("User Profile clicked");
    }

    private void handleBoardcastClick() {
        showToast("Boardcast clicked");
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }


    private void loadFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}