package com.example.successtudy.logic;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.Properties;
import java.util.Random;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Helper class to manage email verification codes and send verification emails via SMTP.
 */
public class EmailVerificationHelper {
    private static final String TAG = "EmailVerification";
    
    public static String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Sends verification email via SMTP.
     */
    public static void sendVerificationEmail(Context context, String recipientEmail, String verificationCode, String userName) {
        new Thread(() -> {
            boolean success = false;
            String errorMessage = "Unknown error";
            try {
                SmtpConfig smtpConfig = new SmtpConfig(context);
                String senderEmail = smtpConfig.getSenderEmail();
                String appPassword = smtpConfig.getSenderPassword();
                
                if (senderEmail.isEmpty() || appPassword.isEmpty()) {
                    errorMessage = "SMTP Credentials missing. Please check settings.";
                    Log.e(TAG, errorMessage);
                } else {
                    Properties props = new Properties();
                    String host = smtpConfig.getSmtpHost();
                    String port = smtpConfig.getSmtpPort();
                    boolean useTls = smtpConfig.isUseTls();

                    props.put("mail.smtp.host", host);
                    props.put("mail.smtp.port", port);
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.ssl.trust", host); // Trust the host
                    
                    if ("465".equals(port)) {
                        props.put("mail.smtp.socketFactory.port", port);
                        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                        props.put("mail.smtp.ssl.enable", "true");
                    } else {
                        props.put("mail.smtp.starttls.enable", "true");
                        if ("587".equals(port) || useTls) {
                            props.put("mail.smtp.starttls.required", "true");
                        }
                    }
                    
                    props.put("mail.smtp.connectiontimeout", "10000");
                    props.put("mail.smtp.timeout", "10000");
                    
                    Session session = Session.getInstance(props, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(senderEmail, appPassword);
                        }
                    });
                    
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(senderEmail));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                    message.setSubject("Successtudy Verification Code");
                    
                    String emailBody = "Hello " + userName + ",\n\n" +
                            "Your Successtudy verification code is: " + verificationCode + "\n\n" +
                            "Please enter this code in the app to activate your account.\n\n" +
                            "Best regards,\n" +
                            "Successtudy Team";
                    
                    message.setText(emailBody);
                    
                    Log.d(TAG, "Attempting to send email to " + recipientEmail + " via " + host + ":" + port);
                    Transport.send(message);
                    Log.i(TAG, "Email sent successfully to: " + recipientEmail);
                    success = true;
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
                Log.e(TAG, "SMTP Error: " + errorMessage, e);
            }

            final boolean finalSuccess = success;
            final String finalError = errorMessage;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!finalSuccess) {
                    Toast.makeText(context, "Email failed: " + finalError + "\n(Dev Mode: Verification code is " + verificationCode + ")", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Verification code sent to " + recipientEmail, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}
