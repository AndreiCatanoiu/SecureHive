package licenta.andrei.catanoiu.securehive.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.databinding.ActivityAddDeviceBinding;

import java.util.HashMap;
import java.util.Map;

public class AddDeviceActivity extends AppCompatActivity {

    private static final String TAG = "AddDeviceActivity";
    private ActivityAddDeviceBinding binding;
    private DatabaseReference db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityAddDeviceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseDatabase.getInstance().getReference();
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
        db.child("devices").child(deviceId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    db.child("users").child(userId).child("userDevices").child(deviceId)
                      .addListenerForSingleValueEvent(new ValueEventListener() {
                          @Override
                          public void onDataChange(DataSnapshot userDeviceSnap) {
                              if (userDeviceSnap.exists()) {
                                  Log.d(TAG, "User already has this device: " + deviceId);
                                  binding.deviceIdInput.setError("You already have this device in your account");
                                  binding.buttonAdd.setEnabled(true);
                              } else {
                                  addDeviceToUser(deviceId, customName, userId);
                              }
                          }
                          @Override
                          public void onCancelled(DatabaseError error) {
                              Log.e(TAG, "Error checking user device", error.toException());
                              binding.buttonAdd.setEnabled(true);
                          }
                      });
                } else {
                    Log.d(TAG, "Device does not exist in system: " + deviceId);
                    binding.deviceIdInput.setError("This device ID does not exist in the system");
                    binding.buttonAdd.setEnabled(true);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error checking device in system: " + deviceId, error.toException());
                Toast.makeText(AddDeviceActivity.this, "Error checking device: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                binding.buttonAdd.setEnabled(true);
            }
        });
    }

    private void addDeviceToUser(String deviceId, String customName, String userId) {
        Log.d(TAG, "Adding device to user's collection: " + deviceId);
        Map<String, Object> deviceData = new HashMap<>();
        deviceData.put("deviceId", deviceId);
        deviceData.put("customName", customName);
        deviceData.put("addedAt", System.currentTimeMillis());
        db.child("users").child(userId).child("userDevices").child(deviceId).setValue(deviceData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Device successfully added to user's collection");
                finish();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding device to user", e);
                Toast.makeText(this, "Error adding device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                binding.buttonAdd.setEnabled(true);
            });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
