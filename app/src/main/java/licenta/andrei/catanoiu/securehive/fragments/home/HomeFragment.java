package licenta.andrei.catanoiu.securehive.fragments.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.activities.DeviceInfoActivity;
import licenta.andrei.catanoiu.securehive.devices.Device;
import licenta.andrei.catanoiu.securehive.devices.DeviceAdapter;

public class HomeFragment extends Fragment implements DeviceAdapter.DeviceAdapterListener {

    private TextView totalDevices, onlineDevices, offlineDevices, emptyText;
    private RecyclerView devicesList;
    private DeviceAdapter deviceAdapter;
    private static final String PREFS_DEVICES = "DevicesPrefs";
    private static final String KEY_DEVICES_LIST = "devices_list";
    private ArrayList<Device> devices;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Inițializare views
        totalDevices = root.findViewById(R.id.totalDevices);
        onlineDevices = root.findViewById(R.id.onlineDevices);
        offlineDevices = root.findViewById(R.id.offlineDevices);
        devicesList = root.findViewById(R.id.devicesList);
        emptyText = root.findViewById(R.id.emptyText);

        // Încărcare dispozitive
        devices = loadDevices();

        // Configurare RecyclerView
        devicesList.setLayoutManager(new LinearLayoutManager(getContext()));
        deviceAdapter = new DeviceAdapter(getContext(), devices, this);
        devicesList.setAdapter(deviceAdapter);

        // Actualizare UI
        updateDeviceStatistics();
        updateVisibility();

        return root;
    }

    private ArrayList<Device> loadDevices() {
        if (getContext() == null) {
            return new ArrayList<>();
        }
        
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_DEVICES, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(KEY_DEVICES_LIST, null);
        
        if (json == null) {
            return new ArrayList<>();
        }
        
        try {
            Type type = new TypeToken<ArrayList<Device>>() {}.getType();
            ArrayList<Device> loadedDevices = new Gson().fromJson(json, type);
            return loadedDevices != null ? loadedDevices : new ArrayList<>();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error loading devices", Toast.LENGTH_SHORT).show();
            return new ArrayList<>();
        }
    }

    private void updateDeviceStatistics() {
        if (devices == null) {
            totalDevices.setText("0");
            onlineDevices.setText("0");
            offlineDevices.setText("0");
            return;
        }

        int total = devices.size();
        int online = 0;
        int offline = 0;

        for (Device device : devices) {
            if (device.isActive()) {
                online++;
            } else {
                offline++;
            }
        }

        totalDevices.setText(String.valueOf(total));
        onlineDevices.setText(String.valueOf(online));
        offlineDevices.setText(String.valueOf(offline));
    }

    private void updateVisibility() {
        if (devices.isEmpty()) {
            devicesList.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
        } else {
            devicesList.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        devices = loadDevices();
        if (deviceAdapter != null) {
            deviceAdapter.updateDevices(devices);
        }
        updateDeviceStatistics();
        updateVisibility();
    }

    @Override
    public void onDeleteClick(Device device, int position) {
        // Implementare ștergere dispozitiv
        if (position >= 0 && position < devices.size()) {
            devices.remove(position);
            saveDevices();
            deviceAdapter.updateDevices(devices);
            updateDeviceStatistics();
            updateVisibility();
        }
    }

    private void saveDevices() {
        if (getContext() == null) return;
        
        try {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_DEVICES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String json = new Gson().toJson(devices);
            editor.putString(KEY_DEVICES_LIST, json);
            editor.apply();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error saving devices", Toast.LENGTH_SHORT).show();
        }
    }
}