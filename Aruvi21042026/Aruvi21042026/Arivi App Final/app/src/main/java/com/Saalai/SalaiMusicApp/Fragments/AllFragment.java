package com.Saalai.SalaiMusicApp.Fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.Saalai.SalaiMusicApp.Activity.SignUpActivity;
import com.Saalai.SalaiMusicApp.Adapters.ParentAdapterAllFragment;
import com.Saalai.SalaiMusicApp.Adapters.SliderAdapter;

import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.ClickInterface.TabNavigationListener;
import com.Saalai.SalaiMusicApp.ClickInterface.TopLayoutVisibilityListener;
import com.Saalai.SalaiMusicApp.Models.ChildItemAllFragment;
import com.Saalai.SalaiMusicApp.Models.ParentItemAllFragment;
import com.Saalai.SalaiMusicApp.Models.SliderItem;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.DashboardResponse;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AllFragment extends Fragment implements TabNavigationListener {

    private ViewPager2 viewPager2;
    private LinearLayout dotsLayout;
    private Handler sliderHandler = new Handler();

    private List<SliderItem> sliderItems = new ArrayList<>();
    private SliderAdapter sliderAdapter;

    private RecyclerView parentRecyclerView;
    private ParentAdapterAllFragment parentAdapter;
    private List<ParentItemAllFragment> parentList = new ArrayList<>();

    private ShimmerFrameLayout shimmerLayout;
    private LinearLayout contentLayout;

    private NestedScrollView nestedScrollView;
    private TopLayoutVisibilityListener visibilityListener;

    private boolean isAccountBlocked = false;
    private String blockedMessage = "";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof TopLayoutVisibilityListener) {
            visibilityListener = (TopLayoutVisibilityListener) getParentFragment();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all, container, false);

        // Initialize views
        viewPager2 = view.findViewById(R.id.sliderViewPager);
        dotsLayout = view.findViewById(R.id.dotsContainer);
        parentRecyclerView = view.findViewById(R.id.parentRecyclerView);
        shimmerLayout = view.findViewById(R.id.shimmerLayout);
        contentLayout = view.findViewById(R.id.contentLayout);
        nestedScrollView = view.findViewById(R.id.nestedScrollView);

        // Setup adapters
        sliderAdapter = new SliderAdapter(getContext(), sliderItems);
        viewPager2.setAdapter(sliderAdapter);

        parentAdapter = new ParentAdapterAllFragment(parentList, this, (AppCompatActivity) getActivity());
        parentRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        parentRecyclerView.setAdapter(parentAdapter);

        // Setup slider
        setupViewPager();
        setupDotsIndicator();
        setupAutoSlide();

        // Start shimmer by default
        shimmerLayout.startShimmer();
        shimmerLayout.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);

        initializeViews(view);
        setupComponents();

        // Fetch data immediately without delay
        fetchDashboardList();

        return view;
    }

    private void showErrorState(String message) {
        if (isAdded() && getContext() != null) {
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
            contentLayout.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    private void setupComponents() {
        // Initialize adapters
        sliderAdapter = new SliderAdapter(getContext(), sliderItems);
        viewPager2.setAdapter(sliderAdapter);

        parentAdapter = new ParentAdapterAllFragment(parentList, this, (AppCompatActivity) getActivity());

        // Pass any existing blocked status to the new adapter
        parentAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);

        parentRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        parentRecyclerView.setAdapter(parentAdapter);

        // Setup slider with proper configuration
        setupViewPager();
        setupDotsIndicator();
        setupAutoSlide();

        // Setup shimmer and content visibility
        shimmerLayout.startShimmer();
        shimmerLayout.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void initializeViews(View view) {
        viewPager2 = view.findViewById(R.id.sliderViewPager);
        dotsLayout = view.findViewById(R.id.dotsContainer);
        parentRecyclerView = view.findViewById(R.id.parentRecyclerView);
        shimmerLayout = view.findViewById(R.id.shimmerLayout);
        contentLayout = view.findViewById(R.id.contentLayout);
        nestedScrollView = view.findViewById(R.id.nestedScrollView);
    }

    @Override
    public void navigateToTab(int tabPosition) {
        // Find the parent SaalaiFragment and call its selectTab method
        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof SaalaiFragment) {
            ((SaalaiFragment) parentFragment).selectTab(tabPosition);
        }
    }

    private void setupViewPager() {
        viewPager2.setClipToPadding(false);
        viewPager2.setClipChildren(false);
        viewPager2.setOffscreenPageLimit(3);
        viewPager2.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

        CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
        compositePageTransformer.addTransformer(new MarginPageTransformer(40));
        compositePageTransformer.addTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(0.85f + r * 0.15f);
        });

        viewPager2.setPageTransformer(compositePageTransformer);

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (isAdded()) {
                    setCurrentDot(position);
                    sliderHandler.removeCallbacks(sliderRunnable);
                    sliderHandler.postDelayed(sliderRunnable, 3000);
                }
            }
        });
    }

    private void setupDotsIndicator() {
        if (!isAdded() || getContext() == null) return;

        dotsLayout.removeAllViews();
        if (sliderItems == null || sliderItems.isEmpty()) return;

        int dotSize = dpToPx(5);
        int dotMargin = dpToPx(2);

        ImageView[] dots = new ImageView[sliderItems.size()];
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new ImageView(requireContext());
            dots[i].setImageDrawable(requireContext().getDrawable(R.drawable.dot_inactive));
            dots[i].setAdjustViewBounds(true);
            dots[i].setPadding(0, 0, 0, 0);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSize, dotSize);
            params.setMargins(dotMargin, 0, dotMargin, 0);
            dotsLayout.addView(dots[i], params);
        }
        setCurrentDot(0);
    }

    private int dpToPx(int dp) {
        return Math.round(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        dp,
                        getResources().getDisplayMetrics()
                )
        );
    }

    private void setCurrentDot(int position) {
        if (!isAdded() || getContext() == null) return;

        int childCount = dotsLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView dot = (ImageView) dotsLayout.getChildAt(i);
            if (i == position) {
                dot.setImageDrawable(getResources().getDrawable(R.drawable.dot_active_white));
            } else {
                dot.setImageDrawable(getResources().getDrawable(R.drawable.dot_inactive_gray));
            }
        }
    }

    private void setupAutoSlide() {
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    private Runnable sliderRunnable = () -> {
        if (sliderItems == null || sliderItems.isEmpty()) return;
        int currentItem = viewPager2.getCurrentItem();
        if (currentItem == sliderItems.size() - 1) {
            viewPager2.setCurrentItem(0);
        } else {
            viewPager2.setCurrentItem(currentItem + 1);
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (sliderHandler != null) {
            sliderHandler.removeCallbacksAndMessages(null);
        }
    }

    private void fetchDashboardList() {
        if (shimmerLayout != null) {
            shimmerLayout.setVisibility(View.VISIBLE);
            shimmerLayout.startShimmer();
        }
        if (contentLayout != null) {
            contentLayout.setVisibility(View.GONE);
        }

        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
        // Force refresh from disk
        String accessToken = sp.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e("Dashboard", "No access token found. Skipping fetch.");
            if (shimmerLayout != null) {
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
            }
            if (contentLayout != null) {
                contentLayout.setVisibility(View.VISIBLE);
            }
            return;
        }

        // Ensure token has Bearer prefix
        String authHeader = accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;
        Log.d("Dashboard", "Fetching dashboard with token: " + authHeader.substring(0, Math.min(20, authHeader.length())) + "...");

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<DashboardResponse> call = apiService.getDashboardList(authHeader);

        call.enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(Call<DashboardResponse> call, Response<DashboardResponse> response) {
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);

                if (response.isSuccessful() && response.body() != null) {
                    DashboardResponse dashboard = response.body();

                    if ("401".equals(dashboard.getErrorType())) {
                        Log.e("Dashboard", "Error type 401 - Session expired, logging out");
                        Toast.makeText(getContext(), "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
                        callLogoutApi();
                        return;
                    }

                    if ("201".equals(dashboard.getErrorType())) {
                        Log.e("Dashboard", "Error type 201 - Account blocked");
                        isAccountBlocked = true;
                        blockedMessage = dashboard.getMessage();

                        if (parentAdapter != null) {
                            parentAdapter.setAccountBlocked(true, blockedMessage);
                        }
                    }

                    if (!"201".equals(dashboard.getErrorType())) {
                        isAccountBlocked = false;
                        blockedMessage = "";

                        if (parentAdapter != null) {
                            parentAdapter.setAccountBlocked(false, "");
                        }
                    }

                    if (!dashboard.isStatus()) {
                        Log.e("Dashboard", "API returned false status: " + dashboard.getMessage());
                        Toast.makeText(getContext(), "Error: " + dashboard.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DashboardResponse.ResponseData responseData = dashboard.getResponse();

                    if (responseData == null) {
                        Log.e("Dashboard", "Response data is null");
                        Toast.makeText(getContext(), "No data available", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Update Slider
                    sliderItems.clear();
                    if (responseData.getBannerList() != null && !responseData.getBannerList().isEmpty()) {
                        for (DashboardResponse.Banner banner : responseData.getBannerList()) {
                            sliderItems.add(new SliderItem(banner.getBanner(), ""));
                        }
                        sliderAdapter.notifyDataSetChanged();
                        setupDotsIndicator();
                        viewPager2.setVisibility(View.VISIBLE);
                        dotsLayout.setVisibility(View.VISIBLE);
                    } else {
                        viewPager2.setVisibility(View.GONE);
                        dotsLayout.setVisibility(View.GONE);
                    }

                    // Update Parent Recycler View
                    parentList.clear();

                    // Movies Section
                    if (responseData.getLatestMovieList() != null && !responseData.getLatestMovieList().isEmpty()) {
                        List<ChildItemAllFragment> movies = new ArrayList<>();
                        for (DashboardResponse.Movie movie : responseData.getLatestMovieList()) {
                            movies.add(new ChildItemAllFragment(
                                    movie.getChannelLogo(),
                                    movie.getChannelName(),
                                    "Movies",
                                    "Movies",
                                    movie.getChannelURL(),
                                    movie.getChannelId()
                            ));
                        }
                        parentList.add(new ParentItemAllFragment("Latest Movies", "Movies", movies));
                    }

                    // TV Shows Section
                    if (responseData.getLatestTVShowList() != null && !responseData.getLatestTVShowList().isEmpty()) {
                        List<ChildItemAllFragment> tvShows = new ArrayList<>();
                        for (DashboardResponse.TVShow tvShow : responseData.getLatestTVShowList()) {
                            tvShows.add(new ChildItemAllFragment(
                                    tvShow.getChannelLogo(),
                                    tvShow.getChannelName(),
                                    "TVShows",
                                    "Latest TV Shows",
                                    tvShow.getChannelURL(),
                                    tvShow.getChannelId()
                            ));
                        }
                        parentList.add(new ParentItemAllFragment("Latest TV Shows", "TVShows", tvShows));
                    }

                    // Channels Section
                    if (responseData.getLatestChannelList() != null && !responseData.getLatestChannelList().isEmpty()) {
                        List<ChildItemAllFragment> channels = new ArrayList<>();
                        for (DashboardResponse.Channel channel : responseData.getLatestChannelList()) {
                            channels.add(new ChildItemAllFragment(
                                    channel.getChannelLogo(),
                                    channel.getChannelName(),
                                    "Channels",
                                    "Channels",
                                    channel.getChannelURL(),
                                    channel.getChannelId()
                            ));
                        }
                        parentList.add(new ParentItemAllFragment("Latest Channels", "Channels", channels));
                    }

                    // Radios Section
                    if (responseData.getLatestRadioList() != null && !responseData.getLatestRadioList().isEmpty()) {
                        List<ChildItemAllFragment> radios = new ArrayList<>();
                        for (DashboardResponse.Radio radio : responseData.getLatestRadioList()) {
                            radios.add(new ChildItemAllFragment(
                                    radio.getChannelLogo(),
                                    radio.getChannelName(),
                                    "Radio",
                                    "Radio",
                                    radio.getChannelURL(),
                                    radio.getChannelId()
                            ));
                        }
                        parentList.add(new ParentItemAllFragment("Latest Radio", "Radio", radios));
                    }

                    parentAdapter.notifyDataSetChanged();
                    Log.d("Dashboard", "Data loaded successfully. Sections: " + parentList.size());

                } else {
                    // Handle API error responses
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorStr);

                            if (jsonObject.has("error_type") && "401".equals(jsonObject.getString("error_type"))) {
                                Log.e("Dashboard", "Error type 401 from error response - logging out");
                                Toast.makeText(getContext(), "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
                                callLogoutApi();
                                return;
                            }

                            if (jsonObject.has("error_type") && "201".equals(jsonObject.getString("error_type"))) {
                                Log.e("Dashboard", "Error type 201 - Account blocked from error response");
                                isAccountBlocked = true;
                                blockedMessage = jsonObject.optString("message", "Account issue detected");
                            }

                            if (jsonObject.has("message")) {
                                String errorMessage = jsonObject.getString("message");
                                Log.e("Dashboard", "Server Error: " + errorMessage);
                                Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e("Dashboard", "Server Error Code: " + response.code() + ", Error: " + errorStr);
                                Toast.makeText(getContext(), "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e("Dashboard", "Empty error body with code: " + response.code());
                            Toast.makeText(getContext(), "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("Dashboard", "Error parsing error response", e);
                        Toast.makeText(getContext(), "Error loading data", Toast.LENGTH_SHORT).show();
                    }

                    if (parentList.isEmpty()) {
                        Log.d("Dashboard", "No data available to display");
                    }
                }
            }

            @Override
            public void onFailure(Call<DashboardResponse> call, Throwable t) {
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);

                Log.e("Dashboard", "Network Error: " + t.getMessage(), t);

                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void callLogoutApi() {
        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
        // Force refresh from disk
        String accessToken = sp.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e("Logout", "No token found, user already logged out");
            return;
        }

        // Ensure token has Bearer prefix
        String authHeader = accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.logout(authHeader);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("Logout", "User Logged Out successfully");
                    sp.clearAccessToken();

                    Intent intent = new Intent(getContext(), SignUpActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Log.e("Logout", "Failed to logout, server code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Logout", "Logout API call failed", t);
            }
        });
    }
}