package com.Saalai.SalaiMusicApp.SharedPrefManager;


import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {

    private static final String PREF_NAME = "MyAppPrefs";
    private static SharedPrefManager instance;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private SharedPrefManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    // ✅ Save User Data
    public void saveUser(int userId, String userName, String userEmail, String userMobile,
                         String userCountry, String userCountryId, String userCreatedDate,
                         String userCountryCode, String accessToken, String refreshToken,
                         String tokenType, long expiresIn) {

        editor.putInt("userId", userId);
        editor.putString("userName", userName);
        editor.putString("userEmail", userEmail);
        editor.putString("userMobile", userMobile);
        editor.putString("userCountry", userCountry);
        editor.putString("userCountryId", userCountryId);
        editor.putString("userCreatedDate", userCreatedDate);
        editor.putString("userCountryCode", userCountryCode);
        editor.putString("access_token", accessToken);
        editor.putString("refresh_token", refreshToken);
        editor.putString("token_type", tokenType);
        editor.putLong("expires_in", expiresIn);
        editor.apply();
    }

    // ✅ Getters
    public int getUserId() { return sharedPreferences.getInt("userId", -1); }
    public String getUserName() { return sharedPreferences.getString("userName", ""); }
    public String getUserEmail() { return sharedPreferences.getString("userEmail", ""); }
    public String getUserMobile() { return sharedPreferences.getString("userMobile", ""); }
    public String getUserCountry() { return sharedPreferences.getString("userCountry", ""); }
    public String getUserCountryId() { return sharedPreferences.getString("userCountryId", ""); }
    public String getUserCreatedDate() { return sharedPreferences.getString("userCreatedDate", ""); }
    public String getUserCountryCode() { return sharedPreferences.getString("userCountryCode", ""); }
    public String getAccessToken() { return sharedPreferences.getString("access_token", ""); }
    public String getRefreshToken() { return sharedPreferences.getString("refresh_token", ""); }
    public String getTokenType() { return sharedPreferences.getString("token_type", ""); }
    public long getExpiresIn() { return sharedPreferences.getLong("expires_in", 0); }

    // ✅ Clear Data (Logout)
    public void clear() {
        editor.clear();
        editor.apply();
    }


    // ✅ Check if user is logged in
    public boolean isLoggedIn() {
        return sharedPreferences.contains("access_token") && !getAccessToken().isEmpty();
    }

    // Clear only access token
    public void clearAccessToken() {
        editor.remove("access_token");
        editor.apply();
    }


}
