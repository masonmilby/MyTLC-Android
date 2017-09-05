package com.milburn.mytlc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.provider.AlarmClock;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BackgroundSync extends BroadcastReceiver {

    private Credentials credentials;
    private Context con;
    private PostLoginAPI postLoginAPI;
    private PrefManager pm;

    private String alarmResult = "";
    private String calendarResult = "";

    @Override
    public void onReceive(final Context context, Intent intent) {
        con = context;
        credentials = new Credentials(context);
        pm = new PrefManager(context, new PrefManager.onPrefChanged() {
            @Override
            public void prefChanged(SharedPreferences sharedPreferences, String s) {
                //
            }
        });

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
                            for (Shift shiftPast : pastList) {
                                if (shiftPast.getSingleDayDate().equals(shift.getSingleDayDate())) {
                                    i++;
                                }
                            }
                        }
                        int total = shiftList.size() - i;

                        credentials.setSchedule(shiftList);

                        if (pm.getSyncAlarm()) {
                            setAlarm(shiftList);
                        }

                        if (total > 0 && pm.getImportCalendar() && checkPerms()) {
                            CalendarHelper calendarHelper = new CalendarHelper(con);
                            calendarHelper.execute(shiftList);
                            calendarResult = "Shifts imported to calendar";
                        } else if (total > 0 && pm.getImportCalendar() && !checkPerms()) {
                            calendarResult = "Cannot import to calendar, permission denied";
                        }

                        createNotification(1, total);
                    } else {
                        createNotification(0, 0);
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
                notification.setContentTitle("Schedule failed to update");
                notification.setContentText(postLoginAPI.errorMessage);
                break;

            case 1:
                if (calendarResult.toLowerCase().contains("cannot") || alarmResult.toLowerCase().contains("cannot")){
                    notification.setContentTitle("Schedule updated with error");
                } else {
                    notification.setContentTitle("Schedule updated");
                }

                if (!calendarResult.equals("")) {
                    calendarResult = calendarResult+"\n";
                }

                notification.setContentText(addedShifts + " shifts added");
                notification.setStyle(new Notification.BigTextStyle().bigText(addedShifts + " shifts added\n" + calendarResult + alarmResult));
                break;
        }

        notificationManager.notify(1, notification.build());
    }

    private boolean checkPerms() {
        return ActivityCompat.checkSelfPermission(con, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(con, android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    private Boolean setAlarm(List<Shift> shiftList) {
        Long currentTime = Calendar.getInstance().getTimeInMillis();
        Shift firstShift = shiftList.get(0);

        if (shiftList.size() > 1 && firstShift.getStartTime().getTime() < currentTime) {
            firstShift = shiftList.get(1);
        }

        Long startTime = firstShift.getStartTime().getTime();
        Long hours = (startTime-currentTime)/3600000;

        if (hours > 0 && hours < 24) {
            String[] pickerValues = pm.getSyncAlarmTime().split("");
            Integer offHour = Integer.valueOf(pickerValues[1]);
            Integer offMinute = Integer.valueOf(pickerValues[3]+pickerValues[4]);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(firstShift.getStartTime());
            Integer shiftHour = calendar.get(Calendar.HOUR_OF_DAY);
            Integer shiftMinute = calendar.get(Calendar.MINUTE);

            Integer finalHour = shiftHour-offHour;
            Integer finalMin = shiftMinute-offMinute;

            Intent setAlarm = new Intent(AlarmClock.ACTION_SET_ALARM);
            setAlarm.putExtra(AlarmClock.EXTRA_HOUR, shiftHour-offHour)
                    .putExtra(AlarmClock.EXTRA_MINUTES, shiftMinute-offMinute)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, "Work at Best Buy")
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, true);

            con.startActivity(setAlarm);

            String setTime = finalHour + ":" + finalMin;

            alarmResult = "Alarm set for " + setTime;
            return true;
        } else if (hours < 0) {
            alarmResult = "Alarm cannot be set for past events";
        } else if (hours > 24) {
            alarmResult = "Alarm cannot be set for shifts over 24 hours away";
        } else {
            alarmResult = "Alarm cannot be set";
        }
        return false;
    }
}
