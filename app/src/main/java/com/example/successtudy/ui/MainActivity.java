package com.example.successtudy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.successtudy.R;
import com.example.successtudy.ui.LoginActivity;
import com.example.successtudy.logic.SessionManager;
import com.example.successtudy.database.AppDatabase;
import com.example.successtudy.database.dao.AppDao;
import com.example.successtudy.database.entity.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabQuickAction;
    private SessionManager sessionManager;
    private AppDao appDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            // Set up toolbar to host the options menu
            androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
            }
            
            // Initialize SessionManager before any usage
            sessionManager = new SessionManager(this);
            appDao = AppDatabase.getDatabase(this).appDao();
            
            // Check if user is logged in; if not, redirect to WelcomeActivity
            if (!sessionManager.isLoggedIn()) {
                startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
                finish();
                return;
            }
            
            // Show current username in toolbar subtitle
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle(sessionManager.getCurrentUserName());
            }

            bottomNavigation = findViewById(R.id.bottom_navigation);
            fabQuickAction = findViewById(R.id.fab_quick_action);

            if (bottomNavigation == null) {
                android.util.Log.e("MainActivity", "bottom_navigation not found in layout");
                return;
            }
            
            if (fabQuickAction == null) {
                android.util.Log.e("MainActivity", "fab_quick_action not found in layout");
            }

            // Load HomeFragment by default
            loadFragment(new HomeFragment(), "Home");

            bottomNavigation.setOnItemSelectedListener(item -> {
                Fragment selectedFragment = null;
                String tag = "";
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                    tag = "Home";
                } else if (itemId == R.id.nav_upload) {
                    selectedFragment = new UploadNotesFragment();
                    tag = "Upload";
                } else if (itemId == R.id.nav_quiz) {
                    selectedFragment = new QuizFragment();
                    tag = "Quiz";
                } else if (itemId == R.id.nav_marketplace) {
                    selectedFragment = new MarketplaceFragment();
                    tag = "Market";
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment();
                    tag = "Profile";
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment, tag);
                    return true;
                }
                return false;
            });

            // FAB Quick Action triggers choice Dialog
            if (fabQuickAction != null) {
                fabQuickAction.setOnClickListener(v -> {
                    String[] choices = {"Quick Upload Notes", "Start AI Quiz Session"};
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("SuccessStudy Quick Launcher")
                        .setItems(choices, (dialog, which) -> {
                            if (which == 0) {
                                // Upload notes
                                bottomNavigation.setSelectedItemId(R.id.nav_upload);
                            } else if (which == 1) {
                                // Quiz
                                bottomNavigation.setSelectedItemId(R.id.nav_quiz);
                            }
                        })
                        .show();
                });
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in onCreate", e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu);
        // Show logout for every logged‑in user
        MenuItem logoutItem = menu.findItem(R.id.action_logout);
        logoutItem.setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            sessionManager.logoutUser();
            try {
                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this,
                        new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                ).signOut();
            } catch (Exception ignored) {}
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void loadFragment(Fragment fragment, String tag) {
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
