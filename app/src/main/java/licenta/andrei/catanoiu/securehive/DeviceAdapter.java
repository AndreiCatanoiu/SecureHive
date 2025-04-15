package licenta.andrei.catanoiu.securehive;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class DeviceAdapter extends ArrayAdapter<Device> {

    public DeviceAdapter(Context context, ArrayList<Device> devices) {
        super(context, android.R.layout.simple_list_item_1, devices);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    android.R.layout.simple_list_item_1, parent, false);
        }

        Device device = getItem(position);
        TextView textView = convertView.findViewById(android.R.id.text1);
        if (device != null) {
            textView.setText(device.getName());
        }

        return convertView;
    }
}