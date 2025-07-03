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
    private boolean isUpdatingRadioGroup = false;
    private AlertDialog reconnectDialog = null;

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
                        if (reconnectDialog != null) {
                            reconnectDialog.dismiss();
                            reconnectDialog = null;
                        }
                        updateUIForConnectionState();
                    });
                }
                @Override
                public void onDisconnected(String reason) {
                    isMqttConnected = false;
                    Log.e(TAG, "[MQTT] Connection lost: " + reason);
                    runOnUiThread(() -> {
                        updateUIForConnectionState();
                    });
                }
                @Override
                public void onError(String error) {
                    isMqttConnected = false;
                    Log.e(TAG, "[MQTT] Error: " + error);
                    runOnUiThread(() -> {
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
        deviceName.setText(userDevice.getCustomName());
        deviceId.setText("ID: " + userDevice.getDeviceId());

        DeviceIdDecoder.DeviceType deviceTypeEnum = DeviceIdDecoder.getDeviceType(userDevice.getDeviceId());
        deviceType.setText("Type: " + deviceTypeEnum.name());

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
            isUpdatingRadioGroup = true;
            statusRadioGroup.clearCheck();
            inactiveRadioButton.setChecked(true);
            isUpdatingRadioGroup = false;
            statusRadioGroup.setEnabled(false);
            activeRadioButton.setEnabled(false);
            inactiveRadioButton.setEnabled(false);
            updateUIForConnectionState();
            return;
        }

        boolean isMaintenance = device.isMaintenance();
        currentStatus = (isMaintenance) ? Device.DeviceStatus.ONLINE : device.getStatus();
        String statusText;
        int statusColor;
        isUpdatingRadioGroup = true;
        if (device.isOnline() || isMaintenance) {
            if (isMaintenance) {
                statusText = "Inactive";
                statusColor = getColor(R.color.maintenance);
            } else {
                statusText = "Online";
                statusColor = getColor(R.color.online);
            }
            statusRadioGroup.setEnabled(true);
            activeRadioButton.setEnabled(true);
            inactiveRadioButton.setEnabled(true);
            if (isMaintenance) {
                statusRadioGroup.clearCheck();
                inactiveRadioButton.setChecked(true);
                activeRadioButton.setChecked(false);
            } else {
                statusRadioGroup.clearCheck();
                activeRadioButton.setChecked(true);
                inactiveRadioButton.setChecked(false);
            }
        } else if (device.isOffline()) {
            statusText = "Offline";
            statusColor = getColor(R.color.offline);
            statusRadioGroup.setEnabled(false);
            activeRadioButton.setEnabled(false);
            inactiveRadioButton.setEnabled(false);
            statusRadioGroup.clearCheck();
            inactiveRadioButton.setChecked(true);
            activeRadioButton.setChecked(false);
            if (mqttManager != null) {
                mqttManager.disconnect();
            }
        } else {
            statusText = "Necunoscut";
            statusColor = getColor(R.color.text_secondary);
            statusRadioGroup.setEnabled(false);
            activeRadioButton.setEnabled(false);
            inactiveRadioButton.setEnabled(false);
            statusRadioGroup.clearCheck();
            inactiveRadioButton.setChecked(true);
            activeRadioButton.setChecked(false);
        }
        isUpdatingRadioGroup = false;
        deviceStatus.setText(statusText);
        deviceStatus.setTextColor(statusColor);
        updateUIForConnectionState();
    }

    private void setupRadioGroup() {
        statusRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (isUpdatingRadioGroup) return;
            if (currentStatus != Device.DeviceStatus.ONLINE && currentStatus != Device.DeviceStatus.MAINTENANCE) {
                updateUIForConnectionState();
                if (inactiveRadioButton != null) {
                    isUpdatingRadioGroup = true;
                    inactiveRadioButton.setChecked(true);
                    isUpdatingRadioGroup = false;
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
                        setupMqttClient();
                        final String msgToSend = message;
                        new android.os.Handler().postDelayed(() -> {
                            if (isMqttConnected && mqttManager != null && mqttManager.isConnected()) {
                                publishStatusChange(msgToSend);
                            } else {
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
            view.findViewById(R.id.editPhone).setVisibility(android.view.View.GONE);
            nameInput.setText(userDevice.getCustomName());
            builder.setView(view)
                    .setTitle("Edit device name")
                    .setPositiveButton(R.string.save, null)
                    .setNegativeButton(R.string.cancel, null);
            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2 -> {
                String newName = nameInput.getText().toString().trim();
                String currentName = userDevice.getCustomName();
                if (newName.length() < 3) {
                    nameInput.setError("Please enter a name. Minimum 3 characters");
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
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    nameInput.setError("Error updating");
                });
        }
    }

    private void publishStatusChange(String message) {
        Log.d(TAG, "[MQTT] Attempting to publish: " + message);
        if (currentStatus != Device.DeviceStatus.ONLINE && currentStatus != Device.DeviceStatus.MAINTENANCE) {
            return;
        }
        if (!isMqttConnected || mqttManager == null || !mqttManager.isConnected()) {
            Log.e(TAG, "[MQTT] Not connected. Attempting to reconnect...");
            setupMqttClient();
            new android.os.Handler().postDelayed(() -> {
                if (isMqttConnected && mqttManager != null && mqttManager.isConnected()) {
                    publishStatusChange(message);
                } else {
                }
            }, 3000);
            return;
        }
        try {
            String topic = "senzor/licenta/andrei/catanoiu/" + userDevice.getDeviceId() + "/down";
            mqttManager.publish(topic, message, 1);
            Log.d(TAG, "[MQTT] Published to " + topic + ": " + message);
            Toast.makeText(this, "Status updated successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "[MQTT] Unexpected publish error", e);
            Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userDevice != null) {
            loadDeviceInfo();
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
                        Toast.makeText(DeviceDetailsActivity.this, "Error reading status", Toast.LENGTH_SHORT).show();
                    }
                };
                dbRealtime.child("devices").child(userDevice.getDeviceId()).addValueEventListener(deviceStatusListener);
            }
            if (!isMqttConnected || mqttManager == null || !mqttManager.isConnected()) {
                Log.d(TAG, "[MQTT] Connection lost, attempting to reconnect in onResume");
                new android.os.Handler().postDelayed(() -> {
                    if (!isMqttConnected) {
                        setupMqttClient();
                    }
                }, 1000);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dbRealtime != null && deviceStatusListener != null && userDevice != null) {
            dbRealtime.child("devices").child(userDevice.getDeviceId()).removeEventListener(deviceStatusListener);
            deviceStatusListener = null;
        }
        if (mqttManager != null && userDevice != null && mqttManager.isConnected()) {
            String topic = "senzor/licenta/andrei/catanoiu/" + userDevice.getDeviceId() + "/down";
            mqttManager.unsubscribe(topic);
        }
        if (mqttManager != null) {
            mqttManager.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttManager != null && userDevice != null && mqttManager.isConnected()) {
            String topic = "senzor/licenta/andrei/catanoiu/" + userDevice.getDeviceId() + "/down";
            mqttManager.unsubscribe(topic);
        }
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
                statusRadioGroup.getChildAt(i).setAlpha(isOffline ? 0.5f : 1.0f);
            }
            if (isOffline) {
                isUpdatingRadioGroup = true;
                statusRadioGroup.clearCheck();
                inactiveRadioButton.setChecked(true);
                isUpdatingRadioGroup = false;
            }
        });
    }
}