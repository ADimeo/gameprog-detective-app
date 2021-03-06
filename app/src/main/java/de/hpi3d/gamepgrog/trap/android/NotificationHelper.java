package de.hpi3d.gamepgrog.trap.android;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import de.hpi3d.gamepgrog.trap.R;
import de.hpi3d.gamepgrog.trap.ui.MainActivity;

/**
 * Helper for sending android notifications
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "de.hpi3d.gamepgrog.trap.fcmnotification";

    public static void sendNotification(Context c, String messageTitle, String messageBody) {
        // Intent to open when the notification is clicked
        Intent intent = new Intent(c, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(c, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        // Builds a new notification with title, icon, body
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(c, CHANNEL_ID)
                        .setContentTitle(messageTitle)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Detective Game FCM Notification",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // Show the notification
        notificationManager.notify(0, notificationBuilder.build());
    }
}
