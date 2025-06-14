package licenta.andrei.catanoiu.securehive.activities;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.devices.Device;
import licenta.andrei.catanoiu.securehive.devices.UserDevice;
import licenta.andrei.catanoiu.securehive.utils.DeviceIdDecoder;
import licenta.andrei.catanoiu.securehive.utils.MqttConnectionManager;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Map;

public class DeviceDetailsActivity extends AppCompatActivity {
    private ImageView deviceImage;
    private TextView deviceName;
    private ImageButton editNameButton;
    private TextView deviceId;
    private TextView deviceType;
    private TextView deviceStatus;
    private TextView lastAlert;
    private RadioGroup statusRadioGroup;
    private RadioButton activeRadioButton;
    private RadioButton inactiveRadioButton;
    private Button saveNameButton;

    private MqttConnectionManager mqttManager;
    private boolean isMqttConnected = false;
    private UserDevice userDevice;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private static final String TAG = "DeviceDetailsActivity";
    private Device.DeviceStatus currentStatus = Device.DeviceStatus.OFFLINE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_details);

        initializeViews();
        userDevice = getIntent().getParcelableExtra("device");
        if (userDevice != null) {
            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();
            setupMqttClient();
            loadDeviceInfo();
            setupRadioGroup();
            setupEditNameButton();
        } else {
            Toast.makeText(this, "Error: Device information not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        deviceImage = findViewById(R.id.deviceImage);
        deviceName = findViewById(R.id.deviceName);
        editNameButton = findViewById(R.id.editNameButton);
        deviceId = findViewById(R.id.deviceId);
        deviceType = findViewById(R.id.deviceType);
        deviceStatus = findViewById(R.id.deviceStatus);
        lastAlert = findViewById(R.id.lastAlert);
        statusRadioGroup = findViewById(R.id.statusRadioGroup);
        activeRadioButton = findViewById(R.id.activeRadioButton);
        inactiveRadioButton = findViewById(R.id.inactiveRadioButton);
    }

    private void setupMqttClient() {
        Log.d(TAG, "[MQTT] Setup started");
        
        // Check network connectivity first
        if (!isNetworkAvailable()) {
            Log.e(TAG, "[MQTT] No network connectivity available");
            runOnUiThread(() -> {
                Toast.makeText(this, "Nu există conexiune la internet. Verifică conexiunea și încearcă din nou.", Toast.LENGTH_LONG).show();
                isMqttConnected = false;
                updateUIForConnectionState();
            });
            return;
        }
        
        try {
            mqttManager = new MqttConnectionManager(this);
            mqttManager.setConnectionCallback(new MqttConnectionManager.ConnectionCallback() {
                @Override
                public void onConnected() {
                    isMqttConnected = true;
                    Log.d(TAG, "[MQTT] Connected successfully!");
                    runOnUiThread(() -> {
                        Toast.makeText(DeviceDetailsActivity.this, "Conectat la serverul MQTT", Toast.LENGTH_SHORT).show();
                        updateUIForConnectionState();
                    });
                }

                @Override
                public void onDisconnected(String reason) {
                    isMqttConnected = false;
                    Log.e(TAG, "[MQTT] Connection lost: " + reason);
                    runOnUiThread(() -> {
                        Toast.makeText(DeviceDetailsActivity.this, "Conexiunea MQTT s-a întrerupt: " + reason, Toast.LENGTH_LONG).show();
                        updateUIForConnectionState();
                    });
                }

                @Override
                public void onError(String error) {
                    isMqttConnected = false;
                    Log.e(TAG, "[MQTT] Error: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(DeviceDetailsActivity.this, "Eroare MQTT: " + error, Toast.LENGTH_LONG).show();
                        updateUIForConnectionState();
                    });
                }

                @Override
                public void onMessageReceived(String topic, String message) {
                    Log.d(TAG, "[MQTT] Message received on topic " + topic + ": " + message);
                }
            });
            
            mqttManager.connect();
            
        } catch (Exception e) {
            isMqttConnected = false;
            Log.e(TAG, "[MQTT] Unexpected error: " + e.getMessage(), e);
            runOnUiThread(() -> Toast.makeText(this, "Eroare neașteptată: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
        updateUIForConnectionState();
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = 
            (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void loadDeviceInfo() {
        // Set device name and ID
        deviceName.setText(userDevice.getCustomName());
        deviceId.setText("ID: " + userDevice.getDeviceId());

        // Set device type
        DeviceIdDecoder.DeviceType deviceTypeEnum = DeviceIdDecoder.getDeviceType(userDevice.getDeviceId());
        deviceType.setText("Type: " + deviceTypeEnum.name());

        // Set device image based on type
        switch (deviceTypeEnum) {
            case PIR:
                deviceImage.setImageResource(R.drawable.pir);
                break;
            case GAS:
                deviceImage.setImageResource(R.drawable.gaz);
                break;
            default:
                deviceImage.setImageResource(R.drawable.ic_device);
                break;
        }

        // Load device status from Firestore
        db.collection("devices").document(userDevice.getDeviceId())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                Device device = documentSnapshot.toObject(Device.class);
                if (device != null) {
                    updateDeviceStatus(device);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load device status", Toast.LENGTH_SHORT).show();
            });
    }

    private void updateDeviceStatus(Device device) {
        currentStatus = device.getStatus();
        String statusText;
        int statusColor;
        switch (currentStatus) {
            case ONLINE:
                statusText = "Online";
                statusColor = getColor(R.color.online);
                activeRadioButton.setChecked(true);
                break;
            case OFFLINE:
                statusText = "Offline";
                statusColor = getColor(R.color.offline);
                inactiveRadioButton.setChecked(true);
                break;
            case MAINTENANCE:
                statusText = "În mentenanță";
                statusColor = getColor(R.color.maintenance);
                inactiveRadioButton.setChecked(true);
                break;
            default:
                statusText = "Necunoscut";
                statusColor = getColor(R.color.text_secondary);
                break;
        }
        deviceStatus.setText(statusText);
        deviceStatus.setTextColor(statusColor);
        updateUIForConnectionState();
    }

    private void setupRadioGroup() {
        statusRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (currentStatus != Device.DeviceStatus.ONLINE) {
                Toast.makeText(this, "Nu se pot face modificări când dispozitivul este offline sau în mentenanță", Toast.LENGTH_LONG).show();
                updateDeviceStatusFromFirestore();
                return;
            }
            if (!isMqttConnected) {
                Toast.makeText(this, "Conexiunea MQTT nu este activă!", Toast.LENGTH_LONG).show();
                return;
            }
            String message;
            if (checkedId == R.id.activeRadioButton) {
                message = "#settings sensor status UP";
            } else if (checkedId == R.id.inactiveRadioButton) {
                message = "#settings sensor status MAINTENANCE";
            } else {
                return;
            }
            publishStatusChange(message);
        });
    }

    private void updateDeviceStatusFromFirestore() {
        db.collection("devices").document(userDevice.getDeviceId())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                Device device = documentSnapshot.toObject(Device.class);
                if (device != null) {
                    updateDeviceStatus(device);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to refresh device status", Toast.LENGTH_SHORT).show();
            });
    }

    private void setupEditNameButton() {
        editNameButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Editează numele senzorului");
            final EditText input = new EditText(this);
            input.setText(userDevice.getCustomName());
            builder.setView(input);
            builder.setPositiveButton("Salvează", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    userDevice.setCustomName(newName);
                    deviceName.setText(newName);
                    // Update Firestore în map-ul userDevices
                    String userId = auth.getCurrentUser().getUid();
                    String deviceKey = userDevice.getDeviceId();
                    Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("userDevices." + deviceKey + ".customName", newName);
                    db.collection("users").document(userId)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Numele a fost actualizat!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Eroare la actualizare nume: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
            builder.setNegativeButton("Anulează", (dialog, which) -> dialog.cancel());
            builder.show();
        });
    }

    private void publishStatusChange(String message) {
        Log.d(TAG, "[MQTT] Attempting to publish: " + message);
        
        // Check if device is online
        if (currentStatus != Device.DeviceStatus.ONLINE) {
            Toast.makeText(this, "Nu se pot face modificări când dispozitivul este offline sau în mentenanță", Toast.LENGTH_LONG).show();
            updateDeviceStatusFromFirestore();
            return;
        }
        
        // Check MQTT connection
        if (!isMqttConnected || mqttManager == null || !mqttManager.isConnected()) {
            Log.e(TAG, "[MQTT] Not connected. Attempting to reconnect...");
            Toast.makeText(this, "Conexiunea MQTT nu este activă. Se încearcă reconectarea...", Toast.LENGTH_LONG).show();
            
            // Try to reconnect
            setupMqttClient();
            
            // Wait a bit and try again
            new android.os.Handler().postDelayed(() -> {
                if (isMqttConnected && mqttManager != null && mqttManager.isConnected()) {
                    publishStatusChange(message); // Retry
                } else {
                    Toast.makeText(this, "Nu s-a putut restabili conexiunea MQTT. Încearcă din nou.", Toast.LENGTH_LONG).show();
                    updateDeviceStatusFromFirestore();
                }
            }, 3000); // 3 seconds delay
            return;
        }

        try {
            String topic = "andrei/catanoiu/licenta/" + userDevice.getDeviceId() + "/down";
            mqttManager.publish(topic, message, 1);
            Log.d(TAG, "[MQTT] Published to " + topic + ": " + message);
            Toast.makeText(this, "Status actualizat cu succes", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "[MQTT] Unexpected publish error", e);
            Toast.makeText(this, "Eroare neașteptată: " + e.getMessage(), Toast.LENGTH_LONG).show();
            updateDeviceStatusFromFirestore();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh device info when returning to this activity
        if (userDevice != null) {
            loadDeviceInfo();
            
            // Check MQTT connection status and try to reconnect if needed
            if (!isMqttConnected || mqttManager == null || !mqttManager.isConnected()) {
                Log.d(TAG, "[MQTT] Connection lost, attempting to reconnect in onResume");
                new android.os.Handler().postDelayed(() -> {
                    if (!isMqttConnected) {
                        setupMqttClient();
                    }
                }, 1000); // 1 second delay
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttManager != null) {
            mqttManager.disconnect();
        }
    }

    private void updateUIForConnectionState() {
        runOnUiThread(() -> {
            boolean shouldEnableControls = currentStatus == Device.DeviceStatus.ONLINE && userDevice != null && isMqttConnected;
            statusRadioGroup.setEnabled(shouldEnableControls);
            for (int i = 0; i < statusRadioGroup.getChildCount(); i++) {
                statusRadioGroup.getChildAt(i).setEnabled(shouldEnableControls);
            }
        });
    }
}