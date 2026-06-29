package com.Saalai.SalaiMusicApp.Response;

import com.Saalai.SalaiMusicApp.Models.CatchUp;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CatchUpResponse {

    @SerializedName("status")
    private boolean status;

    @SerializedName("error_type")
    private String errorType;

    @SerializedName("message")
    private String message;

    @SerializedName("response")
    private ResponseData response;

    // Getters and setters
    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ResponseData getResponse() {
        return response;
    }

    public void setResponse(ResponseData response) {
        this.response = response;
    }

    // Convenience method to get channel list
    public List<CatchUp> getChannelList() {
        if (response != null) {
            return response.getChannelList();
        }
        return null;
    }

    // Inner class for the nested response object
    public static class ResponseData {
        @SerializedName("channelList")
        private List<CatchUp> channelList;

        public List<CatchUp> getChannelList() {
            return channelList;
        }

        public void setChannelList(List<CatchUp> channelList) {
            this.channelList = channelList;
        }
    }
}