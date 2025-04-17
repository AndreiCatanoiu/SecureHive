package licenta.andrei.catanoiu.securehive.activityes;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import licenta.andrei.catanoiu.securehive.devices.Device;
import licenta.andrei.catanoiu.securehive.R;

public class AddDeviceActivity extends AppCompatActivity {

    private EditText editDeviceId;
    private EditText editDeviceName;
    private Button buttonAddDevice;
    private static final String PREFS_DEVICES = "DevicesPrefs";
    private static final String KEY_DEVICES_LIST = "devices_list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle(R.string.add_devices);
        }

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

            String displayName = deviceNameInput.isEmpty() ?
                    "Device ID: " + deviceId : deviceNameInput;

            addDevice(deviceId, displayName);
        });
    }

    private void addDevice(String deviceId, String displayName) {
        buttonAddDevice.setEnabled(false);

        Device newDevice = new Device(deviceId, displayName, "0.0.0.0", true);
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
