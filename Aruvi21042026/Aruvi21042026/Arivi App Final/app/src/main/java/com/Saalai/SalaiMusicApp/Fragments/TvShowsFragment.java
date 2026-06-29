package com.Saalai.SalaiMusicApp.Fragments;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.Saalai.SalaiMusicApp.Activity.SignUpActivity;
import com.Saalai.SalaiMusicApp.Adapters.ParentAdapterTvFragment;
import com.Saalai.SalaiMusicApp.Adapters.SliderAdapterTv;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.SliderItemTv;
import com.Saalai.SalaiMusicApp.Models.TvCategory;
import com.Saalai.SalaiMusicApp.Models.TvChannel;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerParentAdapterTvShow;
import com.Saalai.SalaiMusicApp.SubscriptionBottomSheetFragment;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TvShowsFragment extends Fragment {

    private RecyclerView parentRecyclerView;
    private ParentAdapterTvFragment parentAdapter;
    private List<TvCategory> parentList = new ArrayList<>();

    private ViewPager2 viewPager2;
    private SliderAdapterTv sliderAdapter;
    private LinearLayout dotsLayout;
    private Handler sliderHandler = new Handler();
    private int currentPage = 0;

    private ApiService apiInterface;
    private Runnable sliderRunnable;

    // Account blocked status
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_tv_shows, container, false);

        apiInterface = ApiClient.getClient().create(ApiService.class);

        parentRecyclerView = view.findViewById(R.id.parentRecyclerView);

        parentRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        parentRecyclerView.setAdapter(new ShimmerParentAdapterTvShow(10,10));

        viewPager2 = view.findViewById(R.id.sliderViewPager);
        dotsLayout = view.findViewById(R.id.dotsContainer);

        sliderAdapter = new SliderAdapterTv(new ArrayList<>());
        viewPager2.setAdapter(sliderAdapter);
        viewPager2.setClipToPadding(false);
        viewPager2.setClipChildren(false);
        viewPager2.setOffscreenPageLimit(3);
        viewPager2.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer(new MarginPageTransformer(40));
        transformer.addTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(0.85f + r * 0.15f);
        });
        viewPager2.setPageTransformer(transformer);

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                setCurrentDot(position);
            }
        });

        sliderAdapter.setOnItemClickListener(new SliderAdapterTv.OnItemClickListener() {
            @Override
            public void onItemClick(SliderItemTv item) {
                // Check if account is blocked for slider clicks
                if (isAccountBlocked) {
                    AppCompatActivity activity = (AppCompatActivity) getContext();
                    showAccountBlockedAlert(activity);
                    return;
                }
                openTvShowEpisodes(String.valueOf(item.getChannelId()));
            }
        });

        sliderRunnable = this::slideNextPage;

        ConstraintLayout watchNowBtn = view.findViewById(R.id.watchnowll);

        watchNowBtn.setOnClickListener(v -> {
            // Check if account is blocked for "Watch Now" button
            if (isAccountBlocked) {
                AppCompatActivity activity = (AppCompatActivity) getContext();
                showAccountBlockedAlert(activity);
                return;
            }

            if (sliderAdapter.getItemCount() > 0) {
                SliderItemTv currentItem = sliderAdapter.getItem(currentPage);
                AppCompatActivity activity = (AppCompatActivity) getActivity();

                // Create the TvShowEpisodeFragment with the channel ID
                TvShowEpisodeFragment tvShowEpisodeFragment = TvShowEpisodeFragment.newInstance(String.valueOf(currentItem.getChannelId()));

                FragmentManager fragmentManager = activity.getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                // Use add instead of replace to preserve the current fragment state
                fragmentTransaction.add(R.id.fragment_container, tvShowEpisodeFragment);
                fragmentTransaction.addToBackStack("tv_show_episodes");
                fragmentTransaction.commit();
            }
        });

        fetchTvDashboard();

        return view;
    }

    private void showAccountBlockedAlert(AppCompatActivity activity) {
        if (!isAdded() || getContext() == null) return;

        SubscriptionBottomSheetFragment bottomSheetFragment = new SubscriptionBottomSheetFragment();
        bottomSheetFragment.show(activity.getSupportFragmentManager(), "MenuBottomSheet");
    }

    private void openTvShowEpisodes(String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            Toast.makeText(getContext(), "TV show not available", Toast.LENGTH_SHORT).show();
            return;
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) return;

        // Create the TvShowEpisodeFragment with the channel ID
        TvShowEpisodeFragment tvShowEpisodeFragment = TvShowEpisodeFragment.newInstance(channelId);

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Use add instead of replace to preserve the current fragment state
        fragmentTransaction.add(R.id.fragment_container, tvShowEpisodeFragment);
        fragmentTransaction.addToBackStack("tv_show_episodes");
        fragmentTransaction.commit();

        Log.d("TvShowsFragment", "Opening TV show episodes for channel ID: " + channelId);
    }

    private void slideNextPage() {
        if (sliderAdapter.getItemCount() > 0) {
            currentPage = (currentPage + 1) % sliderAdapter.getItemCount();
            viewPager2.setCurrentItem(currentPage, true);
        }
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    private void fetchTvDashboard() {
        String accessToken = SharedPrefManager.getInstance(getContext()).getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e("TvShows", "No access token found.");
            return;
        }

        apiInterface.getTvShowDashboard(accessToken).enqueue(new Callback<ResponseBody>() {
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
                                Log.e("TvShows", "Error type 201 - Account blocked");
                                isAccountBlocked = true;
                                blockedMessage = rootObj.has("message") ?
                                        rootObj.get("message").getAsString() : "Account issue detected";

                                // Update adapters with blocked status
                                if (parentAdapter != null) {
                                    parentAdapter.setAccountBlocked(true, blockedMessage);
                                }
                                if (sliderAdapter != null) {
                                    // Add this method to SliderAdapterTv if you have one
                                    // sliderAdapter.setAccountBlocked(true, blockedMessage);
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
                        List<SliderItemTv> bannerItems = new ArrayList<>();
                        Gson gson = new Gson();

                        for (JsonElement element : categoriesArray) {
                            TvCategory category = gson.fromJson(element, TvCategory.class);

                            if ("Tv Show Banner List".equalsIgnoreCase(category.getCategoryName())) {
                                if (category.getChannels() != null) {
                                    for (TvChannel channel : category.getChannels()) {
                                        // now include channelId
                                        bannerItems.add(new SliderItemTv(
                                                channel.getChannelId(),
                                                channel.getChannelLogo(),
                                                channel.getChannelName()
                                        ));
                                    }
                                }
                            } else {
                                if (category.getChannels() != null && !category.getChannels().isEmpty()) {
                                    parentList.add(category);
                                }
                            }
                        }

                        requireActivity().runOnUiThread(() -> {
                            sliderAdapter.updateItems(bannerItems);
                            setupDotsIndicator(bannerItems.size());

                            if (!parentList.isEmpty()) {
                                parentAdapter = new ParentAdapterTvFragment(parentList, getContext());

                                // Pass account blocked status to parent adapter
                                parentAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);

                                parentRecyclerView.setAdapter(parentAdapter);
                            }
                            setupAutoSlide();
                        });

                    } catch (Exception e) {
                        Log.e("TvShowsFragment", "JSON parsing error", e);
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorStr);

                            // Check for error_type 201 in error response
                            if (jsonObject.has("error_type") && "201".equals(jsonObject.getString("error_type"))) {
                                Log.e("TvShows", "Error type 201 - Account blocked from error response");
                                isAccountBlocked = true;
                                blockedMessage = jsonObject.optString("message", "Account issue detected");

                                // Update adapters with blocked status
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
                Log.e("TvShowsFragment", "API call failed", t);
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

    private void setupDotsIndicator(int count) {
        if (dotsLayout == null) return;
        dotsLayout.removeAllViews();

        int dotSize = dpToPx(5);
        int dotMargin = dpToPx(2);

        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(getContext());
            dot.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.dot_inactive_gray));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSize, dotSize);
            params.setMargins(dotMargin, 0, dotMargin, 0);
            dot.setLayoutParams(params);
            dotsLayout.addView(dot);
        }

        setCurrentDot(0);
    }

    private void setCurrentDot(int position) {
        if (!isAdded()) return;

        int childCount = dotsLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView dot = (ImageView) dotsLayout.getChildAt(i);
            dot.setImageDrawable(i == position ?
                    ContextCompat.getDrawable(requireContext(), R.drawable.dot_active_white) :
                    ContextCompat.getDrawable(requireContext(), R.drawable.dot_inactive_gray));
        }
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    private void setupAutoSlide() {
        sliderHandler.removeCallbacks(sliderRunnable);
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupAutoSlide();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sliderHandler.removeCallbacks(sliderRunnable);
    }
}