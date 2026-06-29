package com.Saalai.SalaiMusicApp.Response;

public class RegisterResponse {
    private boolean status;
    private String message;
    private String error_type;
    private ResponseData response;

    public boolean isStatus() { return status; }
    public String getMessage() { return message; }
    public String getError_type() { return error_type; }
    public ResponseData getResponse() { return response; }

    public class ResponseData {
        private int userId;
        private String userName;
        private String userEmail;
        private String userMobile;
        private String userCountry;
        private String userCountryId;
        private String userCreatedDate;
        private String userCountryCode;
        private String token_type;
        private long expires_in;
        private String access_token;
        private String refresh_token;

        public int getUserId() { return userId; }
        public String getUserName() { return userName; }
        public String getUserEmail() { return userEmail; }
        public String getUserMobile() { return userMobile; }
        public String getUserCountry() { return userCountry; }
        public String getUserCountryId() { return userCountryId; }
        public String getUserCreatedDate() { return userCreatedDate; }
        public String getUserCountryCode() { return userCountryCode; }
        public String getToken_type() { return token_type; }
        public long getExpires_in() { return expires_in; }
        public String getAccess_token() { return access_token; }
        public String getRefresh_token() { return refresh_token; }
    }

}
