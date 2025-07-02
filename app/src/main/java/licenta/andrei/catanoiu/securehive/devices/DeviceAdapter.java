package licenta.andrei.catanoiu.securehive.devices;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.AlertDialog;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.activities.DeviceDetailsActivity;
import licenta.andrei.catanoiu.securehive.utils.DeviceIdDecoder;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private static final String TAG = "DeviceAdapter";
    private final ArrayList<UserDevice> userDevices;
    private final Map<String, Device> deviceStatuses = new HashMap<>();
    private final Context context;
    private final DeviceAdapterListener listener;
    private final FirebaseFirestore db;
    private final Map<String, ValueEventListener> deviceStatusListeners = new HashMap<>();
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

    public interface DeviceAdapterListener {
        void onDeleteClick(UserDevice userDevice, int position);
        void onDeviceStatusError(String deviceId, String error);
    }

    public DeviceAdapter(Context context, ArrayList<UserDevice> userDevices, DeviceAdapterListener listener) {
        this.context = context;
        this.userDevices = userDevices;
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
        holder.deviceType.setText(DeviceIdDecoder.getDeviceType(userDevice.getDeviceId()).name());

        DeviceIdDecoder.DeviceType deviceType = DeviceIdDecoder.getDeviceType(userDevice.getDeviceId());
        switch (deviceType) {
            case PIR:
                holder.deviceIcon.setImageResource(R.drawable.pir);
                break;
            case GAS:
                holder.deviceIcon.setImageResource(R.drawable.gaz);
                break;
            default:
                holder.deviceIcon.setImageResource(R.drawable.ic_device);
                break;
        }

        String deviceId = userDevice.getDeviceId();
        if (!deviceStatusListeners.containsKey(deviceId)) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Device device = snapshot.getValue(Device.class);
                    if (device != null) {
                        deviceStatuses.put(deviceId, device);
                        updateDeviceStatusUI(holder, device);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            dbRef.child("devices").child(deviceId).addValueEventListener(listener);
            deviceStatusListeners.put(deviceId, listener);
        } else {
            Device device = deviceStatuses.get(deviceId);
            updateDeviceStatusUI(holder, device);
        }

        holder.deleteDeviceButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Șterge dispozitivul");
            builder.setMessage("Ești sigur că vrei să ștergi dispozitivul \"" + userDevice.getCustomName() + "\"?");
            builder.setPositiveButton("Șterge", (dialog, which) -> {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String deviceKey = userDevice.getDeviceId();

                DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                db.child("users").child(userId).child("userDevices").child(deviceKey)
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {

                        int currentPosition = userDevices.indexOf(userDevice);
                        if (currentPosition != -1) {
                            userDevices.remove(currentPosition);
                            notifyItemRemoved(currentPosition);
                            if (currentPosition < userDevices.size()) {
                                notifyItemRangeChanged(currentPosition, userDevices.size() - currentPosition);
                            }
                        }
                        Toast.makeText(context, "Dispozitiv șters cu succes", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Eroare la ștergere: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            });
            builder.setNegativeButton("Anulează", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), DeviceDetailsActivity.class);
            intent.putExtra("device", userDevice);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        for (Map.Entry<String, ValueEventListener> entry : deviceStatusListeners.entrySet()) {
            dbRef.child("devices").child(entry.getKey()).removeEventListener(entry.getValue());
        }
        deviceStatusListeners.clear();
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
        this.deviceStatuses.clear();
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
        final ImageView deviceIcon;
        final TextView deviceType;
        final ImageButton deleteDeviceButton;

        ViewHolder(View view) {
            super(view);
            deviceName = view.findViewById(R.id.deviceName);
            deviceStatus = view.findViewById(R.id.deviceStatus);
            deviceIcon = view.findViewById(R.id.deviceIcon);
            deviceType = view.findViewById(R.id.deviceType);
            deleteDeviceButton = view.findViewById(R.id.deleteDeviceButton);
        }
    }
}