package licenta.andrei.catanoiu.securehive.fragments.alerts;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.adapters.AlertsAdapter;
import licenta.andrei.catanoiu.securehive.databinding.FragmentAlertsBinding;
import licenta.andrei.catanoiu.securehive.models.Alert;
import licenta.andrei.catanoiu.securehive.utils.NotificationService;

public class AlertsFragment extends Fragment {

    private static final String TAG = "AlertsFragment";
    private FragmentAlertsBinding binding;
    private AlertsAdapter adapter;
    private DatabaseReference db;
    private FirebaseAuth mAuth;
    private SimpleDateFormat dateFormat;
    private Date filterStartDate = null;
    private Date filterEndDate = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

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
        loadAlerts();
    }

    private void setupRecyclerView() {
        adapter = new AlertsAdapter(requireContext());
        binding.recyclerAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerAlerts.setAdapter(adapter);
    }

    private void setupFilterListeners() {
        binding.filterButton.setOnClickListener(v -> toggleFilterOptions());

        binding.clearFiltersButton.setOnClickListener(v -> {
            filterStartDate = null;
            filterEndDate = null;
            adapter.clearFilters();
            hideFilterOptions();
            updateEmptyState();
            Toast.makeText(requireContext(), "Filters cleared", Toast.LENGTH_SHORT).show();
        });

        setupFilterOptions();
    }

    private void setupFilterOptions() {
        setupSpinners();

        binding.startDateButton.setOnClickListener(v -> showDatePicker(true));
        binding.endDateButton.setOnClickListener(v -> showDatePicker(false));

        binding.applyFiltersButton.setOnClickListener(v -> applyFilters());

        updateDateRangeText();
    }

    private void setupSpinners() {
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
                    Date selectedDate = selectedCal.getTime();
                    if (isStartDate) {
                        filterStartDate = selectedDate;
                        binding.startDateButton.setText(dateFormat.format(filterStartDate));
                    } else {
                        filterEndDate = selectedDate;
                        binding.endDateButton.setText(dateFormat.format(filterEndDate));
                    }
                    updateDateRangeText();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        if (isStartDate && filterEndDate != null) {
            datePickerDialog.getDatePicker().setMaxDate(filterEndDate.getTime());
        }
        if (!isStartDate && filterStartDate != null) {
            datePickerDialog.getDatePicker().setMinDate(filterStartDate.getTime());
        }
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
        String selectedDeviceType = binding.deviceTypeSpinner.getText().toString();
        if (!selectedDeviceType.equals(getString(R.string.all_types))) {
            adapter.filterByDeviceType(selectedDeviceType);
        } else {
            adapter.filterByDeviceType("");
        }

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
        db.child("users").child(userId).child("userDevices").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userDevicesSnapshot) {
                List<Alert> allAlerts = new ArrayList<>();
                if (userDevicesSnapshot.exists()) {
                    int totalDevices = (int) userDevicesSnapshot.getChildrenCount();
                    if (totalDevices == 0) {
                        adapter.setAlerts(new ArrayList<>());
                        updateEmptyState();
                        return;
                    }
                    final int[] remaining = {totalDevices};
                    for (DataSnapshot deviceSnapshot : userDevicesSnapshot.getChildren()) {
                        String deviceId = deviceSnapshot.child("deviceId").getValue(String.class);
                        Long addedAt = deviceSnapshot.child("addedAt").getValue(Long.class);
                        String customName = deviceSnapshot.child("customName").getValue(String.class);
                        if (deviceId == null || addedAt == null) {
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                adapter.setAlerts(allAlerts);
                                updateEmptyState();
                            }
                            continue;
                        }
                        db.child("mqtt_messages").orderByChild("deviceId").equalTo(deviceId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot alertsSnapshot) {
                                        for (DataSnapshot alertDoc : alertsSnapshot.getChildren()) {
                                            try {
                                                String id = alertDoc.getKey();
                                                String alertDeviceId = alertDoc.child("deviceId").getValue(String.class);
                                                String deviceName = customName;
                                                String deviceType = alertDoc.child("deviceType").getValue(String.class);
                                                String message = alertDoc.child("payload").getValue(String.class);
                                                Long timestampLong = alertDoc.child("timestamp").getValue(Long.class);
                                                String severity = alertDoc.child("severity").getValue(String.class);
                                                String type = alertDoc.child("type").getValue(String.class);

                                                boolean deviceIdMatch = deviceId != null && alertDeviceId != null &&
                                                    deviceId.length() >= 8 && alertDeviceId.length() >= 8 &&
                                                    deviceId.substring(0, 8).equals(alertDeviceId.substring(0, 8));
                                                if ("alerts".equals(type) && deviceIdMatch && timestampLong != null && timestampLong >= addedAt) {
                                                    Date timestamp = new Date(timestampLong);
                                                    Alert alert = new Alert(id, alertDeviceId, deviceName, deviceType, message, timestamp, severity);
                                                    allAlerts.add(alert);
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, "[ALERTS] Error parsing alert document", e);
                                            }
                                        }
                                        remaining[0]--;
                                        if (remaining[0] == 0) {
                                            allAlerts.sort((a, b) -> {
                                                Date timestampA = a.getTimestamp();
                                                Date timestampB = b.getTimestamp();
                                                if (timestampA == null && timestampB == null) return 0;
                                                if (timestampA == null) return 1;
                                                if (timestampB == null) return -1;
                                                return timestampB.compareTo(timestampA);
                                            });
                                            adapter.setAlerts(allAlerts);
                                            updateEmptyState();
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "[ALERTS] Error loading alerts for device " + deviceId, error.toException());
                                        remaining[0]--;
                                        if (remaining[0] == 0) {
                                            adapter.setAlerts(allAlerts);
                                            updateEmptyState();
                                        }
                                    }
                                });
                    }
                } else {
                    adapter.setAlerts(new ArrayList<>());
                    updateEmptyState();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "[ALERTS] Error loading user devices", error.toException());
                Toast.makeText(requireContext(), "Error loading alerts: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
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

        db.child("users").child(userId).child("alerts").push().setValue(testAlert)
                .addOnSuccessListener(aVoid -> {
                    adapter.addAlert(testAlert);
                    updateEmptyState();
                    NotificationService.showAlertNotification(requireContext(), testAlert);
                    Toast.makeText(requireContext(), "Test alert added", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding test alert", e);
                    Toast.makeText(requireContext(), "Error adding test alert: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
