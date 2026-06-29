package com.Saalai.SalaiMusicApp.Fragments;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.AudioAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Models.AddToPlaylistRequest;
import com.Saalai.SalaiMusicApp.Models.ArtistCategory;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Models.PlaylistModel;
import com.Saalai.SalaiMusicApp.Models.PlaylistSection;
import com.Saalai.SalaiMusicApp.Models.PlaylistStatusResponse;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.QRCodeBottomSheet;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.AddToPlaylistResponse;
import com.Saalai.SalaiMusicApp.Response.FollowRequest;
import com.Saalai.SalaiMusicApp.Response.FollowResponse;
import com.Saalai.SalaiMusicApp.Response.FollowStatusResponse;
import com.Saalai.SalaiMusicApp.Response.LikeRequest;
import com.Saalai.SalaiMusicApp.Response.LikeResponse;
import com.Saalai.SalaiMusicApp.Response.LikeStatusResponse;
import com.Saalai.SalaiMusicApp.Response.MyPlaylistResponse;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;
import com.Saalai.SalaiMusicApp.Utils.DownloadPlaylistManager;
import com.Saalai.SalaiMusicApp.Utils.AudioDownloadManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AudioFragment extends Fragment {

    private static final String TAG = "AudioFragment";

    private RecyclerView recyclerView;
    private AudioAdapter audioAdapter;
    private ArrayList<AudioModel> audioList;
    private String artistName, artistImageUrl;

    private ImageView imgAlbumArt, btnFullPlayPause, backbtn, btnDownloadPlaylist, btnAddToPlaylist, btnDownloadedIndicator, backtool;
    private TextView tvFullPlayerSongName, tvFullPlayerSongArtist;

    // Managers
    private DownloadPlaylistManager downloadPlaylistManager;
    private AudioDownloadManager audioDownloadManager;

    // Track download status
    private boolean isPlaylistDownloaded = false;
    private String downloadedPlaylistId = null;

    // Auto-play tracking
    private boolean hasAutoPlayed = false;

    private TextView btnSearch;

    private NestedScrollView nestedScrollView;
    private FrameLayout collapsibleImageContainer;
    private int imageHeight = 0;
    private int maxImageSize = 0;
    private int minImageSize = 0;
    private boolean isAnimating = false;
    private ValueAnimator currentAnimator;
    private static final int SCROLL_THRESHOLD = 5;

    private LinearLayout toolbar;
    private ImageView toolbarAlbumArt;
    private FloatingActionButton toolbarPlayPause;
    private TextView toolbarSongName, toolbarSongArtist;
    private View toolbarLayout;
    private boolean isToolbarVisible = false;
    private static final float HIDE_THRESHOLD = 0.95f;

    private ConstraintLayout linearLayout3;

    private int imageGradientColor = -1;
    private int toolbarGradientColor = -1;
    private boolean isImageVisible = true;

    private CoordinatorLayout mainlayout;

    private static final String ACTION_PREPARE_NEXT = "PREPARE_NEXT";
    private static final String ACTION_PREPARE_PREVIOUS = "PREPARE_PREVIOUS";

    // Debounce timers for prepare receiver
    private long lastNextTime = 0;
    private long lastPrevTime = 0;
    private static final long DEBOUNCE_DELAY = 1000; // 1 second

    private ImageView btnlikes;
    private ApiService apiService;
    private String accessToken;


    private boolean isCurrentlyLiked = false;
    private String currentCategoryId = null;

    private boolean isAddedToPlaylist = false;




    private TextView btnFollow;
    private TextView btnFollowing;
    private boolean isFollowingArtist = false;
    private String currentArtistId;

    private final BroadcastReceiver songChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("SONG_CHANGED".equals(intent.getAction()) && audioAdapter != null) {
                requireActivity().runOnUiThread(() -> {
                    audioAdapter.clearPreparingProgress();
                    audioAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Song changed - cleared preparing progress");
                });
            }
        }
    };

    private final BroadcastReceiver prepareReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (audioAdapter == null || getActivity() == null) return;

            String action = intent.getAction();
            long currentTime = System.currentTimeMillis();

            getActivity().runOnUiThread(() -> {
                if (ACTION_PREPARE_NEXT.equals(action)) {
                    // Debounce next calls
                    if (currentTime - lastNextTime < DEBOUNCE_DELAY) {
                        Log.d(TAG, "Ignoring duplicate next broadcast");
                        return;
                    }
                    lastNextTime = currentTime;

                    int nextIndex = intent.getIntExtra("nextIndex", -1);
                    if (nextIndex != -1 && nextIndex < audioList.size()) {
                        // Show progress on next item
                        audioAdapter.showNextItemProgress(nextIndex);
                        Log.d(TAG, "Received prepare next for index: " + nextIndex);
                    }
                }
                else if (ACTION_PREPARE_PREVIOUS.equals(action)) {
                    // Debounce previous calls
                    if (currentTime - lastPrevTime < DEBOUNCE_DELAY) {
                        Log.d(TAG, "Ignoring duplicate previous broadcast");
                        return;
                    }
                    lastPrevTime = currentTime;

                    int prevIndex = intent.getIntExtra("prevIndex", -1);
                    if (prevIndex != -1 && prevIndex < audioList.size()) {
                        // Show progress on previous item
                        audioAdapter.showPreviousItemProgress(prevIndex);
                        Log.d(TAG, "Received prepare previous for index: " + prevIndex);
                    }
                }
            });
        }
    };

    private final BroadcastReceiver playerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Player broadcast received: " + action);

            if (action != null) {
                switch (action) {
                    case "UPDATE_AUDIO_ADAPTER":
                    case "SONG_CHANGED":
                        // Update adapter when song changes
                        requireActivity().runOnUiThread(() -> {
                            if (audioAdapter != null) {
                                audioAdapter.clearPreparingProgress();
                                audioAdapter.notifyDataSetChanged();
                            }
                            updatePlayPauseButtonUI();
                            updateToolbarContent();
                            Log.d(TAG, "UI updated due to player change");
                        });
                        break;
                }
            }
        }
    };

    private boolean isReceiverRegistered = false;

    private final BroadcastReceiver audioUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("UPDATE_AUDIO_ADAPTER".equals(intent.getAction()) && audioAdapter != null) {
                audioAdapter.notifyDataSetChanged();
            }
        }
    };

    private final BroadcastReceiver miniPlayerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("UPDATE_MINI_PLAYER".equals(intent.getAction())) {
                AudioModel currentAudio = PlayerManager.getCurrentAudio();
                updateFullPlayerUI(currentAudio);
                updatePlayPauseButtonUI();
            }
        }
    };

    private final BroadcastReceiver downloadUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("DOWNLOAD_UPDATE".equals(intent.getAction())) {
                checkPlaylistDownloadStatus();
                if (audioAdapter != null) {
                    audioAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        downloadPlaylistManager = DownloadPlaylistManager.getInstance(requireContext());
        audioDownloadManager = new AudioDownloadManager(requireContext());

        if (getArguments() != null) {
            artistName = getArguments().getString("artist_name");
            artistImageUrl = getArguments().getString("artist_image");

            ArrayList<AudioModel> receivedSongs = (ArrayList<AudioModel>) getArguments().getSerializable("songs_list");

            if (receivedSongs != null) {
                audioList = receivedSongs;
            } else {
                audioList = new ArrayList<>();
            }
        } else {
            audioList = new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio, container, false);

        PlayerManager.init(getContext());

        recyclerView = view.findViewById(R.id.audioRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        imgAlbumArt = view.findViewById(R.id.imgAlbumArt);
        tvFullPlayerSongName = view.findViewById(R.id.tvFullPlayerSongName);
        tvFullPlayerSongArtist = view.findViewById(R.id.tvFullPlayerSongArtist);
        btnFullPlayPause = view.findViewById(R.id.btnFullPlayPause);

        backbtn = view.findViewById(R.id.back);
        backtool = view.findViewById(R.id.backtool);

        if (backbtn != null) {
            backbtn.setVisibility(View.VISIBLE);
            backbtn.bringToFront();
            Log.d(TAG, "Back button found and set visible");
        } else {
            Log.e(TAG, "Back button not found!");
        }

        mainlayout = view.findViewById(R.id.mainlayout);

        btnDownloadPlaylist = view.findViewById(R.id.btndownloadplaylist);
        btnDownloadedIndicator = view.findViewById(R.id.btnDownloadedIndicator);
        btnAddToPlaylist = view.findViewById(R.id.btnplaylist);

        toolbarLayout = view.findViewById(R.id.appBarLayout);
        toolbar = view.findViewById(R.id.toolbar);
        toolbarAlbumArt = view.findViewById(R.id.toolbarAlbumArt);
        toolbarSongName = view.findViewById(R.id.toolbarSongName);
        toolbarSongArtist = view.findViewById(R.id.toolbarSongArtist);
        toolbarPlayPause = view.findViewById(R.id.toolbarPlayPause);
        linearLayout3 = view.findViewById(R.id.linearLayout3);
        btnlikes = view.findViewById(R.id.btnlikes);

        setupToolbar();

        if (backbtn != null) {
            backbtn.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        if (backtool != null) {
            backtool.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        checkPlaylistDownloadStatus();

        if (btnDownloadPlaylist != null) {
            btnDownloadPlaylist.setOnClickListener(v -> showDownloadPlaylistDialog());
        }

        if (btnDownloadedIndicator != null) {
            btnDownloadedIndicator.setOnClickListener(v -> {
                if (downloadedPlaylistId != null) {
                    Toast.makeText(getContext(),
                            "Playlist already downloaded! Go to 'My Playlists' to view.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }


        if (artistName != null && getActivity() != null) {
            getActivity().setTitle(artistName + " - Songs");
        }

        audioAdapter = new AudioAdapter(audioList, getContext(), (audioModel, progressBar) -> {
            // The adapter already handles playing and progress UI.
            // We just need to update the fragment's UI when a song is clicked.
            updateFullPlayerUI(audioModel);
            Toast.makeText(getContext(), "Playing: " + audioModel.getAudioName(), Toast.LENGTH_SHORT).show();
        });

        // Set the RecyclerView reference
        audioAdapter.setRecyclerView(recyclerView);

        recyclerView.setAdapter(audioAdapter);

        setupFullPlayPauseButton();

        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio != null) {
            updateFullPlayerUI(currentAudio);
            updatePlayPauseButtonUI();
        }

        ImageView btnMenu = view.findViewById(R.id.btnmenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                String categoryId = getOriginalCategoryId();
                if (categoryId == null || categoryId.isEmpty()) {
                    Toast.makeText(getContext(),
                            "Cannot generate QR code: Playlist ID not found",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Ensure the ID has the "cat_" prefix for the scanner
                String prefixedId = categoryId.startsWith("cat_") ? categoryId : "cat_" + categoryId;

                QRCodeBottomSheet bottomSheet = QRCodeBottomSheet.newInstance(
                        artistName != null ? artistName : "Discover Weekly",
                        prefixedId,
                        artistImageUrl
                );

                bottomSheet.show(getParentFragmentManager(), "QRCodeBottomSheet");
            });
        }

        nestedScrollView = view.findViewById(R.id.nestedScrollView);
        collapsibleImageContainer = view.findViewById(R.id.collapsibleImageContainer);

        collapsibleImageContainer.post(new Runnable() {
            @Override
            public void run() {
                imageHeight = collapsibleImageContainer.getHeight();
            }
        });

        setupScrollListener();
        setupLinearLayout3Listener();
        updateToolbarContent();

        if (toolbarLayout != null) {
            toolbarLayout.setVisibility(View.INVISIBLE);
        }

        btnSearch = view.findViewById(R.id.searchEditText);

        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                openSearchFragment();
            });
        }

        currentCategoryId = getPlaylistCategoryId();
        initializeApiServiceForLikesandaddtoplaylist();



        initializeFollowButton(view);

        // Set click listener for like button
        if (btnlikes != null) {
            btnlikes.setOnClickListener(v -> toggleLike());
        }

        // Set click listener for add to playlist button
        if (btnAddToPlaylist != null) {
            btnAddToPlaylist.setOnClickListener(v -> toggleAddToPlaylist());
        }

        return view;
    }


    // Add these methods right after your existing methods (before onResume)
    private void initializeApiServiceForLikesandaddtoplaylist() {
        if (ApiClient.isInitialized()) {
            apiService = ApiClient.getClient().create(ApiService.class);
            SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
            accessToken = sp.getAccessToken();

            // Check like status and playlist status after initialization
            if (currentCategoryId != null && !currentCategoryId.isEmpty()) {
                checkLikeStatus(currentCategoryId);
                checkPlaylistItemStatus(currentCategoryId); // Add this line
            }
        } else {
            ApiClient.initialize(new ApiClient.ApiClientCallback() {
                @Override
                public void onUrlLoaded(String baseUrl) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            apiService = ApiClient.getClient().create(ApiService.class);
                            SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
                            accessToken = sp.getAccessToken();

                            if (currentCategoryId != null && !currentCategoryId.isEmpty()) {
                                checkLikeStatus(currentCategoryId);
                                checkPlaylistItemStatus(currentCategoryId); // Add this line
                            }
                        });
                    }
                }

                @Override
                public void onAllUrlsFailed(String error) {
                    Log.e("AudioFragment", "API initialization failed for likes: " + error);
                }

                @Override
                public void onNoUrlsAvailable() {
                    Log.e("AudioFragment", "No server URLs available for likes");
                }
            });
        }
    }

    private void checkLikeStatus(String categoryId) {
        if (apiService == null) return;
        
        // Refresh token from SharedPreferences
        accessToken = SharedPrefManager.getInstance(getContext()).getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            Log.d("AudioFragment", "Cannot check like status: Access token is missing");
            return;
        }

        String authHeader = "Bearer " + accessToken;

        Call<LikeStatusResponse> call = apiService.getCategoryLikeStatus(authHeader, categoryId);

        call.enqueue(new Callback<LikeStatusResponse>() {
            @Override
            public void onResponse(Call<LikeStatusResponse> call, Response<LikeStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LikeStatusResponse statusResponse = response.body();
                    isCurrentlyLiked = statusResponse.isLiked();
                    updateLikeButtonUI();
                    Log.d("AudioFragment", "Like status for category " + categoryId + ": " + isCurrentlyLiked);
                } else {
                    Log.e("AudioFragment", "❌ Failed to get like status: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e("AudioFragment", "Error Body: " + response.errorBody().string());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<LikeStatusResponse> call, Throwable t) {
                Log.e("AudioFragment", "Network error checking like status: " + t.getMessage());
            }
        });
    }

    private void toggleLike() {
        if (apiService == null) return;
        
        accessToken = SharedPrefManager.getInstance(getContext()).getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(getContext(), "Please login to like playlists", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentCategoryId == null || currentCategoryId.isEmpty()) {
            Toast.makeText(getContext(), "Cannot like this playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        String authHeader = "Bearer " + accessToken;
        LikeRequest request = new LikeRequest(currentCategoryId);

        // Show loading state
        if (btnlikes != null) {
            btnlikes.setEnabled(false);
            btnlikes.setAlpha(0.5f);
        }

        Call<LikeResponse> call = apiService.toggleCategoryLike(authHeader, request);

        call.enqueue(new Callback<LikeResponse>() {
            @Override
            public void onResponse(Call<LikeResponse> call, Response<LikeResponse> response) {
                // Re-enable button
                if (btnlikes != null) {
                    btnlikes.setEnabled(true);
                    btnlikes.setAlpha(1f);
                }

                if (response.isSuccessful() && response.body() != null) {
                    LikeResponse likeResponse = response.body();
                    isCurrentlyLiked = likeResponse.isLiked();
                    updateLikeButtonUI();

                    String message = isCurrentlyLiked ?
                            "Added to your liked playlists" :
                            "Removed from your liked playlists";
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

                    // Broadcast that likes have been updated
                    broadcastLikesUpdate();

                    Log.d("AudioFragment", "Toggle like response: " + likeResponse.getMessage());
                } else {
                    Log.e("AudioFragment", "❌ Toggle Like Error: " + response.code());
                    String errorMsg = "Failed to update like status";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                            Log.e("AudioFragment", "Error Body: " + errorMsg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LikeResponse> call, Throwable t) {
                // Re-enable button
                if (btnlikes != null) {
                    btnlikes.setEnabled(true);
                    btnlikes.setAlpha(1f);
                }

                Log.e("AudioFragment", "Network error toggling like: " + t.getMessage());
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLikeButtonUI() {
        if (btnlikes != null && isAdded()) {
            requireActivity().runOnUiThread(() -> {
                if (btnlikes != null && isAdded()) {
                    if (isCurrentlyLiked) {
                        btnlikes.setImageResource(R.drawable.ic_heart_filled);
                        btnlikes.setColorFilter(getResources().getColor(R.color.bgred));
                    } else {
                        btnlikes.setImageResource(R.drawable.ic_heart_outline);
                        btnlikes.setColorFilter(getResources().getColor(R.color.white));
                    }
                }
            });
        }
    }

    private void broadcastLikesUpdate() {
        if (getContext() != null) {
            Intent intent = new Intent("LIKES_UPDATED");
            intent.setPackage(getContext().getPackageName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getContext().sendBroadcast(intent, null);
            } else {
                getContext().sendBroadcast(intent);
            }
        }
    }


    // Add these methods after your existing like methods

    private void checkPlaylistItemStatus(String categoryId) {
        if (apiService == null) return;
        
        accessToken = SharedPrefManager.getInstance(getContext()).getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            Log.d("AudioFragment", "Cannot check playlist status: Access token is missing");
            return;
        }

        String authHeader = "Bearer " + accessToken;

        // Fetch the user's playlist to check if this category exists
        Call<MyPlaylistResponse> call = apiService.getMyPlaylist(authHeader);

        call.enqueue(new Callback<MyPlaylistResponse>() {
            @Override
            public void onResponse(Call<MyPlaylistResponse> call, Response<MyPlaylistResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MyPlaylistResponse playlistResponse = response.body();
                    List<PlaylistSection> sections = playlistResponse.getSections();

                    // Check if the current category exists in any section
                    isAddedToPlaylist = isCategoryInPlaylist(sections, categoryId);
                    updateAddToPlaylistButtonUI();
                    Log.d("AudioFragment", "Playlist status for category " + categoryId + ": " + isAddedToPlaylist);
                } else {
                    Log.e("AudioFragment", "❌ Failed to get playlist: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e("AudioFragment", "Error Body: " + response.errorBody().string());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isAddedToPlaylist = false;
                    updateAddToPlaylistButtonUI();
                }
            }

            @Override
            public void onFailure(Call<MyPlaylistResponse> call, Throwable t) {
                Log.e("AudioFragment", "Network error checking playlist: " + t.getMessage());
                isAddedToPlaylist = false;
                updateAddToPlaylistButtonUI();
            }
        });
    }

    private boolean isCategoryInPlaylist(List<PlaylistSection> sections, String categoryId) {
        if (sections == null || sections.isEmpty() || categoryId == null) {
            return false;
        }

        for (PlaylistSection section : sections) {
            if (section.getArtists() != null) {
                for (ArtistCategory artist : section.getArtists()) {
                    if (categoryId.equals(artist.getCategoryId())) {
                        Log.d("AudioFragment", "Found category " + categoryId + " in playlist section: " + section.getSectionName());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void toggleAddToPlaylist() {
        if (apiService == null) return;
        
        accessToken = SharedPrefManager.getInstance(getContext()).getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(getContext(), "Please login to add to playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentCategoryId == null || currentCategoryId.isEmpty()) {
            Toast.makeText(getContext(), "Cannot add this playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        String authHeader = "Bearer " + accessToken;
        AddToPlaylistRequest request = new AddToPlaylistRequest(currentCategoryId);

        if (btnAddToPlaylist != null) {
            btnAddToPlaylist.setEnabled(false);
            btnAddToPlaylist.setAlpha(0.5f);
        }

        Call<AddToPlaylistResponse> call = apiService.addToMyPlaylist(authHeader, request);

        call.enqueue(new Callback<AddToPlaylistResponse>() {
            @Override
            public void onResponse(Call<AddToPlaylistResponse> call, Response<AddToPlaylistResponse> response) {
                if (btnAddToPlaylist != null) {
                    btnAddToPlaylist.setEnabled(true);
                    btnAddToPlaylist.setAlpha(1f);
                }

                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().getMessage();
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

                    // Refresh the playlist status to get the actual state
                    checkPlaylistItemStatus(currentCategoryId);

                    // Broadcast that playlist has been updated
                    broadcastPlaylistUpdate();

                    Log.d("AudioFragment", "Toggle playlist response: " + message);
                } else {
                    Log.e("AudioFragment", "❌ Toggle Playlist Error: " + response.code());
                    String errorMsg = "Failed to update playlist";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                            Log.e("AudioFragment", "Error Body: " + errorMsg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AddToPlaylistResponse> call, Throwable t) {
                if (btnAddToPlaylist != null) {
                    btnAddToPlaylist.setEnabled(true);
                    btnAddToPlaylist.setAlpha(1f);
                }

                Log.e("AudioFragment", "Network error toggling playlist: " + t.getMessage());
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAddToPlaylistButtonUI() {
        if (btnAddToPlaylist != null && isAdded()) {
            requireActivity().runOnUiThread(() -> {
                if (btnAddToPlaylist != null && isAdded()) {
                    if (isAddedToPlaylist) {
                        btnAddToPlaylist.setImageResource(R.drawable.correct_converted_from_png);
                    } else {
                        btnAddToPlaylist.setImageResource(R.drawable.plusenew);
                    }
                }
            });
        }
    }

    private void broadcastPlaylistUpdate() {
        if (getContext() != null) {
            Intent intent = new Intent("PLAYLIST_UPDATED");
            intent.setPackage(getContext().getPackageName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getContext().sendBroadcast(intent, null);
            } else {
                getContext().sendBroadcast(intent);
            }
        }
    }





    private void initializeFollowButton(View view) {
        Log.d(TAG, "initializeFollowButton: Starting initialization");

        btnFollow = view.findViewById(R.id.btnFollow);
        btnFollowing = view.findViewById(R.id.btnFollowing);

        if (btnFollow == null || btnFollowing == null) {
            Log.e(TAG, "initializeFollowButton: Buttons not found in layout");
            return;
        }

        Log.d(TAG, "initializeFollowButton: Buttons found successfully");

        // Check if opened from artist
        Bundle arguments = getArguments();
        if (arguments != null) {
            boolean fromArtist = arguments.getBoolean("from_artist", false);
            Log.d(TAG, "initializeFollowButton: from_artist = " + fromArtist);

            if (fromArtist) {
                // Get artist ID from arguments
                currentArtistId = arguments.getString("artist_id");
                artistName = arguments.getString("artist_name");
                Log.d(TAG, "initializeFollowButton: artist_id = " + currentArtistId);
                Log.d(TAG, "initializeFollowButton: artist_name = " + artistName);

                // Get artist ID from first song if not provided
                if ((currentArtistId == null || currentArtistId.isEmpty()) &&
                        audioList != null && !audioList.isEmpty() && audioList.get(0) != null) {
                    currentArtistId = audioList.get(0).getCategoryId();
                    Log.d(TAG, "initializeFollowButton: Retrieved artist_id from first song = " + currentArtistId);
                }

                // Check follow status if we have artist ID
                if (currentArtistId != null && !currentArtistId.isEmpty()) {
                    Log.d(TAG, "initializeFollowButton: Valid artist ID, checking follow status");
                    checkFollowStatus(currentArtistId);
                } else {
                    Log.w(TAG, "initializeFollowButton: No valid artist ID found - hiding follow buttons");
                    btnFollow.setVisibility(View.GONE);
                    btnFollowing.setVisibility(View.GONE);
                }

                // Set click listeners
                btnFollow.setOnClickListener(v -> {
                    Log.d(TAG, "Follow button clicked");
                    toggleFollowArtist();
                });

                btnFollowing.setOnClickListener(v -> {
                    Log.d(TAG, "Following button clicked");
                    toggleFollowArtist();
                });

                Log.d(TAG, "initializeFollowButton: Click listeners set on both buttons");

            } else {
                // Hide both buttons when not from artist
                btnFollow.setVisibility(View.GONE);
                btnFollowing.setVisibility(View.GONE);
                Log.d(TAG, "initializeFollowButton: Hiding both buttons - not opened from artist");
            }
        } else {
            Log.w(TAG, "initializeFollowButton: getArguments() is null");
            btnFollow.setVisibility(View.GONE);
            btnFollowing.setVisibility(View.GONE);
        }
    }

    // Update Follow button UI - shows one button, hides the other
    private void updateFollowButtonUI() {
        Log.d(TAG, "updateFollowButtonUI: Called - isFollowingArtist = " + isFollowingArtist);

        if (btnFollow == null || btnFollowing == null) {
            Log.e(TAG, "updateFollowButtonUI: Buttons are null");
            return;
        }

        if (!isAdded()) {
            Log.w(TAG, "updateFollowButtonUI: Fragment not added to activity, skipping UI update");
            return;
        }

        requireActivity().runOnUiThread(() -> {
            if (btnFollow != null && btnFollowing != null && isAdded()) {
                if (isFollowingArtist) {
                    // Show Following button, hide Follow button
                    btnFollow.setVisibility(View.GONE);
                    btnFollowing.setVisibility(View.VISIBLE);
                    Log.d(TAG, "updateFollowButtonUI: Showing 'Following' button, hiding 'Follow' button");
                } else {
                    // Show Follow button, hide Following button
                    btnFollow.setVisibility(View.VISIBLE);
                    btnFollowing.setVisibility(View.GONE);
                    Log.d(TAG, "updateFollowButtonUI: Showing 'Follow' button, hiding 'Following' button");
                }
            } else {
                Log.w(TAG, "updateFollowButtonUI: Button state invalid - btnFollow=" +
                        (btnFollow != null) + ", btnFollowing=" + (btnFollowing != null) +
                        ", isAdded=" + isAdded());
            }
        });
    }

    // Check if user is following the artist
    private void checkFollowStatus(String artistId) {
        Log.d(TAG, "checkFollowStatus: Called with artistId = " + artistId);

        if (apiService == null) {
            Log.e(TAG, "checkFollowStatus: apiService is null");
            return;
        }

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "checkFollowStatus: accessToken is null or empty");
            SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
            if (sp != null) {
                accessToken = sp.getAccessToken();
                Log.d(TAG, "checkFollowStatus: Retrieved accessToken from SharedPref = " +
                        (accessToken != null ? "exists (length: " + accessToken.length() + ")" : "null"));
            }
            if (accessToken == null || accessToken.isEmpty()) {
                Log.e(TAG, "checkFollowStatus: Still no accessToken available");
                return;
            }
        }

        String authHeader = accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;

        Call<FollowStatusResponse> call = apiService.getFollowStatus(authHeader, artistId);

        call.enqueue(new Callback<FollowStatusResponse>() {
            @Override
            public void onResponse(Call<FollowStatusResponse> call, Response<FollowStatusResponse> response) {
                Log.d(TAG, "checkFollowStatus - onResponse: Response code = " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    FollowStatusResponse statusResponse = response.body();
                    isFollowingArtist = statusResponse.isFollowing();
                    Log.d(TAG, "checkFollowStatus - onResponse: SUCCESS - isFollowing = " + isFollowingArtist);

                    updateFollowButtonUI();
                    Log.d(TAG, "checkFollowStatus - onResponse: Follow button UI updated");
                } else {
                    Log.e(TAG, "checkFollowStatus - onResponse: Failed - Response code: " + response.code());
                    isFollowingArtist = false;
                    updateFollowButtonUI();
                }
            }

            @Override
            public void onFailure(Call<FollowStatusResponse> call, Throwable t) {
                Log.e(TAG, "checkFollowStatus - onFailure: Network error", t);
                isFollowingArtist = false;
                updateFollowButtonUI();
            }
        });
    }

    // Toggle follow/unfollow artist
    private void toggleFollowArtist() {
        Log.d(TAG, "toggleFollowArtist: Called - Current isFollowingArtist = " + isFollowingArtist);

        if (apiService == null) {
            Log.e(TAG, "toggleFollowArtist: apiService is null");
            Toast.makeText(getContext(), "API service not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "toggleFollowArtist: accessToken is null or empty");
            SharedPrefManager sp = SharedPrefManager.getInstance(getContext());
            if (sp != null) {
                accessToken = sp.getAccessToken();
            }
            if (accessToken == null || accessToken.isEmpty()) {
                Log.e(TAG, "toggleFollowArtist: No access token available");
                Toast.makeText(getContext(), "Please login to follow artists", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (currentArtistId == null || currentArtistId.isEmpty()) {
            Log.e(TAG, "toggleFollowArtist: currentArtistId is null or empty");
            Toast.makeText(getContext(), "Cannot follow this artist", Toast.LENGTH_SHORT).show();
            return;
        }

        String authHeader = accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;
        FollowRequest request = new FollowRequest(currentArtistId);

        // Disable both buttons while processing
        if (btnFollow != null && btnFollowing != null) {
            btnFollow.setEnabled(false);
            btnFollowing.setEnabled(false);
            btnFollow.setAlpha(0.5f);
            btnFollowing.setAlpha(0.5f);
            Log.d(TAG, "toggleFollowArtist: Both buttons disabled during API call");
        }

        Call<FollowResponse> call = apiService.toggleFollowArtist(authHeader, request);

        call.enqueue(new Callback<FollowResponse>() {
            @Override
            public void onResponse(Call<FollowResponse> call, Response<FollowResponse> response) {
                Log.d(TAG, "toggleFollowArtist - onResponse: Response code = " + response.code());

                // Re-enable both buttons
                if (btnFollow != null && btnFollowing != null && isAdded()) {
                    btnFollow.setEnabled(true);
                    btnFollowing.setEnabled(true);
                    btnFollow.setAlpha(1f);
                    btnFollowing.setAlpha(1f);
                    Log.d(TAG, "toggleFollowArtist - onResponse: Both buttons re-enabled");
                }

                if (response.isSuccessful() && response.body() != null) {
                    FollowResponse followResponse = response.body();
                    isFollowingArtist = followResponse.isFollowing();
                    Log.d(TAG, "toggleFollowArtist - onResponse: New isFollowing = " + isFollowingArtist);

                    updateFollowButtonUI();

                    String message = isFollowingArtist ?
                            "Now following " + (artistName != null ? artistName : "artist") :
                            "Unfollowed " + (artistName != null ? artistName : "artist");
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

                    // Broadcast follow update
                    broadcastFollowUpdate();
                } else {
                    Log.e(TAG, "toggleFollowArtist - onResponse: Failed");
                    Toast.makeText(getContext(), "Failed to update follow status", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FollowResponse> call, Throwable t) {
                Log.e(TAG, "toggleFollowArtist - onFailure: Network error", t);

                // Re-enable both buttons
                if (btnFollow != null && btnFollowing != null && isAdded()) {
                    btnFollow.setEnabled(true);
                    btnFollowing.setEnabled(true);
                    btnFollow.setAlpha(1f);
                    btnFollowing.setAlpha(1f);
                    Log.d(TAG, "toggleFollowArtist - onFailure: Both buttons re-enabled");
                }

                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Broadcast follow updates
    private void broadcastFollowUpdate() {
        Log.d(TAG, "broadcastFollowUpdate: Called");

        if (getContext() != null) {
            Intent intent = new Intent("FOLLOW_UPDATED");
            intent.setPackage(getContext().getPackageName());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getContext().sendBroadcast(intent, null);
            } else {
                getContext().sendBroadcast(intent);
            }
        }
    }




    private void openSearchFragment() {
        if (audioList == null || audioList.isEmpty()) {
            Toast.makeText(getContext(), "No songs to search", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<AudioModel> searchableList = new ArrayList<>();
        for (AudioModel song : audioList) {
            AudioModel copy = new AudioModel(
                    song.getSongId(),
                    song.getAudioName(),
                    song.getAudioUrl(),
                    song.getcategoryName(),
                    song.getImageUrl()
            );
            copy.setCategoryId(song.getCategoryId());
            copy.setDownloaded(song.isDownloaded());
            copy.setDownloadPath(song.getDownloadPath());
            searchableList.add(copy);
        }

        SearchFragment searchFragment = SearchFragment.newInstance(
                searchableList,
                artistName != null ? artistName : "Playlist",
                artistImageUrl
        );

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, searchFragment)
                .addToBackStack(null)
                .commit();
    }

    private void setupToolbar() {
        toolbar.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        if (toolbarPlayPause != null) {
            toolbarPlayPause.setOnClickListener(v -> {
                handlePlayPauseClick();
            });
        }


        if (artistImageUrl != null && !artistImageUrl.isEmpty()) {
            Picasso.get()
                    .load(artistImageUrl)
                    .placeholder(R.drawable.video_placholder)
                    .into(imgAlbumArt, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            imgAlbumArt.setDrawingCacheEnabled(true);
                            imgAlbumArt.buildDrawingCache(true);
                            Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) imgAlbumArt.getDrawable()).getBitmap();
                            if (bitmap != null) {
                                generateDynamicBackground(bitmap);
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            imgAlbumArt.setImageResource(R.drawable.video_placholder);
                            setDefaultDarkBackground();
                        }
                    });
        } else {
            imgAlbumArt.setImageResource(R.drawable.video_placholder);
            setDefaultDarkBackground();
        }
    }

    private void handlePlayPauseClick() {
        boolean isPlaylistPlaying = isAnySongFromThisPlaylistPlaying();

        if (isPlaylistPlaying) {
            if (PlayerManager.isPlaying()) {
                PlayerManager.pausePlayback();
                updatePlayPauseButtons(R.drawable.play_player);
                Log.d("AudioFragment", "Playlist paused");
            } else {
                PlayerManager.resumePlayback();
                updatePlayPauseButtons(R.drawable.pause);
                Log.d("AudioFragment", "Playlist resumed");
            }
        } else {
            if (audioList != null && !audioList.isEmpty()) {
                AudioModel firstSong = audioList.get(0);
                saveFullPlaylistToPrefs();
                PlayerManager.setAudioList(audioList, PlayerManager.PlaylistType.ONLINE);
                PlayerManager.playAudio(firstSong, () -> {
                    if (isAdded()) {
                        updatePlayPauseButtons(R.drawable.pause);
                        updateFullPlayerUI(firstSong);
                        Toast.makeText(getContext(),
                                "Playing: " + firstSong.getAudioName(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
                Log.d("AudioFragment", "Started playing this playlist from index 0");
            } else {
                Toast.makeText(getContext(), "No songs to play", Toast.LENGTH_SHORT).show();
            }
        }
        broadcastUpdate();
    }

    private void updatePlayPauseButtons(int iconResource) {
        if (btnFullPlayPause != null) {
            btnFullPlayPause.setImageResource(iconResource);
        }
        if (toolbarPlayPause != null) {
            toolbarPlayPause.setImageResource(iconResource);
        }
    }

    private void setupLinearLayout3Listener() {
        if (linearLayout3 != null) {
            linearLayout3.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {}

                @Override
                public void onViewDetachedFromWindow(View v) {
                    if (toolbarPlayPause != null && isToolbarVisible) {
                        toolbarPlayPause.show();
                    }
                }
            });
        }
    }

    private void setupScrollListener() {
        nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY,
                                       int oldScrollX, int oldScrollY) {

                if (maxImageSize == 0) {
                    // Get the fixed size from dimens
                    maxImageSize = (int) getResources().getDimension(R.dimen.dp_260);
                    minImageSize = (int) (maxImageSize * 0.4f);
                }

                if (scrollY != oldScrollY) {
                    float scrollProgress = Math.min(1f, (float) Math.abs(scrollY) / 500);
                    applySmoothSizeReduction(scrollProgress, scrollY);
                    handleControlButtonsVisibility(scrollY, oldScrollY);
                    updateFABVisibility();
                }
            }
        });
    }

    private void handleControlButtonsVisibility(int scrollY, int oldScrollY) {
        if (linearLayout3 == null) return;

        int scrollThreshold = 760;

        if (scrollY > oldScrollY && scrollY > scrollThreshold) {
            if (linearLayout3.getVisibility() == View.VISIBLE) {
                animateControlButtons(false);
                Log.d(TAG, "Animating control buttons hidden - scrolling down");
            }
        } else if (scrollY < oldScrollY && scrollY < scrollThreshold) {
            if (linearLayout3.getVisibility() != View.VISIBLE) {
                animateControlButtons(true);
                Log.d(TAG, "Animating control buttons shown - scrolling up");
            }
        }

        View contentView = nestedScrollView.getChildAt(0);
        if (contentView != null) {
            int contentHeight = contentView.getHeight();
            int scrollViewHeight = nestedScrollView.getHeight();

            if (scrollY + scrollViewHeight >= contentHeight - 100) {
                if (linearLayout3.getVisibility() == View.VISIBLE) {
                    animateControlButtons(false);
                    Log.d(TAG, "Animating control buttons hidden - at bottom");
                }
            }
        }
    }

    private void animateControlButtons(boolean show) {
        if (linearLayout3 == null) return;

        // Cancel any ongoing animations
        linearLayout3.animate().cancel();

        if (show) {
            // Show instantly without animation
            linearLayout3.setVisibility(View.VISIBLE);
            linearLayout3.setAlpha(1f);
            linearLayout3.setTranslationY(0f);
            Log.d(TAG, "Controls shown without animation");
            updateFABVisibility();
        } else {
            // Hide instantly without animation
            linearLayout3.setVisibility(View.INVISIBLE);
            linearLayout3.setAlpha(0f);
            linearLayout3.setTranslationY(0f);
            Log.d(TAG, "Controls hidden without animation");
            updateFABVisibility();
        }
    }

    private void applySmoothSizeReduction(float progress, int scrollY) {
        if (collapsibleImageContainer == null || maxImageSize == 0) return;

        try {
            // Calculate new size based on scroll progress (reduces evenly on all sides)
            int newSize = (int) (maxImageSize - (progress * (maxImageSize - minImageSize)));
            newSize = Math.max(newSize, minImageSize);

            // Get the CardView inside the FrameLayout
            CardView albumArtCard = getView().findViewById(R.id.albumArtCard);

            if (albumArtCard != null) {
                // Update CardView dimensions
                ViewGroup.LayoutParams cardParams = albumArtCard.getLayoutParams();
                if (cardParams.width != newSize || cardParams.height != newSize) {
                    cardParams.width = newSize;
                    cardParams.height = newSize;
                    albumArtCard.setLayoutParams(cardParams);

                    // Center the CardView by setting margins on the FrameLayout container
                    if (collapsibleImageContainer.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                        int parentWidth = collapsibleImageContainer.getWidth();
                        int horizontalMargin = (parentWidth - newSize) / 2;

                        // Set margins on the FrameLayout to center the CardView
                        FrameLayout.LayoutParams frameParams = (FrameLayout.LayoutParams) albumArtCard.getLayoutParams();
                        frameParams.setMargins(horizontalMargin, 0, horizontalMargin, 0);
                        frameParams.gravity = Gravity.CENTER_HORIZONTAL;
                        albumArtCard.setLayoutParams(frameParams);
                    }
                }
            }

            handleToolbarVisibility(progress);

            if (toolbarLayout != null && progress > 0.5f) {
                float toolbarAlpha = Math.min(1f, (progress - 0.5f) * 2f);
                toolbarLayout.setAlpha(toolbarAlpha);

                if (progress >= HIDE_THRESHOLD && toolbarGradientColor != -1) {
                    float blendFactor = (progress - HIDE_THRESHOLD) / (1f - HIDE_THRESHOLD);
                    if (imageGradientColor != -1 && toolbarGradientColor != -1) {
                        int blendedColor = blendColors(imageGradientColor, toolbarGradientColor, blendFactor);
                        setStatusBarColor(blendedColor);
                    }
                }
            }

            updateFABVisibility();

        } catch (Exception e) {
            Log.e(TAG, "Error in smooth size reduction: " + e.getMessage());
        }
    }

    private int blendColors(int color1, int color2, float ratio) {
        if (ratio <= 0) return color1;
        if (ratio >= 1) return color2;

        float inverseRatio = 1f - ratio;

        int a1 = android.graphics.Color.alpha(color1);
        int r1 = android.graphics.Color.red(color1);
        int g1 = android.graphics.Color.green(color1);
        int b1 = android.graphics.Color.blue(color1);

        int a2 = android.graphics.Color.alpha(color2);
        int r2 = android.graphics.Color.red(color2);
        int g2 = android.graphics.Color.green(color2);
        int b2 = android.graphics.Color.blue(color2);

        int a = (int) (a1 * inverseRatio + a2 * ratio);
        int r = (int) (r1 * inverseRatio + r2 * ratio);
        int g = (int) (g1 * inverseRatio + g2 * ratio);
        int b = (int) (b1 * inverseRatio + b2 * ratio);

        return android.graphics.Color.argb(a, r, g, b);
    }

    private void handleToolbarVisibility(float progress) {
        if (progress >= HIDE_THRESHOLD && !isToolbarVisible) {
            showToolbar();
            updateFABVisibility();
        } else if (progress < HIDE_THRESHOLD && isToolbarVisible) {
            hideToolbar();
            if (toolbarPlayPause != null) {
                toolbarPlayPause.hide();
            }
        }
    }

    private void updateFABVisibility() {
        if (toolbarPlayPause == null || linearLayout3 == null) return;

        if (isToolbarVisible && linearLayout3.getVisibility() != View.VISIBLE) {
            if (toolbarPlayPause.getVisibility() != View.VISIBLE) {
                toolbarPlayPause.show();
                Log.d(TAG, "FAB shown - toolbar visible & controls hidden");
            }
        } else {
            if (toolbarPlayPause.getVisibility() == View.VISIBLE) {
                toolbarPlayPause.hide();
                Log.d(TAG, "FAB hidden");
            }
        }
    }

    private void showToolbar() {
        if (toolbarLayout == null || isToolbarVisible) return;

        isToolbarVisible = true;

        if (toolbarGradientColor != -1) {
            GradientDrawable toolbarGradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{toolbarGradientColor, toolbarGradientColor}
            );
            toolbarLayout.setBackground(toolbarGradient);
            setStatusBarColor(toolbarGradientColor);
            Log.d(TAG, "Status bar set to toolbar color: " + Integer.toHexString(toolbarGradientColor));
        } else {
            toolbarLayout.setBackgroundColor(getResources().getColor(R.color.bgblack));
            setStatusBarColor(getResources().getColor(R.color.bgblack));
        }

        toolbarLayout.setVisibility(View.VISIBLE);
        toolbarLayout.setAlpha(0f);
        toolbarLayout.animate()
                .alpha(1f)
                .setDuration(300)
                .start();
    }

    private void hideToolbar() {
        if (toolbarLayout == null || !isToolbarVisible) return;

        isToolbarVisible = false;

        toolbarLayout.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    toolbarLayout.setVisibility(View.GONE);
                    if (imageGradientColor != -1) {
                        setStatusBarColor(imageGradientColor);
                        Log.d(TAG, "Status bar restored to image color: " + Integer.toHexString(imageGradientColor));
                    }
                })
                .start();
    }

    private void updateToolbarContent() {
        setToolbarSongNameWithMaxLength(artistName, 20);

        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio != null && toolbarSongName != null) {


            if (artistImageUrl != null && !artistImageUrl.isEmpty()) {
                Picasso.get()
                        .load(artistImageUrl)
                        .placeholder(R.drawable.video_placholder)
                        .into(toolbarAlbumArt);
            }
        }
    }

    private void setToolbarSongNameWithMaxLength(String songName, int maxLength) {
        if (toolbarSongName == null) return;

        if (songName != null && songName.length() > maxLength) {
            String truncated = songName.substring(0, maxLength - 3) + "...";
            toolbarSongName.setText(truncated);
        } else {
            toolbarSongName.setText(songName);
        }

        toolbarSongName.setMaxLines(1);
        toolbarSongName.setEllipsize(TextUtils.TruncateAt.END);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null && getArguments().containsKey("auto_play_song_id") && !hasAutoPlayed) {
            String autoPlaySongId = getArguments().getString("auto_play_song_id");
            autoPlaySpecificSong(autoPlaySongId);
        }
    }

    private void shareQRCode(String qrData, String playlistName) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Playlist: " + playlistName);
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "🎵 Check out this playlist: " + playlistName + "\n\n" +
                        "🔗 Scan this code in the app: " + qrData + "\n\n" +
                        "Download Salai Music App to listen!"
        );

        startActivity(Intent.createChooser(shareIntent, "Share Playlist"));
    }

    private void saveQRCodeToStorage(Bitmap bitmap, String playlistName) {
        if (getContext() == null) return;

        String fileName = "QR_" + playlistName.replace(" ", "_") + "_" + System.currentTimeMillis() + ".png";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);

            android.net.Uri uri = getContext().getContentResolver().insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
            );

            try {
                java.io.OutputStream out = getContext().getContentResolver().openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
                Toast.makeText(getContext(), "QR Code saved to Downloads", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Failed to save QR Code", Toast.LENGTH_SHORT).show();
            }
        } else {
            String path = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
            ).toString();
            java.io.File file = new java.io.File(path, fileName);

            try {
                java.io.FileOutputStream out = new java.io.FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(android.net.Uri.fromFile(file));
                getContext().sendBroadcast(mediaScanIntent);

                Toast.makeText(getContext(), "QR Code saved to Downloads", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Failed to save QR Code", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void autoPlaySpecificSong(String songId) {
        if (audioList == null || audioList.isEmpty()) {
            Log.e(TAG, "Cannot auto-play: audioList is empty");
            return;
        }

        Log.d(TAG, "Attempting to auto-play song with ID: " + songId);

        AudioModel targetSong = null;
        int targetPosition = -1;

        for (int i = 0; i < audioList.size(); i++) {
            AudioModel song = audioList.get(i);
            String currentSongId = song.getSongId();

            if (currentSongId != null && currentSongId.equalsIgnoreCase(songId)) {
                targetSong = song;
                targetPosition = i;
                break;
            }
        }

        if (targetSong != null) {
            Log.d(TAG, "Found song to auto-play: " + targetSong.getAudioName() +
                    " at position: " + targetPosition);

            final AudioModel finalTargetSong = targetSong;
            final int finalTargetPosition = targetPosition;

            new Handler().postDelayed(() -> {
                if (isAdded()) {
                    playSpecificSong(finalTargetSong, finalTargetPosition);
                    hasAutoPlayed = true;
                }
            }, 500);
        } else {
            Log.e(TAG, "Could not find song with ID: " + songId);
            Toast.makeText(getContext(),
                    "Could not find the scanned song in this playlist",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void playSpecificSong(AudioModel song, int position) {
        saveFullPlaylistToPrefs();

        // Show progress in adapter immediately
        if (audioAdapter != null && position >= 0) {
            audioAdapter.showItemProgress(position);
            Log.d(TAG, "Showing progress loader for scanned song at position: " + position);
        }

        PlayerManager.setAudioList(audioList, PlayerManager.PlaylistType.ONLINE);
        PlayerManager.playAudio(song, () -> {
            if (isAdded()) {
                // Clear progress in adapter when playing starts
                if (audioAdapter != null) {
                    audioAdapter.clearPreparingProgress();
                }

                updatePlayPauseButtons(R.drawable.pause);
                updateFullPlayerUI(song);
                if (recyclerView != null && position >= 0) {
                    recyclerView.scrollToPosition(position);
                }
                Toast.makeText(getContext(),
                        "Now playing: " + song.getAudioName(),
                        Toast.LENGTH_SHORT).show();
                broadcastUpdate();
            }
        });
        Log.d(TAG, "Auto-playing scanned song: " + song.getAudioName());
    }

    private String getPlaylistCategoryId() {
        if (audioList != null && !audioList.isEmpty()) {
            AudioModel firstSong = audioList.get(0);
            if (firstSong != null && firstSong.getCategoryId() != null &&
                    !firstSong.getCategoryId().isEmpty()) {
                Log.d("AudioFragment", "Playlist category ID from first song: " + firstSong.getCategoryId());
                return firstSong.getCategoryId();
            }

            for (AudioModel song : audioList) {
                if (song.getCategoryId() != null && !song.getCategoryId().isEmpty()) {
                    Log.d("AudioFragment", "Playlist category ID from song: " + song.getCategoryId());
                    return song.getCategoryId();
                }
            }
        }
        Log.d("AudioFragment", "No playlist category ID found");
        return "";
    }

    private boolean isAnySongFromThisPlaylistPlaying() {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) {
            Log.d("AudioFragment", "No current audio playing");
            return false;
        }

        String currentCategoryId = currentAudio.getCategoryId();
        Log.d("AudioFragment", "Current playing song: " + currentAudio.getAudioName());
        Log.d("AudioFragment", "Current song category ID: " + currentCategoryId);

        String thisPlaylistCategoryId = getPlaylistCategoryId();
        Log.d("AudioFragment", "This playlist category ID: " + thisPlaylistCategoryId);

        if (currentCategoryId != null && thisPlaylistCategoryId != null &&
                !currentCategoryId.isEmpty() && !thisPlaylistCategoryId.isEmpty()) {

            boolean match = currentCategoryId.equals(thisPlaylistCategoryId);
            Log.d("AudioFragment", "Category ID match: " + match);

            if (match) {
                Log.d("AudioFragment", "✓ EXACT MATCH - song belongs to this playlist");
                return true;
            } else {
                Log.d("AudioFragment", "✗ Category IDs don't match - different playlist");
                return false;
            }
        }

        Log.d("AudioFragment", "✗ Missing category IDs - cannot verify");
        return false;
    }

    private void updatePlayPauseButtonUI() {
        if (btnFullPlayPause == null) return;

        boolean isPlaylistPlaying = isAnySongFromThisPlaylistPlaying();

        requireActivity().runOnUiThread(() -> {
            if (isPlaylistPlaying) {
                btnFullPlayPause.setVisibility(View.VISIBLE);


                if (PlayerManager.isPlaying()) {
                    updatePlayPauseButtons(R.drawable.pause);
                    Log.d("AudioFragment", "Buttons: Show PAUSE (this playlist playing - EXACT MATCH)");
                } else {
                    updatePlayPauseButtons(R.drawable.play_player);
                    Log.d("AudioFragment", "Buttons: Show PLAY (this playlist paused)");
                }
            } else {
                btnFullPlayPause.setVisibility(View.VISIBLE);
                updatePlayPauseButtons(R.drawable.play_player);
                Log.d("AudioFragment", "Buttons: Show PLAY (start this playlist - NO MATCH)");
            }
        });
    }

    private void setupFullPlayPauseButton() {
        btnFullPlayPause.setOnClickListener(v -> {
            handlePlayPauseClick();
        });
    }

    private void saveFullPlaylistToPrefs() {
        if (getContext() != null && audioList != null && !audioList.isEmpty()) {
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("SavedPlaylist", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();

            editor.putInt("playlistSize", audioList.size());

            boolean hasDownloadedSongs = false;
            for (AudioModel song : audioList) {
                if (song.getDownloadPath() != null && !song.getDownloadPath().isEmpty()) {
                    hasDownloadedSongs = true;
                    break;
                }
            }

            editor.putBoolean("playlistHasOfflineSongs", hasDownloadedSongs);
            editor.putString("playlistType", "ONLINE");
            editor.putString("playlistName", artistName);
            editor.putString("playlistImage", artistImageUrl);

            for (int i = 0; i < audioList.size(); i++) {
                AudioModel song = audioList.get(i);
                editor.putString("songName_" + i, song.getAudioName());
                editor.putString("songUrl_" + i, song.getAudioUrl());
                editor.putString("songImage_" + i, song.getImageUrl());
                editor.putString("songArtist_" + i, song.getcategoryName());
                editor.putString("songCategoryId_" + i, song.getCategoryId());
                editor.putString("songId_" + i, song.getSongId());

                if (song.getDownloadPath() != null && !song.getDownloadPath().isEmpty()) {
                    editor.putString("songDownloadPath_" + i, song.getDownloadPath());
                    editor.putBoolean("songDownloaded_" + i, true);
                } else {
                    editor.putBoolean("songDownloaded_" + i, false);
                }
            }

            editor.apply();
            Log.d("AudioFragment", "✅ Saved full playlist with " + audioList.size() +
                    " songs via full play button");
        }
    }

    private String getOriginalCategoryId() {
        if (audioList != null && !audioList.isEmpty()) {
            AudioModel firstSong = audioList.get(0);
            if (firstSong != null && firstSong.getCategoryId() != null &&
                    !firstSong.getCategoryId().isEmpty()) {
                Log.d("AudioFragment", "Found category ID in song: " + firstSong.getCategoryId());
                return firstSong.getCategoryId();
            }

            for (AudioModel song : audioList) {
                if (song.getCategoryId() != null && !song.getCategoryId().isEmpty()) {
                    Log.d("AudioFragment", "Found category ID in song: " + song.getCategoryId());
                    return song.getCategoryId();
                }
            }
        }

        Log.d("AudioFragment", "No category ID found, using empty string");
        return "";
    }

    private void playPlaylistFromStart() {
        if (audioList == null || audioList.isEmpty()) {
            Toast.makeText(getContext(), "No songs in playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        AudioModel firstSong = audioList.get(0);
        PlayerManager.setAudioList(audioList, PlayerManager.PlaylistType.ONLINE);
        PlayerManager.playAudio(firstSong, () -> {
            broadcastUpdate();
            updateFullPlayerUI(firstSong);
            updatePlayPauseButtons(R.drawable.pause);
            Toast.makeText(getContext(), "Playing: " + firstSong.getAudioName(), Toast.LENGTH_SHORT).show();
        });
    }

    private void checkPlaylistDownloadStatus() {
        if (artistName == null || artistName.isEmpty()) {
            Log.d("AudioFragment", "checkPlaylistDownloadStatus: artistName is null or empty");
            return;
        }

        Log.d("AudioFragment", "checkPlaylistDownloadStatus: Checking for playlist - " + artistName);

        String originalCategoryId = getOriginalCategoryId();
        Log.d("AudioFragment", "Original category ID: " + originalCategoryId);

        isPlaylistDownloaded = false;
        downloadedPlaylistId = null;

        ArrayList<PlaylistModel> allPlaylists = downloadPlaylistManager.getAllPlaylists();

        Log.d("AudioFragment", "checkPlaylistDownloadStatus: Found " + allPlaylists.size() + " playlists");

        if (originalCategoryId != null && !originalCategoryId.isEmpty()) {
            for (PlaylistModel playlist : allPlaylists) {
                String playlistCategoryId = playlist.getOriginalCategoryId();
                Log.d("AudioFragment", "Comparing category IDs: " +
                        "Playlist[" + playlist.getName() + "]=" + playlistCategoryId +
                        " vs Original=" + originalCategoryId);

                if (playlistCategoryId != null && playlistCategoryId.equals(originalCategoryId)) {
                    Log.d("AudioFragment", "checkPlaylistDownloadStatus: CATEGORY ID MATCH FOUND!");
                    isPlaylistDownloaded = true;
                    downloadedPlaylistId = playlist.getId();

                    int downloadedCount = countDownloadedSongs(playlist.getSongs());
                    Log.d("AudioFragment", "Category ID match: " +
                            downloadedCount + "/" + playlist.getSongs().size() + " songs downloaded");
                    break;
                }
            }
        }

        if (!isPlaylistDownloaded) {
            for (PlaylistModel playlist : allPlaylists) {
                Log.d("AudioFragment", "checkPlaylistDownloadStatus: Comparing names - " +
                        playlist.getName() + " with " + artistName);

                boolean nameMatch = playlist.getName().equalsIgnoreCase(artistName);

                if (nameMatch) {
                    Log.d("AudioFragment", "checkPlaylistDownloadStatus: NAME MATCH FOUND!");
                    isPlaylistDownloaded = true;
                    downloadedPlaylistId = playlist.getId();

                    int downloadedCount = countDownloadedSongs(playlist.getSongs());
                    Log.d("AudioFragment", "Name match: " +
                            downloadedCount + "/" + playlist.getSongs().size() + " songs downloaded");
                    break;
                }
            }
        }

        Log.d("AudioFragment", "checkPlaylistDownloadStatus: isPlaylistDownloaded = " + isPlaylistDownloaded);

        if (isPlaylistDownloaded) {
            Log.d("AudioFragment", "Hiding download button for this specific playlist (Category ID: " +
                    originalCategoryId + ", Name: " + artistName + ")");
        } else {
            Log.d("AudioFragment", "Showing download button - playlist not found in downloads");
        }

        updateDownloadButtonUI(isPlaylistDownloaded);
    }

    private int countDownloadedSongs(ArrayList<AudioModel> playlistSongs) {
        if (playlistSongs == null || playlistSongs.isEmpty()) {
            return 0;
        }

        int downloadedCount = 0;
        for (AudioModel song : playlistSongs) {
            if (song.getDownloadPath() != null && !song.getDownloadPath().isEmpty()) {
                File file = new File(song.getDownloadPath());
                if (file.exists()) {
                    downloadedCount++;
                }
            }
        }
        return downloadedCount;
    }

    private void updateDownloadButtonUI(boolean isDownloaded) {
        if (btnDownloadPlaylist == null || btnDownloadedIndicator == null) {
            return;
        }

        requireActivity().runOnUiThread(() -> {
            if (isDownloaded) {
                btnDownloadPlaylist.setVisibility(View.GONE);
                btnDownloadedIndicator.setVisibility(View.VISIBLE);
            } else {
                btnDownloadPlaylist.setVisibility(View.VISIBLE);
                btnDownloadedIndicator.setVisibility(View.GONE);
            }
        });
    }

    private void showDownloadPlaylistDialog() {
        if (audioList == null || audioList.isEmpty()) {
            Toast.makeText(getContext(), "No songs to download", Toast.LENGTH_SHORT).show();
            return;
        }

        String playlistName = artistName;

        if (downloadPlaylistManager.playlistExists(playlistName)) {
            showOverwriteDialog(playlistName);
        } else {
            downloadPlaylistWithName(playlistName);
        }
    }

    private void showOverwriteDialog(String playlistName) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_overwrite_playlist, null);

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        TextView playlistNameText = dialogView.findViewById(R.id.playlistNameText);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button overwriteButton = dialogView.findViewById(R.id.overwriteButton);

        String message = "A playlist named \"" + playlistName + "\" already exists. " +
                "Do you want to overwrite it?";
        dialogMessage.setText(message);
        playlistNameText.setText(playlistName);

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);
        dialog.setCancelable(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
        }

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        overwriteButton.setOnClickListener(v -> {
            downloadPlaylistWithName(playlistName);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void downloadPlaylistWithName(String playlistName) {
        String originalCategoryId = getOriginalCategoryId();
        Log.d("AudioFragment", "Downloading playlist with category ID: " + originalCategoryId);

        String playlistId = UUID.randomUUID().toString();
        ArrayList<AudioModel> songsToSave = new ArrayList<>();

        for (AudioModel originalSong : audioList) {
            AudioModel downloadedSong = audioDownloadManager.getDownloadedSong(originalSong.getAudioUrl());
            if (downloadedSong != null && downloadedSong.isDownloaded()) {
                downloadedSong.setAudioName(originalSong.getAudioName());
                downloadedSong.setcategoryName(originalSong.getcategoryName());
                downloadedSong.setImageUrl(originalSong.getImageUrl());

                if (originalSong.getCategoryId() != null) {
                    downloadedSong.setCategoryId(originalSong.getCategoryId());
                }

                songsToSave.add(downloadedSong);
            } else {
                AudioModel songToSave = new AudioModel(
                        originalSong.getSongId(),
                        originalSong.getAudioName(),
                        originalSong.getAudioUrl(),
                        originalSong.getcategoryName(),
                        originalSong.getImageUrl()
                );

                if (originalSong.getCategoryId() != null) {
                    songToSave.setCategoryId(originalSong.getCategoryId());
                }

                songsToSave.add(songToSave);
            }
        }

        downloadPlaylistManager.savePlaylistWithCategoryId(
                playlistId,
                playlistName,
                artistImageUrl,
                songsToSave,
                originalCategoryId
        );

        startPlaylistDownload(playlistId, playlistName, songsToSave);
    }

    private void startPlaylistDownload(String playlistId, String playlistName, ArrayList<AudioModel> songsToDownload) {
        AlertDialog.Builder progressBuilder = new AlertDialog.Builder(requireContext());
        progressBuilder.setTitle("Downloading Playlist")
                .setMessage("Preparing to download " + songsToDownload.size() + " songs...")
                .setCancelable(false);

        AlertDialog progressDialog = progressBuilder.create();
        progressDialog.show();

        new Thread(() -> {
            int downloadedCount = 0;
            int totalSongs = songsToDownload.size();

            ArrayList<AudioModel> updatedSongs = new ArrayList<>();

            for (int i = 0; i < totalSongs; i++) {
                AudioModel song = songsToDownload.get(i);

                if (!audioDownloadManager.isSongDownloaded(song.getAudioUrl(), song.getAudioName())) {
                    audioDownloadManager.downloadAudio(song);

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    AudioModel downloadedSong = audioDownloadManager.getDownloadedSong(song.getAudioUrl());
                    if (downloadedSong != null && downloadedSong.isDownloaded()) {
                        if (song.getCategoryId() != null) {
                            downloadedSong.setCategoryId(song.getCategoryId());
                        }
                        updatedSongs.add(downloadedSong);
                        Log.d("AudioFragment", "Downloaded: " + downloadedSong.getAudioName() +
                                " | Path: " + downloadedSong.getDownloadPath());
                    } else {
                        updatedSongs.add(song);
                    }
                } else {
                    AudioModel downloadedSong = audioDownloadManager.getDownloadedSong(song.getAudioUrl());
                    if (downloadedSong != null) {
                        if (song.getCategoryId() != null) {
                            downloadedSong.setCategoryId(song.getCategoryId());
                        }
                        updatedSongs.add(downloadedSong);
                    } else {
                        updatedSongs.add(song);
                    }
                }

                downloadedCount++;
                final int progress = (downloadedCount * 100) / totalSongs;

                int finalDownloadedCount = downloadedCount;
                requireActivity().runOnUiThread(() -> {
                    progressDialog.setMessage(
                            String.format("Downloading %d of %d songs\n%d%% complete",
                                    finalDownloadedCount, totalSongs, progress)
                    );
                });

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!updatedSongs.isEmpty()) {
                String categoryId = "";
                if (!updatedSongs.isEmpty() && updatedSongs.get(0).getCategoryId() != null) {
                    categoryId = updatedSongs.get(0).getCategoryId();
                }

                downloadPlaylistManager.savePlaylistWithCategoryId(
                        playlistId,
                        playlistName,
                        artistImageUrl,
                        updatedSongs,
                        categoryId
                );
                Log.d("AudioFragment", "Playlist updated with downloaded songs and category ID: " + categoryId);
            }

            requireActivity().runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(getContext(),
                        "Playlist \"" + playlistName + "\" downloaded successfully!\n" +
                                updatedSongs.size() + " songs saved locally.",
                        Toast.LENGTH_LONG).show();

                isPlaylistDownloaded = true;
                downloadedPlaylistId = playlistId;
                updateDownloadButtonUI(true);

                broadcastDownloadUpdate();
            });
        }).start();
    }

    private void showAddToExistingPlaylistDialog() {
        ArrayList<PlaylistModel> existingPlaylists =
                downloadPlaylistManager.getAllPlaylists();

        if (existingPlaylists.isEmpty()) {
            Toast.makeText(getContext(), "No playlists found. Create one first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] playlistNames = new String[existingPlaylists.size()];
        for (int i = 0; i < existingPlaylists.size(); i++) {
            playlistNames[i] = existingPlaylists.get(i).getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add to Playlist")
                .setItems(playlistNames, (dialog, which) -> {
                    String selectedPlaylistName = playlistNames[which];
                    String selectedPlaylistId = existingPlaylists.get(which).getId();

                    downloadPlaylistManager.addSongsToPlaylist(selectedPlaylistId, audioList);

                    Toast.makeText(getContext(),
                            "Added to playlist: " + selectedPlaylistName,
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateFullPlayerUI(AudioModel audio) {
        if (audio == null) return;

        tvFullPlayerSongName.setText(artistName);
        tvFullPlayerSongArtist.setText(audio.getcategoryName() != null ?
                audio.getcategoryName() : "Unknown Artist");

        updatePlayPauseButtonUI();

        if (artistImageUrl != null && !artistImageUrl.isEmpty()) {
            Picasso.get()
                    .load(artistImageUrl)
                    .placeholder(R.drawable.video_placholder)
                    .into(imgAlbumArt, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            imgAlbumArt.setDrawingCacheEnabled(true);
                            imgAlbumArt.buildDrawingCache(true);
                            Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) imgAlbumArt.getDrawable()).getBitmap();
                            if (bitmap != null) {
                                generateDynamicBackground(bitmap);
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            imgAlbumArt.setImageResource(R.drawable.video_placholder);
                            setDefaultDarkBackground();
                        }
                    });
        } else {
            imgAlbumArt.setImageResource(R.drawable.video_placholder);
            setDefaultDarkBackground();
        }
    }

    private void setDefaultDarkBackground() {
        if (getView() != null) {
            int darkGray = getResources().getColor(R.color.bgblack);

            float[] hsv = new float[3];
            android.graphics.Color.colorToHSV(darkGray, hsv);
            hsv[2] = hsv[2] * 0.4f;
            int darkerGray = android.graphics.Color.HSVToColor(hsv);

            android.graphics.drawable.GradientDrawable defaultGradient =
                    new android.graphics.drawable.GradientDrawable(
                            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{darkGray, darkerGray}
                    );

            View rootView = getView();
            rootView.setBackground(defaultGradient);

            NestedScrollView scrollView = rootView.findViewById(R.id.nestedScrollView);
            if (scrollView != null) {
                scrollView.setBackground(defaultGradient);
            }
        }
    }

    private boolean isNearBlack(int color) {
        int r = android.graphics.Color.red(color);
        int g = android.graphics.Color.green(color);
        int b = android.graphics.Color.blue(color);

        double brightness = (r * 0.299 + g * 0.587 + b * 0.114) / 255;
        return brightness < 0.2;
    }

    private void broadcastUpdate() {
        if (getContext() != null) {
            Context context = getContext();

            Intent miniPlayerIntent = new Intent("UPDATE_MINI_PLAYER");
            Intent audioAdapterIntent = new Intent("UPDATE_AUDIO_ADAPTER");

            miniPlayerIntent.setPackage(context.getPackageName());
            audioAdapterIntent.setPackage(context.getPackageName());

            context.sendBroadcast(miniPlayerIntent);
            context.sendBroadcast(audioAdapterIntent);
        }
    }

    private void generateDynamicBackground(Bitmap bitmap) {
        if (bitmap == null || !isAdded()) return;

        Palette.from(bitmap).generate(palette -> {
            if (!isAdded() || getContext() == null) return;

            int defaultColor = getResources().getColor(R.color.bgblack);

            int darkVibrantColor = palette.getDarkVibrantColor(defaultColor);
            int darkMutedColor = palette.getDarkMutedColor(defaultColor);
            int mutedColor = palette.getMutedColor(defaultColor);
            int vibrantColor = palette.getVibrantColor(defaultColor);
            int lightVibrantColor = palette.getLightVibrantColor(defaultColor);
            int lightMutedColor = palette.getLightMutedColor(defaultColor);

            int topColor = getMidLightColor(new int[]{
                    lightVibrantColor,
                    lightMutedColor,
                    vibrantColor,
                    mutedColor,
                    darkVibrantColor,
                    darkMutedColor
            }, defaultColor);

            topColor = ensureNotTooDark(topColor, getResources().getColor(R.color.medium_gray));
            int bottomColor = getResources().getColor(R.color.bgblack);

            imageGradientColor = ensureDarkEnough(topColor);

            GradientDrawable mainGradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{topColor, bottomColor}
            );

            applyGradientBackground(mainGradient);

            int toolbarTopColor = darkenColor(topColor, 0.3f);
            int toolbarBottomColor = bottomColor;

            GradientDrawable toolbarGradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{toolbarTopColor, toolbarTopColor}
            );

            if (toolbarLayout != null) {
                toolbarLayout.setBackground(toolbarGradient);
            }

            toolbarGradientColor = ensureDarkEnough(toolbarTopColor);

            if (isImageVisible) {
                setStatusBarColor(imageGradientColor);
                Log.d(TAG, "Fragment opened - Setting status bar to TOP GRADIENT color: " +
                        Integer.toHexString(imageGradientColor));
            }

            Log.d(TAG, "Gradients applied. Top color: " + Integer.toHexString(topColor) +
                    " (status bar: " + Integer.toHexString(imageGradientColor) +
                    "), Bottom color: " + Integer.toHexString(bottomColor));
        });
    }

    private int getMidLightColor(int[] colors, int defaultColor) {
        for (int color : colors) {
            if (!isNearBlack(color) && !isTooBright(color)) {
                return color;
            }
        }
        for (int color : colors) {
            if (!isNearBlack(color)) {
                return color;
            }
        }
        return defaultColor;
    }

    private int darkenColor(int color, float factor) {
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(color, hsv);
        hsv[2] = hsv[2] * (1 - factor);
        return android.graphics.Color.HSVToColor(hsv);
    }

    private int ensureNotTooDark(int color, int fallbackColor) {
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(color, hsv);

        if (hsv[2] < 0.3f) {
            hsv[2] = 0.4f;
            return android.graphics.Color.HSVToColor(hsv);
        }
        return color;
    }

    private int ensureDarkEnough(int color) {
        double brightness = (0.299 * android.graphics.Color.red(color) +
                0.587 * android.graphics.Color.green(color) +
                0.114 * android.graphics.Color.blue(color)) / 255;

        if (brightness > 0.6) {
            float[] hsv = new float[3];
            android.graphics.Color.colorToHSV(color, hsv);
            hsv[2] = hsv[2] * 0.4f;
            return android.graphics.Color.HSVToColor(hsv);
        }
        return color;
    }

    private void applyGradientBackground(GradientDrawable gradient) {
        if (getView() == null) return;

        View rootView = getView();
        rootView.setBackground(gradient);

        if (mainlayout != null) {
            mainlayout.setBackground(gradient);
        }

        if (toolbarLayout != null) {
            toolbarLayout.setBackground(gradient);
        }
    }

    private boolean isTooBright(int color) {
        int r = android.graphics.Color.red(color);
        int g = android.graphics.Color.green(color);
        int b = android.graphics.Color.blue(color);

        double brightness = (r * 0.299 + g * 0.587 + b * 0.114) / 255;
        return brightness > 0.8;
    }

    private void setStatusBarColor(int color) {
        if (getActivity() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            color = ensureDarkEnough(color);
            getActivity().getWindow().setStatusBarColor(color);
            setStatusBarTextColor(color);
        }
    }

    private void setStatusBarTextColor(int backgroundColor) {
        if (getActivity() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = getActivity().getWindow().getDecorView();

            double brightness = (0.299 * android.graphics.Color.red(backgroundColor) +
                    0.587 * android.graphics.Color.green(backgroundColor) +
                    0.114 * android.graphics.Color.blue(backgroundColor)) / 255;

            if (brightness < 0.5) {
                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    private void resetStatusBarColor() {
        if (getActivity() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TypedArray typedArray = getActivity().getTheme().obtainStyledAttributes(new int[]{
                    android.R.attr.statusBarColor
            });

            int defaultColor = typedArray.getColor(0, getResources().getColor(R.color.bgblack));
            typedArray.recycle();

            getActivity().getWindow().setStatusBarColor(defaultColor);
            setStatusBarTextColor(defaultColor);
        }
    }

    private void broadcastDownloadUpdate() {
        if (getContext() != null) {
            Intent downloadUpdateIntent = new Intent("DOWNLOAD_UPDATE");
            downloadUpdateIntent.setPackage(getContext().getPackageName());
            getContext().sendBroadcast(downloadUpdateIntent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        isImageVisible = true;

        if (imageGradientColor != -1) {
            setStatusBarColor(imageGradientColor);
        }

        checkPlaylistDownloadStatus();

        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        updateFullPlayerUI(currentAudio);
        updatePlayPauseButtonUI();

        // Create a single filter with ALL actions
        IntentFilter filter = new IntentFilter();
        filter.addAction("UPDATE_AUDIO_ADAPTER");
        filter.addAction("UPDATE_MINI_PLAYER");
        filter.addAction("DOWNLOAD_UPDATE");
        filter.addAction(ACTION_PREPARE_NEXT);
        filter.addAction(ACTION_PREPARE_PREVIOUS);
        filter.addAction("SONG_CHANGED"); // Add this

        // Register receivers with the comprehensive filter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(audioUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            requireContext().registerReceiver(miniPlayerReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            requireContext().registerReceiver(downloadUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            requireContext().registerReceiver(prepareReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            requireContext().registerReceiver(playerUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            requireContext().registerReceiver(songChangedReceiver, filter, Context.RECEIVER_NOT_EXPORTED); // Add this
        } else {
            requireContext().registerReceiver(audioUpdateReceiver, filter);
            requireContext().registerReceiver(miniPlayerReceiver, filter);
            requireContext().registerReceiver(downloadUpdateReceiver, filter);
            requireContext().registerReceiver(prepareReceiver, filter);
            requireContext().registerReceiver(playerUpdateReceiver, filter);
            requireContext().registerReceiver(songChangedReceiver, filter); // Add this
        }

        isReceiverRegistered = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            // Unregister all receivers at once
            requireContext().unregisterReceiver(audioUpdateReceiver);
            requireContext().unregisterReceiver(miniPlayerReceiver);
            requireContext().unregisterReceiver(downloadUpdateReceiver);
            requireContext().unregisterReceiver(prepareReceiver);
            requireContext().unregisterReceiver(playerUpdateReceiver);
            requireContext().unregisterReceiver(songChangedReceiver); // Add this
        } catch (IllegalArgumentException e) {
            Log.e("AudioFragment", "Receiver not registered: " + e.getMessage());
        }

        resetStatusBarColor();
        isReceiverRegistered = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Picasso.get().cancelRequest(imgAlbumArt);
        resetStatusBarColor();
    }
}