package licenta.andrei.catanoiu.securehive.devices;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import licenta.andrei.catanoiu.securehive.R;

public class DeviceAdapter extends ArrayAdapter<Device> {

    private Context mContext;
    private ArrayList<Device> mDevices;
    private DeviceAdapterListener mListener;

    public interface DeviceAdapterListener {
        void onDeleteClick(Device device, int position);
    }

    public DeviceAdapter(Context context, ArrayList<Device> devices, DeviceAdapterListener listener) {
        super(context, 0, devices);
        mContext = context;
        mDevices = devices;
        mListener = listener;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(mContext).inflate(R.layout.device_item, parent, false);
        }

        Device currentDevice = mDevices.get(position);

        TextView deviceName = listItem.findViewById(R.id.device_name);
        TextView deviceIp = listItem.findViewById(R.id.device_ip);
        TextView deviceStatus = listItem.findViewById(R.id.device_status);
        ImageView deleteIcon = listItem.findViewById(R.id.ic_delete);

        deviceName.setText(currentDevice.getName());
        deviceIp.setText(currentDevice.getIpAddress());

        if (currentDevice.isActive()) {
            deviceStatus.setText("Connected");
            deviceStatus.setTextColor(mContext.getResources().getColor(R.color.green));
        } else {
            deviceStatus.setText("Disconnected");
            deviceStatus.setTextColor(mContext.getResources().getColor(R.color.red));
        }

        deleteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onDeleteClick(currentDevice, position);
                }
            }
        });

        return listItem;
    }
}