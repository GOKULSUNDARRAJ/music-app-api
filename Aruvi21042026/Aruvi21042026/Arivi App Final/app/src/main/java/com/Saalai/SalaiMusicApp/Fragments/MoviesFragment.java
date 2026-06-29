package com.Saalai.SalaiMusicApp.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.Saalai.SalaiMusicApp.Activity.SignUpActivity;
import com.Saalai.SalaiMusicApp.Adapters.ParentAdapterMovieFragment;
import com.Saalai.SalaiMusicApp.Adapters.SliderAdapterForMovie;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.ChildItemMovieFragment;
import com.Saalai.SalaiMusicApp.Models.ParentItemMovieFragment;
import com.Saalai.SalaiMusicApp.Models.SliderItemForMovie;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerParentAdapter;
import com.Saalai.SalaiMusicApp.SubscriptionBottomSheetFragment;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import android.util.TypedValue;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MoviesFragment extends Fragment {

    private ViewPager2 viewPager2;
    private LinearLayout dotsLayout;
    private Handler sliderHandler = new Handler();
    private List<SliderItemForMovie> sliderItems = new ArrayList<>();
    private SliderAdapterForMovie sliderAdapter;

    private RecyclerView parentRecyclerView;
    private ParentAdapterMovieFragment parentAdapter;
    private List<ParentItemMovieFragment> parentList = new ArrayList<>();

    private static final int SHIMMER_PARENT_COUNT = 5;
    private static final int SHIMMER_CHILD_COUNT = 6;

    // Account blocked status
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_movies, container, false);

        viewPager2 = view.findViewById(R.id.sliderViewPager);
        dotsLayout = view.findViewById(R.id.dotsContainer);
        parentRecyclerView = view.findViewById(R.id.parentRecyclerView);

        // ---------------------------
        // Show shimmer placeholders
        // ---------------------------
        parentRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        parentRecyclerView.setAdapter(new ShimmerParentAdapter(SHIMMER_PARENT_COUNT, SHIMMER_CHILD_COUNT));

        // ---------------------------
        // Slider setup
        // ---------------------------
        sliderAdapter = new SliderAdapterForMovie(getContext(), sliderItems);
        viewPager2.setAdapter(sliderAdapter);
        setupViewPager();
        setupDotsIndicator();
        setupAutoSlide();

        sliderAdapter.setOnSliderClickListener(new SliderAdapterForMovie.OnSliderClickListener() {
            @Override
            public void onSliderItemClick(SliderItemForMovie sliderItem, int position) {
                // Check if account is blocked for slider clicks
                if (isAccountBlocked) {
                    AppCompatActivity activity = (AppCompatActivity) getContext();
                    showAccountBlockedAlert(activity);
                    return;
                }
                openMoviePlayer(sliderItem.getChannelId());
            }
        });

        // ---------------------------
        // Fetch movies and banners
        // ---------------------------
        fetchMovieDashboard();

        ConstraintLayout watchNowBtn = view.findViewById(R.id.watchnowll);

        watchNowBtn.setOnClickListener(v -> {
            // Check if account is blocked for "Watch Now" button
            if (isAccountBlocked) {
                AppCompatActivity activity = (AppCompatActivity) getContext();
                showAccountBlockedAlert(activity);
                return;
            }

            if (sliderItems.isEmpty()) {
                Log.d("MoviesFragment", "Slider empty, cannot open movie.");
                return;
            }

            int currentPosition = viewPager2.getCurrentItem();
            if (currentPosition < sliderItems.size()) {
                String movieId = sliderItems.get(currentPosition).getChannelId();

                AppCompatActivity activity = (AppCompatActivity) v.getContext();

                // Create the MovieVideoPlayerFragment with the movie ID
                MovieVideoPlayerFragment moviePlayerFragment = MovieVideoPlayerFragment.newInstance(movieId);

                FragmentManager fragmentManager = activity.getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                // Use add instead of replace to preserve the current fragment state
                fragmentTransaction.add(R.id.fragment_container, moviePlayerFragment);
                fragmentTransaction.addToBackStack("movie_player");
                fragmentTransaction.commit();
            }
        });

        return view;
    }

    private void showAccountBlockedAlert(AppCompatActivity activity) {
        if (!isAdded() || getContext() == null) return;

        SubscriptionBottomSheetFragment bottomSheetFragment = new SubscriptionBottomSheetFragment();
        bottomSheetFragment.show(activity.getSupportFragmentManager(), "MenuBottomSheet");
    }

    private void openMoviePlayer(String movieId) {
        if (movieId == null || movieId.isEmpty()) {
            Toast.makeText(getContext(), "Movie not available", Toast.LENGTH_SHORT).show();
            return;
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) return;

        // Create the MovieVideoPlayerFragment with the movie ID
        MovieVideoPlayerFragment moviePlayerFragment = MovieVideoPlayerFragment.newInstance(movieId);

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Use add instead of replace to preserve the current fragment state
        fragmentTransaction.add(R.id.fragment_container, moviePlayerFragment);
        fragmentTransaction.addToBackStack("movie_player");
        fragmentTransaction.commit();

        Log.d("MoviesFragment", "Opening movie player for ID: " + movieId);
    }

    private void fetchMovieDashboard() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String accessToken = SharedPrefManager.getInstance(getContext()).getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e("Movies", "No access token found.");
            return;
        }

        apiService.getMovieDashboard(accessToken).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String rawJson = response.body().string();
                        JsonObject rootObj = JsonParser.parseString(rawJson).getAsJsonObject();

                        // Check for error_type in the response
                        if (rootObj.has("error_type")) {
                            String errorType = rootObj.get("error_type").getAsString();
                            if ("201".equals(errorType)) {
                                Log.e("Movies", "Error type 201 - Account blocked");
                                isAccountBlocked = true;
                                blockedMessage = rootObj.has("message") ?
                                        rootObj.get("message").getAsString() : "Account issue detected";

                                // Update adapter with blocked status
                                if (parentAdapter != null) {
                                    parentAdapter.setAccountBlocked(true, blockedMessage);
                                }


                                // Continue to load data - don't return
                            } else {
                                // Reset account blocked status if API returns success
                                isAccountBlocked = false;
                                blockedMessage = "";
                                if (parentAdapter != null) {
                                    parentAdapter.setAccountBlocked(false, "");
                                }
                            }
                        }

                        // Get the "response" array from the JSON
                        JsonArray categoriesArray = rootObj.getAsJsonArray("response");

                        parentList.clear();
                        sliderItems.clear();

                        for (JsonElement categoryElem : categoriesArray) {
                            JsonObject categoryObj = categoryElem.getAsJsonObject();

                            // Get categoryId
                            int categoryId = categoryObj.has("categoryId") ? categoryObj.get("categoryId").getAsInt() : -1;
                            String categoryName = categoryObj.has("categoryName") ? categoryObj.get("categoryName").getAsString() : "";

                            JsonArray channelsArray = categoryObj.getAsJsonArray("channels");
                            List<ChildItemMovieFragment> childList = new ArrayList<>();

                            for (JsonElement channelElem : channelsArray) {
                                JsonObject channelObj = channelElem.getAsJsonObject();

                                String movieId = channelObj.has("channelId") ? channelObj.get("channelId").getAsString() : "";
                                String thumbnail = channelObj.has("channelLogo") ? channelObj.get("channelLogo").getAsString() : "";
                                Log.d("ChannelThumbnail", "Thumbnail URL: " + thumbnail);

                                String title = channelObj.has("channelName") ? channelObj.get("channelName").getAsString() : "";
                                String videoUrl = "";

                                if (categoryName.equalsIgnoreCase("Movie Banner List")) {
                                    sliderItems.add(new SliderItemForMovie(thumbnail, title, "", videoUrl, movieId));
                                } else {
                                    childList.add(new ChildItemMovieFragment(
                                            movieId, thumbnail, title, "movie", categoryName, videoUrl
                                    ));
                                }
                            }

                            // Add categoryId instead of hardcoding
                            if (!childList.isEmpty()) {
                                parentList.add(new ParentItemMovieFragment(categoryId, categoryName, categoryName, childList));
                            }
                        }

                        if (isAdded()) {
                            // Stop shimmer and show real data
                            parentAdapter = new ParentAdapterMovieFragment(parentList);

                            // Pass account blocked status to parent adapter
                            parentAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);

                            parentRecyclerView.setAdapter(parentAdapter);

                            sliderAdapter.hideShimmer();
                            sliderAdapter.notifyDataSetChanged();
                            setupDotsIndicator();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorStr);

                            // Check for error_type 201 in error response
                            if (jsonObject.has("error_type") && "201".equals(jsonObject.getString("error_type"))) {
                                Log.e("Movies", "Error type 201 - Account blocked from error response");
                                isAccountBlocked = true;
                                blockedMessage = jsonObject.optString("message", "Account issue detected");

                                // Update adapter with blocked status
                                if (parentAdapter != null) {
                                    parentAdapter.setAccountBlocked(true, blockedMessage);
                                }


                                // Continue - don't return
                            }

                            if (jsonObject.has("error_type") && "401".equals(jsonObject.getString("error_type"))) {
                                Toast.makeText(getContext(), "Access Denied", Toast.LENGTH_SHORT).show();
                                callLogoutApi();
                            } else {
                                Log.e("Dashboard", "Server Error Code: " + response.code() + ", Error: " + errorStr);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Dashboard", "Error parsing errorBody", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void callLogoutApi() {
        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
        String accessToken = sp.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e("Logout", "No token found, user already logged out");
            return;
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.logout(accessToken);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("Logout", "User Logged Out successfully");

                    // Clear token locally
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

    // Rest of your existing methods for slider setup, dots, etc.
    // ... (keep all the existing methods like setupViewPager(), setupDotsIndicator(), etc.)

    private void setupViewPager() {
        viewPager2.getChildAt(0).setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

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
                setCurrentDot(position);
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 3000);
            }
        });
    }

    private void setupDotsIndicator() {
        if (dotsLayout == null) return;

        dotsLayout.removeAllViews();

        int dotCount = sliderItems.size();
        if (dotCount == 0) return;

        int dotSize = dpToPx(5);
        int dotMargin = dpToPx(2);

        for (int i = 0; i < dotCount; i++) {
            ImageView dot = new ImageView(getContext());
            dot.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.dot_inactive));
            dot.setAdjustViewBounds(true);
            dot.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            dot.setPadding(0, 0, 0, 0);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSize, dotSize);
            params.setMargins(dotMargin, 0, dotMargin, 0);
            dot.setLayoutParams(params);

            dotsLayout.addView(dot);
        }

        setCurrentDot(0);
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    private void setCurrentDot(int position) {
        if (!isAdded()) return;

        int childCount = dotsLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView dot = (ImageView) dotsLayout.getChildAt(i);
            dot.setImageDrawable(i == position ?
                    getResources().getDrawable(R.drawable.dot_active_white) :
                    getResources().getDrawable(R.drawable.dot_inactive_gray));
        }
    }

    private void setupAutoSlide() {
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    private Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (sliderItems.size() == 0) return;

            int currentItem = viewPager2.getCurrentItem();
            if (currentItem == sliderItems.size() - 1) {
                viewPager2.setCurrentItem(0);
            } else {
                viewPager2.setCurrentItem(currentItem + 1);
            }
            sliderHandler.postDelayed(this, 3000);
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
        sliderHandler.removeCallbacks(sliderRunnable);
    }
}