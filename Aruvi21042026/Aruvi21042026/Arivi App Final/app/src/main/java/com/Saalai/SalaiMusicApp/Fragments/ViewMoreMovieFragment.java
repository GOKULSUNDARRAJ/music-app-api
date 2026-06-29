package com.Saalai.SalaiMusicApp.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.MovieListAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.MovieItem;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerMovieListAdapter;
import com.google.gson.Gson;
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

public class ViewMoreMovieFragment extends Fragment {

    private static final String TAG = "ViewMoreMovieFragment";
    private Gson gson = new Gson();

    private int categoryId = -1;
    private String categoryName = "";
    private int offset = 0;
    private final int count = 15;

    private boolean isLoading = false;
    private boolean isLastPage = false;

    private RecyclerView movieRecyclerView;
    private MovieListAdapter movieListAdapter;
    private final List<MovieItem> movieList = new ArrayList<>();
    private BroadcastReceiver closeReceiver;
    private ProgressBar paginationProgressBar;
    private GridLayoutManager layoutManager;

    // Track column count and scroll state
    private int currentColumnCount = 3;
    private Parcelable recyclerViewState;

    // Account blocked status
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";

    public ViewMoreMovieFragment() {
        // Required empty public constructor
    }

    public static ViewMoreMovieFragment newInstance(int categoryId, String categoryName) {
        ViewMoreMovieFragment fragment = new ViewMoreMovieFragment();
        Bundle args = new Bundle();
        args.putInt("categoryId", categoryId);
        args.putString("categoryName", categoryName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_more_movie, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            categoryId = args.getInt("categoryId", -1);
            categoryName = args.getString("categoryName", "");
            Log.d(TAG, "CategoryId: " + categoryId + ", CategoryName: " + categoryName);
        }

        initializeViews(view);
        setupRecyclerView();
        setupClickListeners();
        fetchMovies();
        setupCloseReceiver();
    }

