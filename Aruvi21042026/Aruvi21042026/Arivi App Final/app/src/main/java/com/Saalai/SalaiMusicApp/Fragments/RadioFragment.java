package com.Saalai.SalaiMusicApp.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Activity.SignUpActivity;
import com.Saalai.SalaiMusicApp.Adapters.RadioAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.RadioModel;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.RadioResponse;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerAdapterForRadio;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RadioFragment extends Fragment {

    private static final String TAG = "RadioFragment";

    private RecyclerView recyclerView, shimmerRecyclerView;
    private RadioAdapter radioAdapter;
    private ShimmerAdapterForRadio shimmerAdapter;
    private final List<RadioModel> radioList = new ArrayList<>();
    private ProgressBar paginationProgressBar;

    // Pagination variables
    private int offset = 0;
    private final int count = 10;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    // Account blocked status
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";

    private SaalaiFragment.DrawerToggleListener drawerToggleListener;
    private ImageView profileIcon;

    public interface DrawerToggleListener {
        void onToggleDrawer();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            drawerToggleListener = (SaalaiFragment.DrawerToggleListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement DrawerToggleListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_radio, container, false);

        shimmerRecyclerView = root.findViewById(R.id.shimmerRecyclerView);
        recyclerView = root.findViewById(R.id.recyclerView);
        paginationProgressBar = root.findViewById(R.id.paginationProgressBar);

        int columnCount = 2;
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            columnCount = 3;
        }

        // Shimmer placeholder list
        shimmerRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));
        shimmerAdapter = new ShimmerAdapterForRadio(50);
        shimmerRecyclerView.setAdapter(shimmerAdapter);

        // Real data list
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));
        radioAdapter = new RadioAdapter(getActivity(), radioList);

        // Pass initial account blocked status to adapter
        radioAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);

        recyclerView.setAdapter(radioAdapter);

        radioAdapter.setOnRadioClickListener(radio -> {
            Log.d(TAG, "Radio clicked: " + radio.getChannelName());
            RadioPlayerFragment radioPlayerFragment = RadioPlayerFragment.newInstance(radio);
            getParentFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, radioPlayerFragment)
                    .addToBackStack("radio_player")
                    .commit();
        });

        // Endless scroll listener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        Log.d(TAG, "Bottom reached -> Load next page offset=" + offset);
                        loadRadioList(false);
                    }
                }
            }
        });

        profileIcon = root.findViewById(R.id.iv_user_info);



        // Load first page
        loadRadioList(true);
        updateColumnCount();
        return root;
    }

    private void loadRadioList(boolean isFirstLoad) {
        if (isLoading) return;
        isLoading = true;

        if (isFirstLoad) {
            shimmerRecyclerView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            paginationProgressBar.setVisibility(View.GONE);
        } else {
            paginationProgressBar.setVisibility(View.VISIBLE);
        }

        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());

        String accessToken = sp.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "No access token found.");
            shimmerRecyclerView.setVisibility(View.GONE);
            paginationProgressBar.setVisibility(View.GONE);
            return;
        }

        // Ensure token has Bearer prefix
        String authHeader = accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;
        Log.d(TAG, "Fetching Radio List → offset=" + offset + ", token: " + authHeader.substring(0, Math.min(20, authHeader.length())) + "...");

        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Call<RadioResponse> call = apiService.getRadioList(
                authHeader,
                "",
                String.valueOf(offset),
                String.valueOf(count)
        );

        call.enqueue(new Callback<RadioResponse>() {
            @Override
            public void onResponse(Call<RadioResponse> call, Response<RadioResponse> response) {
                isLoading = false;
                paginationProgressBar.setVisibility(View.GONE);

                if (isFirstLoad) {
                    shimmerRecyclerView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }

                if (response.isSuccessful() && response.body() != null) {
                    RadioResponse radioResponse = response.body();

                    // Check for error_type in the response
                    if ("201".equals(radioResponse.getErrorType())) {
                        Log.e(TAG, "Error type 201 - Account blocked");
                        isAccountBlocked = true;
                        blockedMessage = radioResponse.getMessage();

                        // Update adapter with blocked status
                        radioAdapter.setAccountBlocked(true, blockedMessage);


                        // Continue to load radios - don't return
                    } else {
                        // Reset account blocked status if API returns success
                        isAccountBlocked = false;
                        blockedMessage = "";
                        radioAdapter.setAccountBlocked(false, "");
                    }

                    // Check if API call was successful
                    if (radioResponse.isStatus()) {
                        List<RadioModel> apiRadios = radioResponse.getRadioList();

                        if (apiRadios != null && !apiRadios.isEmpty()) {
                            // Calculate current size for proper notification
                            int currentSize = radioList.size();

                            // On first load, you might want to include channelDetails as the first item
                            if (isFirstLoad && offset == 0) {
                                RadioModel channelDetails = radioResponse.getChannelDetails();
                                if (channelDetails != null) {
                                    // Add channel details as first item if you want
                                    // radioList.add(channelDetails);
                                    // currentSize++;
                                }
                            }

                            radioList.addAll(apiRadios);

                            if (isFirstLoad) {
                                radioAdapter.notifyDataSetChanged();
                            } else {
                                // Only notify about the new items for better performance
                                radioAdapter.notifyItemRangeInserted(currentSize, apiRadios.size());
                            }

                            offset++;
                            Log.d(TAG, "Next offset will be: " + offset);

                            if (apiRadios.size() < count) {
                                isLastPage = true;
                                Log.d(TAG, "Reached last page of radio list");
                            }
                        } else {
                            isLastPage = true;
                            Log.d(TAG, "Empty radio list received");

                            // Still set adapter if shimmer is showing
                            if (isFirstLoad) {
                                radioAdapter.notifyDataSetChanged();

                            }
                        }
                    } else {
                        // API returned error status
                        String errorMsg = radioResponse.getMessage();

                        Log.e(TAG, "API Error: " + errorMsg);

                        // Still set adapter if shimmer is showing
                        if (isFirstLoad) {
                            radioAdapter.notifyDataSetChanged();
                        }
                    }
                } else {
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
                                radioAdapter.setAccountBlocked(true, blockedMessage);


                                // Continue - don't return
                            }

                            if (jsonObject.has("error_type") && "401".equals(jsonObject.getString("error_type"))) {

                                callLogoutApi();
                            } else {
                                String errorMessage = jsonObject.optString("message", "Server error");

                                Log.e(TAG, "Server Error Code: " + response.code() + ", Error: " + errorStr);
                            }
                        } else {

                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing errorBody", e);

                    }

                    // Still set adapter if shimmer is showing
                    if (isFirstLoad) {
                        radioAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<RadioResponse> call, Throwable t) {
                isLoading = false;
                paginationProgressBar.setVisibility(View.GONE);

                if (isFirstLoad) {
                    shimmerRecyclerView.setVisibility(View.GONE);
                    radioAdapter.notifyDataSetChanged();
                }

                Log.e(TAG, "API call failed: " + t.getMessage(), t);
            }
        });
    }

    private void callLogoutApi() {
        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
        // Force refresh  sp.reload();
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

    private void handleProfileClick() {
        if (drawerToggleListener != null) {
            drawerToggleListener.onToggleDrawer();
        }
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