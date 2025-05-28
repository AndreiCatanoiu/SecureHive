package licenta.andrei.catanoiu.securehive.fragments.account;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;
import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.databinding.FragmentAccountBinding;
import licenta.andrei.catanoiu.securehive.activities.AddDeviceActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import licenta.andrei.catanoiu.securehive.activities.LoginActivity;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "UserProfilePrefs";
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri tempImageUri;
    private AlertDialog currentDialog;
    private CircleImageView dialogProfileImage;
    private Button buttonLogout;
    private FirebaseAuth mAuth;

    private static final String KEY_NAME = "user_name";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_PHONE = "user_phone";
    private static final String KEY_PROFILE_IMAGE = "user_profile_image";

    private static final String TAG = "AccountFragment";

    private TextView userName, userEmail, userPhone;
    private Button buttonEditProfile, buttonChangePassword;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        try {
            binding = FragmentAccountBinding.inflate(inflater, container, false);
            View root = binding.getRoot();

            sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            mAuth = FirebaseAuth.getInstance();

            loadUserProfile();

            // Inițializare views
            userName = root.findViewById(R.id.userName);
            userEmail = root.findViewById(R.id.userEmail);
            userPhone = root.findViewById(R.id.userPhone);
            buttonEditProfile = root.findViewById(R.id.buttonEditProfile);
            buttonChangePassword = root.findViewById(R.id.buttonChangePassword);
            buttonLogout = root.findViewById(R.id.buttonLogout);

            // Setare date utilizator
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                userName.setText(user.getDisplayName());
                userEmail.setText(user.getEmail());
                // Telefonul va fi setat din Firestore
            }

            // Setare click listeners
            buttonEditProfile.setOnClickListener(v -> showEditProfileDialog());
            buttonChangePassword.setOnClickListener(v -> showChangePasswordDialog());
            buttonLogout.setOnClickListener(v -> logout());

            return root;
        } catch (Exception e) {
            Log.e(TAG, "Error creating fragment", e);
            Toast.makeText(getContext(), "Error loading profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return new View(getContext());
        }
    }

    private void loadUserProfile() {
        try {
            String name = sharedPreferences.getString(KEY_NAME, "Andrei Catanoiu");
            String email = sharedPreferences.getString(KEY_EMAIL, "andrei@example.com");
            String phone = sharedPreferences.getString(KEY_PHONE, "+40 712 345 678");
            String profileImageString = sharedPreferences.getString(KEY_PROFILE_IMAGE, "");

            userName.setText(name);
            userEmail.setText(email);
            userPhone.setText(phone);

            if (!profileImageString.isEmpty()) {
                try {
                    Bitmap profileImage = decodeBase64Image(profileImageString);
                    binding.profileImage.setImageBitmap(profileImage);
                } catch (Exception e) {
                    Log.e(TAG, "Error decoding image", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading profile", e);
            Toast.makeText(getContext(), "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditProfileDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_edit_profile, null);
            builder.setView(dialogView)
                    .setTitle("Edit Profile");

            EditText editName = dialogView.findViewById(R.id.editName);
            EditText editEmail = dialogView.findViewById(R.id.editEmail);
            EditText editPhone = dialogView.findViewById(R.id.editPhone);
            Button buttonChangePhoto = dialogView.findViewById(R.id.buttonChangePhoto);
            Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);
            Button buttonSave = dialogView.findViewById(R.id.buttonSaveProfile);
            dialogProfileImage = dialogView.findViewById(R.id.editProfileImage);

            editName.setText(userName.getText());
            editEmail.setText(userEmail.getText());
            editPhone.setText(userPhone.getText());

            dialogProfileImage.setImageDrawable(binding.profileImage.getDrawable());

            currentDialog = builder.create();
            currentDialog.show();

            buttonChangePhoto.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select an image"), PICK_IMAGE_REQUEST);
            });

            buttonCancel.setOnClickListener(v -> currentDialog.dismiss());

            buttonSave.setEnabled(false);

            TextWatcher textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    validateInputs(editName, editEmail, editPhone, buttonSave);
                }
            };

            editName.addTextChangedListener(textWatcher);
            editEmail.addTextChangedListener(textWatcher);
            editPhone.addTextChangedListener(textWatcher);

            validateInputs(editName, editEmail, editPhone, buttonSave);

            buttonSave.setOnClickListener(v -> {
                try {
                    String newName = editName.getText().toString().trim();
                    String newEmail = editEmail.getText().toString().trim();
                    String newPhone = editPhone.getText().toString().trim();

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    if (!newName.isEmpty()) {
                        editor.putString(KEY_NAME, newName);
                        userName.setText(newName);
                    }
                    if (!newEmail.isEmpty()) {
                        editor.putString(KEY_EMAIL, newEmail);
                        userEmail.setText(newEmail);
                    }
                    if (!newPhone.isEmpty()) {
                        editor.putString(KEY_PHONE, newPhone);
                        userPhone.setText(newPhone);
                    }

                    binding.profileImage.setImageDrawable(dialogProfileImage.getDrawable());
                    saveProfileImage();

                    editor.apply();

                    Toast.makeText(getContext(), "Profile has been updated", Toast.LENGTH_SHORT).show();
                    currentDialog.dismiss();
                } catch (Exception e) {
                    Log.e(TAG, "Error saving profile", e);
                    Toast.makeText(getContext(), "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error displaying dialog", e);
            Toast.makeText(getContext(), "Error displaying dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showChangePasswordDialog() {
        // TODO: Implementare dialog schimbare parolă
    }

    private void validateInputs(EditText nameField, EditText emailField, EditText phoneField, Button saveButton) {
        String name = nameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();

        boolean isNameValid = validateName(name);
        boolean isEmailValid = validateEmail(email);
        boolean isPhoneValid = validatePhoneNumber(phone);

        if (!isNameValid && !name.isEmpty()) {
            nameField.setError("Name must be at least 5 characters");
        }

        if (!isEmailValid && !email.isEmpty()) {
            emailField.setError("Please enter a valid email address");
        }

        if (!isPhoneValid && !phone.isEmpty()) {
            phoneField.setError("Please enter a valid Romanian phone number");
        }

        saveButton.setEnabled(isNameValid && isEmailValid && isPhoneValid);
    }

    private boolean validateName(String name) {
        return name.length() >= 5;
    }

    private boolean validateEmail(String email) {
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return Pattern.matches(emailPattern, email);
    }
    private boolean validatePhoneNumber(String phone) {
        String cleanPhone = phone.replaceAll("[\\s.]", "");

        String phonePattern = "^(0|\\+40)7\\d{8}$";
        return Pattern.matches(phonePattern, cleanPhone);
    }

    private void saveProfileImage() {
        try {
            binding.profileImage.setDrawingCacheEnabled(true);
            binding.profileImage.buildDrawingCache();
            Bitmap bitmap = binding.profileImage.getDrawingCache();

            String encodedImage = encodeToBase64(bitmap);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_PROFILE_IMAGE, encodedImage);
            editor.apply();

            binding.profileImage.setDrawingCacheEnabled(false);
        } catch (Exception e) {
            Log.e(TAG, "Error saving profile image", e);
        }
    }

    private String encodeToBase64(Bitmap image) {
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOS);
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
    }

    private Bitmap decodeBase64Image(String input) {
        byte[] decodedBytes = Base64.decode(input, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == -1 && data != null && data.getData() != null) {
            try {
                tempImageUri = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), tempImageUri);

                if (currentDialog != null && currentDialog.isShowing() && dialogProfileImage != null) {
                    dialogProfileImage.setImageBitmap(bitmap);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error processing selected image", e);
                Toast.makeText(getContext(), "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
    }

    private void logout() {
        // Afișăm un dialog de confirmare
        new AlertDialog.Builder(requireContext())
            .setTitle("Deconectare")
            .setMessage("Sigur doriți să vă deconectați?")
            .setPositiveButton("Da", (dialog, which) -> {
                // Ștergem datele locale
                clearLocalData();
                
                // Deconectare Firebase
                mAuth.signOut();
                
                // Redirectare către LoginActivity
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            })
            .setNegativeButton("Nu", null)
            .show();
    }

    private void clearLocalData() {
        try {
            // Ștergem datele din SharedPreferences
            Context context = requireContext();
            
            // Ștergem datele profilului
            SharedPreferences profilePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            profilePrefs.edit().clear().apply();
            
            // Ștergem lista de dispozitive
            SharedPreferences devicesPrefs = context.getSharedPreferences("DevicesPrefs", Context.MODE_PRIVATE);
            devicesPrefs.edit().clear().apply();
            
            Log.d(TAG, "Local data cleared successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing local data", e);
        }
    }
}