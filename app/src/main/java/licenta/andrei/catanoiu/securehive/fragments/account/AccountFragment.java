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
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.databinding.FragmentAccountBinding;
import licenta.andrei.catanoiu.securehive.activityes.AddDeviceActivity;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "UserProfilePrefs";
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri tempImageUri;
    private AlertDialog currentDialog;
    private CircleImageView dialogProfileImage;

    private static final String KEY_NAME = "user_name";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_PHONE = "user_phone";
    private static final String KEY_LOCATION = "user_location";
    private static final String KEY_PROFILE_IMAGE = "user_profile_image";

    private static final String TAG = "AccountFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        try {
            binding = FragmentAccountBinding.inflate(inflater, container, false);
            View root = binding.getRoot();

            sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            loadUserProfile();

            binding.buttonEditProfile.setOnClickListener(v -> showEditProfileDialog());
            binding.buttonAddDevices.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), AddDeviceActivity.class);
                startActivity(intent);
            });

            binding.buttonLogout.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Delogare...", Toast.LENGTH_SHORT).show();
            });

            return root;
        } catch (Exception e) {
            Log.e(TAG, "Eroare la crearea fragmentului", e);
            Toast.makeText(getContext(), "Eroare la încărcarea profilului: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return new View(getContext());
        }
    }

    private void loadUserProfile() {
        try {
            String name = sharedPreferences.getString(KEY_NAME, "Andrei Catanoiu");
            String email = sharedPreferences.getString(KEY_EMAIL, "andrei@example.com");
            String phone = sharedPreferences.getString(KEY_PHONE, "+40 712 345 678");
            String location = sharedPreferences.getString(KEY_LOCATION, "Pitești, Argeș");
            String profileImageString = sharedPreferences.getString(KEY_PROFILE_IMAGE, "");

            binding.textName.setText(name);
            binding.textEmail.setText(email);
            binding.textPhone.setText(phone);
            binding.textLocation.setText(location);

            if (!profileImageString.isEmpty()) {
                try {
                    Bitmap profileImage = decodeBase64Image(profileImageString);
                    binding.profileImage.setImageBitmap(profileImage);
                } catch (Exception e) {
                    Log.e(TAG, "Eroare la decodarea imaginii", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Eroare la încărcarea profilului", e);
            Toast.makeText(getContext(), "Eroare la încărcarea profilului: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditProfileDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_edit_profile, null);
            builder.setView(dialogView)
                    .setTitle("Editează profilul");

            EditText editName = dialogView.findViewById(R.id.editName);
            EditText editEmail = dialogView.findViewById(R.id.editEmail);
            EditText editPhone = dialogView.findViewById(R.id.editPhone);
            EditText editLocation = dialogView.findViewById(R.id.editLocation);
            Button buttonChangePhoto = dialogView.findViewById(R.id.buttonChangePhoto);
            Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);
            Button buttonSave = dialogView.findViewById(R.id.buttonSaveProfile);
            dialogProfileImage = dialogView.findViewById(R.id.editProfileImage);

            editName.setText(binding.textName.getText());
            editEmail.setText(binding.textEmail.getText());
            editPhone.setText(binding.textPhone.getText());
            editLocation.setText(binding.textLocation.getText());

            dialogProfileImage.setImageDrawable(binding.profileImage.getDrawable());

            currentDialog = builder.create();
            currentDialog.show();

            buttonChangePhoto.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Selectează o imagine"), PICK_IMAGE_REQUEST);
            });

            buttonCancel.setOnClickListener(v -> currentDialog.dismiss());

            buttonSave.setOnClickListener(v -> {
                try {
                    String newName = editName.getText().toString().trim();
                    String newEmail = editEmail.getText().toString().trim();
                    String newPhone = editPhone.getText().toString().trim();
                    String newLocation = editLocation.getText().toString().trim();

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    if (!newName.isEmpty()) {
                        editor.putString(KEY_NAME, newName);
                        binding.textName.setText(newName);
                    }
                    if (!newEmail.isEmpty()) {
                        editor.putString(KEY_EMAIL, newEmail);
                        binding.textEmail.setText(newEmail);
                    }
                    if (!newPhone.isEmpty()) {
                        editor.putString(KEY_PHONE, newPhone);
                        binding.textPhone.setText(newPhone);
                    }
                    if (!newLocation.isEmpty()) {
                        editor.putString(KEY_LOCATION, newLocation);
                        binding.textLocation.setText(newLocation);
                    }

                    binding.profileImage.setImageDrawable(dialogProfileImage.getDrawable());
                    saveProfileImage();

                    editor.apply();

                    Toast.makeText(getContext(), "Profilul a fost actualizat", Toast.LENGTH_SHORT).show();
                    currentDialog.dismiss();
                } catch (Exception e) {
                    Log.e(TAG, "Eroare la salvarea profilului", e);
                    Toast.makeText(getContext(), "Eroare la salvarea profilului: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Eroare la afișarea dialogului", e);
            Toast.makeText(getContext(), "Eroare la afișarea dialogului: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
            Log.e(TAG, "Eroare la salvarea imaginii de profil", e);
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
                Log.e(TAG, "Eroare la procesarea imaginii selectate", e);
                Toast.makeText(getContext(), "Eroare la încărcarea imaginii: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
}