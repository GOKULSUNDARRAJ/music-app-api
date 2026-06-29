package com.Saalai.SalaiMusicApp.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Activity.SignUpActivity;
import com.Saalai.SalaiMusicApp.Adapters.CatchUpAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.CatchUp;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.CatchUpResponse;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerAdapterfocatchup;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CatchUpFragment extends Fragment {

    private static final String TAG = "CatchUpFragment";

    private RecyclerView recyclerView, shimmerRecyclerView;
    private CatchUpAdapter channelAdapter;
    private ShimmerAdapterfocatchup shimmerAdapter;
    private final List<CatchUp> channelList = new ArrayList<>();

    private int offset = 0;
    private final int limit = 10;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    // Account blocked status
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_catch_up, container, false);

        shimmerRecyclerView = root.findViewById(R.id.shimmerRecyclerView);
        recyclerView = root.findViewById(R.id.recyclerView);

        Log.d(TAG, "Initializing Shimmer and RecyclerView");

        int columnCount = 2;
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            columnCount = 4;
        }

        shimmerRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));
        shimmerAdapter = new ShimmerAdapterfocatchup(50);
        shimmerRecyclerView.setAdapter(shimmerAdapter);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));
        channelAdapter = new CatchUpAdapter(requireContext(), channelList, channel -> {
            Log.d(TAG, "User clicked on channel: " + channel.getChannelName() + " ID: " + channel.getChannelId());

            // Use the activity's fragment container
            CatchUpDetailFragment catchUpDetailFragment = CatchUpDetailFragment.newInstance(String.valueOf(channel.getChannelId()));

            // Use requireActivity() to get the activity's fragment manager
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            // Replace the fragment in the activity's container
            fragmentTransaction.add(R.id.fragment_container, catchUpDetailFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });

        // Pass initial account blocked status to adapter
        channelAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);

        recyclerView.setAdapter(channelAdapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                Log.d(TAG, "Scroll: visible=" + visibleItemCount + " total=" + totalItemCount +
                        " firstVisible=" + firstVisibleItemPosition);

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        Log.d(TAG, "Bottom reached -> Load next page offset=" + offset);
                        loadCatchUpChannels(false);
                    }
                }
            }
        });

        Log.d(TAG, "Loading first page of catch-up channels");
        loadCatchUpChannels(true);

        return root;
    }

    private void loadCatchUpChannels(boolean isFirstLoad) {
        isLoading = true;
        Log.d(TAG, "loadCatchUpChannels called. isFirstLoad=" + isFirstLoad + " offset=" + offset);

        if (isFirstLoad) {
            shimmerRecyclerView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }

        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
        String accessToken = sp.getAccessToken();
        Log.d(TAG, "AccessToken retrieved: " + accessToken);

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "No access token found.");
            shimmerRecyclerView.setVisibility(View.GONE);
            return;
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<CatchUpResponse> call = apiService.getCatchUpChannelList(
                accessToken,
                String.valueOf(offset),
                String.valueOf(limit)
        );

        Log.d(TAG, "API call initiated for CatchUp channels");
        call.enqueue(new Callback<CatchUpResponse>() {
            @Override
            public void onResponse(Call<CatchUpResponse> call, Response<CatchUpResponse> response) {
                isLoading = false;
                Log.d(TAG, "API response received. Success=" + response.isSuccessful());

                if (isFirstLoad) {
                    shimmerRecyclerView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }

                if (response.isSuccessful() && response.body() != null) {
                    CatchUpResponse catchUpResponse = response.body();

                    // Check for error_type in the response
                    if ("201".equals(catchUpResponse.getErrorType())) {
                        Log.e(TAG, "Error type 201 - Account blocked");
                        isAccountBlocked = true;
                        blockedMessage = catchUpResponse.getMessage();

                        // Update adapter with blocked status
                        channelAdapter.setAccountBlocked(true, blockedMessage);


                        // Continue to load channels - don't return
                    } else {
                        // Reset account blocked status if API returns success
                        isAccountBlocked = false;
                        blockedMessage = "";
                        channelAdapter.setAccountBlocked(false, "");
                    }

                    // Check if the API call was successful
                    if (catchUpResponse.isStatus()) {
                        List<CatchUp> apiChannels = catchUpResponse.getChannelList();
                        Log.d(TAG, "API returned " + (apiChannels != null ? apiChannels.size() : 0) + " channels");

                        if (apiChannels != null && !apiChannels.isEmpty()) {
                            channelList.addAll(apiChannels);
                            channelAdapter.notifyDataSetChanged();
                            offset++;

                            if (apiChannels.size() < limit) {
                                isLastPage = true;
                                Log.d(TAG, "Reached last page of channels");
                            }
                        } else {
                            isLastPage = true;
                            Log.d(TAG, "No channels returned from API");
                            Toast.makeText(getContext(), "No catch-up channels available", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // API returned error status
                        String errorMsg = catchUpResponse.getMessage();
                        Toast.makeText(getContext(), errorMsg != null ? errorMsg : "Failed to load channels", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "API Error: " + errorMsg);
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
                                channelAdapter.setAccountBlocked(true, blockedMessage);


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
                        Toast.makeText(getContext(), "Error loading channels", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<CatchUpResponse> call, Throwable t) {
                isLoading = false;
                if (isFirstLoad) shimmerRecyclerView.setVisibility(View.GONE);
                Log.e(TAG, "API call failed: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

}