package com.Saalai.SalaiMusicApp.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.Saalai.SalaiMusicApp.Adapters.TopTabAdapter;
import com.Saalai.SalaiMusicApp.Models.NavigationDataManager;
import com.Saalai.SalaiMusicApp.Models.TopNavItem;
import com.Saalai.SalaiMusicApp.R;


import java.util.ArrayList;
import java.util.List;

public class SaalaiFragment extends Fragment {

    // Views
    private ConstraintLayout topLayout;
    private RecyclerView topTabsRecycler;
    private ViewPager2 viewPager;
    private ImageView profileIcon;
    private TextView tvNoData;

    // Data
    private List<TopNavItem> topNavItems = new ArrayList<>();
    private int selectedTab = 0;
    private static final String SELECTED_TAB_KEY = "selected_tab";
    private static int lastSelectedTab = 0;

    // Adapters
    private TopTabAdapter topTabAdapter;
    private TopNavPagerAdapter pagerAdapter;

    // ViewModel
    private TopMenuViewModel viewModel;

    // Drawer toggle
    private DrawerToggleListener drawerToggleListener;



    public interface DrawerToggleListener {
        void onToggleDrawer();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            drawerToggleListener = (DrawerToggleListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement DrawerToggleListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saalai, container, false);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(TopMenuViewModel.class);

        // Initialize views
        initViews(view);

        // Set up RecyclerView for tabs
        setupTopTabsRecycler();

        // Set up ViewPager
        setupViewPager();

        // Observe ViewModel changes
        observeViewModel();

        // Setup profile icon click
        profileIcon = view.findViewById(R.id.iv_user_info);


        view.findViewById(R.id.searchicon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadedVideosFragment fragment = new DownloadedVideosFragment();

                // Use requireActivity() to get the main fragment container
                FragmentTransaction transaction = requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction();

                // Replace the entire screen with the downloaded videos fragment
                transaction.replace(R.id.fragment_container, fragment); // Make sure this ID exists in your activity_main.xml
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check saved state
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(SELECTED_TAB_KEY, 0);
        }

        // Load data immediately from SharedPreferences
        loadTopMenuFromSharedPrefs();

        // Also check ViewModel
        if (viewModel.getTopMenu() != null && !viewModel.getTopMenu().isEmpty()) {
            setTopNavItems(viewModel.getTopMenu());
        }


    }

    private void initViews(View view) {
        topLayout = view.findViewById(R.id.top_layout);
        topTabsRecycler = view.findViewById(R.id.top_tabs_recycler);
        viewPager = view.findViewById(R.id.viewPager);


        // Add a TextView for "no data" message if not in layout
        if (tvNoData == null) {
            tvNoData = new TextView(getContext());
            tvNoData.setText("Loading navigation...");
            tvNoData.setTextColor(getResources().getColor(R.color.white));
            tvNoData.setTextSize(16);
            tvNoData.setVisibility(View.GONE);
        }
    }

