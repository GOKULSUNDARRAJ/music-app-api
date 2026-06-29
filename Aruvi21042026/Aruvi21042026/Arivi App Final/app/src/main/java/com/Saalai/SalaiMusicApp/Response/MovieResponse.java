package com.Saalai.SalaiMusicApp.Response;


import com.Saalai.SalaiMusicApp.Models.MovieItem;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MovieResponse {

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

    // Convenience method
    public List<MovieItem> getMovieList() {
        if (response != null) {
            return response.getMovieList();
        }
        return null;
    }

    public static class ResponseData {
        @SerializedName("movieList")
        private List<MovieItem> movieList;

        public List<MovieItem> getMovieList() {
            return movieList;
        }
    }
}