package licenta.andrei.catanoiu.securehive.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.activities.MainActivity;
import licenta.andrei.catanoiu.securehive.models.Alert;
import java.util.Random;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

public class NotificationService {
    public static final String CHANNEL_ID = "SecureHive_Alerts";
    private static final String CHANNEL_NAME = "Security Alerts";
    private static final String CHANNEL_DESCRIPTION = "Notifications for security alerts and device status changes";
    
    private static int notificationId = 1;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.deleteNotificationChannel(CHANNEL_ID);
            }
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            channel.enableLights(true);
            NotificationManager notificationManager2 = context.getSystemService(NotificationManager.class);
            if (notificationManager2 != null) {
                notificationManager2.createNotificationChannel(channel);
            }
        }
    }

    public static void showAlertNotification(Context context, Alert alert) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("fragment", "alerts");
        
        int requestCode = new Random().nextInt();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                requestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String deviceName = alert.getDeviceName() != null ? alert.getDeviceName() : "Unknown Device";
        String message = alert.getMessage() != null ? alert.getMessage() : "No message";
        String severity = alert.getSeverity() != null ? alert.getSeverity() : "unknown";
        
        String title = "Security Alert - " + deviceName;

        int icon = R.drawable.app_logo;
        if ("high".equalsIgnoreCase(severity)) {
            icon = R.drawable.ic_error;
        } else if ("medium".equalsIgnoreCase(severity)) {
            icon = R.drawable.ic_warning;
        } else {
            icon = R.drawable.ic_info;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setFullScreenIntent(pendingIntent, true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId++, builder.build());
        }
    }

    public static void showDeviceStatusNotification(Context context, String deviceName, String status) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("openFragment", "alerts");
        
        int requestCode = new Random().nextInt();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                requestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "Device Status Update";
        String message = deviceName + " is now " + status;
        
        int icon = "online".equalsIgnoreCase(status) ? 
                R.drawable.ic_check_circle : R.drawable.ic_error;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId++, builder.build());
        }
    }

    public static void showGeneralNotification(Context context, String title, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        int requestCode = new Random().nextInt();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                requestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId++, builder.build());
        }
    }
} 