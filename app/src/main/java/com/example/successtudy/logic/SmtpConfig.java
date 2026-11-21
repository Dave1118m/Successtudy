package com.example.successtudy.logic;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SMTP Configuration Manager
 * Stores and retrieves email server settings for sending verification emails
 */
public class SmtpConfig {
    private static final String PREF_NAME = "SmtpConfig";
    private static final String KEY_SMTP_HOST = "smtp_host";
    private static final String KEY_SMTP_PORT = "smtp_port";
    private static final String KEY_SENDER_EMAIL = "sender_email";
    private static final String KEY_SENDER_PASSWORD = "sender_password";
    private static final String KEY_USE_TLS = "use_tls";
    private static final String KEY_IS_CONFIGURED = "is_configured";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    // Default values for Gmail
    private static final String DEFAULT_SMTP_HOST = "smtp.gmail.com";
    private static final String DEFAULT_SMTP_PORT = "587";
    private static final boolean DEFAULT_USE_TLS = true;

    public SmtpConfig(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /*
     * Set SMTP configuration
     * @param smtpHost SMTP server hostname (e.g., smtp.gmail.com, smtp-mail.outlook.com)
     * @param smtpPort SMTP port (typically 587 for TLS, 465 for SSL)
     * @param senderEmail Email address to send from
     * @param appPassword App-specific password (not regular password)
     * @param useTls Whether to use TLS encryption
     */
    public void setSmtpConfig(String smtpHost, String smtpPort, String senderEmail, String appPassword, boolean useTls) {
        editor.putString(KEY_SMTP_HOST, smtpHost);
        editor.putString(KEY_SMTP_PORT, smtpPort);
        editor.putString(KEY_SENDER_EMAIL, senderEmail);
        editor.putString(KEY_SENDER_PASSWORD, appPassword);
        editor.putBoolean(KEY_USE_TLS, useTls);
        editor.putBoolean(KEY_IS_CONFIGURED, true);
        editor.apply();
    }

    public String getSmtpHost() {
        return pref.getString(KEY_SMTP_HOST, DEFAULT_SMTP_HOST);
    }

    public String getSmtpPort() {
        return pref.getString(KEY_SMTP_PORT, DEFAULT_SMTP_PORT);
    }

    public String getSenderEmail() {
        return pref.getString(KEY_SENDER_EMAIL, "");
    }

    public String getSenderPassword() {
        return pref.getString(KEY_SENDER_PASSWORD, "");
    }

    public boolean isUseTls() {
        return pref.getBoolean(KEY_USE_TLS, DEFAULT_USE_TLS);
    }

    public boolean isConfigured() {
        return pref.getBoolean(KEY_IS_CONFIGURED, false);
    }

    /**
     * Reset to default Gmail config (for testing/development)
     * User must set actual password
     */
    public void resetToDefaults() {
        editor.clear();
        editor.putString(KEY_SMTP_HOST, DEFAULT_SMTP_HOST);
        editor.putString(KEY_SMTP_PORT, DEFAULT_SMTP_PORT);
        editor.putBoolean(KEY_USE_TLS, DEFAULT_USE_TLS);
        editor.apply();
    }
}
