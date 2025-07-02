package licenta.andrei.catanoiu.securehive.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.models.Alert;
import licenta.andrei.catanoiu.securehive.utils.DeviceIdDecoder;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AlertDetailsActivity extends AppCompatActivity {

    private static final String TAG = "AlertDetailsActivity";
    private ImageView alertIcon;
    private TextView alertTitle;
    private TextView deviceName;
    private TextView deviceId;
    private TextView deviceType;
    private TextView alertMessage;
    private TextView alertTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_details);

        initializeViews();
        
        Alert alert = getIntent().getParcelableExtra("alert");
        if (alert != null) {
            loadAlertInfo(alert);
        } else {
            Toast.makeText(this, "Error: Alert information not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        alertIcon = findViewById(R.id.alertIcon);
        alertTitle = findViewById(R.id.alertTitle);
        deviceName = findViewById(R.id.deviceName);
        deviceId = findViewById(R.id.deviceId);
        deviceType = findViewById(R.id.deviceType);
        alertMessage = findViewById(R.id.alertMessage);
        alertTimestamp = findViewById(R.id.alertTimestamp);
    }

    private void loadAlertInfo(Alert alert) {
        alertTitle.setText("Alert Details");

        deviceName.setText(alert.getDeviceName() != null ? alert.getDeviceName() : "Unknown Device");
        deviceId.setText("ID: " + (alert.getDeviceId() != null ? alert.getDeviceId() : "Unknown"));

        if (alert.getDeviceId() != null) {
            DeviceIdDecoder.DeviceType deviceTypeEnum = DeviceIdDecoder.getDeviceType(alert.getDeviceId());
            deviceType.setText("Type: " + deviceTypeEnum.name());

            switch (deviceTypeEnum) {
                case PIR:
                    alertIcon.setImageResource(R.drawable.pir);
                    break;
                case GAS:
                    alertIcon.setImageResource(R.drawable.gaz);
                    break;
                default:
                    alertIcon.setImageResource(R.drawable.ic_device);
                    break;
            }
        } else {
            deviceType.setText("Type: Unknown");
            alertIcon.setImageResource(R.drawable.ic_device);
        }

        alertMessage.setText(alert.getMessage() != null ? alert.getMessage() : "No message available");

        if (alert.getTimestamp() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            alertTimestamp.setText("Time: " + dateFormat.format(alert.getTimestamp()));
        } else {
            alertTimestamp.setText("Time: Unknown");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
} 