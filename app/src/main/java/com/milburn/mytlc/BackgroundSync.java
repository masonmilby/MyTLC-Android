package com.milburn.mytlc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class BackgroundSync extends BroadcastReceiver {

    private Credentials credentials;
    private Context con;
    private PostLoginAPI postLoginAPI;

    @Override
    public void onReceive(final Context context, Intent intent) {
        con = context;
        credentials = new Credentials(context);

        if (!credentials.getCredentials().isEmpty()) {
            postLoginAPI = new PostLoginAPI(context, new PostLoginAPI.AsyncResponse() {
                @Override
                public void processFinish(List<Shift> shiftList) {
                    if (!shiftList.isEmpty()) {
                        Credentials credentials = new Credentials(context);
                        List<Shift> pastList = new ArrayList<>();
                        pastList.addAll(credentials.getSchedule());

                        int i = 0;
                        for (Shift shift : shiftList) {
                            if (!pastList.contains(shift)) {
                                i++;
                            }
                        }

                        credentials.setSchedule(shiftList);
                        createNotification(0, i);
                    } else {
                        createNotification(1, 0);
                    }
                }
            });
            postLoginAPI.execute(credentials.getCredentials());
        }
    }

    private void createNotification(Integer message, Integer addedShifts) {
        NotificationManager notificationManager = (NotificationManager) con.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder notification;

        String channelId = "default";
        CharSequence channelName = "background_sync";
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
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(pendingIntent)
                    .setChannelId(channelId)
                    .setDefaults(-1);
        } else {
            notification = new Notification.Builder(con)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(pendingIntent)
                    .setDefaults(-1);
        }

        switch (message) {
            case 0:
                notification.setContentTitle("Schedule updated");
                notification.setContentText(addedShifts + " shifts added");
                break;

            case 1:
                notification.setContentTitle("Schedule failed to update");
                notification.setContentText(postLoginAPI.errorMessage);
        }

        notificationManager.notify(1, notification.build());
    }
}
