package com.Saalai.SalaiMusicApp.Response;

// MovieDashboardResponse.java
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MovieDashboardResponse {
    @SerializedName("categoryName")
    private String categoryName;

    @SerializedName("channels")
    private List<Channel> channels;

    // Getters
    public String getCategoryName() { return categoryName; }
    public List<Channel> getChannels() { return channels; }

    public static class Channel {
        @SerializedName("channelLogo")
        private String channelLogo;

        @SerializedName("channelName")
        private String channelName;

        // Getters
        public String getChannelLogo() { return channelLogo; }
        public String getChannelName() { return channelName; }
    }
}
