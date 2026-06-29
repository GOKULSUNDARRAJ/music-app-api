package com.Saalai.SalaiMusicApp.Response;


import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CatchUpChannelDetailsResponse {

    @SerializedName("channelId")
    private int channelId;

    @SerializedName("channelName")
    private String channelName;

    @SerializedName("channelDescription")
    private String channelDescription;

    @SerializedName("channelLogo")
    private String channelLogo;

    @SerializedName("showList")
    private List<ShowDate> showList;

    // Getters and Setters
    public int getChannelId() { return channelId; }
    public void setChannelId(int channelId) { this.channelId = channelId; }

    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }

    public String getChannelDescription() { return channelDescription; }
    public void setChannelDescription(String channelDescription) { this.channelDescription = channelDescription; }

    public String getChannelLogo() { return channelLogo; }
    public void setChannelLogo(String channelLogo) { this.channelLogo = channelLogo; }

    public List<ShowDate> getShowList() { return showList; }
    public void setShowList(List<ShowDate> showList) { this.showList = showList; }

    public static class ShowDate {
        @SerializedName("Date")
        private String date;

        @SerializedName("Day")
        private String day;

        @SerializedName("episodeList")
        private List<Episode> episodeList;

        // Getters and Setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public String getDay() { return day; }
        public void setDay(String day) { this.day = day; }

        public List<Episode> getEpisodeList() { return episodeList; }
        public void setEpisodeList(List<Episode> episodeList) { this.episodeList = episodeList; }
    }

    public static class Episode {
        @SerializedName("showName")
        private String showName;

        @SerializedName("showLogo")
        private String showLogo;

        @SerializedName("showType")
        private String showType;

        @SerializedName("showURL")
        private String showURL;

        // Getters and Setters
        public String getShowName() { return showName; }
        public void setShowName(String showName) { this.showName = showName; }

        public String getShowLogo() { return showLogo; }
        public void setShowLogo(String showLogo) { this.showLogo = showLogo; }

        public String getShowType() { return showType; }
        public void setShowType(String showType) { this.showType = showType; }

        public String getShowURL() { return showURL; }
        public void setShowURL(String showURL) { this.showURL = showURL; }
    }
}