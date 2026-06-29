package com.Saalai.SalaiMusicApp.Models;

public class TvShowEpisode {
    private int episodeId;
    private String episodeName;
    private String episodeLogo;
    private String episodeDate;
    private String channelId;
    private String episodeUrl; // 🔥 new field

    // Getters & Setters
    public int getEpisodeId() { return episodeId; }
    public void setEpisodeId(int episodeId) { this.episodeId = episodeId; }

    public String getEpisodeName() { return episodeName; }
    public void setEpisodeName(String episodeName) { this.episodeName = episodeName; }

    public String getEpisodeLogo() { return episodeLogo; }
    public void setEpisodeLogo(String episodeLogo) { this.episodeLogo = episodeLogo; }

    public String getEpisodeDate() { return episodeDate; }
    public void setEpisodeDate(String episodeDate) { this.episodeDate = episodeDate; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getEpisodeUrl() { return episodeUrl; }
    public void setEpisodeUrl(String episodeUrl) { this.episodeUrl = episodeUrl; }
}
