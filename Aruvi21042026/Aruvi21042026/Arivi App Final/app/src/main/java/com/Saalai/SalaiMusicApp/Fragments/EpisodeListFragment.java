package com.Saalai.SalaiMusicApp.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.CatchUpChannelDetailsResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.util.List;
public class EpisodeListFragment extends Fragment {

    private RecyclerView recyclerViewEpisodes;
    private EpisodeAdapter episodeAdapter;
    private List<CatchUpChannelDetailsResponse.Episode> episodes;
    private String channelName, channelLogo, channelDescription, tabTitle;
    private CatchUpDetailFragment parentFragment;
    private EpisodeClickListener episodeClickListener;

    // Interface for episode clicks
    public interface EpisodeClickListener {
        void onEpisodeClicked(CatchUpChannelDetailsResponse.Episode episode);
    }

    // Method to set parent fragment
    public void setParentFragment(CatchUpDetailFragment parentFragment) {
        this.parentFragment = parentFragment;
        this.episodeClickListener = parentFragment; // Parent fragment implements the interface
    }

    // Alternative method to set click listener directly
    public void setEpisodeClickListener(EpisodeClickListener listener) {
        this.episodeClickListener = listener;
    }

    public static EpisodeListFragment newInstance(List<CatchUpChannelDetailsResponse.Episode> episodes,
                                                  String channelName, String channelLogo,
                                                  String channelDescription, String tabTitle) {
        EpisodeListFragment fragment = new EpisodeListFragment();
        Bundle args = new Bundle();
        args.putString("episodes_json", new Gson().toJson(episodes));
        args.putString("channel_name", channelName);
        args.putString("channel_logo", channelLogo);
        args.putString("channel_description", channelDescription);
        args.putString("tab_title", tabTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_episode_list, container, false);

        // Get arguments
        Bundle args = getArguments();
        if (args != null) {
            String episodesJson = args.getString("episodes_json");
            episodes = new Gson().fromJson(episodesJson, new TypeToken<List<CatchUpChannelDetailsResponse.Episode>>(){}.getType());
            channelName = args.getString("channel_name");
            channelLogo = args.getString("channel_logo");
            channelDescription = args.getString("channel_description");
            tabTitle = args.getString("tab_title");
        }

        initializeViews(view);
        setupRecyclerView();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Try to get parent fragment if not set
        if (episodeClickListener == null) {
            Fragment parent = getParentFragment();
            if (parent instanceof CatchUpDetailFragment) {
                episodeClickListener = (CatchUpDetailFragment) parent;
            }
        }
    }

    private void initializeViews(View view) {
        recyclerViewEpisodes = view.findViewById(R.id.recyclerViewEpisodes);
    }

    private void setupRecyclerView() {
        int columnCount = 2;
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            columnCount = 4;
        }

        // Set GridLayoutManager with 3 columns
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), columnCount);
        recyclerViewEpisodes.setLayoutManager(gridLayoutManager);

        episodeAdapter = new EpisodeAdapter(episodes);
        recyclerViewEpisodes.setAdapter(episodeAdapter);
    }

    // Episode Adapter
    private class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder> {
        private List<CatchUpChannelDetailsResponse.Episode> episodes;

        public EpisodeAdapter(List<CatchUpChannelDetailsResponse.Episode> episodes) {
            this.episodes = episodes;
        }

        @Override
        public EpisodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_episode, parent, false);
            return new EpisodeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(EpisodeViewHolder holder, int position) {
            CatchUpChannelDetailsResponse.Episode episode = episodes.get(position);
            holder.bind(episode);
        }

        @Override
        public int getItemCount() {
            return episodes != null ? episodes.size() : 0;
        }

        class EpisodeViewHolder extends RecyclerView.ViewHolder {
            private ImageView ivEpisodeLogo;
            private TextView tvShowName;

            public EpisodeViewHolder(View itemView) {
                super(itemView);
                ivEpisodeLogo = itemView.findViewById(R.id.channel_image);
                tvShowName = itemView.findViewById(R.id.channel_name);

                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        CatchUpChannelDetailsResponse.Episode episode = episodes.get(position);

                        // Try episodeClickListener first
                        if (episodeClickListener != null) {
                            episodeClickListener.onEpisodeClicked(episode);
                        }
                        // Fallback to parentFragment
                        else if (parentFragment != null) {
                            parentFragment.playEpisode(episode);
                        }
                        // Final fallback
                        else {
                            Fragment parent = getParentFragment();
                            if (parent instanceof CatchUpDetailFragment) {
                                ((CatchUpDetailFragment) parent).playEpisode(episode);
                            } else {
                                Log.e("EpisodeListFragment", "No way to handle episode click");
                            }
                        }
                    }
                });
            }

            public void bind(CatchUpChannelDetailsResponse.Episode episode) {
                tvShowName.setText(episode.getShowName());

                String showName = episode.getShowName();
                if (showName.contains("(") && showName.contains(")")) {
                    String time = showName.substring(showName.indexOf("(") + 1, showName.indexOf(")"));
                }

                if (episode.getShowLogo() != null && !episode.getShowLogo().isEmpty()) {
                    Picasso.get()
                            .load(episode.getShowLogo())
                            .placeholder(R.drawable.movies_selected)
                            .into(ivEpisodeLogo);
                }
            }
        }
    }
}