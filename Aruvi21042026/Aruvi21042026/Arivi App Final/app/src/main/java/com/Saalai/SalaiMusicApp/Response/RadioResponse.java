package com.Saalai.SalaiMusicApp.Response;

import com.Saalai.SalaiMusicApp.Models.RadioModel;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RadioResponse {

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

    // Convenience method to get radio list
    public List<RadioModel> getRadioList() {
        if (response != null) {
            return response.getRadioList();
        }
        return null;
    }

    // Convenience method to get channel details (first item)
    public RadioModel getChannelDetails() {
        if (response != null && response.getChannelDetails() != null) {
            return response.getChannelDetails();
        }
        return null;
    }

    public static class ResponseData {
        @SerializedName("channelDetails")
        private RadioModel channelDetails;

        @SerializedName("radioList")
        private List<RadioModel> radioList;

        public RadioModel getChannelDetails() {
            return channelDetails;
        }

        public List<RadioModel> getRadioList() {
            return radioList;
        }
    }
}