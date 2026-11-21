package com.example.successtudy.logic;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.successtudy.database.entity.User;

import java.util.Random;

public class SessionManager {
    private static final String PREF_NAME = "SuccesstudySession";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_CURRENT_USER_ROLE = "userRole";
    private static final String ADMIN_ROLE = "Admin";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_TOKEN = "jwtToken";
    private static final String KEY_VERIFY_CODE = "verificationCode";
    private static final String KEY_IS_VERIFIED = "isVerified";
    private static final String KEY_PENDING_EMAIL = "pendingEmail";
    private static final String KEY_GEMINI_API_KEY = "geminiApiKey";
    private static final String KEY_GEMINI_MODEL = "geminiModel";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void setPendingEmail(String email) {
        editor.putString(KEY_PENDING_EMAIL, email);
        editor.apply();
    }

    public String getPendingEmail() {
        return pref.getString(KEY_PENDING_EMAIL, "");
    }

    public void loginUser(User user) {
        editor.putInt(KEY_USER_ID, user.id);
        editor.putString(KEY_USER_NAME, user.name);
        editor.putString(KEY_USER_EMAIL, user.email);
        editor.putString(KEY_CURRENT_USER_ROLE, user.role);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putBoolean(KEY_IS_VERIFIED, user.isVerified);
        
        // Generate simulated JWT: "header.payload.signature"
        String mockJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiI" + user.id + "\",\"name\":\"" + user.name + "\",\"role\":\"" + user.role + "\"}." +
                "mock_signature_abc123";
        editor.putString(KEY_TOKEN, mockJwt);
        editor.apply();
    }
    public void logoutUser() {
        editor.clear();
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getCurrentUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }

    public String getCurrentUserName() {
        return pref.getString(KEY_USER_NAME, "");
    }

    public String getCurrentUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "");
    }

    public String getCurrentUserRole() {
        return pref.getString(KEY_CURRENT_USER_ROLE, "Student");
    }

    public String getCurrentUserToken() {
        return pref.getString(KEY_TOKEN, "");
    }

    public boolean isCurrentUserVerified() {
        return pref.getBoolean(KEY_IS_VERIFIED, false);
    }

    /**
     * Returns true if the logged‑in user has the admin role.
     */
    public boolean isAdmin() {
        return ADMIN_ROLE.equalsIgnoreCase(getCurrentUserRole());
    }

    public void setCurrentUserVerified(boolean isVerified) {
        editor.putBoolean(KEY_IS_VERIFIED, isVerified);
        editor.apply();
    }

    public String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6 digits
        String codeStr = String.valueOf(code);
        editor.putString(KEY_VERIFY_CODE, codeStr);
        editor.apply();
        return codeStr;
    }

    public String getVerificationCode() {
        return pref.getString(KEY_VERIFY_CODE, "");
    }

    public boolean verifyCode(String inputCode) {
        String storedCode = getVerificationCode();
        if (storedCode != null && storedCode.equals(inputCode)) {
            setCurrentUserVerified(true);
            return true;
        }
        return false;
    }

    public void clearPendingEmail() {
        editor.remove(KEY_PENDING_EMAIL);
        editor.apply();
    }

    public void setGeminiApiKey(String apiKey) {
        editor.putString(KEY_GEMINI_API_KEY, apiKey);
        editor.apply();
    }

    public String getGeminiApiKey() {
        return pref.getString(KEY_GEMINI_API_KEY, "");
    }

    public void setGeminiModel(String model) {
        editor.putString(KEY_GEMINI_MODEL, model);
        editor.apply();
    }

    public String getGeminiModel() {
        return pref.getString(KEY_GEMINI_MODEL, "gemini-flash-latest");
    }
}
