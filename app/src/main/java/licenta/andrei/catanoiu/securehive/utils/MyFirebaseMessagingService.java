package licenta.andrei.catanoiu.securehive.utils;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.navigation.NavDeepLinkBuilder;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.activities.AlertDetailsActivity;
import licenta.andrei.catanoiu.securehive.models.Alert;

import java.util.Date;
import java.util.Random;

import android.Manifest;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d("FCM", "onMessageReceived: primit mesaj FCM");
        Log.d("FCM", "Data: " + remoteMessage.getData());
        if (remoteMessage.getNotification() != null) {
            Log.d("FCM", "Notification: " + remoteMessage.getNotification().getTitle() + " - " + remoteMessage.getNotification().getBody());
        }

        String alertId = remoteMessage.getData().get("id");
        String deviceId = remoteMessage.getData().get("deviceId");
        String deviceName = remoteMessage.getData().get("deviceName");
        String deviceType = remoteMessage.getData().get("deviceType");
        String message = remoteMessage.getData().get("message");
        String severity = remoteMessage.getData().get("severity");
        long timestamp = 0;
        try {
            timestamp = Long.parseLong(remoteMessage.getData().get("timestamp"));
        } catch (Exception ignored) {}

        Alert alert = new Alert(alertId, deviceId, deviceName, deviceType, message, new Date(timestamp), severity);

        Intent parentIntent = new Intent(this, licenta.andrei.catanoiu.securehive.activities.MainActivity.class);
        parentIntent.putExtra("fragment", "devices");
        parentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent intent = new Intent(this, licenta.andrei.catanoiu.securehive.activities.AlertDetailsActivity.class);
        intent.putExtra("alert", alert);

        int requestCode = new Random().nextInt();
        PendingIntent pendingIntent = PendingIntent.getActivities(
                this, requestCode, new Intent[]{parentIntent, intent}, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationService.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("Security Alert - " + deviceName)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setFullScreenIntent(pendingIntent, true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1001, builder.build());
        }
    }
} 