package licenta.andrei.catanoiu.securehive.fragments.alerts;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.adapters.AlertsAdapter;
import licenta.andrei.catanoiu.securehive.databinding.FragmentAlertsBinding;
import licenta.andrei.catanoiu.securehive.models.Alert;
import licenta.andrei.catanoiu.securehive.utils.NotificationService;

public class AlertsFragment extends Fragment {

    private static final String TAG = "AlertsFragment";
    private FragmentAlertsBinding binding;
    private AlertsAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SimpleDateFormat dateFormat;
    private Date filterStartDate = null;
    private Date filterEndDate = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        // Create notification channel
        NotificationService.createNotificationChannel(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAlertsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupRecyclerView();
        setupFilterListeners();
        loadAlerts();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh alerts when returning to this fragment
        loadAlerts();
    }

    private void setupRecyclerView() {
        adapter = new AlertsAdapter(requireContext());
        binding.recyclerAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerAlerts.setAdapter(adapter);
    }

    private void setupFilterListeners() {
        // Filter button - toggles filter options visibility
        binding.filterButton.setOnClickListener(v -> toggleFilterOptions());

        // Clear filters button
        binding.clearFiltersButton.setOnClickListener(v -> {
            filterStartDate = null;
            filterEndDate = null;
            adapter.clearFilters();
            hideFilterOptions();
            updateEmptyState();
            Toast.makeText(requireContext(), "Filters cleared", Toast.LENGTH_SHORT).show();
        });

        // Setup filter options
        setupFilterOptions();
    }

    private void setupFilterOptions() {
        // Setup spinners
        setupSpinners();

        // Setup date buttons
        binding.startDateButton.setOnClickListener(v -> showDatePicker(true));
        binding.endDateButton.setOnClickListener(v -> showDatePicker(false));

        // Setup apply button
        binding.applyFiltersButton.setOnClickListener(v -> applyFilters());

        // Update date range text
        updateDateRangeText();
    }

