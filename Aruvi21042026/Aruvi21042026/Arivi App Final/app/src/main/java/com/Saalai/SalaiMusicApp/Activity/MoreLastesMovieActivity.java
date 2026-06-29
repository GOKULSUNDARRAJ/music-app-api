package com.Saalai.SalaiMusicApp.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.MovieListAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.MovieItem;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;

import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerMovieListAdapter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class MoreLastesMovieActivity extends AppCompatActivity {

    private static final String TAG = "MoreLastesMovieActivity";


    private int offset = 0;
    private final int count = 15;

    private boolean isLoading = false;
    private boolean isLastPage = false;

    private RecyclerView movieRecyclerView;
    private MovieListAdapter movieListAdapter;
    private final List<MovieItem> movieList = new ArrayList<>();


    private BroadcastReceiver closeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Ensures content does NOT draw behind system bars
            getWindow().setDecorFitsSystemWindows(true);
        } else {
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS |
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
            );
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }


        setContentView(R.layout.activity_more_lastes_movie);

        View rootView = findViewById(R.id.root_view); // your root layout ID
        if (rootView != null) {
            rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                v.setPadding(
                        insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom()
                );
                return insets.consumeSystemWindowInsets();
            });
        }






        movieRecyclerView = findViewById(R.id.movieRecyclerView);
        movieRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        movieListAdapter = new MovieListAdapter(this, movieList);
        movieRecyclerView.setAdapter(movieListAdapter);

        ImageView back = findViewById(R.id.backimg);
        back.setOnClickListener(v -> onBackPressed());

        // Endless scroll
        movieRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
                        Log.d(TAG, "Bottom reached → load next page. Offset=" + offset);
                        fetchMovies();
                    }
                }
            }
        });

        ShimmerMovieListAdapter shimmerAdapter = new ShimmerMovieListAdapter(30);
        movieRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));// 6 placeholder items
        movieRecyclerView.setAdapter(shimmerAdapter);

        // First load
        fetchMovies();
        setupCloseReceiver();

    }

    private void fetchMovies() {
        isLoading = true;

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String accessToken = SharedPrefManager.getInstance(this).getAccessToken();

        Log.d(TAG, "📡 Fetching movies: categoryId=" + ", offset=" + offset + ", count=" + count);

        apiService.getLatestMovieList(accessToken, offset, count).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                isLoading = false;

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String rawJson = response.body().string();
                        Log.d(TAG, "✅ API Response: " + rawJson);

                        JsonObject jsonObject = JsonParser.parseString(rawJson).getAsJsonObject();
                        JsonArray movieArray = jsonObject.getAsJsonArray("movieList");

                        if (movieArray != null && movieArray.size() > 0) {
                            for (int i = 0; i < movieArray.size(); i++) {
                                JsonObject obj = movieArray.get(i).getAsJsonObject();
                                int id = obj.get("channelId").getAsInt();
                                String name = obj.get("channelName").getAsString();
                                String logo = obj.get("channelLogo").getAsString();
                                movieList.add(new MovieItem(id, name, logo));
                            }

                            // ✅ Replace shimmer with real adapter
                            if (movieRecyclerView.getAdapter() instanceof ShimmerMovieListAdapter) {
                                movieListAdapter = new MovieListAdapter(MoreLastesMovieActivity.this, movieList);
                                movieRecyclerView.setAdapter(movieListAdapter);
                            } else {
                                movieListAdapter.notifyDataSetChanged();
                            }

                            offset++;

                            if (movieArray.size() < count) {
                                isLastPage = true;
                                Log.d(TAG, "⚠️ Reached last page of movies");
                            }
                        } else {
                            isLastPage = true;
                            Log.d(TAG, "⚠️ No movies found in response");
                        }


                    } catch (IOException e) {
                        Log.e(TAG, "❌ IOException while parsing response", e);
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorStr);
                            if (jsonObject.has("error") && "access_denied".equals(jsonObject.getString("error"))) {
                                Toast.makeText(MoreLastesMovieActivity.this, "Access Denied", Toast.LENGTH_SHORT).show();
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
                isLoading = false;
                Log.e(TAG, "❌ API call failed", t);
            }
        });
    }

    private void callLogoutApi() {
        SharedPrefManager sp = SharedPrefManager.getInstance(MoreLastesMovieActivity.this);
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

                    Intent intent = new Intent(MoreLastesMovieActivity.this, SignUpActivity.class);
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
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }


    private void setupCloseReceiver() {
        closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("CLOSE_MoreLastesMovieActivity".equals(intent.getAction())) {
                    finish();
                }
            }
        };

        IntentFilter filter = new IntentFilter("CLOSE_MoreLastesMovieActivity");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            registerReceiver(closeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(closeReceiver, filter);
        }
    }
}

