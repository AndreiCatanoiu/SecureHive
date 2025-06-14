package licenta.andrei.catanoiu.securehive.fragments.account;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.activities.AddDeviceActivity;
import licenta.andrei.catanoiu.securehive.activities.LoginActivity;
import licenta.andrei.catanoiu.securehive.databinding.FragmentAccountBinding;

public class AccountFragment extends Fragment {

    private static final String TAG = "AccountFragment";
    private FragmentAccountBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String cachedName;
    private String cachedPhone;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Preîncărcăm datele utilizatorului
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            cachedName = document.getString("name");
                            cachedPhone = document.getString("phone");
                            updateUIWithCachedData();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading user data", e);
                    });
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupInitialUI();
        setupButtons();
        updateUIWithCachedData();

        return root;
    }

    private void setupInitialUI() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            binding.userEmail.setText(user.getEmail());
            // Setăm datele din cache dacă există
            if (cachedName != null) {
                binding.userName.setText(cachedName);
            }
            if (cachedPhone != null) {
                binding.userPhone.setText(cachedPhone);
            }
        }
    }

    private void updateUIWithCachedData() {
        if (binding != null) {
            if (cachedName != null && !cachedName.isEmpty()) {
                binding.userName.setText(cachedName);
            }
            if (cachedPhone != null && !cachedPhone.isEmpty()) {
                binding.userPhone.setText(cachedPhone);
            }
        }
    }

    private void setupButtons() {
        binding.editProfileButton.setOnClickListener(v -> showEditProfileDialog());

        binding.addDeviceButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(getActivity(), AddDeviceActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });

        binding.logoutButton.setOnClickListener(v -> showLogoutDialog());
        
        binding.deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private boolean isValidRomanianPhoneNumber(String phone) {
        // Eliminăm spațiile și caracterele speciale
        String cleanPhone = phone.replaceAll("[\\s-]", "");
        
        // Verificăm formatul: +40/0 urmat de 7/2/3 și încă 8 cifre
        return cleanPhone.matches("(\\+40|0)(2|3|7)[0-9]{8}");
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        
        EditText nameInput = view.findViewById(R.id.editName);
        EditText phoneInput = view.findViewById(R.id.editPhone);
        
        nameInput.setText(binding.userName.getText());
        phoneInput.setText(binding.userPhone.getText());
        
        builder.setView(view)
               .setPositiveButton(R.string.save, null) // Setăm null pentru a preveni închiderea automată
               .setNegativeButton(R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Setăm listener-ul pentru butonul de Save după ce dialogul este creat
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = nameInput.getText().toString().trim();
            String newPhone = phoneInput.getText().toString().trim();

            if (newName.isEmpty()) {
                nameInput.setError("Please enter your name");
                return;
            }

            if (newPhone.isEmpty()) {
                phoneInput.setError("Please enter your phone number");
                return;
            }

            if (!isValidRomanianPhoneNumber(newPhone)) {
                phoneInput.setError("Please enter a valid Romanian phone number (e.g. +40722123456 or 0722123456)");
                return;
            }

            updateProfile(newName, newPhone);
            dialog.dismiss();
        });
    }

    private void updateProfile(String newName, String newPhone) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String currentName = binding.userName.getText().toString().trim();
            String currentPhone = binding.userPhone.getText().toString().trim();

            boolean nameChanged = !currentName.equals(newName.trim());
            boolean phoneChanged = !currentPhone.equals(newPhone.trim());

            if (!nameChanged && !phoneChanged) {
                Toast.makeText(getContext(), "No changes to save", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            if (nameChanged) {
                updates.put("name", newName.trim());
            }
            if (phoneChanged) {
                updates.put("phone", newPhone.trim());
            }

            db.collection("users").document(user.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (nameChanged) {
                            binding.userName.setText(newName.trim());
                            cachedName = newName.trim();
                        }
                        if (phoneChanged) {
                            binding.userPhone.setText(newPhone.trim());
                            cachedPhone = newPhone.trim();
                        }

                        String message = "Profile updated successfully";
                        if (nameChanged && phoneChanged) {
                            message = "Name and phone updated successfully";
                        } else if (nameChanged) {
                            message = "Name updated successfully";
                        } else if (phoneChanged) {
                            message = "Phone updated successfully";
                        }

                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to update profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog);
        builder.setTitle(R.string.logout)
               .setMessage("Are you sure you want to log out?")
               .setPositiveButton(R.string.logout, (dialog, which) -> logout())
               .setNegativeButton(R.string.cancel, null)
               .show();
    }

    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog);
        builder.setTitle("Delete Account")
               .setMessage("Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently deleted.")
               .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
               .setNegativeButton("Cancel", null)
               .show();
    }

    private void deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            
            // First delete all user data from Firestore
            db.collection("users").document(userId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Then delete the user account
                        user.delete()
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(getContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to delete account: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to delete user data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}