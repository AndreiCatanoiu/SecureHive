package licenta.andrei.catanoiu.securehive.activityes;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;

import licenta.andrei.catanoiu.securehive.R;

public class DeviceInfoActivity extends AppCompatActivity {

    private static final String TAG = "DeviceInfoActivity";
    private static final String BROKER_URL = "tcp://www.andreicatanoiu.ro:1883";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final String BASE_TOPIC = "senzor/licenta/andrei/catanoiu/";
    private static final String TOPIC_SUFFIX = "/alerts";

    private TextView deviceTitleTextView;
    private TextView deviceIdTextView;
    private TextView deviceIpTextView;
    private TextView deviceStatusTextView;
    private TextView mqttMessageTextView;
    private Button refreshButton;

    private MqttClient mqttClient;
    private String deviceId;
    private String mqttTopic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_device_info);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();

        getDeviceDataFromIntent();


        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reconnectMqtt();
            }
        });

        connectToMqtt();
    }

    private void initializeViews() {
        deviceTitleTextView = findViewById(R.id.device_title);
        deviceIdTextView = findViewById(R.id.device_id);
        deviceIpTextView = findViewById(R.id.device_ip);
        deviceStatusTextView = findViewById(R.id.device_status);
        mqttMessageTextView = findViewById(R.id.mqtt_message);
        refreshButton = findViewById(R.id.btn_refresh);
    }

    private void getDeviceDataFromIntent() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            deviceId = extras.getString("DEVICE_ID", "");
            String deviceName = extras.getString("DEVICE_NAME", "");
            String deviceIp = extras.getString("DEVICE_IP", "");
            boolean isActive = extras.getBoolean("DEVICE_STATUS", false);

            mqttTopic = BASE_TOPIC + "U2Vuem9yR2F6LTE=" + TOPIC_SUFFIX;

            deviceTitleTextView.setText(deviceName);
            deviceIdTextView.setText(deviceId);
            deviceIpTextView.setText(deviceIp);
            deviceStatusTextView.setText(isActive ? "Activ" : "Inactiv");
            mqttMessageTextView.setText("Waiting for messages...");
        }
    }

    private void connectToMqtt() {
        try {
            String clientId = "AndroidClient-" + UUID.randomUUID().toString();

            mqttClient = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(USERNAME);
            options.setPassword(PASSWORD.toCharArray());
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "MQTT connection lost", cause);
                    runOnUiThread(() -> {
                        mqttMessageTextView.setText("Connection lost. Click refresh.");
                        Toast.makeText(DeviceInfoActivity.this, "MQTT connection lost", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    Log.d(TAG, "Message received: " + payload);
                    runOnUiThread(() -> {
                        mqttMessageTextView.setText(payload);
                    });
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            mqttClient.connect(options);

            mqttClient.subscribe(mqttTopic, 0);

            Log.d(TAG, "Successfully connected to MQTT and subscribed to: " + mqttTopic);
            runOnUiThread(() -> {
                Toast.makeText(DeviceInfoActivity.this, "Connected to MQTT", Toast.LENGTH_SHORT).show();
            });

        } catch (MqttException e) {
            Log.e(TAG, "MQTT connection error", e);
            runOnUiThread(() -> {
                mqttMessageTextView.setText("Connection error:" + e.getMessage());
                Toast.makeText(DeviceInfoActivity.this, "MQTT connection error", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void disconnectMqtt() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
                Log.d(TAG, "Disconnected from MQTT");
            } catch (MqttException e) {
                Log.e(TAG, "MQTT disconnect error", e);
            }
        }
    }

    private void reconnectMqtt() {
        disconnectMqtt();
        mqttMessageTextView.setText("Reconnecting...");
        connectToMqtt();
    }

    @Override
    protected void onDestroy() {
        disconnectMqtt();
        super.onDestroy();
    }
}