package com.Saalai.SalaiMusicApp;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.Saalai.SalaiMusicApp.Custom.SpotifyCodeView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.cardview.widget.CardView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class QRCodeBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "QRCodeBottomSheet";
    private static final String ARG_TITLE = "title";
    private static final String ARG_ID = "id";
    private static final String ARG_IMAGE_URL = "image_url";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int MANAGE_STORAGE_REQUEST_CODE = 101;

    private String title;
    private String id;
    private String imageUrl;

    private ImageView imageViewPlaylist, btnClose, aruviLogo;
    private LinearLayout btnDownload, btnShare;
    private TextView tvQRData, tvPlaylistName;
    private CardView mainContainer, layoutQRCode;

    public static QRCodeBottomSheet newInstance(String title, String id, String imageUrl) {
        QRCodeBottomSheet fragment = new QRCodeBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_ID, id);
        args.putString(ARG_IMAGE_URL, imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_qr_code, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutQRCode = view.findViewById(R.id.layoutQRCode);

        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            id = getArguments().getString(ARG_ID);
            imageUrl = getArguments().getString(ARG_IMAGE_URL);

            Log.d(TAG, "Received - Title: " + title);
            Log.d(TAG, "Received - ID: " + id);
            Log.d(TAG, "Received - Image URL: " + imageUrl);
        }

        // Initialize views
        tvQRData = view.findViewById(R.id.tvQRData);
        tvPlaylistName = view.findViewById(R.id.tvPlaylistName);
        btnShare = view.findViewById(R.id.btnShare);
        btnDownload = view.findViewById(R.id.btnDownload);
        imageViewPlaylist = view.findViewById(R.id.imageviewplaylist);
        btnClose = view.findViewById(R.id.btnClose);
        aruviLogo = view.findViewById(R.id.aruviLogo);
        mainContainer = view.findViewById(R.id.layoutQRCode);
        SpotifyCodeView spotifyCodeView = view.findViewById(R.id.spotifyCode);

        // Set title
        tvPlaylistName.setText(title != null ? title : "Aruvi Music");

        // Load image
        loadImage();

        // Set data
        String codeData = id != null ? id : "";
        tvQRData.setText(codeData);
        
        // Update scannable soundwave
        spotifyCodeView.setData(codeData);

        // Set click listeners
        btnShare.setOnClickListener(v -> shareQRCode());
        btnDownload.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                downloadQRCode();
            } else {
                requestStoragePermission();
            }
        });

        btnClose.setOnClickListener(v -> dismiss());
        getDialog().setCanceledOnTouchOutside(true);
    }



    private void loadImage() {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Attempting to load image from: " + imageUrl);

            String decodedUrl = imageUrl;
            try {
                decodedUrl = URLDecoder.decode(imageUrl, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Error decoding URL", e);
            }

            Glide.with(this)
                    .load(decodedUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.video_placholder)
                            .error(R.drawable.video_placholder)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .timeout(10000))
                    .into(imageViewPlaylist);
        } else {
            imageViewPlaylist.setImageResource(R.drawable.video_placholder);
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        } else {
            int result = ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            downloadQRCode();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadQRCode();
            } else {
                Toast.makeText(getContext(), "Storage permission is required to download QR code", Toast.LENGTH_SHORT).show();
                shareQRCodeAsText();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_STORAGE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    downloadQRCode();
                } else {
                    Toast.makeText(getContext(), "Storage permission is required to download QR code", Toast.LENGTH_SHORT).show();
                    shareQRCodeAsText();
                }
            }
        }
    }

    private void shareQRCode() {
        try {
            Bitmap bitmap = getBitmapFromView(mainContainer);

            if (bitmap == null) {
                Toast.makeText(getContext(), "Failed to create QR code image", Toast.LENGTH_SHORT).show();
                shareQRCodeAsText();
                return;
            }

            String fileName = "QRCode_" + System.currentTimeMillis() + ".png";
            File cacheDir = requireContext().getCacheDir();
            File file = new File(cacheDir, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Uri uri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);

            String type = id != null && id.startsWith("song_") ? "Song" : "Playlist";
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Check out this " + type + ": " + title +
                            "\n\nScan the QR code in Salai Music App");

            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share QR Code via"));

        } catch (Exception e) {
            Log.e(TAG, "Error sharing QR code", e);
            Toast.makeText(getContext(), "Sharing as text instead", Toast.LENGTH_SHORT).show();
            shareQRCodeAsText();
        }
    }

    private Bitmap getBitmapFromView(View view) {
        try {
            view.setDrawingCacheEnabled(true);
            view.buildDrawingCache();
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error creating bitmap", e);
            return null;
        }
    }

    private void shareQRCodeAsText() {
        String type = id != null && id.startsWith("song_") ? "Song" : "Playlist";
        String shareText = "🎵 " + type + ": " + title +
                "\n\nID: " + id +
                "\n\nScan this QR code in Salai Music App";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void downloadQRCode() {
        try {
            Bitmap bitmap = getBitmapFromView(mainContainer);

            if (bitmap == null) {
                Toast.makeText(getContext(), "Failed to create QR code image", Toast.LENGTH_SHORT).show();
                return;
            }

            String fileName = "Salai_" + title.replaceAll("[^a-zA-Z0-9]", "_") + "_QR.png";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(bitmap, fileName);
            } else {
                saveToExternalStorage(bitmap, fileName);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving QR code", e);
            Toast.makeText(getContext(), "Failed to save QR Code", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToMediaStore(Bitmap bitmap, String fileName) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SalaiMusic");

            Uri uri = requireContext().getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri)) {
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                        Toast.makeText(getContext(), "QR Code saved to Pictures/SalaiMusic", Toast.LENGTH_LONG).show();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving to MediaStore", e);
            Toast.makeText(getContext(), "Failed to save QR Code", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToExternalStorage(Bitmap bitmap, String fileName) {
        try {
            String picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
            File salaiDir = new File(picturesDir, "SalaiMusic");
            if (!salaiDir.exists()) {
                if (!salaiDir.mkdirs()) {
                    Toast.makeText(getContext(), "Failed to create directory", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            File file = new File(salaiDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(file));
            requireContext().sendBroadcast(mediaScanIntent);

            Toast.makeText(getContext(), "QR Code saved to Pictures/SalaiMusic", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Error saving to external storage", e);
            Toast.makeText(getContext(), "Failed to save QR Code", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        View bottomSheet = getDialog().findViewById(
                com.google.android.material.R.id.design_bottom_sheet);

        if (bottomSheet != null) {
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            bottomSheet.requestLayout();

            com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);

            behavior.setState(
                    com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);

            behavior.setSkipCollapsed(true);
        }
    }
}