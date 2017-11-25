package com.milburn.mytlc;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.provider.AlarmClock;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SwipeRefreshLayout mSwipe;
    private RecyclerView mRecyclerView;
    private CompactCalendarView mCompactCalendarView;
    private Toolbar mToolbar;
    private String tempPass;
    private Credentials credentials;
    private Snackbar mSnackBar;
    private List<Shift> globalSchedule;
    private Boolean importBool = false;
    private PrefManager pm;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        pm = new PrefManager(this, new PrefManager.onPrefChanged() {
            @Override
            public void prefChanged(SharedPreferences sharedPreferences, String s) {
                if (pm.isCriticalAttr(s)) {
                    recreate();
                }
            }
        });
        setTheme(pm.getTheme());

        firebaseHelper = new FirebaseHelper(this);
        credentials = new Credentials(this);
        tempPass = getIntent().getStringExtra("Password");
        if (getIntent().getStringExtra("Schedule") != null) {
            globalSchedule = credentials.getSchedule(getIntent().getStringExtra("Schedule"));
        }

        if (credentials.credsExist() || (credentials.userExists() && tempPass != null)) {
            switch (getResources().getConfiguration().orientation) {
                case Configuration.ORIENTATION_PORTRAIT:
                    setContentView(R.layout.activity_main);
                    break;

                case Configuration.ORIENTATION_LANDSCAPE:
                    setContentView(R.layout.activity_main_landscape);
                    break;
            }
            initMain();
        } else {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void initMain() {
        if (tempPass == null && pm.getSyncBackground() && !pm.getAlarmSet()) {
            pm.changeAlarm(1);
        } else if (tempPass != null) {
            pm.changeAlarm(0);
        }

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mCompactCalendarView = (CompactCalendarView) findViewById(R.id.compactCalendarView);

        mToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCompactCalendarView.getVisibility() == View.VISIBLE) {
                    mCompactCalendarView.setVisibility(View.GONE);
                    mToolbar.setSubtitle("");
                    pm.setCollapsed(true);
                } else {
                    mCompactCalendarView.setVisibility(View.VISIBLE);
                    mToolbar.setSubtitle(getParsedDate(mCompactCalendarView.getFirstDayOfCurrentMonth(), "MMMM yyyy"));
                    pm.setCollapsed(false);
                }
            }
        });

        mSwipe = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        mSwipe.setDistanceToTriggerSync(700);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(false);

        mCompactCalendarView.setFirstDayOfWeek(Calendar.SUNDAY);
        mToolbar.setSubtitle(getParsedDate(mCompactCalendarView.getFirstDayOfCurrentMonth(), "MMMM yyyy"));

        if (pm.getCollapsed()) {
            mCompactCalendarView.setVisibility(View.GONE);
            mToolbar.setSubtitle("");
        }

        mSwipe.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        mCompactCalendarView.setCurrentDate(Calendar.getInstance().getTime());
                        mToolbar.setSubtitle(getParsedDate(mCompactCalendarView.getFirstDayOfCurrentMonth(), "MMMM yyyy"));
                        if (mSnackBar != null && mSnackBar.isShown()) {
                            mSnackBar.dismiss();
                        }
                        getSchedule();
                    }
                }
        );

        mCompactCalendarView.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {
                if (globalSchedule != null) {
                    int i = -1;
                    for (Shift shift : globalSchedule) {
                        i++;
                        if (shift.getSingleDayDate().equals(dateClicked)) {
                            RecyclerAdapter recyclerAdapter = (RecyclerAdapter)mRecyclerView.getAdapter();
                            mRecyclerView.smoothScrollToPosition(recyclerAdapter.getPosition(shift));
                            mCompactCalendarView.setCurrentSelectedDayBackgroundColor(Color.BLACK);
                            break;
                        } else {
                            mCompactCalendarView.setCurrentSelectedDayBackgroundColor(pm.getColorFromAttribute(R.attr.colorPrimaryDark));
                        }
                    }
                }
            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
                mToolbar.setSubtitle(getParsedDate(mCompactCalendarView.getFirstDayOfCurrentMonth(), "MMMM yyyy"));
            }
        });

        if (!getExistingSchedule()) {
            getSchedule();
        }

        updateSelectionColor();
    }

    private void getSchedule() {
        HashMap<String, String> mUserPassMap = new HashMap<>();
        if (tempPass == null) {
            mUserPassMap.putAll(credentials.getCredentials());
        } else {
            mUserPassMap.put("Username", credentials.getUsername());
            mUserPassMap.put("Password", tempPass);
        }

        PostLoginAPI postLoginAPI = new PostLoginAPI(MainActivity.this, new PostLoginAPI.AsyncResponse() {
            @Override
            public void processFinish(List<Shift> shiftList) {
                if (!shiftList.isEmpty()) {
                    RecyclerView.Adapter mRecyclerAdapter = new RecyclerAdapter(shiftList, getApplicationContext());
                    mRecyclerView.setAdapter(mRecyclerAdapter);
                    addToCalendar(shiftList);
                    updateSelectionColor();
                    if (tempPass == null) {
                        credentials.setSchedule(shiftList);
                    }
                    if (importBool) {
                        importBool = false;
                        importToCalendar(shiftList);
                    }
                } else if (mRecyclerView.getAdapter() == null || mRecyclerView.getAdapter().getItemCount() == 0) {
                    getExistingSchedule();
                }
                mSwipe.setRefreshing(false);
                importBool = false;
            }
        });

        postLoginAPI.execute(mUserPassMap);
    }

    private void createSnack(String notice) {
        mSnackBar = Snackbar.make(findViewById(R.id.coordinatorLayout), notice, Snackbar.LENGTH_LONG);
        mSnackBar.show();
    }

    private boolean getExistingSchedule() {
        List<Shift> existingList = new ArrayList<>();
        String updatedTime = "Never updated";
        if (globalSchedule != null && !globalSchedule.isEmpty()) {
            existingList = globalSchedule;
            Date currentTime = Calendar.getInstance().getTime();
            SimpleDateFormat timeFormat = new SimpleDateFormat("'Last refreshed on' E, MMM d yyyy, h:mm aa");
            updatedTime = timeFormat.format(currentTime);
            if (tempPass == null) {
                credentials.setSchedule(existingList);
            }
        } else if (credentials.scheduleExists()) {
            existingList = credentials.getSchedule();
            updatedTime = credentials.getScheduleUpdated();
        }

        if (!existingList.isEmpty() && credentials.isScheduleUpdated(updatedTime)) {
            RecyclerView.Adapter mRecyclerAdapter = new RecyclerAdapter(existingList, this);
            mRecyclerView.setAdapter(mRecyclerAdapter);
            addToCalendar(existingList);
            createSnack(updatedTime);
            RecyclerAdapter recyclerAdapter = (RecyclerAdapter)mRecyclerView.getAdapter();
            mRecyclerView.smoothScrollToPosition(recyclerAdapter.getPosition(existingList.get(0)));
            return true;
        }

        return false;
    }

    private void addToCalendar(List<Shift> shiftList) {
        if (shiftList != null && !shiftList.isEmpty()) {
            globalSchedule = shiftList;
            mCompactCalendarView.removeAllEvents();
            List<Event> eventList = new ArrayList<>();
            for (Shift shift : shiftList) {
                if (shift != null) {
                    Event event = new Event(Color.BLACK, shift.getStartTime().getTime(), "Shift");
                    eventList.add(event);
                }
            }
            if (pm.getDisplay()) {
                List<Shift> pastList = credentials.getPastSchedule();
                for (Shift shift : pastList) {
                    if (shift != null) {
                        Event event = new Event(Color.GRAY, shift.getStartTime().getTime(), "Shift");
                        eventList.add(event);
                    }
                }
            }
            mCompactCalendarView.addEvents(eventList);
            mCompactCalendarView.setCurrentDate(Calendar.getInstance().getTime());
        }
    }

    private String getParsedDate(Date date, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(date);
    }

    private void importToCalendar(List<Shift> shiftList) {
        CalendarHelper calendarHelper = new CalendarHelper(this, false);
        calendarHelper.execute(shiftList);
    }

    private boolean checkPerms() {
        return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        Drawable drawable = mToolbar.getOverflowIcon();
        if (drawable != null) {
            drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
            mToolbar.setOverflowIcon(drawable);
        }

        menu.findItem(R.id.item_past).setChecked(pm.getDisplay());
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.item_import:
                if (!checkPerms()) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR}, 0);
                } else {
                    importBool = true;
                    getSchedule();
                }
                break;

            case R.id.item_past:
                if (item.isChecked()) {
                    item.setChecked(false);
                    pm.setDisplay(false);
                    recreate();
                } else {
                    item.setChecked(true);
                    pm.setDisplay(true);
                    recreate();
                }
                break;

            case R.id.item_alarm:
                if (globalSchedule != null && !globalSchedule.isEmpty()) {
                    Long currentTime = Calendar.getInstance().getTimeInMillis();
                    Shift firstShift = globalSchedule.get(0);

                    if (globalSchedule.size() > 1 && firstShift.getStartTime().getTime() < currentTime) {
                        firstShift = globalSchedule.get(1);
                    }

                    Long startTime = firstShift.getStartTime().getTime();

                    Long hours = (startTime-currentTime)/3600000;

                    if (hours > 0 && hours < 24) {
                        setAlarm(firstShift);
                    } else if (hours > 24) {
                        createSnack("Alarm cannot be created, next shift is over 24 hours away");
                    } else {
                        createSnack("Alarm cannot be created");
                    }
                }
                break;

            case R.id.item_report:
                firebaseHelper.setReport(true);
                getSchedule();
                break;

            case R.id.item_settings:
                intent = new Intent(getBaseContext(), SettingsActivity.class);
                startActivity(intent);
                break;

            case R.id.item_logout:
                if (pm.getDeleteEvents() && !credentials.getEventIds().isEmpty() && checkPerms()) {
                    CalendarHelper calendarHelper = new CalendarHelper(getBaseContext(), false);
                    calendarHelper.deleteEvents();

                    credentials.logout();
                    intent = new Intent(getBaseContext(), LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else if (pm.getDeleteEvents() && !credentials.getEventIds().isEmpty()) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR}, 1);
                } else {
                    credentials.logout();
                    intent = new Intent(getBaseContext(), LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            importBool = true;
            getSchedule();
        } else if (grantResults.length > 0 && requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            CalendarHelper calendarHelper = new CalendarHelper(getBaseContext(), false);
            calendarHelper.deleteEvents();

            credentials.logout();
            Intent intent = new Intent(getBaseContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                setContentView(R.layout.activity_main);
                break;

            case Configuration.ORIENTATION_LANDSCAPE:
                setContentView(R.layout.activity_main_landscape);
                break;
        }
        initMain();
    }

    public void setAlarm(final Shift shift) {
        View v  = getLayoutInflater().inflate(R.layout.dialog_alarm, null);
        final TimePicker timePicker = (TimePicker)v.findViewById(R.id.timepicker_alarm);
        TextView nextShiftText = (TextView)v.findViewById(R.id.textview_nextshift);

        String shiftText = "Next shift starts at: " + shift.getStartTime("h:mm aa");
        Integer hour = Integer.parseInt(shift.getStartTime("H"));
        Integer min = Integer.parseInt(shift.getStartTime("m"));

        nextShiftText.setText(shiftText);
        timePicker.setCurrentHour(hour-1);
        timePicker.setCurrentMinute(min);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(v);
        builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent setAlarm = new Intent(AlarmClock.ACTION_SET_ALARM);
                setAlarm.putExtra(AlarmClock.EXTRA_HOUR, timePicker.getCurrentHour())
                        .putExtra(AlarmClock.EXTRA_MINUTES, timePicker.getCurrentMinute())
                        .putExtra(AlarmClock.EXTRA_MESSAGE, "Work at Best Buy")
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true);

                startActivity(setAlarm);
            }
        });
        builder.create();
        builder.show();
    }

    public void updateSelectionColor() {
        if (globalSchedule != null) {
            for (Shift shift : globalSchedule) {
                if (shift != null && shift.getScheduledToday()) {
                    mCompactCalendarView.setCurrentDayBackgroundColor(Color.BLACK);
                    mCompactCalendarView.setCurrentSelectedDayBackgroundColor(Color.BLACK);
                }
            }
        }
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
}