package licenta.andrei.catanoiu.securehive.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import licenta.andrei.catanoiu.securehive.devices.Device;
import licenta.andrei.catanoiu.securehive.R;

public class AddDeviceActivity extends AppCompatActivity {

    private static final String TAG = "AddDeviceActivity";
    private EditText editDeviceId;
    private EditText editDeviceName;
    private Button buttonAddDevice;
    private static final String PREFS_DEVICES = "DevicesPrefs";
    private static final String KEY_DEVICES_LIST = "devices_list";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle(R.string.add_devices);
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        editDeviceId = findViewById(R.id.editDeviceId);
        editDeviceName = findViewById(R.id.editDeviceName);
        buttonAddDevice = findViewById(R.id.buttonAddDevice);

        buttonAddDevice.setOnClickListener(v -> {
            String deviceId = editDeviceId.getText().toString().trim();
            String deviceNameInput = editDeviceName.getText().toString().trim();

            if (deviceId.isEmpty()) {
                Toast.makeText(this, R.string.device_id_null, Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Log.w(TAG, "User is not authenticated");
                Toast.makeText(this, "Trebuie să fiți autentificat pentru a adăuga dispozitive", Toast.LENGTH_LONG).show();
                return;
            }

            buttonAddDevice.setEnabled(false);

            db.collection("devices")
                .document(deviceId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && task.getResult().exists()) {
                            Log.d(TAG, "Device found in Firestore: " + deviceId);
                            
                            // Verificăm dacă dispozitivul aparține utilizatorului curent
                            String deviceOwnerId = task.getResult().getString("userId");
                            if (deviceOwnerId != null && deviceOwnerId.equals(currentUser.getUid())) {
                                String displayName = deviceNameInput.isEmpty() ?
                                        "Device ID: " + deviceId : deviceNameInput;
                                addDevice(deviceId, displayName);
                            } else {
                                Log.w(TAG, "Device belongs to another user or has no owner");
                                Toast.makeText(this, "Nu aveți permisiunea de a adăuga acest dispozitiv", Toast.LENGTH_LONG).show();
                                buttonAddDevice.setEnabled(true);
                            }
                        } else {
                            Log.w(TAG, "Device not found in Firestore: " + deviceId);
                            Toast.makeText(this, "Acest dispozitiv nu există în baza de date!", Toast.LENGTH_LONG).show();
                            buttonAddDevice.setEnabled(true);
                        }
                    } else {
                        Exception e = task.getException();
                        Log.e(TAG, "Error checking device in Firestore", e);
                        Toast.makeText(this, 
                            "Eroare la verificarea dispozitivului: " + 
                            (e != null ? e.getMessage() : "Eroare necunoscută"), 
                            Toast.LENGTH_LONG).show();
                        buttonAddDevice.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore query failed", e);
                    Toast.makeText(this, 
                        "Eroare la conectarea la baza de date: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                    buttonAddDevice.setEnabled(true);
                });
        });
    }

    private void addDevice(String deviceId, String displayName) {
        Device newDevice = new Device(deviceId, displayName, "No alerts yet", Device.DeviceStatus.OFFLINE);
        saveDevice(newDevice);

        Toast.makeText(this, R.string.device_added_successfully, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void saveDevice(Device newDevice) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_DEVICES, Context.MODE_PRIVATE);
        List<Device> devicesList = getDevicesList();
        devicesList.add(newDevice);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(devicesList);
        editor.putString(KEY_DEVICES_LIST, json);
        editor.apply();
    }

    private List<Device> getDevicesList() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_DEVICES, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(KEY_DEVICES_LIST, null);
        Type type = new TypeToken<ArrayList<Device>>() {}.getType();

        if (json == null) {
            return new ArrayList<>();
        } else {
            return gson.fromJson(json, type);
        }
    }
}
