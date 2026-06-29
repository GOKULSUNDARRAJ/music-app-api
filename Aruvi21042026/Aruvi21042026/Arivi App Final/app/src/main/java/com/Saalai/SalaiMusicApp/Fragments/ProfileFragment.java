package com.Saalai.SalaiMusicApp.Fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import com.Saalai.SalaiMusicApp.Activity.HelpandSupportActivity;
import com.Saalai.SalaiMusicApp.Activity.MainActivity;
import com.Saalai.SalaiMusicApp.Activity.SignUpActivity;
import com.Saalai.SalaiMusicApp.Activity.TermsActivity;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private SharedPrefManager sharedPrefManager;
    private ConstraintLayout cardView;
    private LinearLayout btnHelp, termsll;
    private ImageView closell;
    private LinearLayout lllogout;
    private TextView tvUserName, tvUserMobile, tvVersionName,profilrtext;
    private LinearLayout btnLayout;
    private ProgressBar progressBar;

    private Dialog logoutDialog;
    private Button btnOk, btnCancel;
    private ProgressBar dialogProgressBar;
    private TextView dialogMessage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sharedPrefManager = SharedPrefManager.getInstance(requireContext());

        // Initialize views
        initViews(view);

        // Set user data
        setUserData();

        // Set version info
        setVersionInfo();

        // Setup click listeners
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        cardView = view.findViewById(R.id.cardView);
        btnHelp = view.findViewById(R.id.helpandsupportll);
        termsll = view.findViewById(R.id.termsll);
        closell = view.findViewById(R.id.close);
        lllogout = view.findViewById(R.id.lllogout);
        tvUserName = view.findViewById(R.id.username);
        tvUserMobile = view.findViewById(R.id.usermobile);
        tvVersionName = view.findViewById(R.id.versionname);
        btnLayout = view.findViewById(R.id.btnlayout);
        progressBar = view.findViewById(R.id.progressBar);
        profilrtext=view.findViewById(R.id.iv_user_info);
    }

    private void setUserData() {
        String userName = sharedPrefManager.getUserName();
        String userMobile = sharedPrefManager.getUserMobile();

        if (tvUserName != null) {
            String name = userName != null ? userName : "User Name";

            if (!name.isEmpty()) {
                name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            }

            tvUserName.setText(name);
        }


        if (tvUserMobile != null) {
            tvUserMobile.setText(userMobile != null ? userMobile : "Mobile Number");
        }

        if (profilrtext != null) {
            profilrtext .setText(userName != null ? userName : "User Name");
        }

    }

    private void setVersionInfo() {
        try {
            String versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0)
                    .versionName;

            if (tvVersionName != null) {
                tvVersionName.setText("Version: " + versionName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (tvVersionName != null) {
                tvVersionName.setText("Version: N/A");
            }
        }
    }

    private void setupClickListeners() {
        // CardView click
        if (cardView != null) {
            cardView.setOnClickListener(v -> {
                // Handle profile card click
                // Could open profile edit or details activity
            });
        }

        // Help & Support
        if (btnHelp != null) {
            btnHelp.setOnClickListener(v -> {
                startActivity(new Intent(requireActivity(), HelpandSupportActivity.class));
            });
        }

        // Terms & Conditions
        if (termsll != null) {
            termsll.setOnClickListener(v -> {
                startActivity(new Intent(requireActivity(), TermsActivity.class));
            });
        }

        // Close button (if needed, though in fragment this might close drawer)
        if (closell != null) {
            closell.setOnClickListener(v -> {
                // If you want to close drawer from fragment
                if (getActivity() instanceof MainActivity) {
                    MainActivity activity = (MainActivity) getActivity();
                    activity.onToggleDrawer();
                }
            });
        }

        // Logout
        if (lllogout != null) {
            lllogout.setOnClickListener(v -> {
                showCustomAlert("Do you want to logout ?");
            });
        }
    }


    private void showCustomAlert(String message) {
        // Create dialog
        logoutDialog = new Dialog(requireContext());
        logoutDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        logoutDialog.setContentView(R.layout.logoutdialog);

        // Get references
        dialogMessage = logoutDialog.findViewById(R.id.message);
        btnOk = logoutDialog.findViewById(R.id.btnOk);
        btnCancel = logoutDialog.findViewById(R.id.btnCancel);
        dialogProgressBar = logoutDialog.findViewById(R.id.progressBar);

        // Set message
        dialogMessage.setText(message);

        // Set button click listeners
        btnOk.setOnClickListener(v -> {
            // Disable buttons and show progress bar
            btnOk.setEnabled(false);
            btnCancel.setEnabled(false);
            dialogProgressBar.setVisibility(View.VISIBLE);
            dialogMessage.setText("Logging out...");

            // Call logout API
            callLogoutApi();
        });

        btnCancel.setOnClickListener(v -> {
            logoutDialog.dismiss();
        });

        // Set dialog properties
        logoutDialog.setCancelable(false);
        logoutDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        logoutDialog.getWindow().setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        logoutDialog.getWindow().setGravity(Gravity.CENTER);

        logoutDialog.show();
    }

    private void callLogoutApi() {
        String accessToken = sharedPrefManager.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            if (logoutDialog != null && logoutDialog.isShowing()) {
                logoutDialog.dismiss();
            }
            Toast.makeText(requireContext(), "User already logged out", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.logout(accessToken);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (logoutDialog != null && logoutDialog.isShowing()) {
                    // Hide progress bar and enable buttons
                    dialogProgressBar.setVisibility(View.GONE);
                    btnOk.setEnabled(true);
                    btnCancel.setEnabled(true);

                    if (response.isSuccessful()) {
                        // Clear shared preferences
                        sharedPrefManager.clearAccessToken();

                        // Close dialog
                        logoutDialog.dismiss();

                        // Navigate to login/signup
                        Intent intent = new Intent(requireActivity(), SignUpActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    } else {
                        // Reset dialog state
                        dialogMessage.setText("Do you want to logout ?");
                        Toast.makeText(requireContext(), "Logout failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (logoutDialog != null && logoutDialog.isShowing()) {
                    // Hide progress bar and enable buttons
                    dialogProgressBar.setVisibility(View.GONE);
                    btnOk.setEnabled(true);
                    btnCancel.setEnabled(true);

                    // Reset dialog message
                    dialogMessage.setText("Do you want to logout ?");
                    Toast.makeText(requireContext(), "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        // Dismiss dialog if fragment is being destroyed
        if (logoutDialog != null && logoutDialog.isShowing()) {
            logoutDialog.dismiss();
        }
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user data when fragment resumes
        setUserData();
    }
}