    private void setupSpinners() {
        // Device Name autocomplete (din device-urile userului)
        db.collection("users").document(mAuth.getCurrentUser().getUid())
            .collection("devices")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<String> deviceNames = new ArrayList<>();
                deviceNames.add(getString(R.string.all_devices));
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    String name = doc.getString("customName");
                    if (name != null && !deviceNames.contains(name)) {
                        deviceNames.add(name);
                    }
                }
                ArrayAdapter<String> deviceNameAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, deviceNames);
                binding.deviceNameSpinner.setAdapter(deviceNameAdapter);
            });

        // Device type dropdown (doar PIR, GAS etc)
        List<String> deviceTypes = new ArrayList<>();
        deviceTypes.add(getString(R.string.all_types));
        deviceTypes.add("PIR");
        deviceTypes.add("GAS");
        ArrayAdapter<String> deviceTypeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, deviceTypes);
        binding.deviceTypeSpinner.setAdapter(deviceTypeAdapter);
    }

    private void toggleFilterOptions() {
        if (binding.filterOptionsContainer.getVisibility() == View.VISIBLE) {
            hideFilterOptions();
        } else {
            showFilterOptions();
        }
    }

    private void showFilterOptions() {
        binding.filterOptionsContainer.setVisibility(View.VISIBLE);
        // Update spinners with current data
        setupSpinners();
        updateDateRangeText();
    }

    private void hideFilterOptions() {
        binding.filterOptionsContainer.setVisibility(View.GONE);
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth, 
                                  isStartDate ? 0 : 23, 
                                  isStartDate ? 0 : 59, 
                                  isStartDate ? 0 : 59);
                    
                    if (isStartDate) {
                        filterStartDate = selectedCal.getTime();
                        binding.startDateButton.setText(dateFormat.format(filterStartDate));
                    } else {
                        filterEndDate = selectedCal.getTime();
                        binding.endDateButton.setText(dateFormat.format(filterEndDate));
                    }
                    
                    updateDateRangeText();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateRangeText() {
        if (filterStartDate != null && filterEndDate != null) {
            String range = dateFormat.format(filterStartDate) + " - " + dateFormat.format(filterEndDate);
            binding.dateRangeText.setText(range);
        } else if (filterStartDate != null) {
            binding.dateRangeText.setText("From " + dateFormat.format(filterStartDate));
        } else if (filterEndDate != null) {
            binding.dateRangeText.setText("Until " + dateFormat.format(filterEndDate));
        } else {
            binding.dateRangeText.setText(getString(R.string.no_date_range_selected));
        }
    }

    private void applyFilters() {
        // Device name filter
        String selectedDeviceName = binding.deviceNameSpinner.getText().toString();
        if (!selectedDeviceName.equals(getString(R.string.all_devices))) {
            adapter.filterByDeviceName(selectedDeviceName);
        } else {
            adapter.filterByDeviceName("");
        }

        // Device type filter
        String selectedDeviceType = binding.deviceTypeSpinner.getText().toString();
        if (!selectedDeviceType.equals(getString(R.string.all_types))) {
            adapter.filterByDeviceType(selectedDeviceType);
        } else {
            adapter.filterByDeviceType("");
        }

        // Date range filter cu validare
        if (filterStartDate != null && filterEndDate != null && filterStartDate.after(filterEndDate)) {
            Toast.makeText(requireContext(), "Data de început nu poate fi după data de sfârșit!", Toast.LENGTH_LONG).show();
            return;
        }
        adapter.filterByDateRange(filterStartDate, filterEndDate);

        hideFilterOptions();
        updateEmptyState();
        Toast.makeText(requireContext(), "Filters applied", Toast.LENGTH_SHORT).show();
    }

    private void loadAlerts() {
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "User not authenticated");
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "[ALERTS] Start loading alerts for user: " + userId);
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    List<Alert> allAlerts = new ArrayList<>();
                    if (userDoc.exists() && userDoc.contains("userDevices")) {
                        Map<String, Object> userDevices = (Map<String, Object>) userDoc.get("userDevices");
                        Log.d(TAG, "[ALERTS] Found userDevices map with " + userDevices.size() + " devices");
                        int[] remaining = {userDevices.size()};
                        for (Map.Entry<String, Object> entry : userDevices.entrySet()) {
                            Map<String, Object> deviceData = (Map<String, Object>) entry.getValue();
                            String deviceId = (String) deviceData.get("deviceId");
                            com.google.firebase.Timestamp addedAt = (com.google.firebase.Timestamp) deviceData.get("addedAt");
                            String customName = (String) deviceData.get("customName");
                            Log.d(TAG, "[ALERTS] Device: " + deviceId + ", addedAt: " + addedAt);
                            if (deviceId == null || addedAt == null) {
                                remaining[0]--;
                                if (remaining[0] == 0) {
                                    Log.d(TAG, "[ALERTS] All device queries finished (null deviceId/addedAt). Alerts found: " + allAlerts.size());
                                    allAlerts.sort((a, b) -> {
                                        Date timestampA = a.getTimestamp();
                                        Date timestampB = b.getTimestamp();
                                        if (timestampA == null && timestampB == null) return 0;
                                        if (timestampA == null) return 1; // null timestamps go to end
                                        if (timestampB == null) return -1;
                                        return timestampB.compareTo(timestampA); // newest first
                                    });
                                    adapter.setAlerts(allAlerts);
                                    updateEmptyState();
                                }
                                continue;
                            }
                            db.collection("mqtt_messages")
                                    .whereEqualTo("type", "alerts")
                                    .whereEqualTo("deviceId", deviceId)
                                    .whereGreaterThan("timestamp", addedAt)
                                    .get()
                                    .addOnSuccessListener(alertSnapshots -> {
                                        Log.d(TAG, "[ALERTS] Found " + alertSnapshots.size() + " alerts for device " + deviceId);
                                        for (QueryDocumentSnapshot alertDoc : alertSnapshots) {
                                            try {
                                                Alert alert = alertDoc.toObject(Alert.class);
                                                if (alert != null) {
                                                    alert.setId(alertDoc.getId());
                                                    alert.setDeviceName(customName);
                                                    allAlerts.add(alert);
                                                    Log.d(TAG, "[ALERTS] Added alert: " + alert.getId() + " for device: " + deviceId);
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, "[ALERTS] Error parsing alert document", e);
                                            }
                                        }
                                        remaining[0]--;
                                        if (remaining[0] == 0) {
                                            Log.d(TAG, "[ALERTS] All device queries finished. Alerts found: " + allAlerts.size());
                                            allAlerts.sort((a, b) -> {
                                                Date timestampA = a.getTimestamp();
                                                Date timestampB = b.getTimestamp();
                                                if (timestampA == null && timestampB == null) return 0;
                                                if (timestampA == null) return 1; // null timestamps go to end
                                                if (timestampB == null) return -1;
                                                return timestampB.compareTo(timestampA); // newest first
                                            });
                                            adapter.setAlerts(allAlerts);
                                            updateEmptyState();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "[ALERTS] Error loading alerts for device " + deviceId, e);
                                        remaining[0]--;
                                        if (remaining[0] == 0) {
                                            Log.d(TAG, "[ALERTS] All device queries finished (with errors). Alerts found: " + allAlerts.size());
                                            allAlerts.sort((a, b) -> {
                                                Date timestampA = a.getTimestamp();
                                                Date timestampB = b.getTimestamp();
                                                if (timestampA == null && timestampB == null) return 0;
                                                if (timestampA == null) return 1; // null timestamps go to end
                                                if (timestampB == null) return -1;
                                                return timestampB.compareTo(timestampA); // newest first
                                            });
                                            adapter.setAlerts(allAlerts);
                                            updateEmptyState();
                                        }
                                    });
                        }
                    } else {
                        Log.d(TAG, "[ALERTS] No devices found for user.");
                        adapter.setAlerts(new ArrayList<>());
                        updateEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "[ALERTS] Error loading user devices", e);
                    Toast.makeText(requireContext(), "Error loading alerts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEmptyState() {
        if (binding == null) return;
        if (adapter.getItemCount() == 0) {
            binding.textEmptyAlerts.setVisibility(View.VISIBLE);
            binding.recyclerAlerts.setVisibility(View.GONE);
        } else {
            binding.textEmptyAlerts.setVisibility(View.GONE);
            binding.recyclerAlerts.setVisibility(View.VISIBLE);
        }
    }

    public void addTestAlert() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        Alert testAlert = new Alert(
                null,
                "TEST001",
                "Test Camera",
                "Camera",
                "Motion detected in zone 1",
                new Date(),
                "medium"
        );

        db.collection("users").document(userId)
                .collection("alerts")
                .add(testAlert)
                .addOnSuccessListener(documentReference -> {
                    testAlert.setId(documentReference.getId());
                    adapter.addAlert(testAlert);
                    updateEmptyState();
                    
                    // Show notification
                    NotificationService.showAlertNotification(requireContext(), testAlert);
                    
                    Toast.makeText(requireContext(), "Test alert added", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding test alert", e);
                    Toast.makeText(requireContext(), 
                            "Error adding test alert: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
