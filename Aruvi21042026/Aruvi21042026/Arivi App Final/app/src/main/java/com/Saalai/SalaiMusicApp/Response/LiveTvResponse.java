package com.Saalai.SalaiMusicApp.Response;


import com.Saalai.SalaiMusicApp.Models.Channel;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LiveTvResponse {
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

    // Inner class for the "response" object
    public static class ResponseData {
        @SerializedName("channelList")
        private List<Channel> channelList;

        public List<Channel> getChannelList() {
            return channelList;
        }

        public void setChannelList(List<Channel> channelList) {
            this.channelList = channelList;
        }
    }
}