package com.Saalai.SalaiMusicApp;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.ContactUsAdapter;
import com.Saalai.SalaiMusicApp.Adapters.SocialContactAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Response.ContactUsResponse;
import com.Saalai.SalaiMusicApp.Response.EnquiryResponse;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubscriptionBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "ContactUsBottomSheet";
    private ImageView btnClose;
    private TextView tvTitle, tvDescription;
    private RecyclerView recyclerViewContacts, recyclerViewSocial;
    private ProgressBar progressBar, enquireProgressBar; // Add enquireProgressBar
    private NestedScrollView scrollView;
    private CardView enquireCard;
    private ImageView ivSocialIcon; // Add this
    private TextView tvPhoneNumber; // Add this

    private ContactUsAdapter contactAdapter;
    private SocialContactAdapter socialAdapter;
    private List<ContactUsResponse.SupportContact> contactList = new ArrayList<>();
    private List<ContactUsResponse.SocialContact> socialList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_subscription, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerViews();
        fetchContactUsData();
    }


    private void initViews(View view) {
        btnClose = view.findViewById(R.id.btnClose);
        tvTitle = view.findViewById(R.id.textView5);
        tvDescription = view.findViewById(R.id.textView3);
        recyclerViewContacts = view.findViewById(R.id.recyclerViewContacts);
        recyclerViewSocial = view.findViewById(R.id.recyclerViewSocial);
        progressBar = view.findViewById(R.id.progressBar);
        scrollView = view.findViewById(R.id.scrollView);
        enquireCard = view.findViewById(R.id.enquirecard);

        // Initialize enquire progress bar and other views
        enquireProgressBar = view.findViewById(R.id.enquireprogressBar);
        ivSocialIcon = view.findViewById(R.id.ivSocialIcon);
        tvPhoneNumber = view.findViewById(R.id.tvPhoneNumber);

        btnClose.setOnClickListener(v -> dismiss());

        // Set click listener for enquiry card - direct API call
        enquireCard.setOnClickListener(v -> {
            Log.d(TAG, "Enquiry card clicked - calling API directly");
            callEnquiryApi();
        });

        // Initially show progress, hide content
        progressBar.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.GONE);
    }

    private void callEnquiryApi() {
        // Disable card to prevent multiple clicks
        enquireCard.setEnabled(false);

        // Show progress bar, hide icon and text
        enquireProgressBar.setVisibility(View.VISIBLE);
        ivSocialIcon.setVisibility(View.GONE);
        tvPhoneNumber.setVisibility(View.GONE);

        SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
        String accessToken = sp.getAccessToken();

        Log.d(TAG, "Calling enquiry API with token: " + accessToken);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<EnquiryResponse> call = apiService.sendEnquiry(accessToken);

        call.enqueue(new Callback<EnquiryResponse>() {
            @Override
            public void onResponse(Call<EnquiryResponse> call, Response<EnquiryResponse> response) {
                // Reset UI state
                resetEnquiryUI();

                if (response.isSuccessful() && response.body() != null) {
                    EnquiryResponse enquiryResponse = response.body();
                    Log.d(TAG, "Enquiry API Response: " + enquiryResponse.getMessage());

                    if (enquiryResponse.isStatus()) {
                        // Success - show custom alert
                        showCustomAlert(enquiryResponse.getMessage(), true);
                        dismiss();
                    } else {
                        // API returned error status
                        showCustomAlert("Failed: " + enquiryResponse.getMessage(), false);
                        dismiss();
                    }
                } else {
                    // HTTP error
                    String errorMsg = "Failed to send enquiry";
                    if (response.code() == 401) {
                        errorMsg = "Authentication failed. Please login again.";
                    } else if (response.code() == 500) {
                        errorMsg = "Server error. Please try again later.";
                    }

                    Log.e(TAG, "Enquiry API HTTP Error: " + response.code());
                    showCustomAlert(errorMsg, false);
                }
            }

            @Override
            public void onFailure(Call<EnquiryResponse> call, Throwable t) {
                // Reset UI state
                resetEnquiryUI();

                Log.e(TAG, "Enquiry API Network Error: " + t.getMessage());
                showCustomAlert("Network error. Please check your connection.", false);
            }
        });
    }

    private void resetEnquiryUI() {
        // Re-enable card
        enquireCard.setEnabled(true);

        // Hide progress bar, show icon and text
        enquireProgressBar.setVisibility(View.GONE);
        ivSocialIcon.setVisibility(View.VISIBLE);
        tvPhoneNumber.setVisibility(View.VISIBLE);
    }

    private void showCustomAlert(String message, boolean isSuccess) {
        // Create dialog
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_simple_alert);

        // Get references
        TextView messageText = dialog.findViewById(R.id.message);
        Button btnOk = dialog.findViewById(R.id.btnOk);

        // Set message
        messageText.setText(message);

        // Set button click
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            if (isSuccess) {
                // Close bottom sheet only on success
                dismiss();
            }
        });

        // Set dialog properties
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setGravity(Gravity.CENTER);

        dialog.show();
    }

    // Alternative: Material Design Alert (if you don't want custom layout)
    private void showMaterialAlert(String message, boolean isSuccess) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle(isSuccess ? "Success" : "Alert");
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            if (isSuccess) {
                dismiss();
            }
        });
        builder.setCancelable(false);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Customize button color
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow1));
    }

    private void setupRecyclerViews() {
        // Setup phone contacts RecyclerView
        contactAdapter = new ContactUsAdapter(requireContext(), contactList);
        recyclerViewContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewContacts.setAdapter(contactAdapter);

        // Setup social contacts RecyclerView (horizontal)
        socialAdapter = new SocialContactAdapter(requireContext(), socialList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.VERTICAL, false);
        recyclerViewSocial.setLayoutManager(layoutManager);
        recyclerViewSocial.setAdapter(socialAdapter);
    }

    private void fetchContactUsData() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ContactUsResponse> call = apiService.getContactUs();

        call.enqueue(new Callback<ContactUsResponse>() {
            @Override
            public void onResponse(Call<ContactUsResponse> call, Response<ContactUsResponse> response) {
                progressBar.setVisibility(View.GONE);
                scrollView.setVisibility(View.VISIBLE);

                if (response.isSuccessful() && response.body() != null) {
                    ContactUsResponse contactResponse = response.body();
                    ContactUsResponse.ResponseData responseData = contactResponse.getResponse();

                    if (responseData != null) {
                        updateUI(responseData);
                    } else {
                        showDefaultData();
                    }
                } else {
                    Log.e(TAG, "API Error: " + response.code());
                    showDefaultData();
                }
            }

            @Override
            public void onFailure(Call<ContactUsResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                scrollView.setVisibility(View.VISIBLE);
                Log.e(TAG, "Network Error: " + t.getMessage());
                showDefaultData();
            }
        });
    }

    private void updateUI(ContactUsResponse.ResponseData responseData) {
        // Set title and description
        if (responseData.getTitle() != null && !responseData.getTitle().isEmpty()) {
            tvTitle.setText(responseData.getTitle());
        }
        if (responseData.getDescription() != null && !responseData.getDescription().isEmpty()) {
            tvDescription.setText(responseData.getDescription());
        }

        // Update phone contacts
        if (responseData.getContactList() != null && !responseData.getContactList().isEmpty()) {
            contactList.clear();
            contactList.addAll(responseData.getContactList());
            contactAdapter.notifyDataSetChanged();
            recyclerViewContacts.setVisibility(View.VISIBLE);
        } else {
            showDefaultContacts();
        }

        // Update social contacts
        if (responseData.getSocialList() != null && !responseData.getSocialList().isEmpty()) {
            socialList.clear();
            socialList.addAll(responseData.getSocialList());
            socialAdapter.notifyDataSetChanged();
            recyclerViewSocial.setVisibility(View.VISIBLE);
        } else {
            recyclerViewSocial.setVisibility(View.GONE);
        }
    }

    private void showDefaultData() {
        tvTitle.setText("No Subscription?");
        tvDescription.setText("Please contact our support team.");
        showDefaultContacts();
        recyclerViewSocial.setVisibility(View.GONE);
    }

    private void showDefaultContacts() {
        // Add default contact
        ContactUsResponse.SupportContact defaultContact = new ContactUsResponse.SupportContact();
        contactList.clear();
        contactList.add(defaultContact);
        contactAdapter.notifyDataSetChanged();
        recyclerViewContacts.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setDraggable(true);

                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.setLayoutParams(params);
            }

            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dlg -> {
            BottomSheetDialog d = (BottomSheetDialog) dlg;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setDraggable(true);
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        });

        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        return dialog;
    }

    @Override
    public int getTheme() {
        return R.style.FullExpandedBottomSheet;
    }
}