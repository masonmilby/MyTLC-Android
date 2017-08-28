package com.milburn.mytlc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.Toast;

import java.util.List;

public class BackgroundSync extends BroadcastReceiver {

    private Credentials credentials;
    private Context con;

    @Override
    public void onReceive(final Context context, Intent intent) {
        con = context;
        credentials = new Credentials(context);

        if (!credentials.getCredentials().isEmpty()) {
            Toast.makeText(context, "Started", Toast.LENGTH_SHORT).show();
            PostLoginAPI postLoginAPI = new PostLoginAPI(context, new PostLoginAPI.AsyncResponse() {
                @Override
                public void processFinish(List<Shift> shiftList) {
                    if (!shiftList.isEmpty()) {
                        Credentials credentials = new Credentials(context);
                        credentials.setSchedule(shiftList);
                        createNotification();
                    }
                }
            });
            postLoginAPI.execute(credentials.getCredentials());
        }
    }

    private void createNotification() {
        NotificationManager notificationManager = (NotificationManager) con.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification;

        String channelId = "default";
        CharSequence channelName = "UpdatedSchedule";
        Intent intent = new Intent(con, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(con, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.YELLOW);
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);

            notification = new Notification.Builder(con)
                    .setContentTitle("Schedule updated")
                    .setContentText("New shifts added")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(pendingIntent)
                    .setChannelId(channelId)
                    .setDefaults(-1)
                    .build();
        } else {
            notification = new Notification.Builder(con)
                    .setContentTitle("Schedule updated")
                    .setContentText("New shifts added")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(pendingIntent)
                    .setDefaults(-1)
                    .build();
        }

        notificationManager.notify(1, notification);
    }
}
