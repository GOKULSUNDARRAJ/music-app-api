package com.Saalai.SalaiMusicApp.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.DownloadedTabAdapter;
import com.Saalai.SalaiMusicApp.R;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class DownloadedTabFragment extends Fragment {

    private static final String TAG = "DownloadedTabFragment";

    // UI Components
    private ImageView backbtn;
    private RecyclerView tabRecyclerView;
    private FrameLayout fragmentContainer;

    // Adapter
    private DownloadedTabAdapter tabAdapter;

    // Fragments
    private AudioDownloadFragment audioDownloadFragment;
    private DownloadPlaylistsFragment downloadPlaylistsFragment;
    private Fragment currentFragment;

    // Track last selected tab position
    private int lastSelectedTabPosition = 0;
    private static final String KEY_LAST_TAB_POSITION = "last_tab_position";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        if (savedInstanceState != null) {
            lastSelectedTabPosition = savedInstanceState.getInt(KEY_LAST_TAB_POSITION, 0);
            Log.d(TAG, "Restored last tab position: " + lastSelectedTabPosition);
        }

        // Initialize fragments
        audioDownloadFragment = new AudioDownloadFragment();
        downloadPlaylistsFragment = new DownloadPlaylistsFragment();
        currentFragment = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_LAST_TAB_POSITION, lastSelectedTabPosition);
        Log.d(TAG, "Saved last tab position: " + lastSelectedTabPosition);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");

        View view = inflater.inflate(R.layout.fragment_downloaded_tab, container, false);

        // Initialize views
        backbtn = view.findViewById(R.id.backbtn);
        tabRecyclerView = view.findViewById(R.id.tabRecyclerView);
        fragmentContainer = view.findViewById(R.id.fragment_container);

        Log.d(TAG, "Views initialized");

        // Setup RecyclerView for tabs
        setupTabRecyclerView();

        // Setup back button
        backbtn.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Load initial fragment
        loadFragment(lastSelectedTabPosition);

        return view;
    }

    /**
     * Setup RecyclerView with TabAdapter
     */
    private void setupTabRecyclerView() {
        // Initialize adapter
        tabAdapter = new DownloadedTabAdapter(requireContext());

        // Set layout manager - horizontal
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        tabRecyclerView.setLayoutManager(layoutManager);

        // Set adapter
        tabRecyclerView.setAdapter(tabAdapter);

        // Set selected position
        tabAdapter.setSelectedPosition(lastSelectedTabPosition);

        // Set tab selection listener
        tabAdapter.setOnTabSelectedListener(position -> {
            Log.d(TAG, "Tab selected from RecyclerView: " + position);
            lastSelectedTabPosition = position;
            switchFragment(position);

            // Scroll to selected tab if needed
            tabRecyclerView.smoothScrollToPosition(position);
        });
    }

    /**
     * Load fragment into container
     */
    private void loadFragment(int position) {
        Log.d(TAG, "loadFragment called - Position: " + position);

        if (getActivity() == null || !isAdded()) {
            Log.e(TAG, "loadFragment: Not attached to activity");
            return;
        }

        Fragment selectedFragment;
        if (position == 0) {
            selectedFragment = audioDownloadFragment;
        } else {
            selectedFragment = downloadPlaylistsFragment;
        }

        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Clear any existing fragments
        List<Fragment> fragments = fragmentManager.getFragments();
        if (!fragments.isEmpty()) {
            for (Fragment fragment : fragments) {
                transaction.remove(fragment);
            }
        }

        // Add selected fragment
        transaction.add(R.id.fragment_container, selectedFragment);
        currentFragment = selectedFragment;

        transaction.commitAllowingStateLoss();
        Log.d(TAG, "Fragment loaded: " + (position == 0 ? "AudioDownloadFragment" : "DownloadPlaylistsFragment"));
    }

    /**
     * Switch between fragments
     */
    private void switchFragment(int position) {
        Log.d(TAG, "switchFragment called - Position: " + position);

        if (getActivity() == null || !isAdded()) {
            Log.e(TAG, "switchFragment: Not attached to activity");
            return;
        }

        FragmentManager fragmentManager = getChildFragmentManager();

        if (fragmentManager.isStateSaved()) {
            Log.w(TAG, "switchFragment: FragmentManager state is saved, cannot commit");
            return;
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Hide current fragment
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }

        // Show or add selected fragment
        Fragment selectedFragment;
        if (position == 0) {
            selectedFragment = audioDownloadFragment;
        } else {
            selectedFragment = downloadPlaylistsFragment;
        }

        if (selectedFragment.isAdded()) {
            transaction.show(selectedFragment);
        } else {
            transaction.add(R.id.fragment_container, selectedFragment);
        }

        currentFragment = selectedFragment;
        transaction.commitAllowingStateLoss();
        Log.d(TAG, "Switch transaction committed");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");

        // Ensure correct fragment is shown
        if (currentFragment == null) {
            loadFragment(lastSelectedTabPosition);
        }

        // Update adapter selection
        if (tabAdapter != null) {
            tabAdapter.setSelectedPosition(lastSelectedTabPosition);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");

        // Clean up views
        backbtn = null;
        tabRecyclerView = null;
        fragmentContainer = null;
        tabAdapter = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");

        // Clean up fragment references
        audioDownloadFragment = null;
        downloadPlaylistsFragment = null;
        currentFragment = null;
    }
}