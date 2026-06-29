package com.Saalai.SalaiMusicApp.Response;


public class OtpResponse {
    private boolean status;
    private String message;
    private String error_type;

    public boolean isStatus() { return status; }
    public String getMessage() { return message; }
    public String getError_type() { return error_type; }
}

