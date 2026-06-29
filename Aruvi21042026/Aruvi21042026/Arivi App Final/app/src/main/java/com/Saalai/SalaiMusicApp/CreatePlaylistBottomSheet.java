package com.Saalai.SalaiMusicApp;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Response.LibraryCategory;
import com.Saalai.SalaiMusicApp.Response.LibraryResponse;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;

import com.Saalai.SalaiMusicApp.Models.PlaylistModels;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class CreatePlaylistBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "CreatePlaylistBS";

    private static final String ARG_SONG_NAME = "song_name";
    private static final String ARG_SONG_IMAGE = "song_image";
    private static final String ARG_SONG_ARTIST = "song_artist";
    private static final String ARG_SONG_URL = "song_url";
    private static final String ARG_SONG_ID = "song_id";

    // UI Elements
    private EditText playlistNameEditText;
    private ImageView songImageView;
    private TextView songNameTextView, artistNameTextView;
    private Button createPlaylistButton, cancelButton;
    private ProgressBar progressBar;
    private int lastAddedPlaylistId = -1;
    // Playlist selection UI
    private LinearLayout existingPlaylistSection;
    private RecyclerView playlistRecyclerView;
    private TextView existingPlaylistTitle;
    private ProgressBar playlistProgressBar;
    private TextView noPlaylistText;

    private String songName, songImage, songArtist, songUrl, songId;
    private String accessToken;
    private ApiService apiService;

    private List<PlaylistItem> playlistItems = new ArrayList<>();
    private PlaylistSelectionAdapter adapter;

    // Track which playlists contain the current song
    private Set<Integer> playlistsContainingSong = new HashSet<>();
    private boolean isCheckingPlaylists = false;

    public static CreatePlaylistBottomSheet newInstance(String songName, String songImage,
                                                        String songArtist, String songUrl, String songId) {
        Log.d(TAG, "========== newInstance START ==========");
        Log.d(TAG, "Song Name: " + songName);
        Log.d(TAG, "Song Image: " + songImage);
        Log.d(TAG, "Song Artist: " + songArtist);
        Log.d(TAG, "Song URL: " + songUrl);
        Log.d(TAG, "Song ID: " + songId);

        CreatePlaylistBottomSheet fragment = new CreatePlaylistBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_SONG_NAME, songName);
        args.putString(ARG_SONG_IMAGE, songImage);
        args.putString(ARG_SONG_ARTIST, songArtist);
        args.putString(ARG_SONG_URL, songUrl);
        args.putString(ARG_SONG_ID, songId);
        fragment.setArguments(args);

        Log.d(TAG, "========== newInstance END ==========");
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "========== onCreate START ==========");
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogTheme);

        if (getArguments() != null) {
            Log.d(TAG, "Getting arguments from bundle");
            songName = getArguments().getString(ARG_SONG_NAME);
            songImage = getArguments().getString(ARG_SONG_IMAGE);
            songArtist = getArguments().getString(ARG_SONG_ARTIST);
            songUrl = getArguments().getString(ARG_SONG_URL);
            songId = getArguments().getString(ARG_SONG_ID);

            Log.d(TAG, "Retrieved - Name: " + songName);
            Log.d(TAG, "Retrieved - Artist: " + songArtist);
            Log.d(TAG, "Retrieved - SongId: " + songId);
        } else {
            Log.e(TAG, "No arguments found in bundle");
        }

        // Get auth token
        Log.d(TAG, "Getting auth token from SharedPrefManager");
        SharedPrefManager sp = SharedPrefManager.getInstance(requireContext());
        accessToken = sp.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "Access token is NULL or EMPTY!");
        } else {
            Log.d(TAG, "Access token retrieved: " + accessToken.substring(0, Math.min(20, accessToken.length())) + "...");
        }

        // Initialize API service
        Log.d(TAG, "Checking if ApiClient is initialized");
        if (ApiClient.isInitialized()) {
            Log.d(TAG, "ApiClient is initialized, creating ApiService");
            Retrofit retrofit = ApiClient.getClient();
            apiService = retrofit.create(ApiService.class);
            Log.d(TAG, "ApiService created successfully");
        } else {
            Log.e(TAG, "ApiClient is NOT initialized!");
        }

        Log.d(TAG, "========== onCreate END ==========");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "========== onCreateView START ==========");
        View view = inflater.inflate(R.layout.fragment_create_playlist_bottom_sheet, container, false);

        initViews(view);
        setupUI();
        setupListeners();
        loadExistingPlaylists();

        Log.d(TAG, "========== onCreateView END ==========");
        return view;
    }

    private void initViews(View view) {
        Log.d(TAG, "Initializing views");

        // Create playlist section
        playlistNameEditText = view.findViewById(R.id.playlistNameEditText);
        songImageView = view.findViewById(R.id.songImageView);
        songNameTextView = view.findViewById(R.id.songNameTextView);
        artistNameTextView = view.findViewById(R.id.artistNameTextView);
        createPlaylistButton = view.findViewById(R.id.createPlaylistButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        progressBar = view.findViewById(R.id.progressBar);

        // Existing playlist section
        existingPlaylistSection = view.findViewById(R.id.existingPlaylistSection);
        playlistRecyclerView = view.findViewById(R.id.playlistRecyclerView);
        existingPlaylistTitle = view.findViewById(R.id.existingPlaylistTitle);
        playlistProgressBar = view.findViewById(R.id.playlistProgressBar);
        noPlaylistText = view.findViewById(R.id.noPlaylistText);

        // Setup RecyclerView
        playlistRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PlaylistSelectionAdapter();
        playlistRecyclerView.setAdapter(adapter);

        Log.d(TAG, "Views initialized successfully");
    }

    private void setupUI() {
        Log.d(TAG, "Setting up UI");

        if (songImage != null && !songImage.isEmpty()) {
            Log.d(TAG, "Loading song image from URL: " + songImage);
            Picasso.get().load(songImage)
                    .placeholder(R.drawable.video_placholder)
                    .error(R.drawable.video_placholder)
                    .into(songImageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Song image loaded successfully");
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Failed to load song image: " + e.getMessage());
                        }
                    });
        } else {
            Log.w(TAG, "Song image URL is empty, using placeholder");
            songImageView.setImageResource(R.drawable.video_placholder);
        }

        songNameTextView.setText(songName != null ? songName : "Unknown Song");
        artistNameTextView.setText(songArtist != null ? songArtist : "Unknown Artist");

        Log.d(TAG, "UI setup completed - Song: " + songNameTextView.getText() + ", Artist: " + artistNameTextView.getText());
    }

    private void setupListeners() {
        Log.d(TAG, "Setting up listeners");

        createPlaylistButton.setOnClickListener(v -> {
            Log.d(TAG, "Create Playlist button clicked");
            String playlistName = playlistNameEditText.getText().toString().trim();
            Log.d(TAG, "Playlist name entered: " + playlistName);

            if (TextUtils.isEmpty(playlistName)) {
                Log.w(TAG, "Playlist name is empty");
                playlistNameEditText.setError("Please enter a playlist name");
                playlistNameEditText.requestFocus();
                return;
            }

            createPlaylist(playlistName);
        });

        cancelButton.setOnClickListener(v -> {
            Log.d(TAG, "Cancel button clicked, dismissing bottom sheet");
            dismiss();
        });

        Log.d(TAG, "Listeners setup completed");
    }

    private void loadExistingPlaylists() {
        Log.d(TAG, "========== loadExistingPlaylists START ==========");

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "Access token is missing!");
            return;
        }

        showPlaylistLoading(true);
        Log.d(TAG, "Loading playlists from API...");

        String authHeader = "Bearer " + accessToken;
        Log.d(TAG, "Auth header: Bearer " + accessToken.substring(0, Math.min(10, accessToken.length())) + "...");

        Call<LibraryResponse> call = apiService.getUserPlaylists(authHeader);
        Log.d(TAG, "Request URL: " + call.request().url());
        Log.d(TAG, "Request Method: " + call.request().method());

        call.enqueue(new Callback<LibraryResponse>() {
            @Override
            public void onResponse(Call<LibraryResponse> call, Response<LibraryResponse> response) {
                Log.d(TAG, "========== LOAD PLAYLISTS RESPONSE ==========");
                Log.d(TAG, "Response Code: " + response.code());
                Log.d(TAG, "Response Message: " + response.message());

                showPlaylistLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    LibraryResponse body = response.body();
                    Log.d(TAG, "Response success: " + body.isSuccess());
                    Log.d(TAG, "Response status: " + body.isStatus());

                    if (body.isSuccess() && body.getSections() != null && !body.getSections().isEmpty()) {
                        Log.d(TAG, "Sections count: " + body.getSections().size());

                        List<LibraryCategory> categories = body.getSections().get(0).getCategories();
                        Log.d(TAG, "Categories count: " + (categories != null ? categories.size() : 0));

                        if (categories != null && !categories.isEmpty()) {
                            playlistItems.clear();
                            for (LibraryCategory category : categories) {
                                PlaylistItem item = new PlaylistItem();

                                // Extract ID from categoryId (e.g., "cat_playlist_23" -> 23)
                                int id = 0;
                                if (category.getCategoryId() != null) {
                                    String categoryId = category.getCategoryId();
                                    String numericPart = categoryId.replaceAll("[^0-9]", "");
                                    if (!numericPart.isEmpty()) {
                                        try {
                                            id = Integer.parseInt(numericPart);
                                            Log.d(TAG, "Extracted ID " + id + " from categoryId: " + categoryId);
                                        } catch (NumberFormatException e) {
                                            Log.e(TAG, "Failed to parse ID from: " + categoryId);
                                        }
                                    }
                                }

                                item.id = id;
                                item.name = category.getCategoryName();
                                item.imageUrl = category.getCategoryImage();
                                item.categoryId = category.getCategoryId();
                                playlistItems.add(item);
                                Log.d(TAG, "Added playlist - ID: " + item.id + ", Name: " + item.name + ", categoryId: " + category.getCategoryId());
                            }

                            if (playlistItems.isEmpty()) {
                                showNoPlaylists();
                            } else {
                                existingPlaylistSection.setVisibility(View.VISIBLE);
                                noPlaylistText.setVisibility(View.GONE);
                                playlistRecyclerView.setVisibility(View.VISIBLE);

                                // Check which playlists contain the current song
                                checkPlaylistsForSongEfficient();

                                Log.d(TAG, "Loaded " + playlistItems.size() + " playlists successfully");
                            }
                        } else {
                            Log.w(TAG, "No categories found in response");
                            showNoPlaylists();
                        }
                    } else {
                        Log.w(TAG, "No sections found or response not successful");
                        showNoPlaylists();
                    }
                } else {
                    Log.e(TAG, "Response unsuccessful or body is null");
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Error Body: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body: " + e.getMessage());
                    }
                    showNoPlaylists();
                }
                Log.d(TAG, "========== LOAD PLAYLISTS RESPONSE END ==========");
            }

            @Override
            public void onFailure(Call<LibraryResponse> call, Throwable t) {
                Log.e(TAG, "========== LOAD PLAYLISTS FAILED ==========");
                Log.e(TAG, "Failure: " + t.getMessage(), t);
                showPlaylistLoading(false);
                showNoPlaylists();
                Log.d(TAG, "===========================================");
            }
        });

        Log.d(TAG, "========== loadExistingPlaylists END ==========");
    }

    /**
     * Efficiently check which playlists contain the current song using the /api/playlist/status endpoint
     */
    /**
     * Efficiently check which playlists contain the current song using the /api/playlist/status endpoint
     */
    private void checkPlaylistsForSongEfficient() {
        if (playlistItems.isEmpty() || accessToken == null || accessToken.isEmpty() || songId == null) {
            Log.d(TAG, "Cannot check playlists - missing data");
            if (adapter != null) {
                adapter.updatePlaylistStatus();
            }
            return;
        }

        Log.d(TAG, "========== Efficient Playlist Check START ==========");
        Log.d(TAG, "Checking " + playlistItems.size() + " playlists for song ID: " + songId);

        isCheckingPlaylists = true;
        playlistsContainingSong.clear();

        String authHeader = "Bearer " + accessToken;
        AtomicInteger pendingChecks = new AtomicInteger(playlistItems.size());

        for (PlaylistItem playlist : playlistItems) {
            if (playlist.id <= 0) {
                // Skip invalid playlists
                if (pendingChecks.decrementAndGet() == 0) {
                    finishPlaylistCheck();
                }
                continue;
            }

            Log.d(TAG, "Checking playlist " + playlist.id + " - " + playlist.name);

            // Use the status endpoint
            Call<PlaylistModels.PlaylistStatusResponse> call = apiService.checkSongInPlaylist(
                    authHeader,
                    playlist.id,
                    songId
            );

            final int playlistId = playlist.id;
            final String playlistName = playlist.name;

            call.enqueue(new Callback<PlaylistModels.PlaylistStatusResponse>() {
                @Override
                public void onResponse(Call<PlaylistModels.PlaylistStatusResponse> call,
                                       Response<PlaylistModels.PlaylistStatusResponse> response) {
                    Log.d(TAG, "Response for playlist " + playlistId + " - Code: " + response.code());

                    if (response.isSuccessful() && response.body() != null) {
                        PlaylistModels.PlaylistStatusResponse body = response.body();

                        // Log the full response to see what fields are coming
                        Log.d(TAG, "Full response body: " + new Gson().toJson(body));
                        Log.d(TAG, "isInPlaylist: " + body.isInPlaylist());
                        Log.d(TAG, "isAdded: " + body.isAdded());
                        Log.d(TAG, "status: " + body.isStatus());
                        Log.d(TAG, "success: " + body.isSuccess());
                        Log.d(TAG, "result: " + body.getResult());

                        // Check multiple possible fields for the status
                        boolean isInPlaylist = false;

                        // Try all possible field names
                        if (body.isInPlaylist()) {
                            isInPlaylist = true;
                            Log.d(TAG, "Found via isInPlaylist field");
                        } else if (body.isAdded()) {
                            isInPlaylist = true;
                            Log.d(TAG, "Found via isAdded field");
                        } else if (body.isStatus() && body.isSuccess()) {
                            // If both status and success are true, check result field
                            if (body.getResult() != null && body.getResult().equalsIgnoreCase("true")) {
                                isInPlaylist = true;
                                Log.d(TAG, "Found via result field: " + body.getResult());
                            }
                        }

                        if (isInPlaylist) {
                            playlistsContainingSong.add(playlistId);
                            Log.d(TAG, "✓✓✓ SONG FOUND in playlist: " + playlistName + " (ID: " + playlistId + ") ✓✓✓");
                        } else {
                            Log.d(TAG, "✗ Song not in playlist: " + playlistName);
                        }
                    } else {
                        Log.e(TAG, "Failed to check playlist " + playlistId);
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                Log.e(TAG, "Error Body: " + errorBody);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body: " + e.getMessage());
                        }
                    }

                    if (pendingChecks.decrementAndGet() == 0) {
                        finishPlaylistCheck();
                    }
                }

                @Override
                public void onFailure(Call<PlaylistModels.PlaylistStatusResponse> call, Throwable t) {
                    Log.e(TAG, "Failed to check playlist " + playlistId + ": " + t.getMessage());

                    if (pendingChecks.decrementAndGet() == 0) {
                        finishPlaylistCheck();
                    }
                }
            });
        }

        Log.d(TAG, "========== Efficient Playlist Check END ==========");
    }

    private void finishPlaylistCheck() {
        isCheckingPlaylists = false;
        Log.d(TAG, "Playlist check completed. Found in " + playlistsContainingSong.size() + " playlists");

        // Log which playlists contain the song
        for (Integer playlistId : playlistsContainingSong) {
            Log.d(TAG, "Song found in playlist ID: " + playlistId);
        }

        // Refresh the adapter to show tick/plus icons
        if (adapter != null) {
            adapter.updatePlaylistStatus();
        }
    }

    private void showNoPlaylists() {
        Log.d(TAG, "Showing no playlists message");
        existingPlaylistSection.setVisibility(View.VISIBLE);
        noPlaylistText.setVisibility(View.VISIBLE);
        playlistRecyclerView.setVisibility(View.GONE);
    }

    private void showPlaylistLoading(boolean show) {
        Log.d(TAG, "showPlaylistLoading: " + show);
        if (playlistProgressBar != null) {
            playlistProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (existingPlaylistTitle != null) {
            existingPlaylistTitle.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void createPlaylist(String playlistName) {
        Log.d(TAG, "========== CREATE PLAYLIST STARTED ==========");
        Log.d(TAG, "Playlist Name: " + playlistName);
        Log.d(TAG, "Song Name: " + songName);
        Log.d(TAG, "Song ID: " + songId);

        showLoading(true);

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "Access token is missing!");
            showToast("Please login again");
            showLoading(false);
            return;
        }

        String authHeader = "Bearer " + accessToken;

        PlaylistModels.CreatePlaylistRequest request = new PlaylistModels.CreatePlaylistRequest(playlistName);

        Call<PlaylistModels.CreatePlaylistResponse> call = apiService.createPlaylist(authHeader, request);

        call.enqueue(new Callback<PlaylistModels.CreatePlaylistResponse>() {
            @Override
            public void onResponse(Call<PlaylistModels.CreatePlaylistResponse> call,
                                   Response<PlaylistModels.CreatePlaylistResponse> response) {
                Log.d(TAG, "========== CREATE PLAYLIST RESPONSE ==========");
                Log.d(TAG, "Response Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    PlaylistModels.CreatePlaylistResponse body = response.body();

                    if (body.isStatus() && body.isSuccess()) {
                        Log.d(TAG, "Playlist created SUCCESSFULLY!");
                        showToast("✓ Playlist created: " + playlistName);

                        showLoading(false);

                        // REMOVED: dismiss(); - Keep bottom sheet open

                        // Clear the input field for next playlist creation
                        playlistNameEditText.setText("");

                        // Refresh playlists to show the new one
                        loadExistingPlaylists();

                        // Try to add song to the newly created playlist
                        new android.os.Handler().postDelayed(() -> {
                            broadcastPlaylistUpdate();
                            addSongToNewPlaylist(playlistName);
                        }, 500);

                    } else {
                        Log.e(TAG, "Playlist creation FAILED! Message: " + body.getMessage());
                        showLoading(false);
                        showToast("✗ Failed: " + body.getMessage());
                    }
                } else {
                    Log.e(TAG, "Response NOT successful!");
                    showLoading(false);
                    showToast("✗ Error: " + response.code());
                }
                Log.d(TAG, "========== CREATE PLAYLIST RESPONSE END ==========");
            }

            @Override
            public void onFailure(Call<PlaylistModels.CreatePlaylistResponse> call, Throwable t) {
                Log.e(TAG, "========== CREATE PLAYLIST FAILED ==========");
                Log.e(TAG, "Failure: " + t.getMessage(), t);
                showLoading(false);
                showToast("✗ Network error: " + t.getMessage());
                Log.d(TAG, "===========================================");
            }
        });
    }



    private void addSongToExistingPlaylist(int playlistId, String playlistName, String categoryId) {
        Log.d(TAG, "========== ADD TO EXISTING PLAYLIST STARTED ==========");
        Log.d(TAG, "playlistId: " + playlistId);
        Log.d(TAG, "playlistName: " + playlistName);
        Log.d(TAG, "Song ID: " + songId);

        // Check if song already exists in this playlist
        if (playlistsContainingSong.contains(playlistId)) {
            Log.d(TAG, "Song already exists in playlist: " + playlistName);
            showToast("✓ \"" + songName + "\" is already in \"" + playlistName + "\"");
            return;
        }

        showLoading(true);

        String authHeader = "Bearer " + accessToken;

        PlaylistModels.AddItemRequest request = new PlaylistModels.AddItemRequest(playlistId, songId);

        Gson gson = new Gson();
        String jsonBody = gson.toJson(request);
        Log.d(TAG, "Request JSON Body: " + jsonBody);

        Call<PlaylistModels.AddItemResponse> call = apiService.addItemToPlaylist(authHeader, request);

        final int addedPlaylistId = playlistId;
        final String addedPlaylistName = playlistName;

        call.enqueue(new Callback<PlaylistModels.AddItemResponse>() {
            @Override
            public void onResponse(Call<PlaylistModels.AddItemResponse> call,
                                   Response<PlaylistModels.AddItemResponse> response) {
                Log.d(TAG, "========== ADD TO PLAYLIST RESPONSE ==========");
                Log.d(TAG, "Response Code: " + response.code());

                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    PlaylistModels.AddItemResponse body = response.body();

                    if (body.isAdded()) {
                        Log.d(TAG, "Song added to playlist SUCCESSFULLY!");
                        showToast("✓ Added to playlist: " + addedPlaylistName);

                        // Add to our tracking set
                        playlistsContainingSong.add(addedPlaylistId);
                        lastAddedPlaylistId = addedPlaylistId;

                        // Update the adapter to show tick icon for this playlist
                        if (adapter != null) {
                            adapter.updatePlaylistStatus();
                        }

                        // Optional: Scroll to the updated playlist
                        scrollToPlaylist(addedPlaylistId);

                        // Clear the highlight after 2 seconds
                        new android.os.Handler().postDelayed(() -> {
                            lastAddedPlaylistId = -1;
                            if (adapter != null) {
                                adapter.updatePlaylistStatus();
                            }
                        }, 2000);

                    } else {
                        Log.e(TAG, "Failed to add song: " + body.getMessage());
                        showToast("✗ Failed: " + body.getMessage());
                    }
                } else {
                    Log.e(TAG, "Response NOT successful!");
                    showToast("✗ Failed to add to playlist");
                }
                Log.d(TAG, "========== ADD TO PLAYLIST RESPONSE END ==========");
            }

            @Override
            public void onFailure(Call<PlaylistModels.AddItemResponse> call, Throwable t) {
                Log.e(TAG, "========== ADD TO PLAYLIST FAILED ==========");
                Log.e(TAG, "Failure: " + t.getMessage(), t);
                showLoading(false);
                showToast("✗ Network error: " + t.getMessage());
                Log.d(TAG, "===========================================");
            }
        });
    }

    // Helper method to scroll to a specific playlist
    private void scrollToPlaylist(int playlistId) {
        if (playlistRecyclerView != null && adapter != null) {
            for (int i = 0; i < playlistItems.size(); i++) {
                if (playlistItems.get(i).id == playlistId) {
                    playlistRecyclerView.smoothScrollToPosition(i);
                    break;
                }
            }
        }
    }

    private void addSongToNewPlaylist(String playlistName) {
        Log.d(TAG, "Looking for newly created playlist: " + playlistName);

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "Access token is missing!");
            return;
        }

        String authHeader = "Bearer " + accessToken;

        Call<LibraryResponse> call = apiService.getUserPlaylists(authHeader);
        call.enqueue(new Callback<LibraryResponse>() {
            @Override
            public void onResponse(Call<LibraryResponse> call, Response<LibraryResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().getSections() != null && !response.body().getSections().isEmpty()) {

                    List<LibraryCategory> categories = response.body().getSections().get(0).getCategories();
                    if (categories != null) {
                        for (LibraryCategory category : categories) {
                            if (playlistName.equals(category.getCategoryName())) {
                                int playlistId = 0;
                                if (category.getCategoryId() != null) {
                                    String numericPart = category.getCategoryId().replaceAll("[^0-9]", "");
                                    if (!numericPart.isEmpty()) {
                                        try {
                                            playlistId = Integer.parseInt(numericPart);
                                            Log.d(TAG, "Found playlist: " + category.getCategoryName() +
                                                    " with playlistId: " + playlistId);
                                            addSongToPlaylistDirect(playlistId, category.getCategoryName());
                                        } catch (NumberFormatException e) {
                                            Log.e(TAG, "Failed to parse ID from: " + category.getCategoryId());
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to fetch updated playlists");
                }
            }

            @Override
            public void onFailure(Call<LibraryResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch updated playlists: " + t.getMessage());
            }
        });
    }

    private void addSongToPlaylistDirect(int playlistId, String playlistName) {
        Log.d(TAG, "Adding song to playlist - playlistId: " + playlistId + ", playlistName: " + playlistName);

        if (playlistId <= 0) {
            Log.e(TAG, "No valid playlistId for playlist");
            showToast("✗ Cannot add song - invalid playlist ID");
            return;
        }

        String authHeader = "Bearer " + accessToken;
        PlaylistModels.AddItemRequest request = new PlaylistModels.AddItemRequest(playlistId, songId);

        Call<PlaylistModels.AddItemResponse> call = apiService.addItemToPlaylist(authHeader, request);

        call.enqueue(new Callback<PlaylistModels.AddItemResponse>() {
            @Override
            public void onResponse(Call<PlaylistModels.AddItemResponse> call,
                                   Response<PlaylistModels.AddItemResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PlaylistModels.AddItemResponse body = response.body();
                    if (body.isAdded()) {
                        Log.d(TAG, "Song added successfully to playlist: " + playlistName);
                        showToast("✓ Added to playlist: " + playlistName);
                        broadcastPlaylistUpdate();
                    } else {
                        Log.e(TAG, "Failed to add song: " + body.getMessage());
                        showToast("✗ Failed: " + body.getMessage());
                    }
                } else {
                    Log.e(TAG, "Failed to add song to playlist: " + playlistName);
                    showToast("✗ Failed to add to playlist");
                }
            }

            @Override
            public void onFailure(Call<PlaylistModels.AddItemResponse> call, Throwable t) {
                Log.e(TAG, "Failed to add song: " + t.getMessage());
                showToast("✗ Network error: " + t.getMessage());
            }
        });
    }

    private void broadcastPlaylistUpdate() {
        Log.d(TAG, "Broadcasting playlist update");
        if (getContext() != null) {
            android.content.Intent intent = new android.content.Intent("PLAYLIST_UPDATED");
            intent.setPackage(getContext().getPackageName());
            getContext().sendBroadcast(intent);
            Log.d(TAG, "PLAYLIST_UPDATED broadcast sent");
        }
    }

    private void showLoading(boolean show) {
        Log.d(TAG, "showLoading: " + show);
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (createPlaylistButton != null) {
            createPlaylistButton.setEnabled(!show);
        }
        if (cancelButton != null) {
            cancelButton.setEnabled(!show);
        }
    }

    private void showToast(String message) {
        Log.d(TAG, "Showing toast: " + message);
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            Log.d(TAG, "Dialog window layout set");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
    }

    // Adapter for playlist selection
    class PlaylistSelectionAdapter extends RecyclerView.Adapter<PlaylistSelectionAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Log.d(TAG, "PlaylistSelectionAdapter: onCreateViewHolder");
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_playlist_selection, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PlaylistItem item = playlistItems.get(position);

            holder.playlistName.setText(item.name);

            // Check if song is already in this playlist
            boolean songExists = playlistsContainingSong.contains(item.id);

            // Highlight the recently added playlist
            boolean isRecentlyAdded = (lastAddedPlaylistId == item.id);

            if (songExists) {
                // Song already exists - show tick icon, hide plus icon
                holder.tickIcon.setVisibility(View.VISIBLE);
                holder.plusIcon.setVisibility(View.GONE);
                Log.d(TAG, "✓ TICK icon for: " + item.name);
            } else {
                // Song not in playlist - show plus icon, hide tick icon
                holder.tickIcon.setVisibility(View.GONE);
                holder.plusIcon.setVisibility(View.VISIBLE);
                Log.d(TAG, "✓ PLUS icon for: " + item.name);
            }

            // Apply highlight animation for recently added playlist
            if (isRecentlyAdded) {
                holder.itemView.animate()
                        .alpha(0.7f)
                        .setDuration(200)
                        .withEndAction(() -> {
                            holder.itemView.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .withEndAction(() -> {
                                        holder.itemView.setBackgroundColor(0);
                                    });
                        });
            } else {
                holder.itemView.setBackgroundColor(0);
            }

            // Load playlist image
            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Picasso.get().load(item.imageUrl)
                        .placeholder(R.drawable.video_placholder)
                        .error(R.drawable.video_placholder)
                        .into(holder.playlistImage);
            }

            // Set click listener
            holder.itemView.setOnClickListener(v -> {
                Log.d(TAG, "Playlist clicked: " + item.name);

                if (songExists) {
                    showToast("✓ \"" + songName + "\" is already in \"" + item.name + "\"");
                } else {
                    addSongToExistingPlaylist(item.id, item.name, item.categoryId);
                }
            });
        }

        @Override
        public int getItemCount() {
            return playlistItems.size();
        }

        public void updatePlaylistStatus() {
            Log.d(TAG, "Updating playlist status");
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView playlistImage;
            TextView playlistName;
            ImageView plusIcon;
            ImageView tickIcon;

            ViewHolder(View itemView) {
                super(itemView);
                playlistImage = itemView.findViewById(R.id.playlistImage);
                playlistName = itemView.findViewById(R.id.playlistName);
                plusIcon = itemView.findViewById(R.id.plusIcon);
                tickIcon = itemView.findViewById(R.id.tickIcon);
            }
        }
    }

    static class PlaylistItem {
        int id;
        String name;
        String imageUrl;
        String categoryId;
    }
}