package licenta.andrei.catanoiu.securehive.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttConnectionManager {
    private static final String TAG = "MqttConnectionManager";
    private static final String BROKER_URL = "tcp://andreicatanoiu.ro:1883";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    
    private MqttClient mqttClient;
    private boolean isConnected = false;
    private final Context context;
    private final Handler mainHandler;
    private ConnectionCallback connectionCallback;
    
    public interface ConnectionCallback {
        void onConnected();
        void onDisconnected(String reason);
        void onError(String error);
        void onMessageReceived(String topic, String message);
    }
    
    public MqttConnectionManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }
    
    public boolean isConnected() {
        return isConnected && mqttClient != null && mqttClient.isConnected();
    }
    
    public void connect() {
        if (isConnected()) {
            Log.d(TAG, "Already connected to MQTT");
            return;
        }
        
        if (!isNetworkAvailable()) {
            String error = "There is no internet connection";
            Log.e(TAG, error);
            notifyError(error);
            return;
        }
        
        new Thread(() -> {
            try {
                String clientId = "SecureHive_" + System.currentTimeMillis();
                Log.d(TAG, "Connecting to MQTT broker: " + BROKER_URL + " with clientId: " + clientId);
                
                mqttClient = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());
                
                MqttConnectOptions options = new MqttConnectOptions();
                options.setUserName(USERNAME);
                options.setPassword(PASSWORD.toCharArray());
                options.setCleanSession(true);
                options.setConnectionTimeout(30);
                options.setKeepAliveInterval(60);
                options.setAutomaticReconnect(true);
                options.setMaxInflight(10);
                
                Log.d(TAG, "MQTT Connect Options set - Username: " + USERNAME + ", Timeout: 30s, KeepAlive: 60s");
                
                mqttClient.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        Log.e(TAG, "MQTT connection lost", cause);
                        if (cause != null) {
                            Log.e(TAG, "Connection lost reason: " + cause.getMessage());
                            Log.e(TAG, "Connection lost cause class: " + cause.getClass().getSimpleName());
                            cause.printStackTrace();
                        }
                        isConnected = false;
                        String reason = cause != null ? cause.getMessage() : "Unknown reason";
                        notifyDisconnected(reason);

                        mainHandler.postDelayed(() -> {
                            if (!isConnected) {
                                Log.d(TAG, "Attempting auto-reconnect...");
                                connect();
                            }
                        }, 5000);
                    }
                    
                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        String payload = new String(message.getPayload());
                        Log.d(TAG, "Message received on topic " + topic + ": " + payload);
                        notifyMessageReceived(topic, payload);
                    }
                    
                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        Log.d(TAG, "Message delivery completed");
                    }
                });
                
                Log.d(TAG, "Attempting to connect to MQTT broker...");
                mqttClient.connect(options);
                isConnected = true;
                Log.d(TAG, "Successfully connected to MQTT broker");
                notifyConnected();
                
            } catch (MqttException e) {
                Log.e(TAG, "MQTT connection error", e);
                Log.e(TAG, "MQTT Reason Code: " + e.getReasonCode());
                Log.e(TAG, "MQTT Error Message: " + e.getMessage());
                Log.e(TAG, "MQTT Error Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "No cause"));
                e.printStackTrace();
                isConnected = false;
                String error = getErrorMessage(e);
                notifyError(error);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during MQTT connection", e);
                Log.e(TAG, "Exception class: " + e.getClass().getSimpleName());
                Log.e(TAG, "Exception message: " + e.getMessage());
                e.printStackTrace();
                isConnected = false;
                notifyError("Unexpected error: " + e.getMessage());
            }
        }).start();
    }
    
    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                isConnected = false;
                Log.d(TAG, "Disconnected from MQTT broker");
            } catch (MqttException e) {
                Log.e(TAG, "Error disconnecting from MQTT", e);
            }
        }
    }
    
    public void publish(String topic, String message, int qos) {
        if (!isConnected()) {
            String error = "Nu este conectat la MQTT";
            Log.e(TAG, error);
            notifyError(error);
            return;
        }
        
        new Thread(() -> {
            try {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                mqttMessage.setQos(qos);
                mqttMessage.setRetained(false);
                
                mqttClient.publish(topic, mqttMessage);
                Log.d(TAG, "Published message to " + topic + ": " + message);
                
            } catch (MqttException e) {
                Log.e(TAG, "Error publishing message", e);
                String error = "Eroare la trimiterea mesajului: " + e.getMessage();
                notifyError(error);
            }
        }).start();
    }
    
    public void subscribe(String topic, int qos) {
        if (!isConnected()) {
            String error = "Nu este conectat la MQTT";
            Log.e(TAG, error);
            notifyError(error);
            return;
        }
        try {
            mqttClient.subscribe(topic, qos);
            Log.d(TAG, "Subscribed to topic: " + topic);
        } catch (MqttException e) {
            Log.e(TAG, "Error subscribing to topic: " + topic, e);
            String error = "Eroare la abonarea la topic: " + e.getMessage();
            notifyError(error);
        }
    }

    public void unsubscribe(String topic) {
        if (!isConnected()) {
            String error = "Nu este conectat la MQTT";
            Log.e(TAG, error);
            notifyError(error);
            return;
        }
        try {
            mqttClient.unsubscribe(topic);
            Log.d(TAG, "Unsubscribed from topic: " + topic);
        } catch (MqttException e) {
            Log.e(TAG, "Error unsubscribing from topic: " + topic, e);
            String error = "Eroare la dezabonarea de la topic: " + e.getMessage();
            notifyError(error);
        }
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    private String getErrorMessage(MqttException e) {
        switch (e.getReasonCode()) {
            case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
                return "Autentificare eșuată. Verifică username/password.";
            case MqttException.REASON_CODE_NOT_AUTHORIZED:
                return "Nu ai autorizația necesară.";
            case MqttException.REASON_CODE_CLIENT_TIMEOUT:
                return "Timeout la conexiune.";
            default:
                return e.getMessage();
        }
    }
    
    private void notifyConnected() {
        if (connectionCallback != null) {
            mainHandler.post(() -> connectionCallback.onConnected());
        }
    }
    
    private void notifyDisconnected(String reason) {
        if (connectionCallback != null) {
            mainHandler.post(() -> connectionCallback.onDisconnected(reason));
        }
    }
    
    private void notifyError(String error) {
        if (connectionCallback != null) {
            mainHandler.post(() -> connectionCallback.onError(error));
        }
    }
    
    private void notifyMessageReceived(String topic, String message) {
        if (connectionCallback != null) {
            mainHandler.post(() -> connectionCallback.onMessageReceived(topic, message));
        }
    }
} 