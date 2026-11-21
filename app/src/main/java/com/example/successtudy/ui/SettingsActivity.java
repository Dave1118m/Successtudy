package com.example.successtudy.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.successtudy.R;
import com.example.successtudy.logic.GeminiService;
import com.example.successtudy.logic.SessionManager;
import com.example.successtudy.logic.SmtpConfig;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SettingsActivity extends AppCompatActivity {
    
    // Gemini Views
    private EditText etGeminiApiKey, etGeminiModel;
    private Button btnTestGemini;
    private TextView tvGeminiStatus;

    // SMTP Views
    private EditText etSmtpHost, etSmtpPort, etSenderEmail, etAppPassword;
    private SwitchCompat swUseTls;
    private Button btnSave, btnTestConnection;
    private TextView tvStatus;
    private ImageButton btnBack;

    private SmtpConfig smtpConfig;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        smtpConfig = new SmtpConfig(this);
        sessionManager = new SessionManager(this);

        initializeViews();
        loadSettings();
        setupListeners();
    }

    private void initializeViews() {
        // Gemini
        etGeminiApiKey = findViewById(R.id.etGeminiApiKey);
        etGeminiModel = findViewById(R.id.etGeminiModel);
        btnTestGemini = findViewById(R.id.btnTestGemini);
        tvGeminiStatus = findViewById(R.id.tvGeminiStatus);

        // SMTP
        etSmtpHost = findViewById(R.id.etSmtpHost);
        etSmtpPort = findViewById(R.id.etSmtpPort);
        etSenderEmail = findViewById(R.id.etSenderEmail);
        etAppPassword = findViewById(R.id.etAppPassword);
        swUseTls = findViewById(R.id.swUseTls);
        btnSave = findViewById(R.id.btnSave);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        tvStatus = findViewById(R.id.tvStatus);
        btnBack = findViewById(R.id.btnBack);
    }

    private void loadSettings() {
        // Gemini
        etGeminiApiKey.setText(sessionManager.getGeminiApiKey());
        etGeminiModel.setText(sessionManager.getGeminiModel());

        // SMTP
        etSmtpHost.setText(smtpConfig.getSmtpHost());
        etSmtpPort.setText(smtpConfig.getSmtpPort());
        etSenderEmail.setText(smtpConfig.getSenderEmail());
        etAppPassword.setText(smtpConfig.getSenderPassword());
        swUseTls.setChecked(smtpConfig.isUseTls());
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveAllSettings());
        btnTestGemini.setOnClickListener(v -> testGeminiConnection());
        btnTestConnection.setOnClickListener(v -> testSmtpConnection());
    }

    private void saveAllSettings() {
        // Save Gemini Settings
        String apiKey = etGeminiApiKey.getText().toString().trim();
        String model = etGeminiModel.getText().toString().trim();
        if (model.isEmpty()) model = "gemini-1.5-pro";

        sessionManager.setGeminiApiKey(apiKey);
        sessionManager.setGeminiModel(model);

        // Save SMTP Settings
        String smtpHost = etSmtpHost.getText().toString().trim();
        String smtpPort = etSmtpPort.getText().toString().trim();
        String senderEmail = etSenderEmail.getText().toString().trim();
        String appPassword = etAppPassword.getText().toString().trim();
        boolean useTls = swUseTls.isChecked();

        if (!smtpHost.isEmpty() && !smtpPort.isEmpty() && !senderEmail.isEmpty() && !appPassword.isEmpty()) {
            smtpConfig.setSmtpConfig(smtpHost, smtpPort, senderEmail, appPassword, useTls);
        }

        Toast.makeText(this, "All settings saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void testGeminiConnection() {
        String apiKey = etGeminiApiKey.getText().toString().trim();
        String model = etGeminiModel.getText().toString().trim();
        if (model.isEmpty()) model = "gemini-1.5-pro";

        if (apiKey.isEmpty()) {
            showGeminiStatus("Please enter your Gemini API key first.", true);
            return;
        }

        showGeminiStatus("Connecting to " + model + "...", false);
        btnTestGemini.setEnabled(false);

        final String finalModel = model;
        new Thread(() -> {
            String result = GeminiService.testConnection(apiKey, finalModel);
            runOnUiThread(() -> {
                btnTestGemini.setEnabled(true);
                boolean success = result.startsWith("SUCCESS");
                showGeminiStatus(result, !success);
            });
        }).start();
    }

    private void showGeminiStatus(String message, boolean isError) {
        tvGeminiStatus.setText(message);
        tvGeminiStatus.setTextColor(isError ?
                getResources().getColor(android.R.color.holo_red_light) :
                getResources().getColor(android.R.color.holo_green_light));
        tvGeminiStatus.setVisibility(android.view.View.VISIBLE);
    }

    private void testSmtpConnection() {
        String smtpHost = etSmtpHost.getText().toString().trim();
        String smtpPort = etSmtpPort.getText().toString().trim();
        String senderEmail = etSenderEmail.getText().toString().trim();
        String appPassword = etAppPassword.getText().toString().trim();
        boolean useTls = swUseTls.isChecked();

        if (smtpHost.isEmpty() || smtpPort.isEmpty() || senderEmail.isEmpty() || appPassword.isEmpty()) {
            showSmtpStatus("Please fill in all SMTP fields first", true);
            return;
        }

        showSmtpStatus("Testing connection...", false);
        btnTestConnection.setEnabled(false);

        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", smtpHost);
                props.put("mail.smtp.port", smtpPort);
                props.put("mail.smtp.auth", "true");

                if ("465".equals(smtpPort)) {
                    props.put("mail.smtp.socketFactory.port", smtpPort);
                    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    props.put("mail.smtp.ssl.enable", "true");
                } else if ("587".equals(smtpPort) || useTls) {
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.starttls.required", "true");
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
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(senderEmail));
                message.setSubject("Successtudy SMTP Test");
                message.setText("If you received this, your SMTP configuration is working correctly!");

                Transport.send(message);

                runOnUiThread(() -> {
                    showSmtpStatus("✓ Connection successful! Test email sent to yourself.", false);
                    btnTestConnection.setEnabled(true);
                    Toast.makeText(SettingsActivity.this, "SMTP Working!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e("SettingsActivity", "SMTP Test failed", e);
                runOnUiThread(() -> {
                    showSmtpStatus("✗ Connection failed: " + e.getMessage(), true);
                    btnTestConnection.setEnabled(true);
                });
            }
        }).start();
    }

    private void showSmtpStatus(String message, boolean isError) {
        tvStatus.setText(message);
        tvStatus.setTextColor(isError ? 
            getResources().getColor(android.R.color.holo_red_light) :
            getResources().getColor(android.R.color.holo_green_light));
        tvStatus.setVisibility(android.view.View.VISIBLE);
    }
}
