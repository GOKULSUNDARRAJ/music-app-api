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

import com.Saalai.SalaiMusicApp.Adapters.TvShowAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.TvShow;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerTvListAdapter;
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

public class MoreLatestTvShowListActivity extends AppCompatActivity {

    private static final String TAG = "MoreLatestTvShowListActivity";

    private RecyclerView recyclerView;
    private TvShowAdapter adapter;
    private final List<TvShow> tvShowList = new ArrayList<>();


    private int offset = 0;   // ✅ same as MovieActivity
    private final int limit = 15;

    private boolean isLoading = false;
    private boolean isLastPage = false;
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
        setContentView(R.layout.activity_more_latest_tv_show_list); // reuse same layout

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

        recyclerView = findViewById(R.id.movieRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new TvShowAdapter(this, tvShowList);

        // Back button
        ImageView back = findViewById(R.id.backimg);
        back.setOnClickListener(v -> finish());



        // Set shimmer first
        ShimmerTvListAdapter shimmerAdapter = new ShimmerTvListAdapter(12);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(shimmerAdapter);

        // Endless scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);

                GridLayoutManager layoutManager = (GridLayoutManager) rv.getLayoutManager();
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

        // First load
        fetchTvShows();
        setupCloseReceiver();

    }

    private void fetchTvShows() {
        isLoading = true;

        String accessToken = SharedPrefManager.getInstance(this).getAccessToken();
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Log.d(TAG, "📡 Fetching TV shows: categoryId=" +  ", offset=" + offset + ", limit=" + limit);

        apiService.getLatestTvShowList(accessToken, offset, limit).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                isLoading = false;

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String rawJson = response.body().string();
                        Log.d(TAG, "✅ API Response: " + rawJson);

                        JsonObject jsonObject = JsonParser.parseString(rawJson).getAsJsonObject();
                        JsonArray array = jsonObject.getAsJsonArray("tvShowList");

                        if (array != null && array.size() > 0) {
                            for (int i = 0; i < array.size(); i++) {
                                JsonObject obj = array.get(i).getAsJsonObject();
                                TvShow show = new TvShow();
                                show.setChannelId(obj.get("channelId").getAsInt());
                                show.setChannelName(obj.get("channelName").getAsString());
                                show.setChannelLogo(obj.get("channelLogo").getAsString());
                                tvShowList.add(show);
                            }

                            // ✅ Replace shimmer with real adapter on first load
                            if (recyclerView.getAdapter() instanceof ShimmerTvListAdapter) {
                                recyclerView.setAdapter(adapter);
                            } else {
                                adapter.notifyDataSetChanged();
                            }

                            offset++;

                            if (array.size() < limit) {
                                isLastPage = true;
                                Log.d(TAG, "⚠️ Last page reached for TV shows");
                            }
                        } else {
                            isLastPage = true;
                            Log.d(TAG, "⚠️ No TV shows found");
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "❌ IOException parsing TV show response", e);
                    }
                }else {
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorStr);
                            if (jsonObject.has("error") && "access_denied".equals(jsonObject.getString("error"))) {
                                Toast.makeText(MoreLatestTvShowListActivity.this, "Access Denied", Toast.LENGTH_SHORT).show();
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
        SharedPrefManager sp = SharedPrefManager.getInstance(MoreLatestTvShowListActivity.this);
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

                    Intent intent = new Intent(MoreLatestTvShowListActivity.this, SignUpActivity.class);
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
                if ("CLOSE_MoreLatestTvShowListActivity".equals(intent.getAction())) {
                    finish();
                }
            }
        };

        IntentFilter filter = new IntentFilter("CLOSE_MoreLatestTvShowListActivity");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            registerReceiver(closeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(closeReceiver, filter);
        }
    }
}
