package com.Saalai.SalaiMusicApp.Response;


import com.google.gson.annotations.SerializedName;

public class EnquiryResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("error_type")
    private String errorType;

    @SerializedName("status")
    private boolean status;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}