package com.Saalai.SalaiMusicApp.Fragments;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.VideoDownloadManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadedVideosFragment extends Fragment {

    private RecyclerView recyclerView;
    private DownloadedVideosAdapter adapter;
    private VideoDownloadManager downloadManager;
    private List<VideoDownloadManager.DownloadedVideo> downloadedVideos;
    private ProgressBar progressBar;
    private TextView emptyStateText;

    public DownloadedVideosFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        downloadManager = new VideoDownloadManager(requireContext());
        downloadedVideos = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_downloaded_videos, container, false);

        recyclerView = view.findViewById(R.id.downloadedVideosRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);

        // Setup RecyclerView
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new DownloadedVideosAdapter(getContext(), downloadedVideos, new DownloadedVideosAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(VideoDownloadManager.DownloadedVideo video) {

            }

            @Override
            public void onItemLongClick(VideoDownloadManager.DownloadedVideo video, int position) {

            }
        });

        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDownloadedVideos();
    }

    private void loadDownloadedVideos() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);

        new Thread(() -> {
            List<VideoDownloadManager.DownloadedVideo> videos = downloadManager.getDownloadedVideos();

            // Filter out videos that don't exist
            List<VideoDownloadManager.DownloadedVideo> validVideos = new ArrayList<>();
            for (VideoDownloadManager.DownloadedVideo video : videos) {
                File file = new File(video.getFilePath());
                if (file.exists()) {
                    validVideos.add(video);
                } else {
                    Log.d("DownloadedVideos", "File doesn't exist: " + video.getFilePath());
                }
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                downloadedVideos.clear();
                downloadedVideos.addAll(validVideos);
                adapter.notifyDataSetChanged();

                progressBar.setVisibility(View.GONE);

                if (downloadedVideos.isEmpty()) {
                    emptyStateText.setVisibility(View.VISIBLE);
                } else {
                    emptyStateText.setVisibility(View.GONE);
                }

                Log.d("DownloadedVideos", "Loaded " + downloadedVideos.size() + " videos");
            });
        }).start();
    }







}
