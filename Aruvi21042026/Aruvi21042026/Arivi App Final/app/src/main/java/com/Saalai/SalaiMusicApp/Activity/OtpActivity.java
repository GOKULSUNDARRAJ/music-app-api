package com.Saalai.SalaiMusicApp.Activity;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.OtpResponse;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;

import retrofit2.Call;

public class OtpActivity extends AppCompatActivity {

    private EditText[] otpEditTexts;
    private View otpContainer;
    ImageView backimg;
    LinearLayout changenumberimg;
    private ProgressBar progressBar;
    private ProgressBar resendProgressBar;
    private ConstraintLayout btnVerify;
    private boolean isVerifying = false;

    LinearLayout resendotpll;
    private CountDownTimer resendTimer;
    private boolean isResendEnabled = false;
    private static final long RESEND_DELAY_MS = 60000;
    private TextView resendText;
    private TextView tvVerificationMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.black));
        }

        View rootView = findViewById(R.id.root_view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && Build.VERSION.SDK_INT <= 34) {
            getWindow().setDecorFitsSystemWindows(true);

            if (rootView != null) {
                rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                    v.setPadding(0, 0, 0, 0);
                    return insets;
                });
            }
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            if (rootView != null) {
                rootView.setFitsSystemWindows(true);
            }
        }

        backimg = findViewById(R.id.backimg);
        backimg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(OtpActivity.this, SignUpActivity.class));
                finish();
            }
        });
        changenumberimg = findViewById(R.id.changenumberimg);
        changenumberimg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(OtpActivity.this, SignUpActivity.class));
                finish();
            }
        });

        makeStatusBarTransparent();

        // Initialize views
        resendText = findViewById(R.id.resendtxt);
        otpContainer = findViewById(R.id.otpContainer);
        btnVerify = findViewById(R.id.llbtnVerify);

        // Set up colored "Resend" text
        setupResendText(resendText, false);

        // Initialize OTP EditTexts
        otpEditTexts = new EditText[]{
                findViewById(R.id.otp1),
                findViewById(R.id.otp2),
                findViewById(R.id.otp3),
                findViewById(R.id.otp4)
        };

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        resendProgressBar = findViewById(R.id.progressBar);
        resendProgressBar.setVisibility(View.GONE);

        // Set up OTP listeners
        setupOtpListeners();

        // Verify button click listener
        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyOtpManually();
            }
        });

        tvVerificationMessage = findViewById(R.id.tv_verification_message);

        // Get mobile number from SharedPrefManager
        SharedPrefManager sharedPrefManager = SharedPrefManager.getInstance(this);
        String userMobile = sharedPrefManager.getUserMobile();

        // Set text dynamically
        if (userMobile != null && !userMobile.isEmpty()) {
            tvVerificationMessage.setText("We have sent a verification code on +" + userMobile);
        } else {
            tvVerificationMessage.setText("We have sent a verification code on your registered number");
        }

        resendotpll = findViewById(R.id.resendotpll);
        resendotpll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResendEnabled) {
                    resendOtp();
                }
            }
        });

        // Start the resend timer (60 seconds delay)
        startResendTimer();
    }

    private void setupResendText(TextView textView, boolean isEnabled) {
        String fullText;
        if (isEnabled) {
            fullText = "Didn't receive OTP? Resend";
        } else {
            fullText = "Didn't receive OTP? Resend in 60s";
        }

        SpannableString spannable = new SpannableString(fullText);

        int startIndex = fullText.indexOf("Resend");
        int endIndex = startIndex + "Resend".length();

        if (isEnabled) {
            spannable.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(this, R.color.bgred)),
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        } else {
            spannable.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(this, R.color.gray)),
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        textView.setText(spannable);
    }

    private void startResendTimer() {
        isResendEnabled = false;
        resendotpll.setEnabled(false);
        setupResendText(resendText, false);

        if (resendTimer != null) {
            resendTimer.cancel();
        }

        resendTimer = new CountDownTimer(RESEND_DELAY_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                String text = "Resend OTP in : " + secondsRemaining + "s";

                // Create spannable string
                SpannableString spannable = new SpannableString(text);

                // Get colors
                int redColor = ContextCompat.getColor(OtpActivity.this, R.color.bgred);
                int whiteColor = ContextCompat.getColor(OtpActivity.this, R.color.white);

                // First, set entire text to white
                spannable.setSpan(
                        new ForegroundColorSpan(whiteColor),
                        0,
                        text.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                // Then, set only the seconds part to red
                String secondsPart = secondsRemaining + "s";
                int secondsStartIndex = text.indexOf(secondsPart);

                if (secondsStartIndex >= 0) {
                    spannable.setSpan(
                            new ForegroundColorSpan(redColor),
                            secondsStartIndex,
                            secondsStartIndex + secondsPart.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }

                resendText.setText(spannable);
            }

            @Override
            public void onFinish() {
                isResendEnabled = true;
                resendotpll.setEnabled(true);

                // When timer finishes, show "Resend OTP" in red
                String text = "Resend OTP";
                resendText.setText(text);
                resendText.setTextColor(ContextCompat.getColor(OtpActivity.this, R.color.bgred));
            }
        }.start();
    }

    private void resendOtp() {
        resendProgressBar.setVisibility(View.VISIBLE);
        resendotpll.setEnabled(false);

        SharedPrefManager sp = SharedPrefManager.getInstance(this);
        String accessToken = sp.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            resendProgressBar.setVisibility(View.GONE);
            resendotpll.setEnabled(true);
            return;
        }

        Log.d("OtpActivity", "Resending OTP with token: " + accessToken);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<OtpResponse> call = apiService.resendVerificationCode(accessToken);

        call.enqueue(new retrofit2.Callback<OtpResponse>() {
            @Override
            public void onResponse(Call<OtpResponse> call, retrofit2.Response<OtpResponse> response) {
                resendProgressBar.setVisibility(View.GONE);
                resendotpll.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    OtpResponse res = response.body();
                    if (res.isStatus() && "200".equals(res.getError_type())) {
                        Toast.makeText(OtpActivity.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                        clearOtpFields();
                        startResendTimer();

                        String userMobile = sp.getUserMobile();
                        if (userMobile != null && !userMobile.isEmpty()) {
                            tvVerificationMessage.setText("We have sent a NEW verification code on +" + userMobile);
                        }
                    } else {
                        Toast.makeText(OtpActivity.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(OtpActivity.this, "Failed to resend OTP. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OtpResponse> call, Throwable t) {
                resendProgressBar.setVisibility(View.GONE);
                resendotpll.setEnabled(true);
                Toast.makeText(OtpActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("OtpActivity", "Resend OTP failed: " + t.getMessage());
            }
        });
    }

    private void setupOtpListeners() {
        for (int i = 0; i < otpEditTexts.length; i++) {
            final int current = i;
            otpEditTexts[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && current < otpEditTexts.length - 1) {
                        otpEditTexts[current + 1].requestFocus();
                    }
                    updateVerifyButtonState();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            otpEditTexts[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (otpEditTexts[current].getText().toString().isEmpty() && current > 0) {
                        otpEditTexts[current - 1].requestFocus();
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void updateVerifyButtonState() {
        boolean isOtpComplete = true;
        for (EditText editText : otpEditTexts) {
            if (editText.getText().toString().trim().isEmpty()) {
                isOtpComplete = false;
                break;
            }
        }

        if (isOtpComplete) {
            // Enable verify button
            btnVerify.setEnabled(true);
            btnVerify.setAlpha(1.0f);
        } else {
            // Disable verify button
            btnVerify.setEnabled(false);
            btnVerify.setAlpha(0.5f);
        }
    }

    private void verifyOtpManually() {
        // If already verifying, don't start again
        if (isVerifying) {
            return;
        }

        StringBuilder otp = new StringBuilder();
        for (EditText editText : otpEditTexts) {
            otp.append(editText.getText().toString());
        }

        if (otp.length() == otpEditTexts.length) {
            // OTP is complete
            isVerifying = true;

            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(otpContainer.getWindowToken(), 0);

            // Show progress bar
            progressBar.setVisibility(View.VISIBLE);

            // Disable verify button during verification
            btnVerify.setEnabled(false);
            btnVerify.setAlpha(0.5f);

            verifyOtp(otp.toString());
        } else {
            Toast.makeText(this, "Please enter complete OTP", Toast.LENGTH_SHORT).show();
        }
    }

    private void verifyOtp(String otp) {
        SharedPrefManager sp = SharedPrefManager.getInstance(this);
        String accessToken = sp.getAccessToken();

        Log.d("OtpActivity", "Verifying OTP: " + otp);
        Log.d("OtpActivity", "AccessToken: " + accessToken);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<OtpResponse> call = apiService.verifyOtp(accessToken, otp);

        call.enqueue(new retrofit2.Callback<OtpResponse>() {
            @Override
            public void onResponse(Call<OtpResponse> call, retrofit2.Response<OtpResponse> response) {
                progressBar.setVisibility(View.GONE);
                isVerifying = false;
                updateVerifyButtonState(); // Re-enable button if needed

                Log.d("OtpActivity", "Raw Response: " + response.toString());
                if (response.isSuccessful() && response.body() != null) {
                    OtpResponse res = response.body();
                    if (res.isStatus() && "200".equals(res.getError_type())) {
                        Toast.makeText(OtpActivity.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(OtpActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(OtpActivity.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                        clearOtpFields();
                    }
                } else {
                    Toast.makeText(OtpActivity.this, "Server Error", Toast.LENGTH_SHORT).show();
                    clearOtpFields();
                }
            }

            @Override
            public void onFailure(Call<OtpResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                isVerifying = false;
                updateVerifyButtonState();

                Toast.makeText(OtpActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                clearOtpFields();
            }
        });
    }

    private void clearOtpFields() {
        for (EditText editText : otpEditTexts) {
            editText.setText("");
        }
        if (otpEditTexts.length > 0) {
            otpEditTexts[0].requestFocus();
        }
        updateVerifyButtonState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }

    private void makeStatusBarTransparent() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }
}