package com.milburn.mytlc;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.ColorInt;
import android.util.TypedValue;
import android.view.View;

import java.util.Calendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class PrefManager implements SharedPreferences.OnSharedPreferenceChangeListener{

    private SharedPreferences sharedPref;
    private Context con;
    public onPrefChanged changeInterface;

    public String key_pay = "pay";
    public String key_tax = "tax";
    public String key_base = "base_theme";
    public String key_custom = "custom_colors";
    public String key_primary = "primaryColor";
    public String key_accent = "accentColor";
    public String key_past = "past_shifts";
    public String key_display = "display_past";
    public String key_collapsed = "collapsed";
    public String key_delete_settings = "delete_settings";
    public String key_delete_events = "delete_events";

    public String key_sync_background = "sync_background";
    public String key_sync_import = "sync_import";
    public String key_sync_import_calendar = "sync_import_calendar";
    public String key_sync_alarm = "sync_alarm";
    public String key_sync_alarm_time = "sync_alarm_time";

    public PrefManager(Context context, onPrefChanged onChanged) {
        changeInterface = onChanged;
        con = context;
        sharedPref = android.preference.PreferenceManager.getDefaultSharedPreferences(con);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
    }

    public interface onPrefChanged {
        void prefChanged(SharedPreferences sharedPreferences, String s);
    }

    public Integer getTheme() {
        if (sharedPref.contains(key_primary) && sharedPref.contains(key_accent)) {
            String base;
            String primary;
            String accent;

            switch (getBaseName()) {
                case "Light":
                    base = "CustomLight";
                    primary = sharedPref.getString(key_primary, "");
                    accent = sharedPref.getString(key_accent, "");
                    return con.getResources().getIdentifier("style/" + base + "." + primary + "." + accent, null, con.getPackageName());

                case "Dark":
                    base = "CustomDark";
                    primary = sharedPref.getString(key_primary, "");
                    accent = sharedPref.getString(key_accent, "");
                    return con.getResources().getIdentifier("style/" + base + "." + primary + "." + accent, null, con.getPackageName());

                default:
                    return R.style.CustomLight;
            }
        } else {
            switch (getBaseName()) {
                case "Light":
                    return R.style.CustomLight;

                case "Dark":
                    return R.style.CustomDark;

                default:
                    return R.style.CustomLight;
            }
        }
    }

    public String getBaseName() {
        return sharedPref.getString(key_base, "");
    }

    public void setTheme(View primary, View accent) {
        String primaryName = con.getResources().getResourceEntryName(primary.getId());
        String accentName = con.getResources().getResourceEntryName(accent.getId()).split("_")[0];

        if (!primaryName.contentEquals("-1") && !accentName.contentEquals("-1")) {
            sharedPref.edit()
                    .putString(key_primary, primaryName)
                    .putString(key_accent, accentName)
                    .apply();
        }
    }

    public void setTheme(String primary, String accent) {
        sharedPref.edit()
                .putString(key_primary, primary)
                .putString(key_accent, accent)
                .apply();
    }

    public Integer getColorFromAttribute(Integer attr) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = con.getTheme();
        theme.resolveAttribute(attr, typedValue, true);
        @ColorInt int color = typedValue.data;
        return color;
    }

    public void changeAlarm(Integer enabled) {
        AlarmManager alarmMgr = (AlarmManager)con.getSystemService(con.ALARM_SERVICE);
        Intent intent1 = new Intent(con, BackgroundSync.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(con, 0, intent1, 0);

        switch (enabled) {
            case 0:
                alarmMgr.cancel(alarmIntent);
                break;

            case 1:
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                //TODO: Setup TimeZone
                calendar.set(Calendar.HOUR_OF_DAY, 7);
                calendar.set(Calendar.MINUTE, 0);

                alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, alarmIntent);
                break;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        changeInterface.prefChanged(sharedPreferences, s);
    }

    public Boolean isCriticalAttr(String s) {
        return s.contentEquals(key_base) || s.contentEquals(key_primary) || s.contentEquals(key_accent) || s.contentEquals(key_pay) || s.contentEquals(key_tax);
    }

    public Boolean isThemeChange(String s) {
        return s.contentEquals(key_base) || s.contentEquals(key_primary) || s.contentEquals(key_accent);
    }

    public String getPay() {
        return sharedPref.getString(key_pay, "0");
    }

    public String getTax() {
        return sharedPref.getString(key_tax, "0");
    }

    public String getBase() {
        return sharedPref.getString(key_base, "Light");
    }

    public String getPrimary() {
        return sharedPref.getString(key_primary, "Amber");
    }

    public String getAccent() {
        return sharedPref.getString(key_accent, "Grey");
    }

    public Boolean getPast() {
        return sharedPref.getBoolean(key_past, false);
    }

    public void setPast(Boolean bool) {
        sharedPref.edit()
                .putBoolean(key_past, bool)
                .apply();
    }

    public Boolean getDisplay() {
        return sharedPref.getBoolean(key_display, false);
    }

    public void setDisplay(Boolean bool) {
        sharedPref.edit()
                .putBoolean(key_display, bool)
                .apply();
    }

    public Boolean getCollapsed() {
        return sharedPref.getBoolean(key_collapsed, false);
    }

    public void setCollapsed(Boolean bool) {
        sharedPref.edit()
                .putBoolean(key_collapsed, bool)
                .apply();
    }

    public Boolean getDeleteSettings() {
        return sharedPref.getBoolean(key_delete_settings, false);
    }

    public Boolean getDeleteEvents() {
        return sharedPref.getBoolean(key_delete_events, true);
    }

    public Boolean getSyncBackground() {
        return sharedPref.getBoolean(key_sync_background, true);
    }

    public Boolean getImportCalendar() {
        return sharedPref.getBoolean(key_sync_import, false);
    }

    public String getSelectedCalendar() {
        return sharedPref.getString(key_sync_import_calendar, "Null");
    }

    public String getSyncAlarmTime() {
        return sharedPref.getString(key_sync_alarm_time, "0:00");
    }

    public void setSyncAlarmTime(String time) {
        sharedPref.edit()
                .putString(key_sync_alarm_time, time)
                .apply();
    }

    public Boolean getSyncAlarm() {
        return sharedPref.getBoolean(key_sync_alarm, false);
    }
}