package licenta.andrei.catanoiu.securehive.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import licenta.andrei.catanoiu.securehive.R;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextView errorText;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        MaterialButton loginButton = findViewById(R.id.loginButton);
        TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);
        TextView registerPrompt = findViewById(R.id.registerPrompt);
        errorText = findViewById(R.id.errorText);

        loginButton.setOnClickListener(v -> attemptLogin());
        forgotPasswordText.setOnClickListener(v -> handleForgotPassword());
        registerPrompt.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else if (currentUser != null) {
            mAuth.signOut();
        }
    }

    private void attemptLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address");
            return;
        }

        if (password.isEmpty() || password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        hideError();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                showError("Please verify your email address first");
                                mAuth.signOut();
                            }
                        }
                    } else {
                        String errorMessage = "Invalid email or password";
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("no user record")) {
                                    errorMessage = "No account exists with this email address";
                                } else if (exceptionMessage.contains("password is invalid")) {
                                    errorMessage = "Incorrect password";
                                } else if (exceptionMessage.contains("network")) {
                                    errorMessage = "Network error. Please check your connection";
                                }
                            }
                        }
                        showError(errorMessage);
                    }
                });
    }

    private void handleForgotPassword() {
        String email = emailInput.getText().toString().trim();
        
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter your email address");
            return;
        }

        hideError();

        // Send password reset email directly - Firebase will handle the validation
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(resetTask -> {
                    if (resetTask.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Password reset email sent to " + email,
                                Toast.LENGTH_LONG).show();
                        hideError();
                    } else {
                        String errorMessage = "Failed to send reset email";
                        if (resetTask.getException() != null) {
                            String exceptionMessage = resetTask.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("no user record")) {
                                    errorMessage = "No account exists with this email address";
                                } else if (exceptionMessage.contains("network")) {
                                    errorMessage = "Network error. Please check your connection";
                                } else if (exceptionMessage.contains("too many requests")) {
                                    errorMessage = "Too many requests. Please try again later";
                                }
                            }
                        }
                        showError(errorMessage);
                    }
                });
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorText.setVisibility(View.GONE);
    }
} 