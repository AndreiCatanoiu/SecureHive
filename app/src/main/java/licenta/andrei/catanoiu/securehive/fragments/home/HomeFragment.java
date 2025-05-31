package licenta.andrei.catanoiu.securehive.fragments.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.devices.Device;
import licenta.andrei.catanoiu.securehive.devices.UserDevice;
import licenta.andrei.catanoiu.securehive.devices.DeviceAdapter;

public class HomeFragment extends Fragment implements DeviceAdapter.DeviceAdapterListener {

    private static final String TAG = "HomeFragment";
    private TextView totalDevices, onlineDevices, offlineDevices, emptyText;
    private RecyclerView devicesList;
    private DeviceAdapter deviceAdapter;
    private ArrayList<UserDevice> userDevices;
    private Map<String, Device> deviceStatuses;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration userDevicesListener;
    private Map<String, ListenerRegistration> deviceStatusListeners;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userDevices = new ArrayList<>();
        deviceStatuses = new HashMap<>();
        deviceStatusListeners = new HashMap<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Inițializare views
        totalDevices = root.findViewById(R.id.totalDevices);
        onlineDevices = root.findViewById(R.id.onlineDevices);
        offlineDevices = root.findViewById(R.id.offlineDevices);
        devicesList = root.findViewById(R.id.devicesList);
        emptyText = root.findViewById(R.id.emptyText);

