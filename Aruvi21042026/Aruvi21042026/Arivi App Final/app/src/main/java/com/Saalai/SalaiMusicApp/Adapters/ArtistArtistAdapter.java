package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Models.ArtistCategory;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class ArtistArtistAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ArtistCategory> artistList;
    private OnArtistClickListener onArtistClickListener;
    private Context context;



    public interface OnArtistClickListener {
        void onArtistClick(String artistName, List<AudioModel> songs, String artistImageUrl, String categoryId);
    }

    public ArtistArtistAdapter(List<ArtistCategory> artistList, OnArtistClickListener listener) {
        this.artistList = artistList;
        this.onArtistClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return artistList.get(position).getAdapterType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == 1) {
            View type1View = inflater.inflate(R.layout.item_artist_type1, parent, false);
            return new Type1ViewHolder(type1View);
        } else if (viewType == 2) {
            View type2View = inflater.inflate(R.layout.item_artist_type2, parent, false);
            return new Type2ViewHolder(type2View);
        } else if (viewType == 3) {
            View type3View = inflater.inflate(R.layout.item_artist_type3, parent, false);
            return new Type3ViewHolder(type3View);
        } else {
            // Type 4
            View type4View = inflater.inflate(R.layout.item_artist_type4, parent, false);
            return new Type4ViewHolder(type4View);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ArtistCategory artist = artistList.get(position);

        // Check if this is the current playlist by ID
        boolean isCurrentPlaylist = isCurrentPlaylistById(artist);

        int viewType = holder.getItemViewType();
        if (viewType == 1) {
            ((Type1ViewHolder) holder).bind(artist, isCurrentPlaylist);
        } else if (viewType == 2) {
            ((Type2ViewHolder) holder).bind(artist, isCurrentPlaylist);
        } else if (viewType == 3) {
            ((Type3ViewHolder) holder).bind(artist, isCurrentPlaylist);
        } else {
            ((Type4ViewHolder) holder).bind(artist, isCurrentPlaylist);
        }
    }

    // Method to check if this is the current playlist by comparing ONLINE category IDs
    private boolean isCurrentPlaylistById(ArtistCategory artist) {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) {
            Log.d("ArtistAdapter", "No current audio playing");
            return false;
        }

        // Get category IDs
        String currentCategoryId = currentAudio.getCategoryId();
        String artistCategoryId = artist.getCategoryId();

        // ✅ SIMPLY LOG THE CURRENTLY PLAYING CATEGORY ID
        Log.d("ArtistAdapter", "🔊 Currently playing cat ID: " + currentCategoryId +
                " | Song: " + currentAudio.getAudioName());
        Log.d("ArtistAdapter", "🎯 Comparing with artist cat ID: " + artistCategoryId +
                " | Artist: " + artist.getCategoryName());

        // Direct ID comparison
        boolean isMatch = currentCategoryId != null &&
                artistCategoryId != null &&
                currentCategoryId.equals(artistCategoryId);

        Log.d("ArtistAdapter", "✅ Match: " + isMatch);

        return isMatch;
    }

    @Override
    public int getItemCount() {
        return artistList.size();
    }

    public void updateData(List<ArtistCategory> newArtistList) {
        this.artistList = newArtistList;
        notifyDataSetChanged();
    }

    // Method to refresh highlighting
    public void refreshPlayingState() {
        notifyDataSetChanged();
    }

    // Type 1: Square with radius
    class Type1ViewHolder extends RecyclerView.ViewHolder {
        private TextView artistName;
        private TextView songCount;
        private CardView cardView;
        private ImageView artistImage;

        public Type1ViewHolder(@NonNull View itemView) {
            super(itemView);
            artistName = itemView.findViewById(R.id.artistName);
            songCount = itemView.findViewById(R.id.songCount);
            cardView = itemView.findViewById(R.id.artistCard);
            artistImage = itemView.findViewById(R.id.artistImage);
        }

        public void bind(ArtistCategory artist, boolean isCurrentPlaylist) {
            artistName.setText(artist.getCategoryName());
            songCount.setText(artist.getSongs().size() + " songs");

            // Load image
            Glide.with(itemView.getContext())
                    .load(artist.getArtistImageUrl())
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(32)))
                    .into(artistImage);

            // Highlight if this is the current playlist
            if (isCurrentPlaylist) {
                // Set red text color
                artistName.setTextColor(context.getResources().getColor(R.color.bgred)); // Use your red color
                // You can also highlight other elements if needed

            } else {
                // Reset to normal colors
                artistName.setTextColor(context.getResources().getColor(R.color.white));

            }

            cardView.setOnClickListener(v -> {
                if (onArtistClickListener != null) {
                    onArtistClickListener.onArtistClick(
                            artist.getCategoryName(),
                            artist.getSongs(),
                            artist.getArtistImageUrl(),
                            artist.getCategoryId()

                    );
                }
            });
        }
    }

    // Type 2: Round
    class Type2ViewHolder extends RecyclerView.ViewHolder {
        private TextView artistName;
        private TextView songCount;
        private CardView cardView;
        private ImageView artistImage;

        public Type2ViewHolder(@NonNull View itemView) {
            super(itemView);
            artistName = itemView.findViewById(R.id.artistName);
            songCount = itemView.findViewById(R.id.songCount);
            cardView = itemView.findViewById(R.id.artistCard);
            artistImage = itemView.findViewById(R.id.artistImage);
        }

        public void bind(ArtistCategory artist, boolean isCurrentPlaylist) {
            artistName.setText(artist.getCategoryName());
            songCount.setText(artist.getSongs().size() + " songs");

            Glide.with(itemView.getContext())
                    .load(artist.getArtistImageUrl())
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .into(artistImage);

            // Highlight if this is the current playlist
            if (isCurrentPlaylist) {
                artistName.setTextColor(context.getResources().getColor(R.color.bgred));
                songCount.setTextColor(context.getResources().getColor(R.color.bgred));
                // Optional: highlight card background

            } else {

                artistName.setTextColor(context.getResources().getColor(R.color.white));
                songCount.setTextColor(context.getResources().getColor(R.color.light_gray));
            }

            cardView.setOnClickListener(v -> {
                if (onArtistClickListener != null) {
                    onArtistClickListener.onArtistClick(
                            artist.getCategoryName(),
                            artist.getSongs(),
                            artist.getArtistImageUrl(),
                            artist.getCategoryId()
                    );
                }
            });
        }
    }

    // Type 3: Different layout
    class Type3ViewHolder extends RecyclerView.ViewHolder {
        private TextView artistName;
        private TextView songCount;
        private CardView cardView;
        private ImageView artistImage;

        public Type3ViewHolder(@NonNull View itemView) {
            super(itemView);
            artistName = itemView.findViewById(R.id.artistName);
            songCount = itemView.findViewById(R.id.songCount);
            cardView = itemView.findViewById(R.id.artistCard);
            artistImage = itemView.findViewById(R.id.artistImage);
        }

        public void bind(ArtistCategory artist, boolean isCurrentPlaylist) {
            artistName.setText(artist.getCategoryName());
            songCount.setText(artist.getSongs().size() + " songs");

            Glide.with(itemView.getContext())
                    .load(artist.getArtistImageUrl())
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(16)))
                    .into(artistImage);

            // Highlight if this is the current playlist
            if (isCurrentPlaylist) {
                artistName.setTextColor(context.getResources().getColor(R.color.bgred));
                songCount.setTextColor(context.getResources().getColor(R.color.bgred));
            } else {
                artistName.setTextColor(context.getResources().getColor(R.color.white));
                songCount.setTextColor(context.getResources().getColor(R.color.light_gray));
            }

            cardView.setOnClickListener(v -> {
                if (onArtistClickListener != null) {
                    onArtistClickListener.onArtistClick(
                            artist.getCategoryName(),
                            artist.getSongs(),
                            artist.getArtistImageUrl(),
                            artist.getCategoryId()
                    );
                }
            });
        }
    }

    // Type 4: New layout
    class Type4ViewHolder extends RecyclerView.ViewHolder {
        private TextView artistName;
        private TextView songCount;
        private CardView cardView;
        private ImageView artistImage;

        public Type4ViewHolder(@NonNull View itemView) {
            super(itemView);
            artistName = itemView.findViewById(R.id.artistName);
            songCount = itemView.findViewById(R.id.songCount);
            cardView = itemView.findViewById(R.id.artistCard);
            artistImage = itemView.findViewById(R.id.artistImage);
        }

        public void bind(ArtistCategory artist, boolean isCurrentPlaylist) {
            artistName.setText(artist.getCategoryName());
            songCount.setText(artist.getSongs().size() + " songs");

            // Load image with different transformation for type 4
            Glide.with(itemView.getContext())
                    .load(artist.getArtistImageUrl())
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(24)))
                    .into(artistImage);

            // Highlight if this is the current playlist
            if (isCurrentPlaylist) {
                artistName.setTextColor(context.getResources().getColor(R.color.bgred));
                songCount.setTextColor(context.getResources().getColor(R.color.bgred));
                // Optional: highlight card

            } else {

                artistName.setTextColor(context.getResources().getColor(R.color.white));
                songCount.setTextColor(context.getResources().getColor(R.color.light_gray));
            }

            cardView.setOnClickListener(v -> {
                if (onArtistClickListener != null) {
                    onArtistClickListener.onArtistClick(
                            artist.getCategoryName(),
                            artist.getSongs(),
                            artist.getArtistImageUrl(),
                            artist.getCategoryId()
                    );
                }
            });
        }
    }
}