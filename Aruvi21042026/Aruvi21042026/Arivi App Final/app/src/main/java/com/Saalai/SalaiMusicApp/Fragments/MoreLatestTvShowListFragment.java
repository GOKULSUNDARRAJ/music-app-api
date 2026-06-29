package com.Saalai.SalaiMusicApp.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.TvShowAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.TvShow;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerTvListAdapter;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MoreLatestTvShowListFragment extends Fragment {

    private static final String TAG = "MoreLatestTvShowListFragment";
    private static final String SCROLL_POSITION_KEY = "scroll_position";
    private static final String TV_SHOW_LIST_KEY = "tv_show_list";

    private RecyclerView recyclerView;
    private TvShowAdapter adapter;
    private final List<TvShow> tvShowList = new ArrayList<>();
    private ProgressBar paginationProgressBar;

    private int offset = 0;
    private final int limit = 15;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private BroadcastReceiver closeReceiver;

    // State preservation variables
    private Parcelable recyclerViewState;
    private GridLayoutManager layoutManager;

    // Account blocked status
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";

    public MoreLatestTvShowListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more_latest_tv_show_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();
        setupClickListeners();
        fetchTvShows();
        setupCloseReceiver();
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.movieRecyclerView);
        paginationProgressBar = view.findViewById(R.id.paginationProgressBar);
        ImageView back = view.findViewById(R.id.backimg);

        back.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save RecyclerView state
        if (recyclerView != null && layoutManager != null) {
            outState.putParcelable(SCROLL_POSITION_KEY, layoutManager.onSaveInstanceState());
        }
        // Save TV show list data
        outState.putSerializable(TV_SHOW_LIST_KEY, new ArrayList<>(tvShowList));
        outState.putInt("offset", offset);
        outState.putBoolean("isLastPage", isLastPage);
        outState.putBoolean("isAccountBlocked", isAccountBlocked);
        outState.putString("blockedMessage", blockedMessage);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            // Restore TV show list data
            ArrayList<TvShow> savedList = (ArrayList<TvShow>) savedInstanceState.getSerializable(TV_SHOW_LIST_KEY);
            if (savedList != null && !savedList.isEmpty()) {
                tvShowList.clear();
                tvShowList.addAll(savedList);

                offset = savedInstanceState.getInt("offset", 0);
                isLastPage = savedInstanceState.getBoolean("isLastPage", false);
                isAccountBlocked = savedInstanceState.getBoolean("isAccountBlocked", false);
                blockedMessage = savedInstanceState.getString("blockedMessage", "");

                // Restore adapter with existing data (no shimmer)
                if (recyclerView != null) {
                    adapter = new TvShowAdapter(getContext(), tvShowList);
                    // Restore account blocked status to adapter
                    adapter.setAccountBlocked(isAccountBlocked, blockedMessage);
                    recyclerView.setAdapter(adapter);

                    // Restore scroll position
                    Parcelable savedState = savedInstanceState.getParcelable(SCROLL_POSITION_KEY);
                    if (savedState != null && layoutManager != null) {
                        layoutManager.onRestoreInstanceState(savedState);
                    }
                }
            } else {
                // No saved data, fetch fresh
                fetchTvShows();
            }
        }
    }

    private void setupRecyclerView() {
        int columnCount = 2;
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            columnCount = 4;
        }

        layoutManager = new GridLayoutManager(getContext(), columnCount);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new TvShowAdapter(getContext(), tvShowList);

        // Pass initial account blocked status to adapter
        adapter.setAccountBlocked(isAccountBlocked, blockedMessage);

        // Only show shimmer if we have no data
        if (tvShowList.isEmpty()) {
            ShimmerTvListAdapter shimmerAdapter = new ShimmerTvListAdapter(50);
            recyclerView.setAdapter(shimmerAdapter);
        } else {
            recyclerView.setAdapter(adapter);
        }

        // Endless scroll listener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);

                if (layoutManager == null) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPos = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPos) >= totalItemCount
                            && firstVisibleItemPos >= 0) {
                        Log.d(TAG, "📌 Bottom reached → loading more (offset=" + offset + ")");
                        fetchTvShows();
                    }
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        // Save state when fragment goes to background
        if (layoutManager != null) {
            recyclerViewState = layoutManager.onSaveInstanceState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Restore state when fragment comes to foreground
        if (layoutManager != null && recyclerViewState != null) {
            layoutManager.onRestoreInstanceState(recyclerViewState);
        }
    }

    private void setupClickListeners() {
        // Back button handled in initializeViews
    }

    private void fetchTvShows() {
        if (isLoading) return;

        isLoading = true;

        // Show pagination progress bar only for subsequent loads (not first load)
        if (offset > 0) {
            paginationProgressBar.setVisibility(View.VISIBLE);
        }

        String accessToken = SharedPrefManager.getInstance(getContext()).getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "No access token found.");
            if (recyclerView.getAdapter() instanceof ShimmerTvListAdapter) {
                adapter = new TvShowAdapter(getContext(), tvShowList);
                adapter.setAccountBlocked(isAccountBlocked, blockedMessage);
                recyclerView.setAdapter(adapter);
            }
            paginationProgressBar.setVisibility(View.GONE);
            return;
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Log.d(TAG, "Fetching TV shows: offset=" + offset + ", limit=" + limit);

        apiService.getLatestTvShowList(accessToken, offset, limit).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                isLoading = false;
                paginationProgressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String rawJson = response.body().string();
                        Log.d(TAG, "API Response: " + rawJson);

                        JsonObject jsonObject = JsonParser.parseString(rawJson).getAsJsonObject();

                        // Check for error_type in the response
                        if (jsonObject.has("error_type")) {
                            String errorType = jsonObject.get("error_type").getAsString();
                            if ("201".equals(errorType)) {
                                Log.e(TAG, "Error type 201 - Account blocked");
                                isAccountBlocked = true;
                                blockedMessage = jsonObject.has("message") ?
                                        jsonObject.get("message").getAsString() : "Account issue detected";

                                // Update adapter with blocked status
                                adapter.setAccountBlocked(true, blockedMessage);

                                // Continue to load TV shows - don't return
                            } else {
                                // Reset account blocked status if API returns success
                                isAccountBlocked = false;
                                blockedMessage = "";
                                adapter.setAccountBlocked(false, "");
                            }
                        }

                        // Check if API call was successful
                        boolean status = jsonObject.get("status").getAsBoolean();
                        String message = jsonObject.get("message").getAsString();

                        if (status) {
                            // Get the nested response object
                            JsonObject responseObj = jsonObject.getAsJsonObject("response");
                            if (responseObj != null) {
                                JsonArray array = responseObj.getAsJsonArray("tvShowList");

                                if (array != null && array.size() > 0) {
                                    int currentSize = tvShowList.size();

                                    for (int i = 0; i < array.size(); i++) {
                                        JsonObject obj = array.get(i).getAsJsonObject();
                                        TvShow show = new TvShow();
                                        show.setChannelId(obj.get("channelId").getAsInt());
                                        show.setChannelName(obj.get("channelName").getAsString());
                                        show.setChannelLogo(obj.get("channelLogo").getAsString());
                                        tvShowList.add(show);
                                    }

                                    // Replace shimmer with real adapter on first load
                                    if (recyclerView.getAdapter() instanceof ShimmerTvListAdapter) {
                                        recyclerView.setAdapter(adapter);
                                    } else {
                                        // Only notify about the new items for better performance
                                        adapter.notifyItemRangeInserted(currentSize, array.size());
                                    }

                                    offset++;

                                    if (array.size() < limit) {
                                        isLastPage = true;
                                        Log.d(TAG, "Last page reached for TV shows");
                                    }
                                } else {
                                    isLastPage = true;
                                    Log.d(TAG, "No TV shows found");

                                    // Still replace shimmer with empty adapter
                                    if (recyclerView.getAdapter() instanceof ShimmerTvListAdapter) {
                                        recyclerView.setAdapter(adapter);
                                        Toast.makeText(getContext(), "No TV shows available", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } else {
                                Log.d(TAG, "No response object found");
                                Toast.makeText(getContext(), "Invalid response format", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // API returned error status
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "API Error: " + message);

                            // Still set adapter if shimmer is showing
                            if (recyclerView.getAdapter() instanceof ShimmerTvListAdapter) {
                                recyclerView.setAdapter(adapter);
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "IOException parsing TV show response", e);
                        Toast.makeText(getContext(), "Error parsing response", Toast.LENGTH_SHORT).show();

                        if (recyclerView.getAdapter() instanceof ShimmerTvListAdapter) {
                            recyclerView.setAdapter(adapter);
                        }
                    }
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                isLoading = false;
                paginationProgressBar.setVisibility(View.GONE);
                Log.e(TAG, "API call failed", t);
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();

                // Still set adapter if shimmer is showing
                if (recyclerView.getAdapter() instanceof ShimmerTvListAdapter) {
                    recyclerView.setAdapter(adapter);
                }
            }
        });
    }

    private void handleErrorResponse(Response<ResponseBody> response) {
        try {
            if (response.errorBody() != null) {
                String errorStr = response.errorBody().string();
                JSONObject jsonObject = new JSONObject(errorStr);

                // Check for error_type 201 in error response
                if (jsonObject.has("error_type") && "201".equals(jsonObject.getString("error_type"))) {
                    Log.e(TAG, "Error type 201 - Account blocked from error response");
                    isAccountBlocked = true;
                    blockedMessage = jsonObject.optString("message", "Account issue detected");

                    // Update adapter with blocked status
                    adapter.setAccountBlocked(true, blockedMessage);


                    // Continue - don't return
                }

                if (jsonObject.has("error_type") && "401".equals(jsonObject.getString("error_type"))) {
                    Toast.makeText(getContext(), "Access Denied", Toast.LENGTH_SHORT).show();
                    callLogoutApi();
                } else {
                    String errorMessage = jsonObject.optString("message", "Server error");
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e("Dashboard", "Server Error Code: " + response.code() + ", Error: " + errorStr);
                }
            } else {
                Toast.makeText(getContext(), "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("Dashboard", "Error parsing errorBody", e);
            Toast.makeText(getContext(), "Error loading TV shows", Toast.LENGTH_SHORT).show();
        }

        // Still set adapter if shimmer is showing
        if (recyclerView.getAdapter() instanceof ShimmerTvListAdapter) {
            recyclerView.setAdapter(adapter);
        }
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
                    sp.clearAccessToken();
                    requireActivity().finish();
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

    private void setupCloseReceiver() {
        closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("CLOSE_MoreLatestTvShowListFragment".equals(intent.getAction())) {
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("CLOSE_MoreLatestTvShowListFragment");
        if (getContext() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getContext().registerReceiver(closeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                getContext().registerReceiver(closeReceiver, filter);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister receiver
        if (closeReceiver != null && getContext() != null) {
            getContext().unregisterReceiver(closeReceiver);
        }
    }
}