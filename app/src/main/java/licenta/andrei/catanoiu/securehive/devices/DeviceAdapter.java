package licenta.andrei.catanoiu.securehive.devices;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.utils.DeviceIdDecoder;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private static final String TAG = "DeviceAdapter";
    private final ArrayList<UserDevice> userDevices;
    private final Map<String, Device> deviceStatuses;
    private final Context context;
    private final DeviceAdapterListener listener;
    private final FirebaseFirestore db;

    public interface DeviceAdapterListener {
        void onDeleteClick(UserDevice userDevice, int position);
        void onDeviceStatusError(String deviceId, String error);
    }

    public DeviceAdapter(Context context, ArrayList<UserDevice> userDevices, DeviceAdapterListener listener) {
        this.context = context;
        this.userDevices = userDevices;
        this.deviceStatuses = new HashMap<>();
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.device_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserDevice userDevice = userDevices.get(position);
        holder.deviceName.setText(userDevice.getCustomName());
        
        // Setăm imaginea corectă în funcție de tipul dispozitivului
        DeviceIdDecoder.DeviceType deviceType = DeviceIdDecoder.getDeviceType(userDevice.getDeviceId());
        switch (deviceType) {
            case PIR:
                holder.deviceIcon.setImageResource(R.drawable.senzorpir);
                break;
            case GAS:
                holder.deviceIcon.setImageResource(R.drawable.senzorgaz);
                break;
            default:
                holder.deviceIcon.setImageResource(R.drawable.ic_device);
                break;
        }
        
        // Încărcăm statusul device-ului din Firestore dacă nu îl avem deja
        if (!deviceStatuses.containsKey(userDevice.getDeviceId())) {
            holder.deviceStatus.setText("Loading...");
            holder.deviceStatus.setTextColor(context.getColor(R.color.text_secondary));
            
            loadDeviceStatus(userDevice.getDeviceId(), holder);
        } else {
            updateDeviceStatusUI(holder, deviceStatuses.get(userDevice.getDeviceId()));
        }

        // Click listener pentru butonul de ștergere
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(userDevice, holder.getAdapterPosition());
            }
        });
    }

    private void loadDeviceStatus(String deviceId, ViewHolder holder) {
        db.collection("devices").document(deviceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Device device = documentSnapshot.toObject(Device.class);
                    if (device != null) {
                        deviceStatuses.put(deviceId, device);
                        updateDeviceStatusUI(holder, device);
                    } else {
                        if (listener != null) {
                            listener.onDeviceStatusError(deviceId, "Device not found");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onDeviceStatusError(deviceId, e.getMessage());
                    }
                });
    }

    private void updateDeviceStatusUI(ViewHolder holder, Device device) {
        if (device == null) return;

        Device.DeviceStatus status = device.getStatus();
        String statusText;
        int statusColor;
        
        switch (status) {
            case ONLINE:
                statusText = "Online";
                statusColor = context.getColor(R.color.online);
                break;
            case OFFLINE:
                statusText = "Offline";
                statusColor = context.getColor(R.color.offline);
                break;
            case MAINTENANCE:
                statusText = "În mentenanță";
                statusColor = context.getColor(R.color.maintenance);
                break;
            default:
                statusText = "Necunoscut";
                statusColor = context.getColor(R.color.text_secondary);
                break;
        }
        
        holder.deviceStatus.setText(statusText);
        holder.deviceStatus.setTextColor(statusColor);
    }

    @Override
    public int getItemCount() {
        return userDevices != null ? userDevices.size() : 0;
    }

    public void updateDevices(ArrayList<UserDevice> newUserDevices) {
        this.userDevices.clear();
        this.userDevices.addAll(newUserDevices);
        this.deviceStatuses.clear(); // Resetăm cache-ul de statusuri
        notifyDataSetChanged();
    }

    public void updateDeviceStatus(String deviceId, Device.DeviceStatus newStatus) {
        Device device = deviceStatuses.get(deviceId);
        if (device != null) {
            device.setStatus(newStatus);
            notifyDataSetChanged();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView deviceName;
        final TextView deviceStatus;
        final ImageButton deleteButton;
        final ImageView deviceIcon;

        ViewHolder(View view) {
            super(view);
            deviceName = view.findViewById(R.id.deviceName);
            deviceStatus = view.findViewById(R.id.deviceStatus);
            deleteButton = view.findViewById(R.id.deleteButton);
            deviceIcon = view.findViewById(R.id.deviceIcon);
        }
    }
}