    private void setupTopTabsRecycler() {
        Log.d("SaalaiFragment", "setupTopTabsRecycler called");

        // Get column count - initially use default, will be updated when data loads
        int columnCount = getColumnCount(topNavItems.size());
        Log.d("SaalaiFragment", "Initial column count: " + columnCount);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),
                columnCount,
                GridLayoutManager.VERTICAL,
                false);

        topTabsRecycler.setLayoutManager(layoutManager);
        topTabsRecycler.setHasFixedSize(false);

        topTabAdapter = new TopTabAdapter(
                requireContext(),
                topNavItems,
                new TopTabAdapter.OnTabClickListener() {
                    @Override
                    public void onTabClick(int position) {
                        Log.d("SaalaiFragment", "Tab clicked at: " + position);
                        selectTab(position);
                    }
                }
        );
        topTabsRecycler.setAdapter(topTabAdapter);
        topTabsRecycler.requestLayout();
    }

    private int getColumnCount(int itemCount) {
        Log.d("SaalaiFragment", "Calculating column count for " + itemCount + " items");

        // Logic for column count:
        // If 5 items, show 5 columns (all in one row)
        // If less than 5, show exact count
        // If more than 5, show all in one row
        int columnCount;
        if (itemCount <= 0) {
            columnCount = 5; // Default when no items
        } else {
            columnCount = itemCount; // Show exact count for all items
        }

        Log.d("SaalaiFragment", "Column count determined: " + columnCount);
        return columnCount;
    }



    private void setupViewPager() {
        pagerAdapter = new TopNavPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setUserInputEnabled(false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                selectedTab = position;
                lastSelectedTab = position;
                if (topTabAdapter != null) {
                    topTabAdapter.setSelectedPosition(position);
                }
                scrollToPosition(position);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getTopMenuLiveData().observe(getViewLifecycleOwner(), new Observer<List<TopNavItem>>() {
            @Override
            public void onChanged(List<TopNavItem> items) {
                if (items != null && !items.isEmpty()) {
                    Log.d("SaalaiFragment", "ViewModel data changed: " + items.size() + " items");
                    setTopNavItems(items);
                }
            }
        });
    }

    // ===================== LOAD FROM SHARED PREFERENCES =====================
    private void loadTopMenuFromSharedPrefs() {
        Log.d("SaalaiFragment", "Loading top menu from SharedPreferences...");

        NavigationDataManager navManager = NavigationDataManager.getInstance(requireContext());

        if (navManager.isNavigationLoaded()) {
            List<TopNavItem> topItems = navManager.getTopNavigation();
            if (!topItems.isEmpty()) {
                Log.d("SaalaiFragment", "Found top nav in SharedPreferences: " + topItems.size() + " items");
                setTopNavItems(topItems);
            } else {
                Log.d("SaalaiFragment", "No top navigation in SharedPreferences");
                showNoDataMessage("No navigation data available");
            }
        } else {
            Log.d("SaalaiFragment", "Navigation not loaded in SharedPreferences");
            showNoDataMessage("Waiting for navigation data...");
        }
    }

    // ===================== UPDATE TOP MENU =====================
    // Public method to update menu from MainActivity
    public void updateTopMenu(List<TopNavItem> items) {
        Log.d("SaalaiFragment", "updateTopMenu called with " + items.size() + " items");

        if (viewModel != null) {
            viewModel.setTopMenu(items);
        } else {
            // Direct update if ViewModel not ready
            setTopNavItems(items);
        }
    }

    private void setTopNavItems(List<TopNavItem> items) {
        Log.d("SaalaiFragment", "setTopNavItems called with " + items.size() + " items");

        this.topNavItems.clear();
        this.topNavItems.addAll(items);

        Log.d("SaalaiFragment", "Fragment list now has: " + topNavItems.size() + " items");
        updateTopTabs();
        hideNoDataMessage();
    }

    public void updateSelectedTab(int tabPosition) {
        selectTab(tabPosition);
    }

    private void updateTopTabs() {
        Log.d("SaalaiFragment", "updateTopTabs called");

        if (topTabAdapter != null) {
            topTabAdapter.notifyDataSetChanged();
            topTabAdapter.setSelectedPosition(selectedTab);
        }

        if (pagerAdapter != null) {
            pagerAdapter.notifyDataSetChanged();
            if (viewPager.getCurrentItem() != selectedTab) {
                viewPager.setCurrentItem(selectedTab, false);
            }
        }

        if (topTabsRecycler != null) {
            topTabsRecycler.post(() -> {
                topTabsRecycler.requestLayout();
                topTabsRecycler.invalidate();
                scrollToPosition(selectedTab);
            });
        }
    }

    public void selectTab(int position) {
        if (position < 0 || position >= topNavItems.size()) {
            Log.e("SaalaiFragment", "Invalid tab position: " + position);
            return;
        }

        Log.d("SaalaiFragment", "selectTab: " + position);
        selectedTab = position;
        lastSelectedTab = position;

        if (topTabAdapter != null) {
            topTabAdapter.setSelectedPosition(position);
        }

        if (viewPager != null) {
            viewPager.setCurrentItem(position, true);
        }

        scrollToPosition(position);
    }

    private void scrollToPosition(int position) {
        if (topTabsRecycler != null && topTabAdapter != null) {
            topTabsRecycler.smoothScrollToPosition(position);
        }
    }

    // ===================== NO DATA MESSAGES =====================
    private void showNoDataMessage(String message) {
        if (tvNoData != null) {
            tvNoData.setText(message);
            tvNoData.setVisibility(View.VISIBLE);
        }
        if (topTabsRecycler != null) {
            topTabsRecycler.setVisibility(View.GONE);
        }
        if (viewPager != null) {
            viewPager.setVisibility(View.GONE);
        }
    }

    private void hideNoDataMessage() {
        if (tvNoData != null) {
            tvNoData.setVisibility(View.GONE);
        }
        if (topTabsRecycler != null) {
            topTabsRecycler.setVisibility(View.VISIBLE);
        }
        if (viewPager != null) {
            viewPager.setVisibility(View.VISIBLE);
        }
    }

    // ===================== VIEWPAGER ADAPTER =====================
    private class TopNavPagerAdapter extends FragmentStateAdapter {
        public TopNavPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position >= 0 && position < topNavItems.size()) {
                TopNavItem item = topNavItems.get(position);
                String tabName = item.getTopmenuName().toLowerCase();

                switch (tabName) {
                    case "all":
                        return new AllFragment();
                    case "live tv":
                    case "live":
                        return new LiveTvFragment();
                    case "movies":
                        return new MoviesFragment();
                    case "tv shows":
                    case "tvshows":
                        return new TvShowsFragment();
                    case "catch up":
                    case "catchup":
                        return new CatchUpFragment();
                    default:
                        return new AllFragment();
                }
            }
            return new AllFragment();
        }

        @Override
        public int getItemCount() {
            return topNavItems.size();
        }
    }

    // ===================== LIFECYCLE METHODS =====================
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_TAB_KEY, selectedTab);
        lastSelectedTab = selectedTab;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(SELECTED_TAB_KEY, lastSelectedTab);
            lastSelectedTab = selectedTab;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (selectedTab != lastSelectedTab) {
            selectedTab = lastSelectedTab;
            if (pagerAdapter != null && topTabAdapter != null) {
                if (viewPager != null) {
                    viewPager.setCurrentItem(selectedTab, false);
                }
                topTabAdapter.setSelectedPosition(selectedTab);
            }
        }

        // Refresh from SharedPreferences if needed
        if (topNavItems.isEmpty()) {
            loadTopMenuFromSharedPrefs();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        lastSelectedTab = selectedTab;
    }

    private void handleProfileClick() {
        if (drawerToggleListener != null) {
            drawerToggleListener.onToggleDrawer();
        }
    }
}