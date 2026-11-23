package com.example.successtudy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
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

        // If user is already logged in, skip the welcome screen and go directly to main app
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_welcome);

        Button btnGetStarted = findViewById(R.id.btnGetStarted);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnExploreGuest = findViewById(R.id.btnExploreGuest);

        btnGetStarted.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        });

        btnExploreGuest.setOnClickListener(v -> {
            handleGuestLogin();
        });
    }

    private void handleGuestLogin() {
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
                    Toast.makeText(WelcomeActivity.this, "Welcome, Guest Learner!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(WelcomeActivity.this, "Error entering guest mode: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
