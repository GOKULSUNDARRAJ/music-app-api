package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Models.TvShowEpisode;
import com.Saalai.SalaiMusicApp.R;
import com.squareup.picasso.Picasso;

import org.jspecify.annotations.NonNull;

import java.util.List;
public class TvShowEpisodeAdapter extends RecyclerView.Adapter<TvShowEpisodeAdapter.ViewHolder> {

    private Context context;
    private List<TvShowEpisode> episodeList;
    private OnEpisodeClickListener listener;

    public TvShowEpisodeAdapter(Context context, List<TvShowEpisode> episodeList, OnEpisodeClickListener listener) {
        this.context = context;
        this.episodeList = episodeList;
        this.listener = listener;
    }

    public interface OnEpisodeClickListener {
        void onEpisodeClick(TvShowEpisode episode);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tvshow_episode, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TvShowEpisode episode = episodeList.get(position);
        holder.name.setText(episode.getEpisodeName());
        holder.episodeSubInfo.setText(episode.getEpisodeDate());

        Picasso.get()
                .load(episode.getEpisodeLogo())
                .placeholder(R.drawable.video_placholder) // optional placeholder
                .error(R.drawable.video_placholder)             // optional error image
                .into(holder.logo);


        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEpisodeClick(episode);
        });
    }

    @Override
    public int getItemCount() {
        return episodeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView logo;
        TextView name, episodeSubInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.posterImage);
            name = itemView.findViewById(R.id.episodeTitle);
            episodeSubInfo = itemView.findViewById(R.id.episodeSubInfo);
        }
    }
}
