package com.example.successtudy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.successtudy.R;
import com.example.successtudy.database.AppDatabase;
import com.example.successtudy.database.dao.AppDao;
import com.example.successtudy.database.entity.User;
import com.example.successtudy.logic.SessionManager;

public class WelcomeActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private AppDao appDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        appDao = AppDatabase.getDatabase(this).appDao();

        // If user is already logged in, navigate straight to main app
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_welcome);

        // Header settings
        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                startActivity(new Intent(WelcomeActivity.this, SettingsActivity.class));
            });
        }

        // Action Buttons
        View btnGetStartedPill = findViewById(R.id.btnGetStartedPill);
        if (btnGetStartedPill != null) {
            btnGetStartedPill.setOnClickListener(v -> {
                startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
            });
        }

        View btnLoginPill = findViewById(R.id.btnLoginPill);
        if (btnLoginPill != null) {
            btnLoginPill.setOnClickListener(v -> {
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
            });
        }

        View llExploreGuest = findViewById(R.id.llExploreGuest);
        if (llExploreGuest != null) {
            llExploreGuest.setOnClickListener(v -> {
                handleGuestLogin(null);
            });
        }

        // Interactive 2x2 Feature Cards
        View cardUploadFiles = findViewById(R.id.cardUploadFiles);
        if (cardUploadFiles != null) {
            cardUploadFiles.setOnClickListener(v -> handleGuestLogin("upload"));
        }

        View cardAiSummaries = findViewById(R.id.cardAiSummaries);
        if (cardAiSummaries != null) {
            cardAiSummaries.setOnClickListener(v -> handleGuestLogin("home"));
        }

        View cardTailoredQuizzes = findViewById(R.id.cardTailoredQuizzes);
        if (cardTailoredQuizzes != null) {
            cardTailoredQuizzes.setOnClickListener(v -> handleGuestLogin("quiz"));
        }

        View cardStudyHub = findViewById(R.id.cardStudyHub);
        if (cardStudyHub != null) {
            cardStudyHub.setOnClickListener(v -> handleGuestLogin("home"));
        }
    }

    private void handleGuestLogin(String targetTab) {
        new Thread(() -> {
            try {
                User guest = appDao.getUserByEmail("guest@successtudy.app");
                if (guest == null) {
                    guest = new User("Guest Learner", "guest@successtudy.app", "", "Student", "Visitor", 0.0, true);
                    long id = appDao.insertUser(guest);
                    guest.id = (int) id;
                }
                sessionManager.loginUser(guest);

                runOnUiThread(() -> {
                    Toast.makeText(WelcomeActivity.this, "Welcome to SuccessStudy!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                    if (targetTab != null) {
                        intent.putExtra("EXTRA_TARGET_TAB", targetTab);
                    }
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(WelcomeActivity.this, "Error starting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
