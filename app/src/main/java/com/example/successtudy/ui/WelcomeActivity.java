package com.example.successtudy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
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

        // Always display the Landing Page when opening the app (do not auto-redirect)
        setContentView(R.layout.activity_welcome);

        // Header settings
        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                startActivity(new Intent(WelcomeActivity.this, SettingsActivity.class));
            });
        }

        TextView tvGetStartedText = findViewById(R.id.tvGetStartedText);
        View btnGetStartedPill = findViewById(R.id.btnGetStartedPill);
        View btnLoginPill = findViewById(R.id.btnLoginPill);
        View llExploreGuest = findViewById(R.id.llExploreGuest);

        boolean isLoggedIn = sessionManager.isLoggedIn();

        if (isLoggedIn && tvGetStartedText != null) {
            String userName = sessionManager.getCurrentUserName();
            if (userName != null && !userName.trim().isEmpty()) {
                tvGetStartedText.setText("Continue as " + userName);
            } else {
                tvGetStartedText.setText("Continue to Study Hub");
            }
        }

        // Action Buttons
        if (btnGetStartedPill != null) {
            btnGetStartedPill.setOnClickListener(v -> {
                if (sessionManager.isLoggedIn()) {
                    startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                } else {
                    startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
                }
            });
        }

        if (btnLoginPill != null) {
            btnLoginPill.setOnClickListener(v -> {
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
            });
        }

        if (llExploreGuest != null) {
            llExploreGuest.setOnClickListener(v -> {
                if (sessionManager.isLoggedIn()) {
                    startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                } else {
                    handleGuestLogin(null);
                }
            });
        }

        // Interactive 2x2 Feature Cards
        View cardUploadFiles = findViewById(R.id.cardUploadFiles);
        if (cardUploadFiles != null) {
            cardUploadFiles.setOnClickListener(v -> openFeature("upload"));
        }

        View cardAiSummaries = findViewById(R.id.cardAiSummaries);
        if (cardAiSummaries != null) {
            cardAiSummaries.setOnClickListener(v -> openFeature("home"));
        }

        View cardTailoredQuizzes = findViewById(R.id.cardTailoredQuizzes);
        if (cardTailoredQuizzes != null) {
            cardTailoredQuizzes.setOnClickListener(v -> openFeature("quiz"));
        }

        View cardStudyHub = findViewById(R.id.cardStudyHub);
        if (cardStudyHub != null) {
            cardStudyHub.setOnClickListener(v -> openFeature("home"));
        }
    }

    private void openFeature(String targetTab) {
        if (sessionManager.isLoggedIn()) {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            if (targetTab != null) {
                intent.putExtra("EXTRA_TARGET_TAB", targetTab);
            }
            startActivity(intent);
        } else {
            handleGuestLogin(targetTab);
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
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(WelcomeActivity.this, "Error starting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
