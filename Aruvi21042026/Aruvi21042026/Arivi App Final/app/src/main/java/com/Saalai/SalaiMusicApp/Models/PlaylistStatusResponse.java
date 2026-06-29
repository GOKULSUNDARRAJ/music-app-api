package com.Saalai.SalaiMusicApp.Models;


public class PlaylistStatusResponse {
    private boolean status;
    private String result;
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