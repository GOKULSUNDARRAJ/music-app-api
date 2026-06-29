package com.Saalai.SalaiMusicApp.Response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DashboardResponse {

    @SerializedName("status")
    private boolean status;

    @SerializedName("error_type")
    private String errorType;

    @SerializedName("message")
    private String message;

    @SerializedName("response")
    private ResponseData response;

    // Getters
    public boolean isStatus() {
        return status;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    public ResponseData getResponse() {
        return response;
    }

    // Inner class for response data
    public static class ResponseData {
        @SerializedName("bannerList")
        private List<Banner> bannerList;

        @SerializedName("latestMovieList")
        private List<Movie> latestMovieList;

        @SerializedName("latestTVShowList")
        private List<TVShow> latestTVShowList;

        @SerializedName("latestChannelList")
        private List<Channel> latestChannelList;

        @SerializedName("latestRadioList")
        private List<Radio> latestRadioList;

        // Getters
        public List<Banner> getBannerList() {
            return bannerList;
        }

        public List<Movie> getLatestMovieList() {
            return latestMovieList;
        }

        public List<TVShow> getLatestTVShowList() {
            return latestTVShowList;
        }

        public List<Channel> getLatestChannelList() {
            return latestChannelList;
        }

        public List<Radio> getLatestRadioList() {
            return latestRadioList;
        }
    }

    // ---------- Inner Classes ----------
    public static class Banner {
        @SerializedName("banner")
        private String banner;

        public String getBanner() {
            return banner;
        }
    }

    public static class Movie {
        @SerializedName("channelId")
        private int channelId;

        @SerializedName("channelName")
        private String channelName;

        @SerializedName("channelLogo")
        private String channelLogo;

        @SerializedName("channelURL")
        private String channelURL;

        public int getChannelId() {
            return channelId;
        }

        public String getChannelName() {
            return channelName;
        }

        public String getChannelLogo() {
            return channelLogo;
        }

        public String getChannelURL() {
            return channelURL;
        }
    }

    public static class TVShow {
        @SerializedName("channelId")
        private int channelId;

        @SerializedName("channelName")
        private String channelName;

        @SerializedName("channelLogo")
        private String channelLogo;

        @SerializedName("channelURL")
        private String channelURL;

        public int getChannelId() {
            return channelId;
        }

        public String getChannelName() {
            return channelName;
        }

        public String getChannelLogo() {
            return channelLogo;
        }

        public String getChannelURL() {
            return channelURL;
        }
    }

    public static class Channel {
        @SerializedName("channelId")
        private int channelId;

        @SerializedName("channelName")
        private String channelName;

        @SerializedName("channelLogo")
        private String channelLogo;

        @SerializedName("channelURL")
        private String channelURL;

        public int getChannelId() {
            return channelId;
        }

        public String getChannelName() {
            return channelName;
        }

        public String getChannelLogo() {
            return channelLogo;
        }

        public String getChannelURL() {
            return channelURL;
        }
    }

    public static class Radio {
        @SerializedName("channelId")
        private int channelId;

        @SerializedName("channelName")
        private String channelName;

        @SerializedName("channelLogo")
        private String channelLogo;

        @SerializedName("channelURL")
        private String channelURL;

        public int getChannelId() {
            return channelId;
        }

        public String getChannelName() {
            return channelName;
        }

        public String getChannelLogo() {
            return channelLogo;
        }

        public String getChannelURL() {
            return channelURL;
        }
    }
}