package com.example.successtudy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.successtudy.R;
import com.example.successtudy.database.AppDatabase;
import com.example.successtudy.database.dao.AppDao;
import com.example.successtudy.database.entity.User;
import com.example.successtudy.logic.EmailVerificationHelper;
import com.example.successtudy.logic.SessionManager;

public class VerifyActivity extends AppCompatActivity {

    private EditText etVerificationCode;
    private Button btnVerify;
    private TextView tvInstructions;
    private SessionManager sessionManager;
    private AppDao appDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);

        sessionManager = new SessionManager(this);
        appDao = AppDatabase.getDatabase(this).appDao();

        String pendingEmail = sessionManager.getPendingEmail();
        
        tvInstructions = findViewById(R.id.tvVerifyDescription);
        etVerificationCode = findViewById(R.id.etVerifyCode);
        btnVerify = findViewById(R.id.btnVerifyCode);
        
        tvInstructions.setText("A verification code has been sent to " + pendingEmail + 
                ". Please check your email and enter the code below.");

        btnVerify.setOnClickListener(v -> verifyCode());

        findViewById(R.id.tvResendLink).setOnClickListener(v -> resendCode());
    }

    private void resendCode() {
        String pendingEmail = sessionManager.getPendingEmail();
        if (pendingEmail == null || pendingEmail.isEmpty()) {
            Toast.makeText(this, "No email found for verification. Please register or log in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            User user = appDao.getUserByEmail(pendingEmail);
            if (user != null) {
                String newCode = EmailVerificationHelper.generateVerificationCode();
                user.verificationCode = newCode;
                appDao.updateUser(user);

                runOnUiThread(() -> {
                    EmailVerificationHelper.sendVerificationEmail(VerifyActivity.this, pendingEmail, newCode, user.name);
                    tvInstructions.setText("A new verification code has been sent to " + pendingEmail + 
                            ". Please check your email and enter the code below.");
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(VerifyActivity.this, "User not found.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void verifyCode() {
        String code = etVerificationCode.getText().toString().trim();
        if (code.isEmpty()) {
            Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show();
            return;
        }

        String pendingEmail = sessionManager.getPendingEmail();
        new Thread(() -> {
            User user = appDao.getUserByEmail(pendingEmail);
            if (user != null && user.verificationCode != null && user.verificationCode.equals(code)) {
                // Verification successful
                user.isVerified = true;
                user.verificationCode = ""; // Clear code
                appDao.updateUser(user);

                runOnUiThread(() -> {
                    Toast.makeText(VerifyActivity.this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                    sessionManager.clearPendingEmail();
                    startActivity(new Intent(VerifyActivity.this, LoginActivity.class));
                    finish();
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(VerifyActivity.this, "Invalid verification code. Please try again.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
