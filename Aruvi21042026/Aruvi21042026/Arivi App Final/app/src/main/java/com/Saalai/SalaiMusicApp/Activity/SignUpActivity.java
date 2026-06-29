package com.Saalai.SalaiMusicApp.Activity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.Saalai.SalaiMusicApp.Adapters.CountryDialogAdapter;

import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.BottomNavItem;
import com.Saalai.SalaiMusicApp.Models.NavigationDataManager;
import com.Saalai.SalaiMusicApp.Models.NavigationResponse;
import com.Saalai.SalaiMusicApp.Models.TopNavItem;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.RegisterResponse;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    ConstraintLayout llsignupbtn;
    private TextView countryCodeTextView;
    private TextView countryNameTextView;
    private String selectedCountry = "United Kingdom";
    private String countryCode = "+235";
    private String countryCodeKey = "228";
    CheckBox myCheckbox;
    private ProgressBar progressBar,progressBarDialog;
    TextView termsText;

    LinearLayout btnlayout,cc_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

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

        // Initialize views
        progressBar = findViewById(R.id.progressBar);
        progressBarDialog = findViewById(R.id.progressBarDialog );
        btnlayout=findViewById(R.id.btnlayout);
        countryCodeTextView = findViewById(R.id.countryCodeTextView); // Add this TextView in your XML
        countryNameTextView = findViewById(R.id.countryNameTextView); // Add this TextView in your XML
        myCheckbox = findViewById(R.id.myCheckbox);
        cc_layout= findViewById(R.id.cc_layout);

        // Set click listeners for country selection
        countryCodeTextView.setOnClickListener(this);
        countryNameTextView.setOnClickListener(this);
        cc_layout.setOnClickListener(this);

        termsText=findViewById(R.id.termsText);
        termsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(v.getContext(),TermsActivity.class));
            }
        });


        // Initialize terms text
        TextView termsText = findViewById(R.id.termsText);
        String fullText = "By entering your number, you are agreeing to our Terms and Conditions and Privacy Policy";

        SpannableString spannableString = new SpannableString(fullText);

        int termsStart = fullText.indexOf("Terms and Conditions");
        int termsEnd = termsStart + "Terms and Conditions".length();
        int privacyStart = fullText.indexOf("Privacy Policy");
        int privacyEnd = privacyStart + "Privacy Policy".length();

        int yellowColor = ContextCompat.getColor(this, R.color.yellow);

        spannableString.setSpan(
                new ForegroundColorSpan(yellowColor),
                termsStart,
                termsEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        spannableString.setSpan(
                new ForegroundColorSpan(yellowColor),
                privacyStart,
                privacyEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        termsText.setText(spannableString);

        llsignupbtn = findViewById(R.id.llsignup);
        llsignupbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                String name = ((EditText) findViewById(R.id.ipedt)).getText().toString().trim();
                String userMobile = ((EditText) findViewById(R.id.postedt)).getText().toString().trim();
                String referalCode = ((EditText) findViewById(R.id.passwordedt)).getText().toString().trim();
                String deviceID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

                if (name.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "Please enter your name", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (userMobile.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "Please enter your mobile number", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!myCheckbox.isChecked()) {
                    Toast.makeText(SignUpActivity.this, "Please agree to Terms & Privacy Policy", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                btnlayout.setVisibility(View.INVISIBLE);

                ApiService apiService = ApiClient.getClient().create(ApiService.class);

                Call<RegisterResponse> call = apiService.checkRegister(
                        "password",           // grant_type
                        "saalai_app",         // client_id
                        countryCodeKey,                // userCountry
                        userMobile,           // userMobile
                        "a43c5951b27652d123",             // deviceID
                        "A",                  // mobileType
                        "your_device_token_here", // replace with real FCM token
                        name,                 // name
                        referalCode           // referalCode
                );
                Log.d("REGISTER_REQUEST", "====================");
                Log.d("REGISTER_REQUEST", "grant_type: password");
                Log.d("REGISTER_REQUEST", "client_id: saalai_app");
                Log.d("REGISTER_REQUEST", "userCountry (Dial Code): " + countryCode);
                Log.d("REGISTER_REQUEST", "userMobile: " + userMobile);
                Log.d("REGISTER_REQUEST", "deviceID: " + "a43c5951b27652d123");
                Log.d("REGISTER_REQUEST", "mobileType: A");
                Log.d("REGISTER_REQUEST", "deviceToken: " + "your_device_token_here");
                Log.d("REGISTER_REQUEST", "name: " + name);
                Log.d("REGISTER_REQUEST", "referalCode: " + referalCode);
                Log.d("REGISTER_REQUEST", "====================");
                call.enqueue(new retrofit2.Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            RegisterResponse res = response.body();

                            progressBar.setVisibility(View.INVISIBLE);
                            btnlayout.setVisibility(View.VISIBLE);

                            if (res.isStatus()) {
                                //  Access message
                                String message = res.getMessage();

                                RegisterResponse.ResponseData data = res.getResponse();

                                String accessToken = data.getAccess_token();
                                String refreshToken = data.getRefresh_token();
                                String tokenType = data.getToken_type();
                                long expiresIn = data.getExpires_in();

                                // Show in logs (for testing)
                                Log.d("API_SUCCESS", "Message: " + message);
                                Log.d("API_SUCCESS", "Status: " + res.isStatus());
                                Log.d("API_SUCCESS", "error: " + res.getError_type());
                                Log.d("API_SUCCESS", "User ID: " + data.getUserId());
                                Log.d("API_SUCCESS", "User Name: " + name);
                                Log.d("API_SUCCESS", "User Email: " + userMobile);
                                Log.d("API_SUCCESS", "User Mobile: " + data.getUserMobile());
                                Log.d("API_SUCCESS", "User Country: " + data.getUserCountry());
                                Log.d("API_SUCCESS", "User Country ID: " + data.getUserCountryId());
                                Log.d("API_SUCCESS", "User Created Date: " + data.getUserCreatedDate());
                                Log.d("API_SUCCESS", "User Country Code: " + data.getUserCountryCode());
                                Log.d("API_SUCCESS", "Token Type: " + tokenType);
                                Log.d("API_SUCCESS", "Expires In: " + expiresIn);
                                Log.d("API_SUCCESS", "Access Token: " + accessToken);
                                Log.d("API_SUCCESS", "Refresh Token: " + refreshToken);

                                SharedPrefManager.getInstance(SignUpActivity.this).saveUser(
                                        data.getUserId(),
                                        name,
                                        data.getUserEmail(),
                                        userMobile,
                                        data.getUserCountry(),
                                        data.getUserCountryId(),
                                        data.getUserCreatedDate(),
                                        data.getUserCountryCode(),
                                        data.getAccess_token(),
                                        data.getRefresh_token(),
                                        data.getToken_type(),
                                        data.getExpires_in()
                                );

                                if (res.getError_type().equals("200")){
                                    loadNavigationDataAfterSignup(data.getAccess_token());

                                }else {
                                    Intent intent = new Intent(SignUpActivity.this, OtpActivity.class);
                                    startActivity(intent);
                                    finish();
                                }


                            } else {
                                Toast.makeText(SignUpActivity.this, res.getMessage(), Toast.LENGTH_LONG).show();
                                progressBar.setVisibility(View.INVISIBLE);
                                btnlayout.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Toast.makeText(SignUpActivity.this, "Server error", Toast.LENGTH_SHORT).show();

                            progressBar.setVisibility(View.INVISIBLE);
                            btnlayout.setVisibility(View.VISIBLE);
                        }
                    }


                    @Override
                    public void onFailure(Call<RegisterResponse> call, Throwable t) {
                        Toast.makeText(SignUpActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        Log.d("API_Fail","Network error: " + t.getMessage());
                        progressBar.setVisibility(View.INVISIBLE);
                        btnlayout.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

    }

    private void loadNavigationDataAfterSignup(String accessToken) {
        progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<NavigationResponse> call = apiService.getNavigationMenu(accessToken);

        call.enqueue(new Callback<NavigationResponse>() {
            @Override
            public void onResponse(Call<NavigationResponse> call, Response<NavigationResponse> response) {
                progressBar.setVisibility(View.INVISIBLE);

                if (response.isSuccessful() && response.body() != null) {
                    NavigationResponse navResponse = response.body();

                    if (navResponse.isStatus()) {
                        // Filter active items
                        List<BottomNavItem> activeBottomItems = navResponse.getBottomMenu() != null ?
                                navResponse.getBottomMenu().stream()
                                        .filter(BottomNavItem::isActive)
                                        .collect(Collectors.toList()) : new ArrayList<>();

                        List<TopNavItem> activeTopItems = navResponse.getTopMenu() != null ?
                                navResponse.getTopMenu().stream()
                                        .filter(item -> "Active".equals(item.getTopmenuStatus()) || item.isActive())
                                        .collect(Collectors.toList()) : new ArrayList<>();

                        // Save navigation data
                        NavigationDataManager.getInstance(SignUpActivity.this)
                                .saveNavigation(activeBottomItems, activeTopItems);

                        Log.d("SignUp", "Navigation loaded successfully");
                    }
                }

                // Navigate to MainActivity regardless of navigation API result
                Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<NavigationResponse> call, Throwable t) {
                progressBar.setVisibility(View.INVISIBLE);
                Log.e("SignUp", "Failed to load navigation", t);

                // Still navigate to MainActivity (navigation data is optional)
                Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }



    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.countryCodeTextView || v.getId() == R.id.countryNameTextView) {
            showCountryDialog();
        }
    }

    private void showCountryDialog() {
        // Show activity progress bar
        progressBarDialog .setVisibility(View.VISIBLE);


        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.getCountryList();

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBarDialog .setVisibility(View.GONE);


                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);

                        if (jsonObject.optString("result").equalsIgnoreCase("true")) {
                            JSONArray iptvArray = jsonObject.optJSONArray("iptv");

                            // Create dialog only after data loaded
                            final Dialog dialog = new Dialog(SignUpActivity.this);
                            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            dialog.setContentView(R.layout.dialog_country);

                            EditText searchText = dialog.findViewById(R.id.country_dialog_search_text);
                            ListView listView = dialog.findViewById(R.id.country_dialog_list_view);

                            CountryDialogAdapter adapter = new CountryDialogAdapter(
                                    SignUpActivity.this,
                                    iptvArray,
                                    (position, jsonArray) -> {
                                        try {
                                            JSONObject obj = jsonArray.optJSONObject(position);
                                            selectedCountry = obj.optString("CountryName");
                                            countryCodeKey = obj.optString("CountryId");

                                            countryCodeTextView.setText("+"+countryCodeKey);
                                            countryNameTextView.setText(selectedCountry);

                                            dialog.dismiss();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });

                            listView.setAdapter(adapter);

                            // Enable live search
                            searchText.addTextChangedListener(new TextWatcher() {
                                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    adapter.getFilter().filter(s.toString());
                                }
                                @Override public void afterTextChanged(Editable s) {}
                            });

                            dialog.show();

                        } else {
                            Toast.makeText(SignUpActivity.this, "No countries found", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(SignUpActivity.this, "Parsing error", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(SignUpActivity.this, "Failed to load countries", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBarDialog .setVisibility(View.GONE);

                Toast.makeText(SignUpActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}