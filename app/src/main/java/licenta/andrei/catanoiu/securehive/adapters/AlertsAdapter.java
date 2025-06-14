package licenta.andrei.catanoiu.securehive.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.models.Alert;

public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.AlertViewHolder> {
    
    private List<Alert> allAlerts;
    private List<Alert> filteredAlerts;
    private Context context;
    private SimpleDateFormat dateFormat;
    
    // Filter criteria
    private String filterDeviceId = "";
    private String filterDeviceType = "";
    private String filterSeverity = "";
    private Date filterStartDate = null;
    private Date filterEndDate = null;
    private String filterDeviceName = "";

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
        this.allAlerts.add(0, alert); // Add to beginning
        applyFilters();
    }

    public void clearAlerts() {
        this.allAlerts.clear();
        this.filteredAlerts.clear();
        notifyDataSetChanged();
    }

    // Filter methods
    public void filterByDeviceId(String deviceId) {
        this.filterDeviceId = deviceId != null ? deviceId.toLowerCase() : "";
        applyFilters();
    }

    public void filterByDeviceType(String deviceType) {
        this.filterDeviceType = deviceType != null ? deviceType.toLowerCase() : "";
        applyFilters();
    }

    public void filterBySeverity(String severity) {
        this.filterSeverity = severity != null ? severity.toLowerCase() : "";
        applyFilters();
    }

    public void filterByDateRange(Date startDate, Date endDate) {
        this.filterStartDate = startDate;
        this.filterEndDate = endDate;
        applyFilters();
    }

    public void filterByDeviceName(String deviceName) {
        this.filterDeviceName = deviceName != null ? deviceName.toLowerCase() : "";
        applyFilters();
    }

    public void clearFilters() {
        this.filterDeviceId = "";
        this.filterDeviceType = "";
        this.filterSeverity = "";
        this.filterStartDate = null;
        this.filterEndDate = null;
        applyFilters();
    }

    private void applyFilters() {
        filteredAlerts.clear();
        
        for (Alert alert : allAlerts) {
            // Add null checks for all alert fields
            String deviceId = alert.getDeviceId() != null ? alert.getDeviceId().toLowerCase() : "";
            String deviceName = alert.getDeviceName() != null ? alert.getDeviceName().toLowerCase() : "";
            String deviceType = alert.getDeviceType() != null ? alert.getDeviceType().toLowerCase() : "";
            String severity = alert.getSeverity() != null ? alert.getSeverity().toLowerCase() : "";
            Date timestamp = alert.getTimestamp();
            
            boolean matchesDeviceId = filterDeviceId.isEmpty() || 
                    deviceId.contains(filterDeviceId) ||
                    deviceName.contains(filterDeviceId);
            
            boolean matchesDeviceType = filterDeviceType.isEmpty() || 
                    deviceType.contains(filterDeviceType);
            
            boolean matchesSeverity = filterSeverity.isEmpty() || 
                    severity.contains(filterSeverity);
            
            boolean matchesDeviceName = filterDeviceName.isEmpty() || 
                    deviceName.contains(filterDeviceName);
            
            boolean matchesDateRange = true;
            if (timestamp != null) {
                if (filterStartDate != null && filterEndDate != null) {
                    matchesDateRange = !timestamp.before(filterStartDate) && 
                                     !timestamp.after(filterEndDate);
                } else if (filterStartDate != null) {
                    matchesDateRange = !timestamp.before(filterStartDate);
                } else if (filterEndDate != null) {
                    matchesDateRange = !timestamp.after(filterEndDate);
                }
            } else {
                // If timestamp is null, don't match any date filters
                matchesDateRange = filterStartDate == null && filterEndDate == null;
            }
            
            if (matchesDeviceId && matchesDeviceType && matchesSeverity && matchesDateRange && matchesDeviceName) {
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
            String deviceType = alert.getDeviceType();
            if (deviceType != null && !deviceType.isEmpty() && !types.contains(deviceType)) {
                types.add(deviceType);
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
        private TextView messageText;
        private TextView timestampText;
        private TextView severityText;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceNameText = itemView.findViewById(R.id.textDeviceName);
            deviceTypeText = itemView.findViewById(R.id.textDeviceType);
            messageText = itemView.findViewById(R.id.textMessage);
            timestampText = itemView.findViewById(R.id.textTimestamp);
            severityText = itemView.findViewById(R.id.textSeverity);
        }

        public void bind(Alert alert) {
            // Add null checks to prevent crashes
            deviceNameText.setText(alert.getDeviceName() != null ? alert.getDeviceName() : "Unknown Device");
            deviceTypeText.setText(alert.getDeviceType() != null ? alert.getDeviceType() : "Unknown Type");
            messageText.setText(alert.getMessage() != null ? alert.getMessage() : "No message");
            
            if (alert.getTimestamp() != null) {
                timestampText.setText(dateFormat.format(alert.getTimestamp()));
            } else {
                timestampText.setText("Unknown time");
            }
            
            // Set severity color with null check
            String severity = alert.getSeverity();
            if (severity != null) {
                switch (severity.toLowerCase()) {
                    case "high":
                        severityText.setTextColor(context.getResources().getColor(R.color.error));
                        break;
                    case "medium":
                        severityText.setTextColor(context.getResources().getColor(R.color.warning));
                        break;
                    default:
                        severityText.setTextColor(context.getResources().getColor(R.color.info));
                        break;
                }
                severityText.setText(severity.toUpperCase());
            } else {
                severityText.setTextColor(context.getResources().getColor(R.color.info));
                severityText.setText("UNKNOWN");
            }
            
            // Set background based on read status
            if (alert.isRead()) {
                itemView.setAlpha(0.7f);
            } else {
                itemView.setAlpha(1.0f);
            }
        }
    }
} 