package com.Saalai.SalaiMusicApp.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.DisplayMetrics;
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

import com.Saalai.SalaiMusicApp.Adapters.MovieListAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.MovieItem;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerMovieListAdapter;
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

public class MoreLastesMovieFragment extends Fragment {

    private static final String TAG = "MoreLastesMovieFragment";
    private static final String SCROLL_POSITION_KEY = "scroll_position";
    private static final String MOVIE_LIST_KEY = "movie_list";

    private int offset = 0;
    private final int count = 15;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    private RecyclerView movieRecyclerView;
    private MovieListAdapter movieListAdapter;
    private final List<MovieItem> movieList = new ArrayList<>();
    private BroadcastReceiver closeReceiver;
    private ProgressBar paginationProgressBar;

    private Parcelable recyclerViewState;
    private GridLayoutManager layoutManager;

    // Account blocked status
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";

    public MoreLastesMovieFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more_lastes_movie, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();
        setupClickListeners();

        // Check if we have saved data
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            fetchMovies();
        }

        setupCloseReceiver();
    }

    private void initializeViews(View view) {
        movieRecyclerView = view.findViewById(R.id.movieRecyclerView);
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
        if (movieRecyclerView != null && layoutManager != null) {
            outState.putParcelable(SCROLL_POSITION_KEY, layoutManager.onSaveInstanceState());
        }
        // Save movie list data
        outState.putSerializable(MOVIE_LIST_KEY, new ArrayList<>(movieList));
        outState.putInt("offset", offset);
        outState.putBoolean("isLastPage", isLastPage);
        outState.putBoolean("isLoading", isLoading);
        outState.putBoolean("isAccountBlocked", isAccountBlocked);
        outState.putString("blockedMessage", blockedMessage);
    }

    private void restoreState(Bundle savedInstanceState) {
        ArrayList<MovieItem> savedList = (ArrayList<MovieItem>) savedInstanceState.getSerializable(MOVIE_LIST_KEY);
        if (savedList != null && !savedList.isEmpty()) {
            movieList.clear();
            movieList.addAll(savedList);

            offset = savedInstanceState.getInt("offset", 0);
            isLastPage = savedInstanceState.getBoolean("isLastPage", false);
            isLoading = savedInstanceState.getBoolean("isLoading", false);
            isAccountBlocked = savedInstanceState.getBoolean("isAccountBlocked", false);
            blockedMessage = savedInstanceState.getString("blockedMessage", "");

            // Set up adapter with existing data (no shimmer)
            movieListAdapter = new MovieListAdapter(getContext(), movieList);
            // Restore account blocked status to adapter
            movieListAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);
            movieRecyclerView.setAdapter(movieListAdapter);

            // Restore scroll position
            Parcelable savedState = savedInstanceState.getParcelable(SCROLL_POSITION_KEY);
            if (savedState != null && layoutManager != null) {
                layoutManager.onRestoreInstanceState(savedState);
            }

            // If we were loading, continue loading
            if (isLoading) {
                paginationProgressBar.setVisibility(View.VISIBLE);
            }
        } else {
            // No saved data, fetch fresh with shimmer
            ShimmerMovieListAdapter shimmerAdapter = new ShimmerMovieListAdapter(50);
            movieRecyclerView.setAdapter(shimmerAdapter);
            fetchMovies();
        }
    }

    private void setupRecyclerView() {
        int columnCount = calculateColumnCount();
        layoutManager = new GridLayoutManager(getContext(), columnCount);
        movieRecyclerView.setLayoutManager(layoutManager);

        // Only set shimmer if we have no data
        if (movieList.isEmpty()) {
            ShimmerMovieListAdapter shimmerAdapter = new ShimmerMovieListAdapter(10);
            movieRecyclerView.setAdapter(shimmerAdapter);
        } else {
            movieListAdapter = new MovieListAdapter(getContext(), movieList);
            // Pass account blocked status to adapter
            movieListAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);
            movieRecyclerView.setAdapter(movieListAdapter);
        }

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
        DisplayMetrics displayMetrics = new DisplayMetrics();

        if (getActivity() != null) {
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        } else {
            displayMetrics = getResources().getDisplayMetrics();
        }

        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;

        // For phones
        if (config.smallestScreenWidthDp < 600) {
            return config.orientation == Configuration.ORIENTATION_LANDSCAPE ? 5 : 3;
        }
        // For tablets (smallestScreenWidthDp >= 600)
        else {
            if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return 9;
            } else {
                return 6;
            }
        }
    }

    // Override onConfigurationChanged to update layout when orientation changes
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Recalculate column count
        int newColumnCount = calculateColumnCount();

        if (layoutManager != null) {
            // Save scroll position
            Parcelable scrollPosition = layoutManager.onSaveInstanceState();

            // Update layout manager with new column count
            layoutManager = new GridLayoutManager(getContext(), newColumnCount);
            movieRecyclerView.setLayoutManager(layoutManager);

            // Restore scroll position
            if (scrollPosition != null) {
                layoutManager.onRestoreInstanceState(scrollPosition);
            }

            Log.d(TAG, "Orientation changed. New column count: " + newColumnCount);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (layoutManager != null) {
            recyclerViewState = layoutManager.onSaveInstanceState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (layoutManager != null && recyclerViewState != null) {
            layoutManager.onRestoreInstanceState(recyclerViewState);
        }
    }

    private void setupClickListeners() {
        // Back button handled in initializeViews
    }

    private void fetchMovies() {
        if (isLoading) return;

        isLoading = true;

        // Show pagination progress bar only for subsequent loads
        if (offset > 0) {
            paginationProgressBar.setVisibility(View.VISIBLE);
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String accessToken = SharedPrefManager.getInstance(getContext()).getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "No access token found.");
            if (movieRecyclerView.getAdapter() instanceof ShimmerMovieListAdapter) {
                movieListAdapter = new MovieListAdapter(getContext(), movieList);
                movieListAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);
                movieRecyclerView.setAdapter(movieListAdapter);
            }
            paginationProgressBar.setVisibility(View.GONE);
            return;
        }

        Log.d(TAG, "Fetching movies: offset=" + offset + ", count=" + count);

        apiService.getLatestMovieList(accessToken, offset, count).enqueue(new Callback<ResponseBody>() {
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
                                if (movieListAdapter != null) {
                                    movieListAdapter.setAccountBlocked(true, blockedMessage);
                                }


                                // Continue to load movies - don't return
                            } else {
                                // Reset account blocked status if API returns success
                                isAccountBlocked = false;
                                blockedMessage = "";
                                if (movieListAdapter != null) {
                                    movieListAdapter.setAccountBlocked(false, "");
                                }
                            }
                        }

                        // Check if API call was successful
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
                                        movieListAdapter = new MovieListAdapter(getContext(), movieList);
                                        // Pass account blocked status to new adapter
                                        movieListAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);
                                        movieRecyclerView.setAdapter(movieListAdapter);
                                    } else {
                                        // Only notify about the new items for better performance
                                        movieListAdapter.notifyItemRangeInserted(currentSize, movieArray.size());
                                    }

                                    offset++;

                                    if (movieArray.size() < count) {
                                        isLastPage = true;
                                        Log.d(TAG, "Reached last page of movies");
                                    }
                                } else {
                                    isLastPage = true;
                                    Log.d(TAG, "No movies found in response");

                                    // Still replace shimmer with empty adapter
                                    if (movieRecyclerView.getAdapter() instanceof ShimmerMovieListAdapter) {
                                        movieListAdapter = new MovieListAdapter(getContext(), movieList);
                                        // Pass account blocked status to new adapter
                                        movieListAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);
                                        movieRecyclerView.setAdapter(movieListAdapter);
                                        Toast.makeText(getContext(), "No movies available", Toast.LENGTH_SHORT).show();
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
                            if (movieRecyclerView.getAdapter() instanceof ShimmerMovieListAdapter) {
                                movieListAdapter = new MovieListAdapter(getContext(), movieList);
                                // Pass account blocked status to new adapter
                                movieListAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);
                                movieRecyclerView.setAdapter(movieListAdapter);
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "IOException while parsing response", e);
                        Toast.makeText(getContext(), "Error parsing response", Toast.LENGTH_SHORT).show();

                        if (movieRecyclerView.getAdapter() instanceof ShimmerMovieListAdapter) {
                            movieListAdapter = new MovieListAdapter(getContext(), movieList);
                            // Pass account blocked status to new adapter
                            movieListAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);
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
                    movieListAdapter = new MovieListAdapter(getContext(), movieList);
                    // Pass account blocked status to new adapter
                    movieListAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);
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
                    if (movieListAdapter != null) {
                        movieListAdapter.setAccountBlocked(true, blockedMessage);
                    }



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
            movieListAdapter = new MovieListAdapter(getContext(), movieList);
            // Pass account blocked status to new adapter
            movieListAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);
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
                if ("CLOSE_MoreLastesMovieFragment".equals(intent.getAction())) {
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("CLOSE_MoreLastesMovieFragment");
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