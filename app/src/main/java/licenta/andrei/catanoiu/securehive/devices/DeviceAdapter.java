package licenta.andrei.catanoiu.securehive.devices;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import licenta.andrei.catanoiu.securehive.R;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private ArrayList<Device> devices;
    private final Context context;
    private final DeviceAdapterListener listener;

    public interface DeviceAdapterListener {
        void onDeleteClick(Device device, int position);
    }

    public DeviceAdapter(Context context, ArrayList<Device> devices, DeviceAdapterListener listener) {
        this.context = context;
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.device_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Device device = devices.get(position);
        holder.deviceName.setText(device.getName());
        
        // Setare status și culoare
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

        // Click listener pentru butonul de ștergere
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(device, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices != null ? devices.size() : 0;
    }

    public void updateDevices(ArrayList<Device> newDevices) {
        this.devices = newDevices;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView deviceName;
        final TextView deviceStatus;
        final ImageButton deleteButton;

        ViewHolder(View view) {
            super(view);
            deviceName = view.findViewById(R.id.deviceName);
            deviceStatus = view.findViewById(R.id.deviceStatus);
            deleteButton = view.findViewById(R.id.deleteButton);
        }
    }
}