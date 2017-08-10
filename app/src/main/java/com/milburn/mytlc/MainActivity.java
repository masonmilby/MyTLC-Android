package com.milburn.mytlc;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
    private Integer currentTheme;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefManager = new PrefManager(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        currentTheme = prefManager.getTheme();
        setTheme(currentTheme);

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
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mCompactCalendarView = (CompactCalendarView) findViewById(R.id.compactCalendarView);

        mToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCompactCalendarView.getVisibility() == View.VISIBLE) {
                    mCompactCalendarView.setVisibility(View.GONE);
                    mToolbar.setSubtitle("");
                } else {
                    mCompactCalendarView.setVisibility(View.VISIBLE);
                    mToolbar.setSubtitle(getParsedDate(mCompactCalendarView.getFirstDayOfCurrentMonth(), "MMMM yyyy"));
                }
            }
        });

        mSwipe = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        mSwipe.setDistanceToTriggerSync(800);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(false);

        mCompactCalendarView.setFirstDayOfWeek(Calendar.SUNDAY);
        mToolbar.setSubtitle(getParsedDate(mCompactCalendarView.getFirstDayOfCurrentMonth(), "MMMM yyyy"));

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
                            mRecyclerView.smoothScrollToPosition(i);
                            //mCompactCalendarView.setCurrentSelectedDayTextColor(Color.WHITE);
                            //mCompactCalendarView.setCurrentSelectedDayBackgroundColor(Color.BLACK);
                            break;
                        } else {
                            //mCompactCalendarView.setCurrentSelectedDayTextColor(Color.BLACK);
                            //mCompactCalendarView.setCurrentSelectedDayBackgroundColor(Color.WHITE);
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
            mCompactCalendarView.addEvents(eventList);
            mCompactCalendarView.setCurrentDate(Calendar.getInstance().getTime());
        }
    }

    private String getParsedDate(Date date, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(date);
    }

    private void importToCalendar(List<Shift> shiftList) {
        CalendarHelper calendarHelper = new CalendarHelper(this);
        calendarHelper.execute(shiftList);
    }

    private boolean checkPerms() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        Drawable drawable = mToolbar.getOverflowIcon();
        if (drawable != null) {
            drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
            mToolbar.setOverflowIcon(drawable);
        }
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

            case R.id.item_settings:
                intent = new Intent(getBaseContext(), SettingsActivity.class);
                startActivity(intent);
                break;

            case R.id.item_logout:
                if (!credentials.getEventIds().isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Delete imported calendar events?");
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!checkPerms()) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR}, 1);
                            } else {
                                CalendarHelper calendarHelper = new CalendarHelper(getBaseContext());
                                calendarHelper.deleteEvents();

                                credentials.logout();
                                Intent intent = new Intent(getBaseContext(), LoginActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                    });
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            credentials.logout();
                            Intent intent = new Intent(getBaseContext(), LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                    builder.create();
                    builder.show();
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
            CalendarHelper calendarHelper = new CalendarHelper(getBaseContext());
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
        /*if (globalSchedule != null) {
            for (Shift shift : globalSchedule) {
                if (shift != null && shift.getScheduledToday()) {
                    mCompactCalendarView.setCurrentSelectedDayTextColor(Color.WHITE);
                    mCompactCalendarView.setCurrentSelectedDayBackgroundColor(Color.BLACK);
                    mCompactCalendarView.setCurrentDayTextColor(Color.WHITE);
                    mCompactCalendarView.setCurrentDayBackgroundColor(Color.BLACK);
                }
            }
        *///}
    }

    @Override
    public void onResume() {
        super.onResume();
        if (prefManager.getTheme() != currentTheme) {
            recreate();
        }
    }
}