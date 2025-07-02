package licenta.andrei.catanoiu.securehive.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.activities.AlertDetailsActivity;
import licenta.andrei.catanoiu.securehive.models.Alert;
import licenta.andrei.catanoiu.securehive.utils.DeviceIdDecoder;

public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.AlertViewHolder> {
    
    private List<Alert> allAlerts;
    private List<Alert> filteredAlerts;
    private Context context;
    private SimpleDateFormat dateFormat;
    private String filterDeviceType = "";
    private Date filterStartDate = null;
    private Date filterEndDate = null;

    public AlertsAdapter(Context context) {
        this.context = context;
        this.allAlerts = new ArrayList<>();
        this.filteredAlerts = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        Alert alert = filteredAlerts.get(position);
        holder.bind(alert);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AlertDetailsActivity.class);
            intent.putExtra("alert", alert);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return filteredAlerts.size();
    }

    public void setAlerts(List<Alert> alerts) {
        this.allAlerts = new ArrayList<>(alerts);
        applyFilters();
    }

    public void addAlert(Alert alert) {
        this.allAlerts.add(0, alert);
        applyFilters();
    }

    public void clearAlerts() {
        this.allAlerts.clear();
        this.filteredAlerts.clear();
        notifyDataSetChanged();
    }

    public void filterByDeviceType(String deviceType) {
        this.filterDeviceType = deviceType != null ? deviceType.toLowerCase() : "";
        applyFilters();
    }

    public void filterByDateRange(Date startDate, Date endDate) {
        this.filterStartDate = startDate;
        this.filterEndDate = endDate;
        applyFilters();
    }

    public void clearFilters() {
        this.filterDeviceType = "";
        this.filterStartDate = null;
        this.filterEndDate = null;
        applyFilters();
    }

    private void applyFilters() {
        filteredAlerts.clear();
        for (Alert alert : allAlerts) {
            boolean matchesDeviceType = true;
            if (filterDeviceType != null && !filterDeviceType.isEmpty()) {
                String deviceTypeString = "Unknown";
                if (alert.getDeviceId() != null) {
                    try {
                        DeviceIdDecoder.DeviceType deviceType = DeviceIdDecoder.getDeviceType(alert.getDeviceId());
                        deviceTypeString = deviceType.name();
                    } catch (Exception e) {
                        deviceTypeString = "Unknown";
                    }
                }
                matchesDeviceType = deviceTypeString.equalsIgnoreCase(filterDeviceType);
            }

            boolean matchesDateRange = true;
            Date timestamp = alert.getTimestamp();
            if (timestamp != null) {
                if (filterStartDate != null && filterEndDate != null) {
                    matchesDateRange = !timestamp.before(filterStartDate) && !timestamp.after(filterEndDate);
                } else if (filterStartDate != null) {
                    matchesDateRange = !timestamp.before(filterStartDate);
                } else if (filterEndDate != null) {
                    matchesDateRange = !timestamp.after(filterEndDate);
                }
            } else {
                if (filterStartDate != null || filterEndDate != null) {
                    matchesDateRange = false;
                }
            }

            if (matchesDeviceType && matchesDateRange) {
                filteredAlerts.add(alert);
            }
        }
        notifyDataSetChanged();
    }

    public List<Alert> getFilteredAlerts() {
        return new ArrayList<>(filteredAlerts);
    }

    public List<String> getAvailableDeviceTypes() {
        List<String> types = new ArrayList<>();
        for (Alert alert : allAlerts) {
            if (alert.getDeviceId() != null) {
                try {
                    DeviceIdDecoder.DeviceType deviceType = DeviceIdDecoder.getDeviceType(alert.getDeviceId());
                    String typeName = deviceType.name();
                    if (!types.contains(typeName)) {
                        types.add(typeName);
                    }
                } catch (Exception e) {
                }
            }
        }
        return types;
    }

    public List<String> getAvailableDeviceIds() {
        List<String> ids = new ArrayList<>();
        for (Alert alert : allAlerts) {
            String deviceId = alert.getDeviceId();
            if (deviceId != null && !deviceId.isEmpty() && !ids.contains(deviceId)) {
                ids.add(deviceId);
            }
        }
        return ids;
    }

    class AlertViewHolder extends RecyclerView.ViewHolder {
        private TextView deviceNameText;
        private TextView deviceTypeText;
        private ImageView deviceTypeIconRight;
        private TextView messageText;
        private TextView timestampText;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceNameText = itemView.findViewById(R.id.textDeviceName);
            deviceTypeText = itemView.findViewById(R.id.textDeviceType);
            deviceTypeIconRight = itemView.findViewById(R.id.imageDeviceTypeRight);
            messageText = itemView.findViewById(R.id.textMessage);
            timestampText = itemView.findViewById(R.id.textTimestamp);
        }

        public void bind(Alert alert) {
            deviceNameText.setText(alert.getDeviceName() != null ? alert.getDeviceName() : "Unknown Device");

            String deviceTypeString = "Unknown";
            int iconRes = R.drawable.ic_device;
            if (alert.getDeviceId() != null) {
                try {
                    DeviceIdDecoder.DeviceType deviceType = DeviceIdDecoder.getDeviceType(alert.getDeviceId());
                    deviceTypeString = deviceType.name();
                    switch (deviceType) {
                        case PIR:
                            iconRes = R.drawable.pir;
                            break;
                        case GAS:
                            iconRes = R.drawable.gaz;
                            break;
                        default:
                            iconRes = R.drawable.ic_device;
                            break;
                    }
                } catch (Exception e) {
                    deviceTypeString = "Unknown";
                    iconRes = R.drawable.ic_device;
                }
            }
            deviceTypeText.setText("Type: " + deviceTypeString);
            if (deviceTypeIconRight != null) deviceTypeIconRight.setImageResource(iconRes);

            String messageText = "No message";
            if (alert.getMessage() != null && !alert.getMessage().isEmpty()) {
                messageText = alert.getMessage();
            }
            this.messageText.setText(messageText);
            
            if (alert.getTimestamp() != null) {
                timestampText.setText(dateFormat.format(alert.getTimestamp()));
            } else {
                timestampText.setText("Unknown time");
            }

            if (alert.isRead()) {
                itemView.setAlpha(0.7f);
            } else {
                itemView.setAlpha(1.0f);
            }
        }
    }
} 