        // Configurare RecyclerView
        devicesList.setLayoutManager(new LinearLayoutManager(getContext()));
        deviceAdapter = new DeviceAdapter(getContext(), userDevices, this);
        devicesList.setAdapter(deviceAdapter);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");
        loadUserDevices();
    }

    private void loadUserDevices() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No user is currently logged in");
            Toast.makeText(getContext(), "Please log in to view your devices", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Loading devices for user: " + userId);
        Log.d(TAG, "User email: " + currentUser.getEmail());

        // Dezabonare de la listener-ul anterior dacă există
        if (userDevicesListener != null) {
            Log.d(TAG, "Removing existing listener");
            userDevicesListener.remove();
        }

        // Citim documentul utilizatorului pentru a obține dispozitivele
        userDevicesListener = db.collection("users")
                .document(userId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for user devices", error);
                        Log.e(TAG, "Error code: " + error.getCode());
                        Log.e(TAG, "Error message: " + error.getMessage());
                        Toast.makeText(getContext(), "Error loading devices: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        try {
                            // Curățăm listele existente
                            userDevices.clear();
                            deviceStatuses.clear();

                            // Oprim toți listener-ii existenți pentru statusurile device-urilor
                            for (ListenerRegistration listener : deviceStatusListeners.values()) {
                                listener.remove();
                            }
                            deviceStatusListeners.clear();

                            // Obținem câmpul userDevices
                            Map<String, Object> userDevicesMap = (Map<String, Object>) documentSnapshot.get("userDevices");
                            
                            if (userDevicesMap != null && !userDevicesMap.isEmpty()) {
                                Log.d(TAG, "Found userDevices map with " + userDevicesMap.size() + " devices");

                                for (Map.Entry<String, Object> entry : userDevicesMap.entrySet()) {
                                    String deviceId = entry.getKey();
                                    Map<String, Object> deviceData = (Map<String, Object>) entry.getValue();
                                    
                                    Log.d(TAG, "Processing device: " + deviceId);
                                    Log.d(TAG, "Device data: " + deviceData.toString());

                                    String customName = (String) deviceData.get("customName");
                                    if (customName == null || customName.isEmpty()) {
                                        customName = deviceId; // Folosim ID-ul ca nume implicit
                                    }

                                    UserDevice userDevice = new UserDevice(deviceId, customName);
                                    userDevices.add(userDevice);
                                    Log.d(TAG, "Added device to list: " + deviceId + " with name: " + customName);

                                    // Setăm listener pentru statusul device-ului
                                    setupDeviceStatusListener(deviceId);
                                }
                            } else {
                                Log.d(TAG, "No userDevices found in document or map is empty");
                            }

                            Log.d(TAG, "Final userDevices list size: " + userDevices.size());
                            updateUI();

                        } catch (Exception e) {
                            Log.e(TAG, "Error processing user devices", e);
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error processing devices: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "User document does not exist");
                        userDevices.clear();
                        updateUI();
                    }
                });

        Log.d(TAG, "Listener setup completed");
    }

    private void setupDeviceStatusListener(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "Attempted to setup listener for null or empty deviceId");
            return;
        }

        // Verificăm dacă avem deja un listener pentru acest device
        if (deviceStatusListeners.containsKey(deviceId)) {
            Log.d(TAG, "Listener already exists for device: " + deviceId);
            return;
        }

        Log.d(TAG, "Setting up listener for device: " + deviceId);

        // Creăm un nou listener pentru statusul device-ului
        ListenerRegistration listener = db.collection("devices")
                .document(deviceId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for device status: " + deviceId, error);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        try {
                            Device device = snapshot.toObject(Device.class);
                            if (device != null) {
                                Log.d(TAG, "Device " + deviceId + " status updated to: " + device.getStatus());
                                deviceStatuses.put(deviceId, device);
                                updateDeviceStatistics();
                                deviceAdapter.updateDeviceStatus(deviceId, device.getStatus());
                            } else {
                                Log.e(TAG, "Failed to parse device data for: " + deviceId);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing device data for: " + deviceId, e);
                        }
                    } else {
                        Log.w(TAG, "Device " + deviceId + " does not exist in database");
                    }
                });

        deviceStatusListeners.put(deviceId, listener);
    }

    private void updateDeviceStatistics() {
        int total = userDevices.size();
        int online = 0;
        int offline = 0;

        for (UserDevice userDevice : userDevices) {
            Device device = deviceStatuses.get(userDevice.getDeviceId());
            if (device != null && device.isActive()) {
                online++;
            } else {
                offline++;
            }
        }

        if (totalDevices != null) totalDevices.setText(String.valueOf(total));
        if (onlineDevices != null) onlineDevices.setText(String.valueOf(online));
        if (offlineDevices != null) offlineDevices.setText(String.valueOf(offline));
    }

    private void updateVisibility() {
        Log.d(TAG, "updateVisibility called with userDevices size: " + userDevices.size());
        if (userDevices == null || userDevices.isEmpty()) {
            Log.d(TAG, "No devices to display, showing empty state");
            if (devicesList != null) devicesList.setVisibility(View.GONE);
            if (emptyText != null) emptyText.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "Showing " + userDevices.size() + " devices");
            if (devicesList != null) devicesList.setVisibility(View.VISIBLE);
            if (emptyText != null) emptyText.setVisibility(View.GONE);
        }
    }

    private void updateUI() {
        Log.d(TAG, "Updating UI with " + userDevices.size() + " devices");
        if (deviceAdapter != null) {
            deviceAdapter.updateDevices(new ArrayList<>(userDevices));
        }
        updateDeviceStatistics();
        updateVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        if (mAuth.getCurrentUser() != null) {
            loadUserDevices();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }

    @Override
    public void onDeleteClick(UserDevice userDevice, int position) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String deviceId = userDevice.getDeviceId();
        Log.d(TAG, "Attempting to delete device: " + deviceId);

        // Ștergem device-ul din map-ul userDevices
        db.collection("users").document(currentUser.getUid())
                .update("userDevices." + deviceId, com.google.firebase.firestore.FieldValue.delete())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Device successfully removed: " + deviceId);
                    Toast.makeText(getContext(), "Device removed successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing device: " + deviceId, e);
                    Toast.makeText(getContext(), "Error removing device: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDeviceStatusError(String deviceId, String error) {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Error loading device status: " + error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");
        // Curățăm toți listenerii
        if (userDevicesListener != null) {
            userDevicesListener.remove();
        }
        for (ListenerRegistration listener : deviceStatusListeners.values()) {
            listener.remove();
        }
        deviceStatusListeners.clear();
    }
}