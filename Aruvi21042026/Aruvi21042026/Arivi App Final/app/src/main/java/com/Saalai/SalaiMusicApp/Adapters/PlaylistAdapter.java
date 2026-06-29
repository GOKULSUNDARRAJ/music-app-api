package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Models.PlaylistModel;
import com.Saalai.SalaiMusicApp.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private ArrayList<PlaylistModel> playlistList;
    private Context context;
    private PlaylistClickListener clickListener;

    // Track playing playlist by CATEGORY ID (not playlist ID)
    private List<String> currentlyPlayingCategoryIds = new ArrayList<>();
    private RecyclerView recyclerView;

    public interface PlaylistClickListener {
        void onPlaylistClick(PlaylistModel playlist);
        void onPlaylistDelete(PlaylistModel playlist);
        void onPlaylistRename(PlaylistModel playlist);
    }

    public PlaylistAdapter(ArrayList<PlaylistModel> playlistList, PlaylistClickListener clickListener) {
        this.playlistList = playlistList;
        this.clickListener = clickListener;
    }

    // Set recycler view reference
    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    // Update playing playlist by CATEGORY ID
    public void setCurrentlyPlayingPlaylists(List<String> categoryIds) {
        this.currentlyPlayingCategoryIds.clear();
        if (categoryIds != null) {
            this.currentlyPlayingCategoryIds.addAll(categoryIds);
        }
        notifyDataSetChanged();
        Log.d("PlaylistAdapter", "Currently playing category IDs: " + currentlyPlayingCategoryIds);
    }

    // For backward compatibility
    public void setCurrentlyPlayingPlaylist(String categoryId) {
        this.currentlyPlayingCategoryIds.clear();
        if (categoryId != null) {
            this.currentlyPlayingCategoryIds.add(categoryId);
        }
        notifyDataSetChanged();
        Log.d("PlaylistAdapter", "Currently playing category ID: " + categoryId);
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_downloadplaylist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        PlaylistModel playlist = playlistList.get(position);
        String playlistCategoryId = playlist.getOriginalCategoryId();

        // 🔴 GET CURRENT PLAYING CATEGORY ID FROM PLAYERMANAGER
        String currentPlayingCategoryId = null;
        com.Saalai.SalaiMusicApp.Models.AudioModel currentAudio = com.Saalai.SalaiMusicApp.PlayerManager.getCurrentAudio();
        if (currentAudio != null) {
            currentPlayingCategoryId = currentAudio.getCategoryId();
            Log.d("PlaylistAdapter", "🔊 Current playing cat ID from PlayerManager: " + currentPlayingCategoryId +
                    " | Song: " + currentAudio.getAudioName());
        }

        // Set playlist name with color based on playing status
        holder.tvPlaylistName.setText(playlist.getName());

        // 🔴 CHECK IF THIS PLAYLIST'S CATEGORY ID MATCHES CURRENT PLAYING CATEGORY ID
        boolean isPlaying = false;

        if (playlistCategoryId != null && currentPlayingCategoryId != null) {
            Log.d("PlaylistAdapter", "🔍 Comparing: Playlist '" + playlist.getName() +
                    "' cat ID: " + playlistCategoryId +
                    " vs Current: " + currentPlayingCategoryId);

            // ONLY EXACT MATCH - NO SUFFIX HANDLING
            if (playlistCategoryId.equals(currentPlayingCategoryId)) {
                isPlaying = true;
                Log.d("PlaylistAdapter", "✅ EXACT MATCH: " + playlistCategoryId + " = " + currentPlayingCategoryId);
            } else {
                Log.d("PlaylistAdapter", "❌ NO MATCH: " + playlistCategoryId + " != " + currentPlayingCategoryId);
            }
        } else {
            Log.d("PlaylistAdapter", "⚠️ Cannot check: Playlist cat ID: " + playlistCategoryId +
                    ", Current cat ID: " + currentPlayingCategoryId);
        }

        // Set colors based on playing status
        if (isPlaying && com.Saalai.SalaiMusicApp.PlayerManager.isPlaying()) {
            holder.tvPlaylistName.setTextColor(ContextCompat.getColor(context, R.color.bgred));
            holder.tvSongCount.setTextColor(ContextCompat.getColor(context, R.color.bgred));
            Log.d("PlaylistAdapter", "🎵 HIGHLIGHTING PLAYLIST: " + playlist.getName() + " is currently playing");
        } else {
            holder.tvPlaylistName.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.tvSongCount.setTextColor(ContextCompat.getColor(context, R.color.gray));
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }

        // Rest of your code remains the same...
        // Set song count
        int songCount = playlist.getSongCount();
        holder.tvSongCount.setText(songCount + " song" + (songCount != 1 ? "s" : ""));

        // Load playlist image
        if (playlist.getImageUrl() != null && !playlist.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(playlist.getImageUrl())
                    .apply(new RequestOptions()
                            .transform(new RoundedCorners(16))
                            .placeholder(R.drawable.video_placholder)
                            .error(R.drawable.video_placholder))
                    .into(holder.imgPlaylist);
        } else {
            holder.imgPlaylist.setImageResource(R.drawable.video_placholder);
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                Log.d("PlaylistAdapter", "Playlist clicked: " + playlist.getName() +
                        " (Category ID: " + playlistCategoryId + ")");
                clickListener.onPlaylistClick(playlist);
            }
        });

        // Menu button click - show custom popup
        holder.btnMenu.setOnClickListener(v -> {
            showCustomMenu(v, position, playlist);
        });
    }

    private void showCustomMenu(View anchorView, int position, PlaylistModel playlist) {
        if (context != null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View popupView = inflater.inflate(R.layout.popup_playlist_menu, null);

            // Find the layouts
            View menuDeleteLayout = popupView.findViewById(R.id.menuDelete);

            // Get the exact width from the inflated view
            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int widthInPx = popupView.getMeasuredWidth();
            int heightInPx = popupView.getMeasuredHeight();

            // Create the popup window
            PopupWindow popupWindow = new PopupWindow(
                    popupView,
                    widthInPx,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );

            // Configure popup window
            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupWindow.setOutsideTouchable(true);
            popupWindow.setElevation(16f);
            popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

            // Handle Delete click
            menuDeleteLayout.setOnClickListener(v -> {
                popupWindow.dismiss();
                if (clickListener != null) {
                    clickListener.onPlaylistDelete(playlist);
                }
            });

            // Get the anchor view location
            int[] location = new int[2];
            anchorView.getLocationOnScreen(location);

            // Calculate position to show popup aligned to right edge and vertically centered
            int xPos = location[0] + anchorView.getWidth() - widthInPx;
            int yPos = location[1] - (heightInPx / 2) + (anchorView.getHeight() / 2);

            // Adjust if goes off screen
            if (yPos < 0) yPos = 0;

            // Also check if popup goes off bottom of screen
            int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            if (yPos + heightInPx > screenHeight) {
                yPos = screenHeight - heightInPx - 50; // 50dp margin from bottom
            }

            // Show popup at calculated position
            popupWindow.showAtLocation(
                    anchorView,
                    Gravity.NO_GRAVITY,
                    xPos,
                    yPos
            );

            // Close popup when clicked outside
            popupWindow.setTouchInterceptor((v1, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_OUTSIDE) {
                    popupWindow.dismiss();
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public int getItemCount() {
        return playlistList.size();
    }

    public void updateList(ArrayList<PlaylistModel> newList) {
        playlistList.clear();
        playlistList.addAll(newList);
        notifyDataSetChanged();
    }

    public void clearPlayingStatus() {
        currentlyPlayingCategoryIds.clear();
        notifyDataSetChanged();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPlaylist, btnMenu;
        TextView tvPlaylistName, tvSongCount;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);

            imgPlaylist = itemView.findViewById(R.id.imgPlaylist);
            tvPlaylistName = itemView.findViewById(R.id.tvPlaylistName);
            tvSongCount = itemView.findViewById(R.id.tvSongCount);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }
    }
}