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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
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
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inițializare Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inițializare views
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        TextView loginPrompt = findViewById(R.id.loginPrompt);
        errorText = findViewById(R.id.errorText);

        // Click listeners
        registerButton.setOnClickListener(v -> attemptRegister());
        loginPrompt.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Validări
        if (name.isEmpty()) {
            showError("Please enter your name");
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

        if (password.isEmpty() || password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        // Dezactivăm butonul pentru a preveni multiple clicuri
        registerButton.setEnabled(false);

        // Ascunde eroarea dacă există
        hideError();

        // Creare cont
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Actualizare profil utilizator
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();

                        mAuth.getCurrentUser().updateProfile(profileUpdates)
                                .addOnCompleteListener(profileTask -> {
                                    if (profileTask.isSuccessful()) {
                                        // Salvare date în Firestore
                                        Map<String, Object> userData = new HashMap<>();
                                        userData.put("name", name);
                                        userData.put("email", email);
                                        userData.put("phone", phone);
                                        userData.put("devices", new ArrayList<String>());
                                        userData.put("createdAt", System.currentTimeMillis());

                                        db.collection("users")
                                                .document(mAuth.getCurrentUser().getUid())
                                                .set(userData)
                                                .addOnSuccessListener(aVoid -> {
                                                    // Trimite email de verificare
                                                    mAuth.getCurrentUser().sendEmailVerification()
                                                            .addOnCompleteListener(verificationTask -> {
                                                                if (verificationTask.isSuccessful()) {
                                                                    Toast.makeText(RegisterActivity.this,
                                                                            "Account created successfully! Please check your email to verify your account.",
                                                                            Toast.LENGTH_LONG).show();
                                                                    
                                                                    // Deconectare
                                                                    mAuth.signOut();
                                                                    
                                                                    // Așteptăm 2 secunde și închidem activitatea
                                                                    new Handler().postDelayed(() -> {
                                                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                                                        finish();
                                                                    }, 2000);
                                                                } else {
                                                                    showError("Failed to send verification email");
                                                                    registerButton.setEnabled(true);
                                                                }
                                                            });
                                                })
                                                .addOnFailureListener(e -> {
                                                    showError("Failed to save user data: " + e.getMessage());
                                                    registerButton.setEnabled(true);
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