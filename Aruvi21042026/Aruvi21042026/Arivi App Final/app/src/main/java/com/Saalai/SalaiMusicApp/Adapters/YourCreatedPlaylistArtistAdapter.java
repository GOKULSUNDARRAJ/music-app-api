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

public class YourCreatedPlaylistArtistAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ArtistCategory> artistList;
    private OnArtistClickListener onArtistClickListener;
    private Context context;

    public interface OnArtistClickListener {
        void onArtistClick(String artistName, List<AudioModel> songs, String artistImageUrl);
    }

    public YourCreatedPlaylistArtistAdapter(List<ArtistCategory> artistList, OnArtistClickListener listener) {
        this.artistList = artistList;
        this.onArtistClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        ArtistCategory artist = artistList.get(position);
        return artist.getAdapterType() != 0 ? artist.getAdapterType() : 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case 1:
                View type1View = inflater.inflate(R.layout.item_artist_type1, parent, false);
                return new Type1ViewHolder(type1View);
            case 2:
                View type2View = inflater.inflate(R.layout.item_artist_type2, parent, false);
                return new Type2ViewHolder(type2View);
            case 3:
                View type3View = inflater.inflate(R.layout.item_artist_type3, parent, false);
                return new Type3ViewHolder(type3View);
            case 4:
                View type4View = inflater.inflate(R.layout.item_artist_type4, parent, false);
                return new Type4ViewHolder(type4View);
            case 5:
                View type5View = inflater.inflate(R.layout.item_artist_type5, parent, false);
                return new Type5ViewHolder(type5View);
            case 6:
                View type6View = inflater.inflate(R.layout.item_artist_type6, parent, false);
                return new Type6ViewHolder(type6View);
            default:
                View defaultView = inflater.inflate(R.layout.item_artist_type1, parent, false);
                return new Type1ViewHolder(defaultView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ArtistCategory artist = artistList.get(position);

        // Check if this is the current playlist by ID
        boolean isCurrentPlaylist = isCurrentPlaylistById(artist);

        int viewType = holder.getItemViewType();
        switch (viewType) {
            case 1:
                ((Type1ViewHolder) holder).bind(artist, isCurrentPlaylist);
                break;
            case 2:
                ((Type2ViewHolder) holder).bind(artist, isCurrentPlaylist);
                break;
            case 3:
                ((Type3ViewHolder) holder).bind(artist, isCurrentPlaylist);
                break;
            case 4:
                ((Type4ViewHolder) holder).bind(artist, isCurrentPlaylist);
                break;
            case 5:
                ((Type5ViewHolder) holder).bind(artist, isCurrentPlaylist);
                break;
            case 6:
                ((Type6ViewHolder) holder).bind(artist, isCurrentPlaylist);
                break;
            default:
                ((Type1ViewHolder) holder).bind(artist, isCurrentPlaylist);
                break;
        }
    }

    private boolean isCurrentPlaylistById(ArtistCategory artist) {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) {
            return false;
        }

        String currentCategoryId = currentAudio.getCategoryId();
        String artistCategoryId = artist.getCategoryId();

        Log.d("YourCreatedPlaylistArtistAdapter", "Currently playing cat ID: " + currentCategoryId);
        Log.d("YourCreatedPlaylistArtistAdapter", "Artist cat ID: " + artistCategoryId);

        return currentCategoryId != null &&
                artistCategoryId != null &&
                currentCategoryId.equals(artistCategoryId);
    }

    @Override
    public int getItemCount() {
        return artistList != null ? artistList.size() : 0;
    }

    public void updateData(List<ArtistCategory> newArtistList) {
        this.artistList = newArtistList;
        notifyDataSetChanged();
    }

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
            if (artist.getArtistImageUrl() != null && !artist.getArtistImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(artist.getArtistImageUrl())
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(32)))
                        .placeholder(R.drawable.video_placholder)
                        .into(artistImage);
            } else {
                artistImage.setImageResource(R.drawable.video_placholder);
            }

            // Highlight if this is the current playlist
            if (isCurrentPlaylist) {
                artistName.setTextColor(context.getResources().getColor(R.color.bgred));
            } else {
                artistName.setTextColor(context.getResources().getColor(R.color.white));
            }

            cardView.setOnClickListener(v -> {
                if (onArtistClickListener != null) {
                    onArtistClickListener.onArtistClick(
                            artist.getCategoryName(),
                            artist.getSongs(),
                            artist.getArtistImageUrl()
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

            if (artist.getArtistImageUrl() != null && !artist.getArtistImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(artist.getArtistImageUrl())
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .placeholder(R.drawable.video_placholder)
                        .into(artistImage);
            } else {
                artistImage.setImageResource(R.drawable.video_placholder);
            }

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
                            artist.getArtistImageUrl()
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

            if (artist.getArtistImageUrl() != null && !artist.getArtistImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(artist.getArtistImageUrl())
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(16)))
                        .placeholder(R.drawable.video_placholder)
                        .into(artistImage);
            } else {
                artistImage.setImageResource(R.drawable.video_placholder);
            }

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
                            artist.getArtistImageUrl()
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

            if (artist.getArtistImageUrl() != null && !artist.getArtistImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(artist.getArtistImageUrl())
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(24)))
                        .placeholder(R.drawable.video_placholder)
                        .into(artistImage);
            } else {
                artistImage.setImageResource(R.drawable.video_placholder);
            }

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
                            artist.getArtistImageUrl()
                    );
                }
            });
        }
    }

    // Type 5: Featured layout (Large image with overlay text and badge)
    class Type5ViewHolder extends RecyclerView.ViewHolder {
        private TextView artistName;
        private TextView songCount;

        private CardView cardView;
        private ImageView artistImage;

        public Type5ViewHolder(@NonNull View itemView) {
            super(itemView);
            artistName = itemView.findViewById(R.id.artistName);
            songCount = itemView.findViewById(R.id.songCount);

            cardView = itemView.findViewById(R.id.artistCard);
            artistImage = itemView.findViewById(R.id.artistImage);
        }

        public void bind(ArtistCategory artist, boolean isCurrentPlaylist) {
            artistName.setText(artist.getCategoryName());
            songCount.setText(artist.getSongs().size() + " songs");



            // Load image with larger rounded corners for featured style
            if (artist.getArtistImageUrl() != null && !artist.getArtistImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(artist.getArtistImageUrl())
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(16)))
                        .placeholder(R.drawable.video_placholder)
                        .override(400, 400)
                        .into(artistImage);
            } else {
                artistImage.setImageResource(R.drawable.video_placholder);
            }

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
                            artist.getArtistImageUrl()
                    );
                }
            });
        }
    }

    // Type 6: Compact horizontal layout (for dense displays)
    class Type6ViewHolder extends RecyclerView.ViewHolder {
        private TextView artistName;
        private TextView songCount;

        private CardView cardView;
        private ImageView artistImage;

        public Type6ViewHolder(@NonNull View itemView) {
            super(itemView);
            artistName = itemView.findViewById(R.id.artistName);
            songCount = itemView.findViewById(R.id.songCount);

            cardView = itemView.findViewById(R.id.artistCard);
            artistImage = itemView.findViewById(R.id.artistImage);
        }

        public void bind(ArtistCategory artist, boolean isCurrentPlaylist) {
            artistName.setText(artist.getCategoryName());
            songCount.setText(artist.getSongs().size() + " songs");




            // Load image with small rounded corners
            if (artist.getArtistImageUrl() != null && !artist.getArtistImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(artist.getArtistImageUrl())
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(12)))
                        .placeholder(R.drawable.video_placholder)
                        .override(80, 80)
                        .into(artistImage);
            } else {
                artistImage.setImageResource(R.drawable.video_placholder);
            }

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
                            artist.getArtistImageUrl()
                    );
                }
            });
        }
    }
}