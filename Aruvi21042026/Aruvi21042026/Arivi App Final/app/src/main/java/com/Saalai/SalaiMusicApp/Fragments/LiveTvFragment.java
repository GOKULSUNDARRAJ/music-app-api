package com.Saalai.SalaiMusicApp.Fragments;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Activity.SignUpActivity;
import com.Saalai.SalaiMusicApp.Adapters.ChannelAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.Channel;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.LiveTvResponse;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerAdapterforLiveChannel;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveTvFragment extends Fragment {

    private static final String TAG = "LiveTvFragment";

    private RecyclerView recyclerView, shimmerRecyclerView;
    private ProgressBar progressBarPagination;
    private ChannelAdapter channelAdapter;
    private ShimmerAdapterforLiveChannel shimmerAdapter;
    private final List<Channel> channelList = new ArrayList<>();

    // Pagination
    private int offset = 0;
    private final int limit = 15;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    // Account blocked status
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_live_tv, container, false);

        shimmerRecyclerView = root.findViewById(R.id.shimmerRecyclerView);
        recyclerView = root.findViewById(R.id.recyclerView);
        progressBarPagination = root.findViewById(R.id.progressBarPagination);

        // Determine column count based on screen size
        int columnCount = 2;
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            columnCount = 3;
        }

        // Shimmer placeholder grid
        shimmerRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));
        shimmerAdapter = new ShimmerAdapterforLiveChannel(90);
        shimmerRecyclerView.setAdapter(shimmerAdapter);

        // Real data grid
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));
        channelAdapter = new ChannelAdapter(requireContext(), channelList, channel -> {
            navigateToVideoPlayer(channel);
        });

        // Pass initial account blocked status to adapter
        channelAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);

        recyclerView.setAdapter(channelAdapter);

        // Endless scroll listener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        Log.d(TAG, "Bottom reached -> Load next page offset=" + offset);
                        loadLiveTvChannels(false);
                    }
                }
            }
        });

        // First load
        loadLiveTvChannels(true);
        updateColumnCount();
        return root;
    }

    // Method to update account blocked status (call this from parent if needed)
    public void setAccountBlocked(boolean isBlocked, String message) {
        this.isAccountBlocked = isBlocked;
        this.blockedMessage = message;

        if (channelAdapter != null) {
            channelAdapter.setAccountBlocked(isBlocked, message);
        }
    }

    private void navigateToVideoPlayer(Channel channel) {
        Log.d(TAG, "Selected Channel: " + channel.getChannelName() + ", URL: " + channel.getChannelURL());

        VideoPlayerFragment videoPlayerFragment = VideoPlayerFragment.newInstance(
                channel.getChannelURL(),
                channel.getChannelName()
        );

        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, videoPlayerFragment);
        transaction.addToBackStack("video_player");
        transaction.commit();
    }

    private void loadLiveTvChannels(boolean isFirstLoad) {
        isLoading = true;

        if (isFirstLoad) {
            shimmerRecyclerView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            progressBarPagination.setVisibility(View.GONE);
        } else {
            // Show progress bar for pagination
            progressBarPagination.setVisibility(View.VISIBLE);
        }

        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
        String accessToken = sp.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e("LiveTv", "No access token found.");
            shimmerRecyclerView.setVisibility(View.GONE);
            progressBarPagination.setVisibility(View.GONE);
            return;
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<LiveTvResponse> call = apiService.getLiveTvList(
                accessToken,
                String.valueOf(offset),
                String.valueOf(limit)
        );

        call.enqueue(new Callback<LiveTvResponse>() {
            @Override
            public void onResponse(Call<LiveTvResponse> call, Response<LiveTvResponse> response) {
                isLoading = false;
                progressBarPagination.setVisibility(View.GONE); // Hide progress bar

                if (isFirstLoad) {
                    shimmerRecyclerView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }

                if (response.isSuccessful() && response.body() != null) {
                    LiveTvResponse liveTvResponse = response.body();

                    // Check error_type for account blocked
                    if ("201".equals(liveTvResponse.getErrorType())) {
                        Log.e("LiveTv", "Error type 201 - Account blocked");
                        isAccountBlocked = true;
                        blockedMessage = liveTvResponse.getMessage();

                        // Update adapter with blocked status
                        if (channelAdapter != null) {
                            channelAdapter.setAccountBlocked(true, blockedMessage);
                        }


                        // DON'T return here - continue to load channels
                    } else {
                        // Reset account blocked status if API returns success
                        isAccountBlocked = false;
                        blockedMessage = "";
                        if (channelAdapter != null) {
                            channelAdapter.setAccountBlocked(false, "");
                        }
                    }

                    // Check if response is successful
                    if (liveTvResponse.isStatus() && liveTvResponse.getResponse() != null) {
                        List<Channel> apiChannels = liveTvResponse.getResponse().getChannelList();

                        if (apiChannels != null && !apiChannels.isEmpty()) {
                            // Calculate the position where new items start
                            int currentSize = channelList.size();

                            channelList.addAll(apiChannels);

                            if (isFirstLoad) {
                                channelAdapter.notifyDataSetChanged();
                            } else {
                                // Only notify for the new items for better performance
                                channelAdapter.notifyItemRangeInserted(currentSize, apiChannels.size());
                            }

                            offset++; // next page

                            if (apiChannels.size() < limit) {
                                isLastPage = true;
                                Log.d(TAG, "Last page reached");
                            }

                            // Log success
                            Log.d(TAG, "Loaded " + apiChannels.size() + " channels. Total now: " + channelList.size());

                        } else {
                            // No channels in response
                            isLastPage = true;
                            if (isFirstLoad) {
                                Toast.makeText(getContext(), "No channels available", Toast.LENGTH_SHORT).show();
                            }
                            Log.d(TAG, "No channels in response");
                        }
                    } else {
                        // API returned error status
                        String errorMessage = liveTvResponse.getMessage();
                        Toast.makeText(getContext(), errorMessage != null ? errorMessage : "Failed to load channels", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "API Error: " + liveTvResponse.getMessage());
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorStr);

                            // Check for error_type 201 in error response
                            if (jsonObject.has("error_type") && "201".equals(jsonObject.getString("error_type"))) {
                                Log.e("LiveTv", "Error type 201 - Account blocked from error response");
                                isAccountBlocked = true;
                                blockedMessage = jsonObject.optString("message", "Account issue detected");

                                // Update adapter with blocked status
                                if (channelAdapter != null) {
                                    channelAdapter.setAccountBlocked(true, blockedMessage);
                                }


                                // DON'T return here - let the fragment show empty or existing data
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
            public void onFailure(Call<LiveTvResponse> call, Throwable t) {
                isLoading = false;
                progressBarPagination.setVisibility(View.GONE); // Hide progress bar on failure

                if (isFirstLoad) {
                    shimmerRecyclerView.setVisibility(View.GONE);
                }
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "API call failed: " + t.getMessage());
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

    @Override
    public void onResume() {
        super.onResume();
        if (getParentFragment() instanceof SaalaiFragment) {
            ((SaalaiFragment) getParentFragment()).updateSelectedTab(1);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Update column count based on new orientation
        updateColumnCount();
    }

    private void updateColumnCount() {
        int columnCount = 2;
        Configuration config = getResources().getConfiguration();

        Log.d("ColumnDebug", "onConfigurationChanged - smallestScreenWidthDp: " + config.smallestScreenWidthDp);
        Log.d("ColumnDebug", "onConfigurationChanged - Orientation: " + (config.orientation == Configuration.ORIENTATION_LANDSCAPE ? "LANDSCAPE" : "PORTRAIT"));

        if (config.smallestScreenWidthDp >= 600) {
            columnCount = 3;
            if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                columnCount = 5;
            }
        }

        Log.d("ColumnDebug", "Updated column count: " + columnCount);

        // Update layout manager
        if (recyclerView != null && recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
            if (layoutManager != null && layoutManager.getSpanCount() != columnCount) {
                layoutManager.setSpanCount(columnCount);
                layoutManager.requestLayout();
            }
        }

        // Also update shimmer recycler view
        if (shimmerRecyclerView != null && shimmerRecyclerView.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager shimmerLayoutManager = (GridLayoutManager) shimmerRecyclerView.getLayoutManager();
            if (shimmerLayoutManager != null && shimmerLayoutManager.getSpanCount() != columnCount) {
                shimmerLayoutManager.setSpanCount(columnCount);
                shimmerLayoutManager.requestLayout();
            }
        }
    }
}