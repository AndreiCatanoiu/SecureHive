package licenta.andrei.catanoiu.securehive.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.databinding.ActivityAddDeviceBinding;
import licenta.andrei.catanoiu.securehive.devices.Device;
import licenta.andrei.catanoiu.securehive.devices.UserDevice;

import java.util.HashMap;
import java.util.Map;

public class AddDeviceActivity extends AppCompatActivity {

    private static final String TAG = "AddDeviceActivity";
    private ActivityAddDeviceBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityAddDeviceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        binding.buttonAdd.setOnClickListener(v -> addDevice());
        binding.buttonCancel.setOnClickListener(v -> finish());
        
        Log.d(TAG, "Activity initialized successfully");
    }

    private void addDevice() {
        String deviceId = binding.deviceIdInput.getText().toString().trim();
        String customName = binding.deviceNameInput.getText().toString().trim();

        Log.d(TAG, "Attempting to add device with ID: " + deviceId);

        if (deviceId.isEmpty()) {
            binding.deviceIdInput.setError(getString(R.string.device_id_null));
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "No user is currently logged in");
            Toast.makeText(this, "You must be logged in to add a device", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.buttonAdd.setEnabled(false);
        checkDeviceExists(deviceId, customName, user.getUid());
    }

    private void checkDeviceExists(String deviceId, String customName, String userId) {
        db.collection("devices").document(deviceId)
                .get()
                .addOnSuccessListener(deviceDoc -> {
                    if (deviceDoc.exists()) {
                        db.collection("users").document(userId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        Map<String, Object> userDevices = (Map<String, Object>) userDoc.get("userDevices");
                                        if (userDevices != null && userDevices.containsKey(deviceId)) {
                                            Log.d(TAG, "User already has this device: " + deviceId);
                                            binding.deviceIdInput.setError("You already have this device in your account");
                                            binding.buttonAdd.setEnabled(true);
                                        } else {
                                            addDeviceToUser(deviceId, customName, userId);
                                        }
                                    } else {
                                        addDeviceToUser(deviceId, customName, userId);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error checking user document", e);
                                    Toast.makeText(this, "Error checking user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    binding.buttonAdd.setEnabled(true);
                                });
                    } else {
                        Log.d(TAG, "Device does not exist in system: " + deviceId);
                        binding.deviceIdInput.setError("This device ID does not exist in the system");
                        binding.buttonAdd.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking device in system: " + deviceId, e);
                    Toast.makeText(this, "Error checking device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.buttonAdd.setEnabled(true);
                });
    }

    private void addDeviceToUser(String deviceId, String customName, String userId) {
        Log.d(TAG, "Adding device to user's collection: " + deviceId);

        Map<String, Object> deviceData = new HashMap<>();
        deviceData.put("deviceId", deviceId);
        deviceData.put("customName", customName);
        deviceData.put("addedAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(userId)
                .update("userDevices." + deviceId, deviceData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Device successfully added to user's collection");
                    Toast.makeText(this, R.string.device_added_successfully, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == 
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND) {
                        Map<String, Object> userData = new HashMap<>();
                        Map<String, Object> userDevices = new HashMap<>();
                        userDevices.put(deviceId, deviceData);
                        userData.put("userDevices", userDevices);
                        
                        db.collection("users").document(userId)
                                .set(userData)
                                .addOnSuccessListener(aVoid2 -> {
                                    Log.d(TAG, "Created new user document with device");
                                    Toast.makeText(this, R.string.device_added_successfully, Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e2 -> {
                                    Log.e(TAG, "Error creating user document", e2);
                                    Toast.makeText(this, "Error adding device: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
                                    binding.buttonAdd.setEnabled(true);
                                });
                    } else {
                        Log.e(TAG, "Error adding device to user's collection", e);
                        Toast.makeText(this, "Error adding device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        binding.buttonAdd.setEnabled(true);
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
