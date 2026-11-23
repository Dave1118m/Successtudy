package com.example.successtudy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.successtudy.R;

public class WelcomeActivity extends AppCompatActivity {

    private final Handler typewriterHandler = new Handler(Looper.getMainLooper());
    private Runnable typewriterRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always display the Landing Page when opening the app
        setContentView(R.layout.activity_welcome);

        // Header settings
        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                startActivity(new Intent(WelcomeActivity.this, SettingsActivity.class));
            });
        }

        // Horizontal Action Buttons: Explore (Register) and Log In
        View btnExplorePill = findViewById(R.id.btnExplorePill);
        if (btnExplorePill != null) {
            btnExplorePill.setOnClickListener(v -> {
                startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
            });
        }

        View btnLoginPill = findViewById(R.id.btnLoginPill);
        if (btnLoginPill != null) {
            btnLoginPill.setOnClickListener(v -> {
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
            });
        }

        // Interactive 2x2 Feature Cards redirect to Register
        View.OnClickListener cardClickListener = v -> {
            startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
        };

        View cardUploadFiles = findViewById(R.id.cardUploadFiles);
        if (cardUploadFiles != null) {
            cardUploadFiles.setOnClickListener(cardClickListener);
        }

        View cardAiSummaries = findViewById(R.id.cardAiSummaries);
        if (cardAiSummaries != null) {
            cardAiSummaries.setOnClickListener(cardClickListener);
        }

        View cardTailoredQuizzes = findViewById(R.id.cardTailoredQuizzes);
        if (cardTailoredQuizzes != null) {
            cardTailoredQuizzes.setOnClickListener(cardClickListener);
        }

        View cardStudyHub = findViewById(R.id.cardStudyHub);
        if (cardStudyHub != null) {
            cardStudyHub.setOnClickListener(cardClickListener);
        }

        // Animated typewriter letter transition for "developed by Mihrete"
        TextView tvDevelopedBy = findViewById(R.id.tvDevelopedBy);
        if (tvDevelopedBy != null) {
            startTypewriterAnimation(tvDevelopedBy, "developed by Mihrete");
        }
    }

    private void startTypewriterAnimation(TextView textView, String fullText) {
        typewriterRunnable = new Runnable() {
            int charIndex = 0;
            boolean isTyping = true;

            @Override
            public void run() {
                if (isTyping) {
                    if (charIndex <= fullText.length()) {
                        textView.setText(fullText.substring(0, charIndex));
                        charIndex++;
                        typewriterHandler.postDelayed(this, 110);
                    } else {
                        isTyping = false;
                        typewriterHandler.postDelayed(this, 3000); // Hold for 3s
                    }
                } else {
                    charIndex = 0;
                    isTyping = true;
                    textView.setText("");
                    typewriterHandler.postDelayed(this, 400);
                }
            }
        };
        typewriterHandler.post(typewriterRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (typewriterHandler != null && typewriterRunnable != null) {
            typewriterHandler.removeCallbacks(typewriterRunnable);
        }
    }
}
