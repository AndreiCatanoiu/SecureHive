package licenta.andrei.catanoiu.securehive.fragments.account;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.activities.AddDeviceActivity;
import licenta.andrei.catanoiu.securehive.activities.LoginActivity;
import licenta.andrei.catanoiu.securehive.databinding.FragmentAccountBinding;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupUserInfo();
        setupButtons();

        return root;
    }

    private void setupUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            binding.userEmail.setText(user.getEmail());
            
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String name = document.getString("name");
                            String phone = document.getString("phone");

                            if (name != null && !name.isEmpty()) {
                                binding.userName.setText(name);
                            }
                            if (phone != null && !phone.isEmpty()) {
                                binding.userPhone.setText(phone);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error loading user data: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setupButtons() {
        binding.editProfileButton.setOnClickListener(v -> showEditProfileDialog());

        binding.addDeviceButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddDeviceActivity.class);
            startActivity(intent);
        });

        binding.logoutButton.setOnClickListener(v -> showLogoutDialog());
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        
        EditText nameInput = view.findViewById(R.id.editName);
        EditText phoneInput = view.findViewById(R.id.editPhone);
        
        nameInput.setText(binding.userName.getText());
        phoneInput.setText(binding.userPhone.getText());
        
        builder.setView(view)
               .setPositiveButton(R.string.save, (dialog, which) -> {
                    String newName = nameInput.getText().toString().trim();
                    String newPhone = phoneInput.getText().toString().trim();
                    updateProfile(newName, newPhone);
               })
               .setNegativeButton(R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateProfile(String newName, String newPhone) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .update("name", newName, "phone", newPhone)
                    .addOnSuccessListener(aVoid -> {
                        binding.userName.setText(newName);
                        binding.userPhone.setText(newPhone);
                        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
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