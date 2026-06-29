package com.Saalai.SalaiMusicApp.Response;



public class AddToPlaylistResponse {
    private boolean status;
    private String result;
    private String message;
    private boolean isAdded;
    private boolean added;

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isAdded() {
        return isAdded || added;
    }

    public void setAdded(boolean added) {
        this.isAdded = added;
        this.added = added;
    }

    public boolean getAdded() {
        return added;
    }
}