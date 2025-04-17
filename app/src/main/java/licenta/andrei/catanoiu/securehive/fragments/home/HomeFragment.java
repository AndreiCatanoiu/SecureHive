package licenta.andrei.catanoiu.securehive.fragments.home;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.activityes.DeviceInfoActivity;
import licenta.andrei.catanoiu.securehive.databinding.FragmentHomeBinding;
import licenta.andrei.catanoiu.securehive.devices.Device;
import licenta.andrei.catanoiu.securehive.devices.DeviceAdapter;

public class HomeFragment extends Fragment implements DeviceAdapter.DeviceAdapterListener {

    private FragmentHomeBinding binding;
    private ArrayList<Device> devices;
    private DeviceAdapter adapter;
    private static final String PREFS_DEVICES = "DevicesPrefs";
    private static final String KEY_DEVICES_LIST = "devices_list";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        ListView listView = binding.listDevices;
        TextView emptyView = binding.textEmpty;

        devices = new ArrayList<>();

        try {
            devices.addAll(getDevicesList());
        } catch (Exception e) {
            clearDevicesPrefs();
            Toast.makeText(getContext(), "Initializing new device list", Toast.LENGTH_SHORT).show();
        }

        adapter = new DeviceAdapter(getContext(), devices, this);
        listView.setAdapter(adapter);
        updateVisibility();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < devices.size()) {
                    Device selectedDevice = devices.get(position);
                    Intent intent = new Intent(getContext(), DeviceInfoActivity.class);
                    // Send device data
                    intent.putExtra("DEVICE_ID", selectedDevice.getId());
                    intent.putExtra("DEVICE_NAME", selectedDevice.getName());
                    intent.putExtra("DEVICE_IP", selectedDevice.getIpAddress());
                    intent.putExtra("DEVICE_STATUS", selectedDevice.isActive());
                    startActivity(intent);
                }
            }
        });

        binding.getRoot().post(new Runnable() {
            @Override
            public void run() {
                if (devices.isEmpty()) {
                    addTestDevice();
                }
            }
        });

        return root;
    }

    @Override
    public void onDeleteClick(Device device, int position) {
        confirmDeleteDevice(device, position);
    }

    private void clearDevicesPrefs() {
        if (getContext() != null) {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_DEVICES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
        }
    }

    private void addTestDevice() {
        Device testDevice = new Device("1", "Test Device", "192.168.1.1", true);
        devices.add(testDevice);
        adapter.notifyDataSetChanged();
        saveDevicesList();
        updateVisibility();
    }

    private void confirmDeleteDevice(final Device device, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Delete Confirmation");
        builder.setMessage("Are you sure you want to delete the device " + device.getName() + "?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String deviceId = device.getId();
                deleteDevice(position);
            }
        });

        builder.setNegativeButton("No", null);
        builder.show();
    }

    private void deleteDevice(int position) {
        if (position >= 0 && position < devices.size()) {
            devices.remove(position);
            adapter.notifyDataSetChanged();
            saveDevicesList();
            updateVisibility();
            Toast.makeText(getContext(), "Device deleted", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDevicesList() {
        if (getContext() == null) {
            return;
        }
        try {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_DEVICES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            Gson gson = new Gson();
            String json = gson.toJson(devices);
            editor.putString(KEY_DEVICES_LIST, json);
            editor.apply();

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error saving data", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDevicesList();
    }

    private void updateDevicesList() {
        devices.clear();
        try {
            devices.addAll(getDevicesList());
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            clearDevicesPrefs();
        }
        updateVisibility();
    }

    private List<Device> getDevicesList() {
        if (getContext() == null) {
            return new ArrayList<>();
        }
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_DEVICES, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(KEY_DEVICES_LIST, null);
        if (json == null) {
            return new ArrayList<>();
        } else {
            try {
                Type type = new TypeToken<ArrayList<Device>>() {}.getType();
                return gson.fromJson(json, type);
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
    }

    private void updateVisibility() {
        if (devices.isEmpty()) {
            binding.listDevices.setVisibility(View.GONE);
            binding.textEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.listDevices.setVisibility(View.VISIBLE);
            binding.textEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}