    private void initializeViews(View view) {
        movieRecyclerView = view.findViewById(R.id.movieRecyclerView);
        paginationProgressBar = view.findViewById(R.id.paginationProgressBar);

        TextView titleText = view.findViewById(R.id.txtCategoryTitle);
        if (categoryName != null && !categoryName.isEmpty()) {
            titleText.setText(categoryName);
        }

        ImageView back = view.findViewById(R.id.backimg);
        back.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    private void setupRecyclerView() {
        currentColumnCount = calculateColumnCount();
        layoutManager = new GridLayoutManager(getContext(), currentColumnCount);
        movieRecyclerView.setLayoutManager(layoutManager);

        movieListAdapter = new MovieListAdapter(getContext(), movieList);

        // Pass initial account blocked status to adapter
        movieListAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);

        // Set shimmer adapter initially
        ShimmerMovieListAdapter shimmerAdapter = new ShimmerMovieListAdapter(50);
        movieRecyclerView.setAdapter(shimmerAdapter);

        // Endless scroll listener
        movieRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (layoutManager == null) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        Log.d(TAG, "Bottom reached → load next page. Offset=" + offset);
                        fetchMovies();
                    }
                }
            }
        });
    }

    // Method to calculate column count based on screen size and orientation
    private int calculateColumnCount() {
        Configuration config = getResources().getConfiguration();

        // For tablets (smallestScreenWidthDp >= 600)
        if (config.smallestScreenWidthDp >= 600) {
            if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return 9;
            } else {
                return 6;
            }
        }
        // For phones
        else {
            if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return 5;
            } else {
                return 3;
            }
        }
    }

    // Handle configuration changes (orientation change)
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int newColumnCount = calculateColumnCount();

        // Only update if column count actually changed
        if (newColumnCount != currentColumnCount && layoutManager != null) {
            currentColumnCount = newColumnCount;

            // Save current scroll position
            Parcelable scrollState = null;
            if (movieRecyclerView.getLayoutManager() != null) {
                scrollState = movieRecyclerView.getLayoutManager().onSaveInstanceState();
            }

            // Create new layout manager with updated column count
            layoutManager = new GridLayoutManager(getContext(), currentColumnCount);
            movieRecyclerView.setLayoutManager(layoutManager);

            // Restore scroll position
            if (scrollState != null) {
                movieRecyclerView.getLayoutManager().onRestoreInstanceState(scrollState);
            }

            Log.d(TAG, "Orientation changed. Updated column count to: " + currentColumnCount);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Save scroll position when fragment is paused
        if (layoutManager != null) {
            recyclerViewState = layoutManager.onSaveInstanceState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Restore scroll position when fragment is resumed
        if (layoutManager != null && recyclerViewState != null) {
            layoutManager.onRestoreInstanceState(recyclerViewState);
        }
    }

    private void setupClickListeners() {
        // Back button already handled in initializeViews
    }

    private void fetchMovies() {
        if (isLoading) return;

        isLoading = true;

        // Show pagination progress bar only for subsequent loads (not first load)
        if (offset > 0) {
            paginationProgressBar.setVisibility(View.VISIBLE);
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String accessToken = SharedPrefManager.getInstance(getContext()).getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "No access token found.");
            if (movieRecyclerView.getAdapter() instanceof ShimmerMovieListAdapter) {
                movieRecyclerView.setAdapter(movieListAdapter);
            }
            paginationProgressBar.setVisibility(View.GONE);
            return;
        }

        Log.d(TAG, "Fetching movies: categoryId=" + categoryId + ", offset=" + offset + ", count=" + count);

        apiService.getMovieList(accessToken, categoryId, offset, count).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                isLoading = false;
                paginationProgressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String rawJson = response.body().string();
                        Log.d(TAG, "✅ API Response: " + rawJson);

                        // Parse the JSON response
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
                                movieListAdapter.setAccountBlocked(true, blockedMessage);


                                // Continue to load movies - don't return
                            } else {
                                // Reset account blocked status if API returns success
                                isAccountBlocked = false;
                                blockedMessage = "";
                                movieListAdapter.setAccountBlocked(false, "");
                            }
                        }

                        // Check if the API call was successful
                        boolean status = jsonObject.get("status").getAsBoolean();
                        String message = jsonObject.get("message").getAsString();

                        if (status) {
                            // Get the nested response object
                            JsonObject responseObj = jsonObject.getAsJsonObject("response");
                            if (responseObj != null) {
                                JsonArray movieArray = responseObj.getAsJsonArray("movieList");

                                if (movieArray != null && movieArray.size() > 0) {
                                    int currentSize = movieList.size();

                                    for (int i = 0; i < movieArray.size(); i++) {
                                        JsonObject movieObj = movieArray.get(i).getAsJsonObject();
                                        int channelId = movieObj.get("channelId").getAsInt();
                                        String channelName = movieObj.get("channelName").getAsString();
                                        String channelLogo = movieObj.get("channelLogo").getAsString();

                                        movieList.add(new MovieItem(channelId, channelName, channelLogo));
                                    }

                                    // Replace shimmer with real adapter on first load
                                    if (movieRecyclerView.getAdapter() instanceof ShimmerMovieListAdapter) {
                                        movieRecyclerView.setAdapter(movieListAdapter);
                                    } else {
                                        // Only notify about the new items for better performance
                                        movieListAdapter.notifyItemRangeInserted(currentSize, movieArray.size());
                                    }

                                    offset++;

                                    if (movieArray.size() < count) {
                                        isLastPage = true;
                                        Log.d(TAG, "⚠️ Reached last page of movies");
                                    }
                                } else {
                                    isLastPage = true;
                                    Log.d(TAG, "⚠️ No movies found in response");

                                    // Still replace shimmer with empty adapter
                                    if (movieRecyclerView.getAdapter() instanceof ShimmerMovieListAdapter) {
                                        movieRecyclerView.setAdapter(movieListAdapter);
                                        Toast.makeText(getContext(), "No movies available", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } else {
                                Log.d(TAG, "⚠️ No response object found");
                                Toast.makeText(getContext(), "Invalid response format", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // API returned error status
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "API Error: " + message);

                            // Still set adapter if shimmer is showing
                            if (movieRecyclerView.getAdapter() instanceof ShimmerMovieListAdapter) {
                                movieRecyclerView.setAdapter(movieListAdapter);
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "❌ IOException while parsing response", e);
                        Toast.makeText(getContext(), "Error parsing response", Toast.LENGTH_SHORT).show();

                        if (movieRecyclerView.getAdapter() instanceof ShimmerMovieListAdapter) {
                            movieRecyclerView.setAdapter(movieListAdapter);
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
                if (movieRecyclerView.getAdapter() instanceof ShimmerMovieListAdapter) {
                    movieRecyclerView.setAdapter(movieListAdapter);
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
                    movieListAdapter.setAccountBlocked(true, blockedMessage);


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
            Toast.makeText(getContext(), "Error loading movies", Toast.LENGTH_SHORT).show();
        }

        // Still set adapter if shimmer is showing
        if (movieRecyclerView.getAdapter() instanceof ShimmerMovieListAdapter) {
            movieRecyclerView.setAdapter(movieListAdapter);
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
                if ("CLOSE_ViewMoreMovieFragment".equals(intent.getAction())) {
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("CLOSE_ViewMoreMovieFragment");
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
        if (closeReceiver != null && getContext() != null) {
            getContext().unregisterReceiver(closeReceiver);
        }
    }
}