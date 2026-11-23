package com.example.successtudy.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.successtudy.R;
import com.example.successtudy.database.AppDatabase;
import com.example.successtudy.database.dao.AppDao;
import com.example.successtudy.database.entity.User;
import com.example.successtudy.logic.EmailVerificationHelper;
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

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final int RC_SIGN_IN = 9001;
    public static final String DEBUG_SHA1 = "B5:62:C7:C1:A6:6B:FF:BF:B1:5F:34:AD:50:99:45:4D:0A:9F:26:FC";

    private EditText etRegName, etRegEmail, etRegPassword, etRegUniversity;
    private RadioGroup rgRole;
    private Button btnRegister;
    private TextView tvLoginLink;
    private AppDao appDao;
    private SessionManager sessionManager;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        appDao = AppDatabase.getDatabase(this).appDao();
        sessionManager = new SessionManager(this);

        etRegName = findViewById(R.id.etRegName);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        etRegUniversity = findViewById(R.id.etRegUniversity);
        rgRole = findViewById(R.id.rgRole);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        findViewById(R.id.btnBackRegister).setOnClickListener(v -> finish());
        findViewById(R.id.btnSmtpSettingsReg).setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, SettingsActivity.class));
        });

        btnRegister.setOnClickListener(v -> registerUser());
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
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

    private void registerUser() {
        String name = etRegName.getText().toString().trim();
        String email = etRegEmail.getText().toString().trim();
        String password = etRegPassword.getText().toString().trim();
        String university = etRegUniversity.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || university.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if SMTP is configured (non-blocking fallback for development)
        com.example.successtudy.logic.SmtpConfig smtpConfig = new com.example.successtudy.logic.SmtpConfig(this);
        if (!smtpConfig.isConfigured() || smtpConfig.getSenderEmail().isEmpty() || smtpConfig.getSenderPassword().isEmpty()) {
            Toast.makeText(this, "SMTP settings not configured. Using Dev Mode bypass.", Toast.LENGTH_LONG).show();
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        int checkedId = rgRole.getCheckedRadioButtonId();
        String role = "Student";
        double startingBalance = 50.00;

        if (checkedId == R.id.rbTeacher) {
            role = "Teacher";
            startingBalance = 200.00;
        } else if (checkedId == R.id.rbAdmin) {
            role = "Admin";
            startingBalance = 1000.00;
        }

        final String finalRole = role;
        final double finalBalance = startingBalance;
        final String finalUniversity = university;

        new Thread(() -> {
            // Check if email already exists
            User existingUser = appDao.getUserByEmail(email);
            if (existingUser != null) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Email already registered", Toast.LENGTH_SHORT).show());
                return;
            }

            // Generate verification code
            String verificationCode = EmailVerificationHelper.generateVerificationCode();

            // Hash password
            String passwordHash = PasswordHelper.hashPassword(password);

            // Create user (not verified yet)
            User newUser = new User(name, email, passwordHash, finalRole, finalUniversity, finalBalance, false);
            newUser.verificationCode = verificationCode;
            appDao.insertUser(newUser);

            runOnUiThread(() -> {
                sessionManager.setPendingEmail(email);
                // Send verification email
                EmailVerificationHelper.sendVerificationEmail(RegisterActivity.this, email, verificationCode, name);

                // Navigate to VerifyActivity
                startActivity(new Intent(RegisterActivity.this, VerifyActivity.class));
                finish();
            });
        }).start();
    }

    private void signInWithGoogle() {
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
            Log.e(TAG, "Google Sign-In failed with statusCode: " + statusCode, e);

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
                        + "Would you like to copy the SHA-1 or register with a simulated Google account for now?")
                .setPositiveButton("Copy SHA-1", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("SHA1 Fingerprint", DEBUG_SHA1);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "SHA-1 copied to clipboard!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Test Google Account", (dialog, which) -> {
                    String name = etRegName.getText().toString().trim();
                    if (name.isEmpty()) name = "Google Test User";
                    processGoogleUser("testuser@gmail.com", name);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void processGoogleUser(String email, String name) {
        int checkedId = rgRole.getCheckedRadioButtonId();
        String role = "Student";
        double startingBalance = 50.00;

        if (checkedId == R.id.rbTeacher) {
            role = "Teacher";
            startingBalance = 200.00;
        } else if (checkedId == R.id.rbAdmin) {
            role = "Admin";
            startingBalance = 1000.00;
        }

        String university = etRegUniversity.getText().toString().trim();
        if (university.isEmpty()) {
            university = "Not Specified";
        }

        final String finalRole = role;
        final double finalBalance = startingBalance;
        final String finalUniversity = university;

        new Thread(() -> {
            User user = appDao.getUserByEmail(email);
            if (user == null) {
                user = new User(name, email, "", finalRole, finalUniversity, finalBalance, true);
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
                Toast.makeText(RegisterActivity.this, "Welcome " + finalUser.name + "!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                finish();
            });
        }).start();
    }
}
