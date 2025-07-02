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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.devices.Device;
import licenta.andrei.catanoiu.securehive.devices.UserDevice;
import licenta.andrei.catanoiu.securehive.utils.DeviceIdDecoder;
import licenta.andrei.catanoiu.securehive.utils.MqttConnectionManager;


public class DeviceDetailsActivity extends AppCompatActivity {
    private ImageView deviceImage;
    private TextView deviceName;
    private ImageButton editNameButton;
    private TextView deviceId;
    private TextView deviceType;
    private TextView deviceStatus;
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
    private com.google.firebase.database.ValueEventListener deviceStatusListener;
    private com.google.firebase.database.DatabaseReference dbRealtime;
    private boolean isTryingReconnect = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_details);

        initializeViews();
        userDevice = getIntent().getParcelableExtra("device");
        if (userDevice != null) {
            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();
            dbRealtime = com.google.firebase.database.FirebaseDatabase.getInstance().getReference();
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
        statusRadioGroup = findViewById(R.id.statusRadioGroup);
        activeRadioButton = findViewById(R.id.activeRadioButton);
        inactiveRadioButton = findViewById(R.id.inactiveRadioButton);
    }

    private void setupMqttClient() {
        Log.d(TAG, "[MQTT] Setup started");
        
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
    }

    private void updateDeviceStatus(Device device) {
        if (device == null || device.getStatus() == null) {
            currentStatus = Device.DeviceStatus.OFFLINE;
            deviceStatus.setText("Necunoscut");
            deviceStatus.setTextColor(getColor(R.color.text_secondary));
            statusRadioGroup.clearCheck();
            inactiveRadioButton.setChecked(true);
            statusRadioGroup.setEnabled(false);
            activeRadioButton.setEnabled(false);
            inactiveRadioButton.setEnabled(false);
            updateUIForConnectionState();
            return;
        }
        currentStatus = device.getStatus();
        String statusText;
        int statusColor;
        if (device.isOnline()) {
            statusText = "Online";
            statusColor = getColor(R.color.online);
            statusRadioGroup.setEnabled(true);
            activeRadioButton.setEnabled(true);
            inactiveRadioButton.setEnabled(true);
            activeRadioButton.setChecked(true);
        } else if (device.isMaintenance()) {
            statusText = "În mentenanță";
            statusColor = getColor(R.color.maintenance);
            statusRadioGroup.setEnabled(true);
            activeRadioButton.setEnabled(true);
            inactiveRadioButton.setEnabled(true);
            inactiveRadioButton.setChecked(true);
        } else if (device.isOffline()) {
            statusText = "Offline";
            statusColor = getColor(R.color.offline);
            statusRadioGroup.setEnabled(false);
            activeRadioButton.setEnabled(false);
            inactiveRadioButton.setEnabled(false);
            inactiveRadioButton.setChecked(true);
            if (mqttManager != null) {
                mqttManager.disconnect();
            }
        } else {
            statusText = "Necunoscut";
            statusColor = getColor(R.color.text_secondary);
            statusRadioGroup.setEnabled(false);
            activeRadioButton.setEnabled(false);
            inactiveRadioButton.setEnabled(false);
            inactiveRadioButton.setChecked(true);
        }
        deviceStatus.setText(statusText);
        deviceStatus.setTextColor(statusColor);
        updateUIForConnectionState();
    }

    private void setupRadioGroup() {
        statusRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (currentStatus != Device.DeviceStatus.ONLINE) {
                // Nu permite modificarea statusului dacă nu e online
                updateUIForConnectionState();
                if (inactiveRadioButton != null) {
                    inactiveRadioButton.setChecked(true);
                }
                return;
            }
            final String message;
            if (checkedId == R.id.activeRadioButton) {
                message = "#settings sensor status UP";
            } else if (checkedId == R.id.inactiveRadioButton) {
                message = "#settings sensor status MAINTENANCE";
            } else {
                message = null;
            }
            if (message != null) {
                if (!isMqttConnected) {
                    if (!isTryingReconnect) {
                        isTryingReconnect = true;
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Eroare MQTT")
                                .setMessage("Conexiunea MQTT nu este activă! Se încearcă reconectarea...")
                                .setCancelable(false)
                                .show();
                        setupMqttClient();
                        final String msgToSend = message;
                        new android.os.Handler().postDelayed(() -> {
                            if (isMqttConnected && mqttManager != null && mqttManager.isConnected()) {
                                publishStatusChange(msgToSend);
                            } else {
                                new androidx.appcompat.app.AlertDialog.Builder(this)
                                        .setTitle("Eroare MQTT")
                                        .setMessage("Nu s-a putut restabili conexiunea MQTT. Încearcă din nou.")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                            isTryingReconnect = false;
                        }, 3000);
                    }
                } else {
                    publishStatusChange(message);
                }
            }
        });
    }

    private void setupEditNameButton() {
        editNameButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
            android.view.View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
            EditText nameInput = view.findViewById(R.id.editName);
            view.findViewById(R.id.editPhone).setVisibility(android.view.View.GONE); // ascundem câmpul de telefon
            nameInput.setText(userDevice.getCustomName());
            builder.setView(view)
                    .setTitle("Editează numele dispozitivului")
                    .setPositiveButton(R.string.save, null)
                    .setNegativeButton(R.string.cancel, null);
            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2 -> {
                String newName = nameInput.getText().toString().trim();
                String currentName = userDevice.getCustomName();
                if (newName.isEmpty()) {
                    nameInput.setError("Introduceți un nume");
                    return;
                }
                if (newName.equals(currentName)) {
                    Toast.makeText(this, "Nicio modificare de salvat", Toast.LENGTH_SHORT).show();
                    return;
                }
                updateDeviceNameInDatabase(newName, dialog, nameInput);
            });
        });
    }

    private void updateDeviceNameInDatabase(String newName, AlertDialog dialog, EditText nameInput) {
        String userId = auth.getCurrentUser().getUid();
        String deviceKey = userDevice.getDeviceId();
        if (userId != null && deviceKey != null) {
            com.google.firebase.database.DatabaseReference dbRealtime = com.google.firebase.database.FirebaseDatabase.getInstance().getReference();
            dbRealtime.child("users").child(userId).child("userDevices").child(deviceKey).child("customName")
                .setValue(newName)
                .addOnSuccessListener(aVoid -> {
                    userDevice.setCustomName(newName);
                    deviceName.setText(newName);
                    Toast.makeText(this, "Numele a fost actualizat!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la actualizare nume: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    nameInput.setError("Eroare la actualizare");
                });
        }
    }

    private void publishStatusChange(String message) {
        Log.d(TAG, "[MQTT] Attempting to publish: " + message);
        
        // Check if device is online
        if (currentStatus != Device.DeviceStatus.ONLINE) {
            Toast.makeText(this, "Nu se pot face modificări când dispozitivul este offline sau în mentenanță", Toast.LENGTH_LONG).show();
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
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userDevice != null) {
            loadDeviceInfo();
            // Ascultă statusul live din Realtime Database
            if (dbRealtime != null && deviceStatusListener == null) {
                deviceStatusListener = new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                        Device device = snapshot.getValue(Device.class);
                        if (device != null) {
                            updateDeviceStatus(device);
                        }
                    }
                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
                        Toast.makeText(DeviceDetailsActivity.this, "Eroare la citirea statusului", Toast.LENGTH_SHORT).show();
                    }
                };
                dbRealtime.child("devices").child(userDevice.getDeviceId()).addValueEventListener(deviceStatusListener);
            }
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
    protected void onPause() {
        super.onPause();
        // Curăț listenerul de status
        if (dbRealtime != null && deviceStatusListener != null && userDevice != null) {
            dbRealtime.child("devices").child(userDevice.getDeviceId()).removeEventListener(deviceStatusListener);
            deviceStatusListener = null;
        }
        if (mqttManager != null) {
            mqttManager.disconnect();
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
            boolean isOffline = currentStatus == Device.DeviceStatus.OFFLINE;
            boolean shouldEnableControls = currentStatus == Device.DeviceStatus.ONLINE && userDevice != null;
            statusRadioGroup.setEnabled(!isOffline);
            for (int i = 0; i < statusRadioGroup.getChildCount(); i++) {
                statusRadioGroup.getChildAt(i).setEnabled(!isOffline);
                // Gri dacă e offline
                statusRadioGroup.getChildAt(i).setAlpha(isOffline ? 0.5f : 1.0f);
            }
            if (isOffline) {
                statusRadioGroup.clearCheck();
                inactiveRadioButton.setChecked(true);
            }
        });
    }
}