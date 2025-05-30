package licenta.andrei.catanoiu.securehive.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.databinding.ActivityAddDeviceBinding;
import licenta.andrei.catanoiu.securehive.devices.Device;

public class AddDeviceActivity extends AppCompatActivity {

    private ActivityAddDeviceBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddDeviceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.add_a_new_device);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        binding.buttonAdd.setOnClickListener(v -> addDevice());
        binding.buttonCancel.setOnClickListener(v -> finish());
    }

    private void addDevice() {
        String deviceId = binding.deviceIdInput.getText().toString().trim();
        String deviceName = binding.deviceNameInput.getText().toString().trim();

        if (deviceId.isEmpty()) {
            binding.deviceIdInput.setError(getString(R.string.device_id_null));
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Adăugăm device-ul în lista utilizatorului
            db.collection("users").document(user.getUid())
                    .update("devices", FieldValue.arrayUnion(deviceId))
                    .addOnSuccessListener(aVoid -> {
                        // Creăm un document pentru device
                        db.collection("devices").document(deviceId)
                                .set(new Device(deviceId, deviceName))
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(this, R.string.device_added_successfully, Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error adding device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error updating user devices: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
