package com.Saalai.SalaiMusicApp;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

public class DownloadPlaylistBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "DownloadPlaylistBottomSheet";

    private String artistName;
    private String artistImageUrl;
    private int songCount;
    private ArrayList<String> songNames;
    private ArrayList<String> songUrls;

    // UI Elements
    private EditText etPlaylistName;
    private TextView   tvCancel, tvDownload;
    private ImageView ivClose;

    private DownloadPlaylistListener listener;

    public interface DownloadPlaylistListener {
        void onDownloadConfirmed(String playlistName);
        void onCancel();
    }

    public void setDownloadPlaylistListener(DownloadPlaylistListener listener) {
        this.listener = listener;
    }

    public static DownloadPlaylistBottomSheetFragment newInstance(String artistName, String artistImageUrl, int songCount) {
        DownloadPlaylistBottomSheetFragment fragment = new DownloadPlaylistBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("artist_name", artistName);
        args.putString("artist_image", artistImageUrl);
        args.putInt("song_count", songCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogTheme);

        if (getArguments() != null) {
            artistName = getArguments().getString("artist_name");
            artistImageUrl = getArguments().getString("artist_image");
            songCount = getArguments().getInt("song_count", 0);
            songNames = getArguments().getStringArrayList("song_names");
            songUrls = getArguments().getStringArrayList("song_urls");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_download_playlist, container, false);

        // Initialize views


        etPlaylistName = view.findViewById(R.id.etPlaylistName);
        tvCancel = view.findViewById(R.id.tvCancel);
        tvDownload = view.findViewById(R.id.tvDownload);
        ivClose = view.findViewById(R.id.ivClose);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Update UI with data
        updateUI();

        // Set default playlist name
        String defaultName = artistName != null ? artistName + " Playlist" : "My Playlist";
        etPlaylistName.setText(defaultName);
        etPlaylistName.setSelection(defaultName.length());

        // Auto-focus and show keyboard
        etPlaylistName.postDelayed(() -> {
            etPlaylistName.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etPlaylistName, InputMethodManager.SHOW_IMPLICIT);
        }, 200);

        // Text watcher to enable/disable download button
        etPlaylistName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateDownloadButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Close button click
        ivClose.setOnClickListener(v -> {
            hideKeyboard();
            dismiss();
            if (listener != null) {
                listener.onCancel();
            }
        });

        // Cancel button click
        tvCancel.setOnClickListener(v -> {
            hideKeyboard();
            dismiss();
            if (listener != null) {
                listener.onCancel();
            }
        });

        // Download button click
        tvDownload.setOnClickListener(v -> {
            String playlistName = etPlaylistName.getText().toString().trim();
            if (playlistName.isEmpty()) {
                showToast("Please enter playlist name");
                return;
            }

            hideKeyboard();
            dismiss();

            if (listener != null) {
                listener.onDownloadConfirmed(playlistName);
            }
        });

        // Initial button state
        updateDownloadButtonState();
    }

    private void updateUI() {


    }

    private void updateDownloadButtonState() {
        String playlistName = etPlaylistName.getText().toString().trim();
        boolean isValid = !playlistName.isEmpty();

        tvDownload.setEnabled(isValid);
        tvDownload.setAlpha(isValid ? 1.0f : 0.5f);
    }

    private void hideKeyboard() {
        if (getContext() != null && etPlaylistName != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(etPlaylistName.getWindowToken(), 0);
        }
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}