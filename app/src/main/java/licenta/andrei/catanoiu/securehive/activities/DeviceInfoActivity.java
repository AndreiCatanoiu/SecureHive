package licenta.andrei.catanoiu.securehive.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import licenta.andrei.catanoiu.securehive.R;

public class DeviceInfoActivity extends AppCompatActivity {

    private static final String TAG = "DeviceInfoActivity";

    private TextView deviceTitleTextView;
    private TextView deviceIdTextView;
    private TextView deviceStatusTextView;

    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            int systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), systemBarInsets, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        initializeViews();
        getDeviceDataFromIntent();
    }

    private void initializeViews() {
        deviceTitleTextView = findViewById(R.id.device_title);
        deviceIdTextView = findViewById(R.id.device_id);
        deviceStatusTextView = findViewById(R.id.device_status);
    }

    private void getDeviceDataFromIntent() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            deviceId = extras.getString("DEVICE_ID", "");
            String deviceName = extras.getString("DEVICE_NAME", "");
            boolean isActive = extras.getBoolean("DEVICE_STATUS", false);

            deviceTitleTextView.setText(deviceName);
            deviceIdTextView.setText(deviceId);
            deviceStatusTextView.setText(isActive ? "Activ" : "Inactiv");
        }
    }

    private void refreshDeviceData() {
        // Aici vom implementa actualizarea datelor din Firebase
        Toast.makeText(this, "Se actualizează datele...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}