package com.Saalai.SalaiMusicApp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.squareup.picasso.Picasso;

import java.util.List;

public class MenuBottomSheetFragmentDownloads extends BottomSheetDialogFragment {

    private static final String TAG = "MenuBottomSheetFragment";

    private String audioName;
    private String audioUrl;
    private String artistName;
    private String imageUrl;
    private ImageView songimage, downloadIndicatorDot, songInfoDownloadDot;
    private TextView songname, artistname;
    private AudioDownloadManager downloadManager;

    // Download UI elements
    private TextView downloadText;
    private ImageView downloadIcon;
    private ProgressBar downloadProgress;
    private TextView downloadProgressText;
    private LinearLayout downloadLayout;
    private LinearLayout removeLayout;
    private RelativeLayout progressContainer;



    // Track download state
    private boolean isDownloadInProgress = false;
    private int currentProgress = 0;

    public interface MenuBottomSheetListener {
        void onAddToPlaylist();
        void onShare();
        void onDownload();
        void onViewArtist();
    }

    private MenuBottomSheetListener listener;

    public static MenuBottomSheetFragmentDownloads newInstance(String audioName, String audioUrl, String artistName, String imageUrl) {
        Log.d("MenuBottomSheetFragment", "newInstance called with: " + audioName);
        MenuBottomSheetFragmentDownloads fragment = new MenuBottomSheetFragmentDownloads();
        Bundle args = new Bundle();
        args.putString("audioName", audioName);
        args.putString("audioUrl", audioUrl);
        args.putString("artistName", artistName);
        args.putString("imageUrl", imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    public static MenuBottomSheetFragmentDownloads newInstance(AudioModel audioModel) {
        Log.d("MenuBottomSheetFragment", "newInstance called with AudioModel: " + audioModel.getAudioName());
        MenuBottomSheetFragmentDownloads fragment = new MenuBottomSheetFragmentDownloads();
        Bundle args = new Bundle();
        args.putString("audioName", audioModel.getAudioName());
        args.putString("audioUrl", audioModel.getAudioUrl());
        args.putString("artistName", audioModel.getcategoryName());
        args.putString("imageUrl", audioModel.getImageUrl());
        fragment.setArguments(args);
        return fragment;
    }

    public void setMenuBottomSheetListener(MenuBottomSheetListener listener) {
        Log.d(TAG, "setMenuBottomSheetListener called");
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogTheme);

        if (getArguments() != null) {
            audioName = getArguments().getString("audioName");
            audioUrl = getArguments().getString("audioUrl");
            artistName = getArguments().getString("artistName");
            imageUrl = getArguments().getString("imageUrl");
            Log.d(TAG, "Arguments loaded - Name: " + audioName + ", URL: " + audioUrl + ", Artist: " + artistName);
        } else {
            Log.w(TAG, "No arguments found in onCreate");
        }

        downloadManager = new AudioDownloadManager(requireContext());
        Log.d(TAG, "AudioDownloadManager initialized");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_menu_bottom_sheet_downloada, container, false);

        // Initialize song info views
        songimage = view.findViewById(R.id.songImage);
        songname = view.findViewById(R.id.songname);
        artistname = view.findViewById(R.id.artistName);
        songInfoDownloadDot = view.findViewById(R.id.downloadIndicatorDot);

        // Initialize download UI elements
        downloadText = view.findViewById(R.id.downloadText);
        downloadIcon = view.findViewById(R.id.downloadIcon);
        downloadProgress = view.findViewById(R.id.downloadProgress);
        downloadProgressText = view.findViewById(R.id.downloadProgressText);
        downloadLayout = view.findViewById(R.id.download);
        removeLayout = view.findViewById(R.id.remove);
        progressContainer = view.findViewById(R.id.progressContainer);
        downloadIndicatorDot = view.findViewById(R.id.downloadIndicatorDot);

        Log.d(TAG, "UI elements initialized");

        // Load image with Picasso
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Loading image: " + imageUrl);
            Picasso.get().load(imageUrl)
                    .placeholder(R.drawable.video_placholder)
                    .error(R.drawable.video_placholder)
                    .into(songimage, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Image loaded successfully: " + imageUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Image loading failed: " + imageUrl, e);
                        }
                    });
        } else {
            Log.w(TAG, "Image URL is empty, using placeholder");
            songimage.setImageResource(R.drawable.video_placholder);
        }

        songname.setText(audioName);
        artistname.setText(artistName);

        Log.d(TAG, "Text views set - Name: " + audioName + ", Artist: " + artistName);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        LinearLayout addToPlaylist = view.findViewById(R.id.addToPlaylist);
        LinearLayout share = view.findViewById(R.id.share);
        LinearLayout viewArtist = view.findViewById(R.id.viewArtist);

        // Update download button state and indicator dots
        Log.d(TAG, "Updating download button state and indicator dots");
        updateDownloadButtonState();
        updateDownloadIndicatorDots();

        // Check if download was in progress and restore state
        checkAndRestoreDownloadState();

        addToPlaylist.setOnClickListener(v -> {
            Log.d(TAG, "Add to playlist clicked");
            if (listener != null) {
                listener.onAddToPlaylist();
            } else {
                addToPlaylist();
            }
            dismiss();
        });

        share.setOnClickListener(v -> {
            Log.d(TAG, "Share clicked");
            if (listener != null) {
                listener.onShare();
            } else {
                shareAudio();
            }
            dismiss();
        });

        downloadLayout.setOnClickListener(v -> {
            Log.d(TAG, "Download clicked");
            if (listener != null) {
                listener.onDownload();
            } else {
                // Check if already downloaded before attempting download
                if (isSongDownloaded()) {
                    Log.d(TAG, "Song already downloaded: " + audioName);
                    showToast("Song already downloaded");
                    return;
                }
                Log.d(TAG, "Starting download for: " + audioName);
                downloadAudio();
            }
        });

        viewArtist.setOnClickListener(v -> {
            Log.d(TAG, "View artist clicked");
            if (listener != null) {
                listener.onViewArtist();
            } else {
                viewArtist();
            }
            dismiss();
        });

        removeLayout.setOnClickListener(v -> {
            Log.d(TAG, "Remove/Delete clicked for: " + audioName);
            deleteDownloadedSong();
            dismiss();
        });
    }

    private void deleteDownloadedSong() {
        Log.d(TAG, "Attempting to delete downloaded song: " + audioName);

        // Create AudioModel from current data
        AudioModel audioModel = new AudioModel(audioName, audioUrl, imageUrl);
        audioModel.setcategoryName(artistName);

        // Initialize download manager
        AudioDownloadManager downloadManager = new AudioDownloadManager(requireContext());

        // Delete the song
        boolean isDeleted = downloadManager.deleteDownloadedSong(audioModel);

        if (isDeleted) {
            Log.d(TAG, "Song deleted successfully: " + audioName);
            showToast("Song deleted successfully");

            // Broadcast update to refresh the downloads list
            broadcastDownloadUpdate();

            // Also broadcast song changed in case the deleted song was playing
            broadcastSongChanged();
        } else {
            Log.e(TAG, "Failed to delete song: " + audioName);
            showToast("Failed to delete song");
        }
    }

    // Add this broadcast method for song changed
    private void broadcastSongChanged() {
        Log.d(TAG, "Broadcasting song changed");
        if (getContext() != null) {
            Intent songChangedIntent = new Intent("SONG_CHANGED");
            songChangedIntent.setPackage(getContext().getPackageName());
            getContext().sendBroadcast(songChangedIntent);
            Log.d(TAG, "SONG_CHANGED broadcast sent");
        }
    }


    // Replace the checkAndRestoreDownloadState method with this:

    private void checkAndRestoreDownloadState() {
        Log.d(TAG, "checkAndRestoreDownloadState - Checking state for: " + audioName);

        boolean isSongAlreadyDownloaded = isSongDownloaded();
        boolean isDownloadInProgressGlobally = DownloadProgressManager.isDownloadInProgress(audioName);

        Log.d(TAG, "State check - Downloaded: " + isSongAlreadyDownloaded +
                ", InProgress: " + isDownloadInProgressGlobally +
                ", Global Progress: " + DownloadProgressManager.getProgress(audioName));

        // If song is already downloaded but progress system thinks it's still downloading,
        // clear the progress state and show downloaded state
        if (isSongAlreadyDownloaded) {
            Log.d(TAG, "Song is already downloaded, clearing any progress state");
            DownloadProgressManager.downloadComplete(audioName); // Clear progress state
            isDownloadInProgress = false;
            currentProgress = 0;
            setDownloadedState();
            return;
        }

        // Check if this song is currently being downloaded
        if (isDownloadInProgressGlobally) {
            Log.d(TAG, "Download is in progress for: " + audioName + ", restoring state");
            isDownloadInProgress = true;
            currentProgress = DownloadProgressManager.getProgress(audioName);

            // Show progress UI with current progress
            showDownloadProgress();

            // Set up progress listener for ongoing download
            setupProgressListener();

            Log.d(TAG, "Download state restored - Progress: " + currentProgress + "%");
        } else {
            Log.d(TAG, "No download in progress for: " + audioName);
            isDownloadInProgress = false;
            currentProgress = 0;
        }
    }

    private void setupProgressListener() {
        downloadManager.setProgressListener(new AudioDownloadManager.OnDownloadProgressListener() {
            @Override
            public void onProgress(int progress) {
                Log.d(TAG, "Download progress callback: " + progress + "% for: " + audioName);

                // Double-check if song is already downloaded (in case of race condition)
                if (isSongDownloaded()) {
                    Log.d(TAG, "Song already downloaded but progress received, ignoring progress");
                    return;
                }

                // Update global progress manager
                DownloadProgressManager.updateProgress(audioName, progress);
                updateProgress(progress);
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "Download completed successfully: " + audioName);
                // Update global progress manager
                DownloadProgressManager.downloadComplete(audioName);
                isDownloadInProgress = false;
                currentProgress = 0;

                new Handler(Looper.getMainLooper()).post(() -> {
                    updateProgress(100);
                    setDownloadedState();
                    showToast("Download completed: " + audioName);

                    // Notify other parts of the app about the download update
                    broadcastDownloadUpdate();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Download error: " + error + " for song: " + audioName);
                // Update global progress manager
                DownloadProgressManager.downloadError(audioName);
                isDownloadInProgress = false;
                currentProgress = 0;

                new Handler(Looper.getMainLooper()).post(() -> {
                    hideDownloadProgress();
                    setDownloadableState(); // Reset to downloadable state on error
                    showToast("Download failed: " + error);
                });
            }
        });
    }

    private void updateDownloadIndicatorDots() {
        boolean isDownloaded = isSongDownloaded();
        Log.d(TAG, "updateDownloadIndicatorDots - isDownloaded: " + isDownloaded + " for song: " + audioName);

        if (isDownloaded) {
            downloadIndicatorDot.setVisibility(View.VISIBLE);
            songInfoDownloadDot.setVisibility(View.VISIBLE);
            Log.d(TAG, "Download indicator dots set to VISIBLE");
        } else {
            downloadIndicatorDot.setVisibility(View.GONE);
            songInfoDownloadDot.setVisibility(View.GONE);
            Log.d(TAG, "Download indicator dots set to GONE");
        }
    }

    private boolean isSongDownloaded() {
        Log.d(TAG, "Checking if song is downloaded: " + audioName);

        // Method 1: Check using AudioDownloadManager by NAME
        boolean method1Result = downloadManager.isSongDownloadedByName(audioName);
        Log.d(TAG, "Method 1 (AudioDownloadManager by name) result: " + method1Result);

        if (method1Result) {
            return true;
        }

        // Method 2: Check using both URL and name
        boolean method2Result = downloadManager.isSongDownloaded(audioUrl, audioName);
        Log.d(TAG, "Method 2 (AudioDownloadManager by URL+name) result: " + method2Result);

        if (method2Result) {
            return true;
        }

        // Method 3: Check your downloaded songs list
        return checkInDownloadedList();
    }

    private boolean checkInDownloadedList() {
        Log.d(TAG, "checkInDownloadedList called for URL: " + audioUrl);
        try {
            // Get your downloaded songs list from AudioDownloadManager
            List<AudioModel> downloadedSongs = downloadManager.getDownloadedSongs();
            Log.d(TAG, "Downloaded songs list size: " + (downloadedSongs != null ? downloadedSongs.size() : "null"));

            if (downloadedSongs != null) {
                for (AudioModel song : downloadedSongs) {
                    Log.d(TAG, "Checking song: " + song.getAudioName() + " with URL: " + song.getAudioUrl());
                    if (song.getAudioUrl().equals(audioUrl) || song.getAudioName().equals(audioName)) {
                        Log.d(TAG, "Song found in downloaded list: " + audioName);
                        return true;
                    }
                }
                Log.d(TAG, "Song NOT found in downloaded list: " + audioName);
            } else {
                Log.w(TAG, "Downloaded songs list is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking downloaded list", e);
        }
        return false;
    }

    private void updateDownloadButtonState() {
        boolean isDownloaded = isSongDownloaded();
        Log.d(TAG, "updateDownloadButtonState - isDownloaded: " + isDownloaded);

        // If song is downloaded, always show downloaded state regardless of progress
        if (isDownloaded) {
            setDownloadedState();
        } else {
            setDownloadableState();
        }
    }

    private void setDownloadedState() {
        Log.d(TAG, "Setting downloaded state for: " + audioName);
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!isAdded()) return;

            downloadText.setText("Downloaded");
            downloadLayout.setEnabled(false);
            downloadLayout.setClickable(false);
            downloadLayout.setAlpha(0.7f);

            // Hide progress container - IMPORTANT!
            progressContainer.setVisibility(View.GONE);

            // Show download indicator dots, hide download icon
            downloadIndicatorDot.setVisibility(View.VISIBLE);
            songInfoDownloadDot.setVisibility(View.VISIBLE);
            downloadIcon.setVisibility(View.GONE);

            Log.d(TAG, "Download state set to DOWNLOADED for: " + audioName);
        });
    }

    private void setDownloadableState() {
        Log.d(TAG, "Setting downloadable state for: " + audioName);
        new Handler(Looper.getMainLooper()).post(() -> {
            downloadText.setText("Download");
            downloadLayout.setEnabled(true);
            downloadLayout.setClickable(true);
            downloadLayout.setAlpha(1.0f);
            // Hide progress container
            progressContainer.setVisibility(View.GONE);

            // Show download icon, hide download indicator dots
            downloadIcon.setVisibility(View.VISIBLE);
            downloadIndicatorDot.setVisibility(View.GONE);
            songInfoDownloadDot.setVisibility(View.GONE);

            Log.d(TAG, "Download state set to DOWNLOADABLE for: " + audioName);
        });
    }

    private void showDownloadProgress() {
        Log.d(TAG, "Showing download progress for: " + audioName + " with progress: " + currentProgress + "%");
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!isAdded()) return;

            // Hide both icons and dots, show progress container
            downloadIcon.setVisibility(View.GONE);
            downloadIndicatorDot.setVisibility(View.GONE);
            songInfoDownloadDot.setVisibility(View.GONE);
            progressContainer.setVisibility(View.VISIBLE);
            downloadLayout.setEnabled(false);
            downloadText.setText("Downloading");

            // Set current progress
            downloadProgress.setProgress(currentProgress);
            downloadProgressText.setText(currentProgress + "%");

            Log.d(TAG, "Download progress UI shown with progress: " + currentProgress + "%");
        });
    }

    private void hideDownloadProgress() {
        Log.d(TAG, "Hiding download progress");
        new Handler(Looper.getMainLooper()).post(() -> {
            progressContainer.setVisibility(View.GONE);
            Log.d(TAG, "Download progress UI hidden");
        });
    }

    private void updateProgress(int progress) {
        Log.d(TAG, "Download progress update: " + progress + "% for: " + audioName);
        currentProgress = progress;
        new Handler(Looper.getMainLooper()).post(() -> {
            if (isAdded() && getContext() != null) {
                downloadProgress.setProgress(progress);
                downloadProgressText.setText(progress + "%");

                if (progress == 100) {
                    // Download completed - update state
                    Log.d(TAG, "Download completed: " + audioName);
                    setDownloadedState();
                    showToast("Download completed: " + audioName);

                    // Notify other parts of the app about the download update
                    broadcastDownloadUpdate();
                } else {
                    downloadText.setText("Downloading");
                }
            }
        });
    }

    private void addToPlaylist() {
        Log.d(TAG, "addToPlaylist executed for: " + audioName);
        showToast("Added to playlist: " + audioName);
    }

    private void shareAudio() {
        Log.d(TAG, "shareAudio executed for: " + audioName);
        if (getContext() != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this audio: " + audioName);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Listen to: " + audioName + "\n\n" + audioUrl);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
            Log.d(TAG, "Share intent started");
        }
    }

    private void downloadAudio() {
        Log.d(TAG, "downloadAudio started for: " + audioName);

        // Double check if already downloaded
        if (isSongDownloaded()) {
            Log.w(TAG, "Song already downloaded, aborting download: " + audioName);
            showToast("Song already downloaded");
            setDownloadedState();
            return;
        }

        // Show progress UI
        isDownloadInProgress = true;
        currentProgress = 0;
        showDownloadProgress();

        // Register with global progress manager
        DownloadProgressManager.startDownload(audioName);

        // Create AudioModel and download
        AudioModel audioModel = new AudioModel(audioName, audioUrl, imageUrl);
        audioModel.setcategoryName(artistName);

        Log.d(TAG, "AudioModel created: " + audioModel.toString());

        // Set progress listener
        setupProgressListener();

        downloadManager.downloadAudio(audioModel);
        Log.d(TAG, "Download manager downloadAudio called");
    }

    private void broadcastDownloadUpdate() {
        Log.d(TAG, "Broadcasting download update");
        if (getContext() != null) {
            Intent downloadUpdateIntent = new Intent("DOWNLOAD_UPDATE");
            downloadUpdateIntent.setPackage(getContext().getPackageName());
            getContext().sendBroadcast(downloadUpdateIntent);
            Log.d(TAG, "DOWNLOAD_UPDATE broadcast sent");
        }
    }

    private void viewArtist() {
        Log.d(TAG, "viewArtist executed for: " + artistName);
        showToast("Viewing artist: " + artistName);
    }

    // Safe Toast method to avoid null context issues
    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach called");
        if (context instanceof MenuBottomSheetListener) {
            listener = (MenuBottomSheetListener) context;
            Log.d(TAG, "MenuBottomSheetListener attached");
        } else {
            Log.d(TAG, "No MenuBottomSheetListener attached");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach called");
        listener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called - refreshing download state");
        // Refresh the download state when the bottom sheet is shown
        updateDownloadButtonState();
        updateDownloadIndicatorDots();

        // Check if download state needs to be restored
        checkAndRestoreDownloadState();
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
        // Clear the progress listener when fragment is destroyed
        downloadManager.setProgressListener(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");
    }
}