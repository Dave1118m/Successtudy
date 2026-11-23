package com.example.successtudy.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.successtudy.R;
import com.example.successtudy.database.AppDatabase;
import com.example.successtudy.database.dao.AppDao;
import com.example.successtudy.database.entity.User;
import com.example.successtudy.logic.PasswordHelper;
import com.example.successtudy.logic.SessionManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;
    public static final String DEBUG_SHA1 = "B5:62:C7:C1:A6:6B:FF:BF:B1:5F:34:AD:50:99:45:4D:0A:9F:26:FC";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink;
    private SessionManager sessionManager;
    private AppDao appDao;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        appDao = AppDatabase.getDatabase(this).appDao();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);

        findViewById(R.id.btnBackLogin).setOnClickListener(v -> finish());
        findViewById(R.id.btnSmtpSettings).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SettingsActivity.class));
        });

        btnLogin.setOnClickListener(v -> loginUser());
        tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // Initialize Google Sign-In with Google Cloud configuration
        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile();

        String webClientId = getString(R.string.google_web_client_id);
        if (webClientId != null && !webClientId.trim().isEmpty() && !webClientId.contains("YOUR_")) {
            try {
                gsoBuilder.requestIdToken(webClientId.trim());
            } catch (Exception e) {
                Log.w(TAG, "Could not set requestIdToken", e);
            }
        }

        mGoogleSignInClient = GoogleSignIn.getClient(this, gsoBuilder.build());

        SignInButton btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE);
            btnGoogleSignIn.setColorScheme(SignInButton.COLOR_DARK);
            btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        }
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            User user = appDao.getUserByEmail(email);

            runOnUiThread(() -> {
                if (user == null) {
                    Toast.makeText(LoginActivity.this, "Email not registered. Please sign up first.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!user.isVerified) {
                    Toast.makeText(LoginActivity.this, "Please verify your email first. Redirecting...", Toast.LENGTH_LONG).show();
                    sessionManager.setPendingEmail(email);
                    startActivity(new Intent(LoginActivity.this, VerifyActivity.class));
                    return;
                }

                // If user registered with Google and has no local password hash
                if (user.passwordHash == null || user.passwordHash.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "This account was registered with Google. Please use 'Sign in with Google'.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (!PasswordHelper.verifyPassword(password, user.passwordHash)) {
                    Toast.makeText(LoginActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Login successful
                sessionManager.loginUser(user);
                Toast.makeText(LoginActivity.this, "Logged in successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            });
        }).start();
    }

    private void signInWithGoogle() {
        // Clear prior sign-in state to ensure account picker shows up
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String email = account.getEmail();
                String name = account.getDisplayName();
                if (name == null || name.trim().isEmpty()) {
                    name = email != null ? email.split("@")[0] : "Google User";
                }
                processGoogleUser(email, name);
            }
        } catch (ApiException e) {
            int statusCode = e.getStatusCode();
            Log.e(TAG, "Google Sign-In failed with status code: " + statusCode, e);

            if (statusCode == CommonStatusCodes.DEVELOPER_ERROR || statusCode == 10) {
                showGoogleConfigDialog("Developer Error (Code 10)");
            } else if (statusCode == 12500) {
                showGoogleConfigDialog("Sign-In Failed (Code 12500)");
            } else if (statusCode == CommonStatusCodes.NETWORK_ERROR || statusCode == 7) {
                Toast.makeText(this, "Network error during Google Sign-In. Check your internet connection.", Toast.LENGTH_LONG).show();
            } else if (statusCode == CommonStatusCodes.SIGN_IN_REQUIRED || statusCode == 12501) {
                Toast.makeText(this, "Google Sign-In cancelled.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Google Sign-In failed (Code " + statusCode + "): " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showGoogleConfigDialog(String errorTitle) {
        new AlertDialog.Builder(this)
                .setTitle(errorTitle)
                .setMessage("Google Cloud Console needs your app's SHA-1 fingerprint registered under Credentials > OAuth 2.0 Client ID (Android):\n\n"
                        + "Package Name:\ncom.example.successtudy\n\n"
                        + "SHA-1 Fingerprint:\n" + DEBUG_SHA1 + "\n\n"
                        + "Would you like to copy the SHA-1 or test with a simulated Google login for now?")
                .setPositiveButton("Copy SHA-1", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("SHA1 Fingerprint", DEBUG_SHA1);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "SHA-1 copied to clipboard!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Test Google Login", (dialog, which) -> {
                    processGoogleUser("testuser@gmail.com", "Google Test User");
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void processGoogleUser(String email, String name) {
        new Thread(() -> {
            User user = appDao.getUserByEmail(email);
            if (user == null) {
                // Register new Google user (automatically verified)
                user = new User(name, email, "", "Student", "Not Specified", 50.00, true);
                appDao.insertUser(user);
                user = appDao.getUserByEmail(email);
            } else {
                if (!user.isVerified) {
                    user.isVerified = true;
                    user.verificationCode = "";
                    appDao.updateUser(user);
                }
            }

            final User finalUser = user;
            runOnUiThread(() -> {
                sessionManager.loginUser(finalUser);
                Toast.makeText(LoginActivity.this, "Logged in as " + finalUser.name, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            });
        }).start();
    }
}
