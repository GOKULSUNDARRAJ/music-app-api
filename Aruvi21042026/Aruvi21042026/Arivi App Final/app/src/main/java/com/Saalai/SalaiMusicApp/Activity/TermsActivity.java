package com.Saalai.SalaiMusicApp.Activity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TermsActivity extends AppCompatActivity {

    private TextView tvHelpTitle, tvHelpContent;
    private ShimmerFrameLayout shimmerFrameLayout;
    ImageView backimg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);


        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.black)); // replace with your color
        }

        View rootView = findViewById(R.id.root_view);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && Build.VERSION.SDK_INT <= 34) {
            // For Android 11 to 14, explicitly disable edge-to-edge
            getWindow().setDecorFitsSystemWindows(true);

            if (rootView != null) {
                rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                    v.setPadding(0, 0, 0, 0);
                    return insets;
                });
            }
        } else {
            // Pre-Android 12
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            if (rootView != null) {
                rootView.setFitsSystemWindows(true); // critical for proper layout
            }
        }

        tvHelpTitle = findViewById(R.id.tvHelpTitle);
        tvHelpContent = findViewById(R.id.tvHelpContent);
        shimmerFrameLayout = findViewById(R.id.shimmer_view_container);
        backimg = findViewById(R.id.backimg);

        backimg.setOnClickListener(v -> {
            onBackPressed();
        });
        getHelpData();
    }
    private void getHelpData() {
        shimmerFrameLayout.startShimmer();
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        tvHelpContent.setVisibility(View.GONE);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.getTermsAndConditions();

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
                tvHelpContent.setVisibility(View.VISIBLE);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        Log.d("TermsAndConditions", jsonString);

                        JSONObject jsonObject = new JSONObject(jsonString);
                        JSONObject responseObject = jsonObject.getJSONObject("response");

                        String title = responseObject.getString("title");
                        String helpText = responseObject.getString("termsAndConditions");

                        tvHelpTitle.setText(title);
                        tvHelpContent.setText(helpText.replace("\\r\\n", "\n"));

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(TermsActivity.this, "Parse error", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(TermsActivity.this, "Failed to load termsAndConditions info", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
                tvHelpContent.setVisibility(View.VISIBLE);

                Log.e("termsAndConditionsError", "API call failed", t);
                Toast.makeText(TermsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }


}