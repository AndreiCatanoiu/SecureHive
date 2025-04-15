package licenta.andrei.catanoiu.securehive.fragments.home;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

import licenta.andrei.catanoiu.securehive.Device;
import licenta.andrei.catanoiu.securehive.DeviceAdapter;
import licenta.andrei.catanoiu.securehive.SwipeToDeleteHelper;
import licenta.andrei.catanoiu.securehive.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

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
            Toast.makeText(getContext(), "Inițializare listă nouă de dispozitive", Toast.LENGTH_SHORT).show();
        }

        adapter = new DeviceAdapter(getContext(), devices);
        listView.setAdapter(adapter);
        updateVisibility();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < devices.size()) {
                    showDeviceDetails(devices.get(position));
                }
            }
        });

        SwipeToDeleteHelper swipeHelper = new SwipeToDeleteHelper(listView) {
            @Override
            public void onSwipeLeft(int position) {
                if (position >= 0 && position < devices.size()) {
                    Device deviceToRemove = devices.get(position);
                    confirmDeleteDevice(deviceToRemove, position);
                }
            }
        };

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

    private void clearDevicesPrefs() {
        if (getContext() != null) {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_DEVICES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
        }
    }

    private void addTestDevice() {
        Device testDevice = new Device("1", "Dispozitiv Test", "192.168.1.1", true);
        devices.add(testDevice);
        adapter.notifyDataSetChanged();
        saveDevicesList();
        updateVisibility();
    }

    private void showDeviceDetails(Device device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Detalii dispozitiv");

        View detailView = LayoutInflater.from(getContext()).inflate(
                android.R.layout.simple_list_item_2, null);

        TextView titleView = detailView.findViewById(android.R.id.text1);
        TextView detailsView = detailView.findViewById(android.R.id.text2);

        titleView.setText(device.getName());

        String details = "ID: " + device.getId() + "\n" +
                "IP: " + device.getIpAddress() + "\n" +
                "Status: " + (device.isActive() ? "Activ" : "Inactiv");

        detailsView.setText(details);

        builder.setView(detailView);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void confirmDeleteDevice(final Device device, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Confirmare ștergere");
        builder.setMessage("Ești sigur că vrei să ștergi dispozitivul " + device.getName() + "?");

        builder.setPositiveButton("Da", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteDevice(position);
            }
        });

        builder.setNegativeButton("Nu", null);
        builder.show();
    }

    private void deleteDevice(int position) {
        if (position >= 0 && position < devices.size()) {
            devices.remove(position);
            adapter.notifyDataSetChanged();
            saveDevicesList();
            updateVisibility();
            Toast.makeText(getContext(), "Dispozitiv șters", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(), "Eroare la salvare", Toast.LENGTH_SHORT).show();
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