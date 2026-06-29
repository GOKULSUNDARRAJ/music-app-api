package com.Saalai.SalaiMusicApp.Fragments;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.Saalai.SalaiMusicApp.Fragments.AudioFragment;
import com.Saalai.SalaiMusicApp.Models.ArtistCategory;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.R;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScannerFragment extends Fragment {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final String TAG = "ScannerFragment";

    private PreviewView previewView;
    private TextView scanStatusText;
    private ImageView btnToggleFlash;
    private ImageView btnCloseScanner;
    private View scanLine;
    private ObjectAnimator scanAnimator;
    private ProgressBar progressBar;
    private View permissionDeniedView;
    private Button btnRetryPermission;

    private boolean isScanning = false;
    private boolean isPermissionRequested = false;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private ImageView btnUploadGallery;
    private ActivityResultLauncher<Intent> galleryLauncher;

    // Data structures for quick lookup
    private Map<String, ArtistCategory> playlistMap = new HashMap<>();        // cat_XXX -> Playlist
    private Map<String, AudioModel> songsMap = new HashMap<>();               // song_XXX -> Song
    private Map<String, String> songToPlaylistMap = new HashMap<>();          // song_XXX -> cat_XXX

    private Handler handler = new Handler();

    private String authToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authToken = "Bearer " + com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager.getInstance(requireContext()).getAccessToken();
        
        // Initialize API and load data
        com.Saalai.SalaiMusicApp.ApiService.ApiClient.initialize(new com.Saalai.SalaiMusicApp.ApiService.ApiClient.ApiClientCallback() {
            @Override
            public void onUrlLoaded(String baseUrl) {
                loadDataFromApi();
            }

            @Override
            public void onAllUrlsFailed(String error) {
                Log.e(TAG, "API Init failed: " + error);
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "Scanner offline: Connect to internet", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onNoUrlsAvailable() {
                Log.e(TAG, "No API URLs available");
            }
        });

        cameraExecutor = Executors.newSingleThreadExecutor();
        setupGalleryLauncher();
    }

    private void loadDataFromApi() {
        if (!isAdded()) return;
        
        com.Saalai.SalaiMusicApp.ApiService.ApiService apiService = 
            com.Saalai.SalaiMusicApp.ApiService.ApiClient.getClient().create(com.Saalai.SalaiMusicApp.ApiService.ApiService.class);

        // Refresh token
        String accessToken = com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager.getInstance(requireContext()).getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "❌ Cannot load scanner data: Access token is missing");
            updateScanStatus("Session error. Please login.");
            return;
        }
        authToken = "Bearer " + accessToken;

        // Clear existing maps
        playlistMap.clear();
        songsMap.clear();
        songToPlaylistMap.clear();

        Log.d(TAG, "📡 Fetching fresh playlist data for scanner...");

        // 1. Load Home Data
        apiService.getHomeData(authToken).enqueue(new retrofit2.Callback<com.Saalai.SalaiMusicApp.Response.HomeResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.Saalai.SalaiMusicApp.Response.HomeResponse> call, 
                                 retrofit2.Response<com.Saalai.SalaiMusicApp.Response.HomeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    parseSections(response.body().getSections());
                } else {
                    Log.e(TAG, "❌ Home Data Error: " + response.code());
                    if (response.code() == 401) updateScanStatus("Auth error. Please login.");
                }
            }
            @Override
            public void onFailure(retrofit2.Call<com.Saalai.SalaiMusicApp.Response.HomeResponse> call, Throwable t) {
                Log.e(TAG, "Home API failed", t);
            }
        });

        // 2. Load Artist Data
        apiService.getArtistData(authToken).enqueue(new retrofit2.Callback<com.Saalai.SalaiMusicApp.Models.ArtistResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.Saalai.SalaiMusicApp.Models.ArtistResponse> call, 
                                 retrofit2.Response<com.Saalai.SalaiMusicApp.Models.ArtistResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    parseSections(response.body().getSections());
                } else {
                    Log.e(TAG, "❌ Artist Data Error: " + response.code());
                }
            }
            @Override
            public void onFailure(retrofit2.Call<com.Saalai.SalaiMusicApp.Models.ArtistResponse> call, Throwable t) {
                Log.e(TAG, "Artist API failed", t);
            }
        });

        // 3. Load Devotional Data
        apiService.getDevotionalData(authToken).enqueue(new retrofit2.Callback<com.Saalai.SalaiMusicApp.Models.DevotionalResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.Saalai.SalaiMusicApp.Models.DevotionalResponse> call, 
                                 retrofit2.Response<com.Saalai.SalaiMusicApp.Models.DevotionalResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    parseSections(response.body().getSections());
                } else {
                    Log.e(TAG, "❌ Devotional Data Error: " + response.code());
                }
            }
            @Override
            public void onFailure(retrofit2.Call<com.Saalai.SalaiMusicApp.Models.DevotionalResponse> call, Throwable t) {
                Log.e(TAG, "Devotional API failed", t);
            }
        });
    }

    private void parseSections(List<com.Saalai.SalaiMusicApp.Models.PlaylistSection> sections) {
        if (sections == null) return;
        for (com.Saalai.SalaiMusicApp.Models.PlaylistSection section : sections) {
            parseCategories(section.getArtistCategories());
        }
    }

    private void parseCategories(List<com.Saalai.SalaiMusicApp.Models.ArtistCategory> categories) {
        if (categories == null) return;
        for (com.Saalai.SalaiMusicApp.Models.ArtistCategory category : categories) {
            String rawId = category.getCategoryId().toLowerCase();
            String categoryId = rawId.startsWith("cat_") ? rawId : "cat_" + rawId;
            
            playlistMap.put(categoryId, category);

            if (category.getSongs() != null) {
                for (AudioModel song : category.getSongs()) {
                    String songId = song.getSongId().toLowerCase();
                    
                    // Ensure the mapping uses the "song_" prefix if it's missing
                    // This is critical because the scanner looks for "song_XXX"
                    String searchKey = songId.startsWith("song_") ? songId : "song_" + songId;
                    
                    songsMap.put(searchKey, song);
                    songToPlaylistMap.put(searchKey, categoryId);
                }
            }
        }
        
        Log.d(TAG, "📊 Syncing Scanner: " + songsMap.size() + " songs, " + playlistMap.size() + " categories");
    }

    private void setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        processGalleryImage(imageUri);
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scanner, container, false);

        initializeViews(view);
        setupClickListeners();
        startScanAnimation();

        return view;
    }

    private void initializeViews(View view) {
        previewView = view.findViewById(R.id.previewView);
        scanStatusText = view.findViewById(R.id.scanStatusText);
        btnToggleFlash = view.findViewById(R.id.btnToggleFlash);
        btnUploadGallery = view.findViewById(R.id.btnUploadGallery);
        btnCloseScanner = view.findViewById(R.id.btnCloseScanner);
        scanLine = view.findViewById(R.id.scanLine);
        progressBar = view.findViewById(R.id.progressBar);
        permissionDeniedView = view.findViewById(R.id.permissionDeniedView);
        btnRetryPermission = view.findViewById(R.id.btnRetryPermission);
    }

    private void setupClickListeners() {
        btnToggleFlash.setOnClickListener(v -> toggleFlash());
        btnCloseScanner.setOnClickListener(v -> closeScanner());
        btnUploadGallery.setOnClickListener(v -> openGallery());

        if (btnRetryPermission != null) {
            btnRetryPermission.setOnClickListener(v -> {
                isPermissionRequested = false;
                checkCameraPermissionAndStart();
            });
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }


    private void closeScanner() {
        stopScanning();
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void startScanAnimation() {
        if (scanLine != null) {
            scanAnimator = ObjectAnimator.ofFloat(scanLine, "translationY", 0f, 260f);
            scanAnimator.setDuration(2000);
            scanAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            scanAnimator.setRepeatMode(ObjectAnimator.REVERSE);
            scanAnimator.start();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        checkCameraPermissionAndStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
        stopScanning();
        if (scanAnimator != null) {
            scanAnimator.pause();
        }
    }

    private void checkCameraPermissionAndStart() {
        Log.d(TAG, "checkCameraPermissionAndStart called");

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission already granted");
            hidePermissionDeniedView();
            startScanning();
        } else {
            Log.d(TAG, "Camera permission not granted");

            if (!isPermissionRequested) {
                isPermissionRequested = true;
                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            } else {
                showPermissionDeniedView();
            }
        }
    }

    private void showPermissionDeniedView() {
        Log.d(TAG, "Showing permission denied view");
        if (previewView != null) previewView.setVisibility(View.GONE);
        if (permissionDeniedView != null) permissionDeniedView.setVisibility(View.VISIBLE);
        if (scanStatusText != null) scanStatusText.setVisibility(View.GONE);
        if (btnToggleFlash != null) btnToggleFlash.setVisibility(View.GONE);
        if (scanLine != null) scanLine.setVisibility(View.GONE);
    }

    private void hidePermissionDeniedView() {
        Log.d(TAG, "Hiding permission denied view");
        if (previewView != null) previewView.setVisibility(View.VISIBLE);
        if (permissionDeniedView != null) permissionDeniedView.setVisibility(View.GONE);
        if (scanStatusText != null) scanStatusText.setVisibility(View.VISIBLE);
        if (btnToggleFlash != null) btnToggleFlash.setVisibility(View.VISIBLE);
        if (scanLine != null) scanLine.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult called");

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission GRANTED by user");
                hidePermissionDeniedView();
                startScanning();
            } else {
                Log.d(TAG, "Camera permission DENIED by user");
                isPermissionRequested = true;

                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    Toast.makeText(getContext(),
                            "Camera permission is needed to scan codes",
                            Toast.LENGTH_LONG).show();
                    showPermissionDeniedView();
                } else {
                    Toast.makeText(getContext(),
                            "Camera permission permanently denied. Please enable in settings.",
                            Toast.LENGTH_LONG).show();
                    showPermissionDeniedView();
                }
            }
        }
    }

    private void startScanning() {
        Log.d(TAG, "startScanning called");
        if (isScanning) return;

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
                isScanning = true;
                updateScanStatus("Align Aruvi Wave with line");
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (scanAnimator != null) scanAnimator.resume();
            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private androidx.camera.core.Camera camera;
    private boolean isFlashOn = false;

    private void bindCameraUseCases() {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            Log.d(TAG, "Camera bound successfully");
        } catch (Exception e) {
            Log.e(TAG, "Binding failed", e);
        }
    }

    private void toggleFlash() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            isFlashOn = !isFlashOn;
            camera.getCameraControl().enableTorch(isFlashOn);
            btnToggleFlash.setImageResource(isFlashOn ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            btnToggleFlash.setColorFilter(isFlashOn ? 
                getResources().getColor(R.color.bgred) : 
                getResources().getColor(R.color.white));
            
            Log.d(TAG, "Flash toggled: " + isFlashOn);
        } else {
            Toast.makeText(getContext(), "Flash not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void analyzeImage(@NonNull ImageProxy image) {
        if (!isScanning) {
            image.close();
            return;
        }

        int rotation = image.getImageInfo().getRotationDegrees();
        boolean isRotated = (rotation == 90 || rotation == 270);

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] grayscale = new byte[buffer.remaining()];
        buffer.get(grayscale);
        
        int width = image.getWidth();
        int height = image.getHeight();
        int rowStride = image.getPlanes()[0].getRowStride();

        // If rotated, the bars are actually horizontal in the buffer (vertical in reality)
        // So we scan across the shorter dimension
        int scanWidth = isRotated ? height : width;
        int scanHeight = isRotated ? width : height;

        int center = scanHeight / 2;
        // Try multiple "rows" (or columns if rotated)
        for (int offset = -50; offset <= 50; offset += 25) {
            int linePos = center + offset;
            if (linePos < 0 || linePos >= scanHeight) continue;

            // Calculate adaptive threshold for this row
            long sum = 0;
            for (int i = 0; i < scanWidth; i++) {
                if (isRotated) sum += grayscale[i * rowStride + linePos] & 0xFF;
                else sum += grayscale[linePos * rowStride + i] & 0xFF;
            }
            int avg = (int) (sum / scanWidth);
            int threshold = Math.max(40, avg - 35); // Bars must be significantly darker than average

            List<Integer> barPositions = new ArrayList<>();
            boolean inBar = false;
            
            for (int i = 20; i < scanWidth - 20; i++) {
                int pixel;
                if (isRotated) {
                    // Rotated: scan horizontally across columns
                    pixel = grayscale[i * rowStride + linePos] & 0xFF;
                } else {
                    // Normal: scan across a row
                    pixel = grayscale[linePos * rowStride + i] & 0xFF;
                }

                if (pixel < threshold && !inBar) {
                    inBar = true;
                    barPositions.add(i);
                } else if (pixel > threshold + 30 && inBar) {
                    inBar = false;
                }
            }

            if (barPositions.size() >= 23) {
                if (barPositions.size() > 23) {
                    List<Integer> subList = new ArrayList<>();
                    for (int j = barPositions.size() - 23; j < barPositions.size(); j++) {
                        subList.add(barPositions.get(j));
                    }
                    barPositions = subList;
                }

                int[] rawHeights = new int[23];
                int maxHeight = 0;
                for (int j = 0; j < 23; j++) {
                    int barX = barPositions.get(j);
                    int barH = 0;
                    for (int dy = -40; dy < 40; dy++) {
                        int pY = linePos + dy;
                        if (pY >= 0 && pY < scanHeight) {
                            int p;
                            if (isRotated) {
                                p = grayscale[barX * rowStride + pY] & 0xFF;
                            } else {
                                p = grayscale[pY * rowStride + barX] & 0xFF;
                            }
                            if (p < threshold + 20) barH++;
                        }
                    }
                    rawHeights[j] = barH;
                    if (barH > maxHeight) maxHeight = barH;
                }

                if (maxHeight > 15) {
                    int[] levels = new int[23];
                    for (int j = 0; j < 23; j++) {
                        // Corrected formula to reverse: heightFactor = 0.2 + (level * 0.1)
                        // level = (heightFactor - 0.2) / 0.1 = (raw/max * 0.9 - 0.2) * 10 = raw/max * 9 - 2
                        levels[j] = Math.max(0, Math.min(7, Math.round((float) rawHeights[j] * 9 / maxHeight - 2)));
                    }

                    Log.d(TAG, "🎯 Live Pattern Matched (Rotation: " + rotation + ") Row: " + linePos + " Pattern: " + Arrays.toString(levels));
                    
                    String matchedId = findMatch(levels);
                    if (matchedId != null) {
                        isScanning = false;
                        Log.d(TAG, "🏆 Match Confirmed: " + matchedId);
                        requireActivity().runOnUiThread(() -> processScannedCode(matchedId));
                        image.close();
                        return;
                    }
                }
            }
        }

        image.close();
    }

    private String findMatch(int[] levels) {
        String bestMatch = null;
        int minTotalError = 999;
        
        // Combine all maps for searching
        Map<String, Object> allData = new HashMap<>();
        for(String key : playlistMap.keySet()) allData.put(key, playlistMap.get(key));
        for(String key : songsMap.keySet()) allData.put(key, songsMap.get(key));

        for (String id : allData.keySet()) {
            int error = getMatchError(id, levels);
            
            // Log every ID with its error score for debugging
            Log.d(TAG, "Scoring " + id + ": Total Error = " + error);
            
            if (error < minTotalError) {
                minTotalError = error;
                bestMatch = id;
            }
        }
        
        // Threshold: A perfect match has error 0. 
        // We allow an error up to 15 (avg < 0.65 per bar) to account for noise.
        if (minTotalError <= 15) {
            Log.d(TAG, "🏆 WINNER: " + bestMatch + " with score " + minTotalError);
            return bestMatch;
        }
        
        return null;
    }

    private int getMatchError(String id, int[] levels) {
        long seed = id.hashCode();
        int totalError = 0;
        
        for (int i = 0; i < 23; i++) {
            int expectedLevel;
            if (i == 0 || i == 22) {
                expectedLevel = 3;
            } else {
                expectedLevel = (int) Math.abs((seed ^ (i * 997)) % 8);
            }
            
            // Square the error to penalize large individual mismatches more than small ones
            int diff = Math.abs(expectedLevel - levels[i]);
            totalError += diff;
        }
        return totalError;
    }

    private void stopScanning() {
        Log.d(TAG, "stopScanning called");
        isScanning = false;
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    private void processScannedCode(String scannedText) {
        // Stop scanning temporarily
        isScanning = false;

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        updateScanStatus("Processing...");

        // Clean the scanned text - remove any spaces, convert to lowercase
        String cleanText = scannedText.trim().toLowerCase();

        Log.d(TAG, "🔍 Processing: " + cleanText);

        // CASE 1: DIRECT PLAYLIST SCAN (cat_XXX)
        if (cleanText.startsWith("cat_")) {
            Log.d(TAG, "📁 Detected PLAYLIST code: " + cleanText);

            ArtistCategory playlist = playlistMap.get(cleanText);

            if (playlist != null) {
                Log.d(TAG, "✅ Found playlist: " + playlist.getCategoryName());
                handlePlaylistFound(playlist, null); // null means don't auto-play specific song
            } else {
                Log.d(TAG, "❌ Playlist not found: " + cleanText);
                handleUnknownCode(cleanText);
            }
        }

        // CASE 2: SONG SCAN (song_XXX) - Find its parent playlist
        else if (cleanText.startsWith("song_")) {
            Log.d(TAG, "🎵 Detected SONG code: " + cleanText);

            // Find which playlist contains this song
            String parentPlaylistId = songToPlaylistMap.get(cleanText);

            if (parentPlaylistId != null) {
                ArtistCategory parentPlaylist = playlistMap.get(parentPlaylistId);

                if (parentPlaylist != null) {
                    Log.d(TAG, "✅ Found parent playlist: " + parentPlaylist.getCategoryName() +
                            " (" + parentPlaylistId + ")");

                    // Open the playlist that contains this song and auto-play the scanned song
                    handlePlaylistFound(parentPlaylist, cleanText);
                } else {
                    Log.d(TAG, "❌ Parent playlist not found in map: " + parentPlaylistId);
                    handleUnknownCode(cleanText);
                }
            } else {
                // Try to find song directly (fallback)
                AudioModel song = songsMap.get(cleanText);
                if (song != null) {
                    Log.d(TAG, "⚠️ Song found but no playlist mapping, creating single-song playlist");

                    // Create a single-song playlist as fallback
                    ArrayList<AudioModel> singleSongList = new ArrayList<>();
                    singleSongList.add(song);

                    ArtistCategory singlePlaylist = new ArtistCategory(
                            "temp_" + cleanText,
                            song.getAudioName(),
                            singleSongList,
                            song.getImageUrl(),
                            1
                    );

                    handlePlaylistFound(singlePlaylist, cleanText);
                } else {
                    Log.d(TAG, "❌ No song or playlist found for: " + cleanText);
                    handleUnknownCode(cleanText);
                }
            }
        }

        // CASE 3: INVALID CODE FORMAT
        else {
            Log.d(TAG, "❌ Invalid code format (must start with cat_ or song_): " + cleanText);
            handleUnknownCode(cleanText);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == android.app.Activity.RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                processGalleryImage(selectedImage);
            }
        }
    }

    private void processGalleryImage(Uri uri) {
        try {
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            updateScanStatus("Analyzing image...");

            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            if (bitmap != null) {
                // Analyze bitmap on a background thread
                cameraExecutor.execute(() -> {
                    analyzeBitmap(bitmap);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Gallery error", e);
        }
    }

    private void analyzeBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        byte[] grayscale = new byte[width * height];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = bitmap.getPixel(x, y);
                grayscale[y * width + x] = (byte) (((p >> 16 & 0xFF) + (p >> 8 & 0xFF) + (p & 0xFF)) / 3);
            }
        }

        Log.d(TAG, "🔍 Starting Bitmap Analysis (" + width + "x" + height + ")");

        for (int rowY = height / 4; rowY < height * 0.95; rowY += 15) {
            int min = 255, max = 0;
            for (int x = width / 4; x < width * 3 / 4; x++) {
                int v = grayscale[rowY * width + x] & 0xFF;
                if (v < min) min = v;
                if (v > max) max = v;
            }
            
            if (max - min < 30) continue; // Lowered contrast requirement
            int threshold = min + (max - min) / 3;

            List<Integer> barPositions = new ArrayList<>();
            boolean inBar = false;
            // Ignore far left (first 15%) to avoid the Aruvi logo
            // Ignore far right (last 10%)
            for (int x = (int)(width * 0.15); x < (int)(width * 0.9); x++) {
                int v = grayscale[rowY * width + x] & 0xFF;
                if (v < threshold && !inBar) {
                    inBar = true;
                    barPositions.add(x);
                } else if (v > threshold + 10 && inBar) {
                    inBar = false;
                }
            }

            // If we found more than 23, the wave is likely in there. 
            // We'll take the first 23 if they look like a valid sequence.
            if (barPositions.size() >= 23) {
                if (barPositions.size() > 23) {
                    List<Integer> subList = new ArrayList<>();
                    for (int i = barPositions.size() - 23; i < barPositions.size(); i++) {
                        subList.add(barPositions.get(i));
                    }
                    barPositions = subList;
                }

                int[] rawHeights = new int[23];
                int maxHeight = 0;
                for (int i = 0; i < 23; i++) {
                    int barX = barPositions.get(i);
                    int barH = 0;
                    for (int dy = -70; dy < 70; dy++) {
                        int y = rowY + dy;
                        if (y >= 0 && y < height && (grayscale[y * width + barX] & 0xFF) < threshold + 15) {
                            barH++;
                        }
                    }
                    rawHeights[i] = barH;
                    if (barH > maxHeight) maxHeight = barH;
                }

                // Adaptive Scaling: Correctly reverse the 0.2 to 0.9 height range
                int[] levels = new int[23];
                for (int i = 0; i < 23; i++) {
                    if (maxHeight > 10) {
                        levels[i] = Math.max(0, Math.min(7, Math.round((float) rawHeights[i] * 9 / maxHeight - 2)));
                    } else {
                        levels[i] = 0;
                    }
                }

                Log.d(TAG, "📊 Row " + rowY + " Pattern: " + Arrays.toString(levels));

                String matchedId = findMatch(levels);
                if (matchedId != null) {
                    Log.d(TAG, "🎯 Gallery Match Found: " + matchedId + " at row " + rowY);
                    requireActivity().runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        processScannedCode(matchedId);
                    });
                    return;
                }
            }
        }

        requireActivity().runOnUiThread(() -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            updateScanStatus("Wave not found in photo");
            Toast.makeText(getContext(), "Could not find Aruvi wave", Toast.LENGTH_SHORT).show();
        });
    }

    private void handlePlaylistFound(ArtistCategory playlist, String scannedSongId) {
        updateScanStatus("✓ Playlist found: " + playlist.getCategoryName());
        Toast.makeText(getContext(), "📀 " + playlist.getCategoryName(), Toast.LENGTH_LONG).show();

        stopScanning();
        if (progressBar != null) progressBar.setVisibility(View.GONE);

        // Navigate to AudioFragment with the full playlist
        AudioFragment fragment = new AudioFragment();
        Bundle args = new Bundle();
        args.putString("artist_name", playlist.getCategoryName());
        args.putString("artist_image", playlist.getArtistImageUrl());
        args.putSerializable("songs_list", new ArrayList<>(playlist.getSongs()));

        // If we have a specific song ID to play, pass it
        if (scannedSongId != null) {
            args.putString("auto_play_song_id", scannedSongId);
            Log.d(TAG, "Will auto-play song: " + scannedSongId);
        }

        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        // Close scanner after navigation
        handler.postDelayed(this::closeScanner, 800);
    }

    private void handleUnknownCode(String scannedText) {
        updateScanStatus("✗ Invalid code");
        Toast.makeText(getContext(),
                "❌ Invalid code. Use cat_XXX or song_XXX format",
                Toast.LENGTH_LONG).show();

        if (progressBar != null) progressBar.setVisibility(View.GONE);

        // Resume scanning after delay
        handler.postDelayed(() -> {
            if (!isScanning && isAdded()) {
                startScanning();
            }
        }, 2000);
    }

    private void updateScanStatus(String status) {
        if (scanStatusText != null && isAdded()) {
            requireActivity().runOnUiThread(() -> scanStatusText.setText(status));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");
        stopScanning();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (scanAnimator != null) {
            scanAnimator.cancel();
        }
    }
}
