package com.milburn.mytlc;

import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class SettingsActivity extends AppCompatActivity {

    private PrefManager pm;
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pm = new PrefManager(this, new PrefManager.onPrefChanged() {
            @Override
            public void prefChanged(SharedPreferences sharedPreferences, String s) {
                if (pm.isThemeChange(s)) {
                    recreate();
                }
            }
        });

        setTheme(pm.getTheme());
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        settingsFragment = new SettingsFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, settingsFragment)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Drawable draw = getPackageManager().getApplicationIcon(this.getApplicationInfo());
            Bitmap icon = ((BitmapDrawable) draw).getBitmap();

            ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(getString(R.string.app_name), icon, pm.getColorFromAttribute(R.attr.colorPrimary));
            this.setTaskDescription(taskDesc);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            CalendarHelper calendarHelper = new CalendarHelper(this);
            CharSequence[] calNames = calendarHelper.getCalendarNames();
            settingsFragment.listCalendars.setEntries(calNames);
            settingsFragment.listCalendars.setEntryValues(calNames);
            settingsFragment.listCalendars.setDefaultValue(calNames[0]);
            settingsFragment.listCalendars.setValue(calNames[0].toString());

            settingsFragment.listCalendars.setEnabled(true);
        } else if (grantResults.length > 0 && requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            settingsFragment.importCalendar.setChecked(false);
            settingsFragment.listCalendars.setEnabled(false);
        }
    }
}
