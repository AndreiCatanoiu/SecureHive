package licenta.andrei.catanoiu.securehive.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

import licenta.andrei.catanoiu.securehive.R;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private TextView errorText;
    private FirebaseAuth mAuth;
    private MaterialButton registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        TextView loginPrompt = findViewById(R.id.loginPrompt);
        errorText = findViewById(R.id.errorText);

        registerButton.setOnClickListener(v -> attemptRegister());
        loginPrompt.setOnClickListener(v -> finish());
    }

    private boolean isValidRomanianPhoneNumber(String phone) {
        String cleanPhone = phone.replaceAll("[\\s-]", "");

        return cleanPhone.matches("(\\+40|0)(2|3|7)[0-9]{8}");
    }

    private void attemptRegister() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (name.length() < 3) {
            showError("Please enter your name, minimum 3 characters");
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address");
            return;
        }

        if (phone.isEmpty()) {
            showError("Please enter your phone number");
            return;
        }

        if (!isValidRomanianPhoneNumber(phone)) {
            showError("Please enter a valid Romanian phone number (e.g. +40722123456 or 0722123456)");
            return;
        }

        if (password.isEmpty() || password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        registerButton.setEnabled(false);

        hideError();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();

                        mAuth.getCurrentUser().updateProfile(profileUpdates)
                                .addOnCompleteListener(profileTask -> {
                                    if (profileTask.isSuccessful()) {
                                        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(tokenTask -> {
                                            DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                                            String userId = mAuth.getCurrentUser().getUid();
                                            Map<String, Object> userData = new HashMap<>();
                                            userData.put("name", name);
                                            userData.put("email", email);
                                            userData.put("phone", phone);
                                            userData.put("createdAt", System.currentTimeMillis());
                                            db.child("users").child(userId).setValue(userData);
                                        });

                                        mAuth.getCurrentUser().sendEmailVerification()
                                                .addOnCompleteListener(verificationTask -> {
                                                    if (verificationTask.isSuccessful()) {
                                                        Toast.makeText(RegisterActivity.this,
                                                                "Account created successfully! Please check your email to verify your account.",
                                                                Toast.LENGTH_LONG).show();

                                                        mAuth.signOut();

                                                        new Handler().postDelayed(() -> {
                                                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                                            finish();
                                                        }, 2000);
                                                    } else {
                                                        showError("Failed to send verification email");
                                                        registerButton.setEnabled(true);
                                                    }
                                                });
                                    } else {
                                        showError("Failed to update profile");
                                        registerButton.setEnabled(true);
                                        mAuth.getCurrentUser().delete();
                                    }
                                });
                    } else {
                        showError(task.getException() != null ? 
                                task.getException().getMessage() : 
                                "Registration failed");
                        registerButton.setEnabled(true);
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