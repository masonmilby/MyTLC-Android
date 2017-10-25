package com.milburn.mytlc;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CalendarContract;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class CalendarHelper extends AsyncTask<List<Shift>, Integer, Void> {

    private Context context;
    private List<Shift> shiftList;
    private Credentials credentials;
    private ContentResolver cr;
    private Uri calUri;
    private Uri eventUri;

    private Spinner spinnerCalendar = null;
    private EditText editEventTitle = null;
    private EditText editAddress = null;
    private EditText editStore = null;
    private Button buttonStore = null;
    private CheckBox checkDelete = null;

    private AlertDialog.Builder builder;
    private Snackbar snackBar;
    private String snackString;
    private View dialogView;
    private CharSequence[] calendarNames;
    private HashMap<String, Integer> calendarMap;
    private PrefManager pm;

    private String storeAddress = "";
    private Boolean isBackground = false;

    public CalendarHelper(Context con, Boolean isSync) {
        context = con;
        credentials = new Credentials(context);
        pm = new PrefManager(con, new PrefManager.onPrefChanged() {
            @Override
            public void prefChanged(SharedPreferences sharedPreferences, String s) {
                //
            }
        });

        cr = context.getContentResolver();
        calUri = CalendarContract.Calendars.CONTENT_URI;
        eventUri = CalendarContract.Events.CONTENT_URI;

        isBackground = isSync;
    }

    @Override
    protected Void doInBackground(List<Shift>... params) {
        System.out.println(context.getClass());
        shiftList = params[0];
        if (!isBackground) {
            getCalendarNames(true);
        } else {
            getCalendarNames(false);
            getStoreAddress();
        }
        return null;
    }

    public void deleteEvents(String calName) {
        if (credentials.getEventIds(calName) != null) {
            List<Long> eventIds = credentials.getEventIds(calName);
            for (Long id : eventIds) {
                Uri deleteUri = ContentUris.withAppendedId(eventUri, id);
                cr.delete(deleteUri, null, null);
            }
            credentials.removeEventIds(calName);
        }
    }

    public void deleteEvents() {
        if (!credentials.getEventIds().isEmpty()) {
            for (List<Long> listIds : credentials.getEventIds().values()) {
                for (Long id : listIds) {
                    Uri deleteUri = ContentUris.withAppendedId(eventUri, id);
                    cr.delete(deleteUri, null, null);
                }
            }
            credentials.setEventIds(new HashMap<String, List<Long>>());
        }
    }

    private void getStoreAddress() {
        BBYApi bbyApi = new BBYApi(context, new BBYApi.AsyncResponse() {
            @Override
            public void processFinish(String address) {
                if (address != null && editAddress != null) {
                    editAddress.setText(address);
                } else if (address != null && isBackground) {
                    storeAddress = address;
                    syncImport();
                } else if (isBackground) {
                    syncImport();
                }
            }
        });

        String storeId = shiftList.get(0).getStoreNumber();
        if (!storeId.isEmpty()) {
            bbyApi.execute(storeId);
        }
    }

    public CharSequence[] getCalendarNames(Boolean showUI) {
        Cursor cur;

        final String[] EVENT_PROJECTION = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        };

        try {
            cur = cr.query(calUri, EVENT_PROJECTION, null, null, null);
        } catch (SecurityException se) {
            se.printStackTrace();
            if (showUI) {
                snackString = "No calendars available";
                publishProgress(1);
            }
            return new CharSequence[0];
        }

        calendarMap = new HashMap<>();
        while (cur.moveToNext()) {
            calendarMap.put(cur.getString(1), cur.getInt(0));
        }
        cur.close();
        calendarNames = calendarMap.keySet().toArray(new CharSequence[calendarMap.size()]);
        if (calendarNames.length < 1) {
            if (showUI) {
                snackString = "No calendars available";
                publishProgress(1);
            }
            return new CharSequence[0];
        }

        if (showUI) {
            publishProgress(2);
        }
        return calendarNames;
    }

    public void importToCalendar() {
        String calName = spinnerCalendar.getSelectedItem().toString();

        if (checkDelete.isChecked()) {
            deleteEvents(calName + "Manual");
            deleteEvents(calName + "Background");
        }

        if (editAddress.getText().toString().contentEquals("")) {
            credentials.setGetAddress(false);
        } else {
            credentials.setGetAddress(true);
        }

        List<Long> eventIds = new ArrayList<>();
        for (Shift shift : shiftList) {
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, shift.getStartTime().getTime());
            values.put(CalendarContract.Events.DTEND, shift.getEndTime().getTime());
            values.put(CalendarContract.Events.TITLE, editEventTitle.getText().toString());
            values.put(CalendarContract.Events.DESCRIPTION, "Departments: " + shift.getCombinedDepts() + "\n" + "Activities: " + shift.getCombinedAct());
            values.put(CalendarContract.Events.CALENDAR_ID, calendarMap.get(calName));
            values.put(CalendarContract.Events.EVENT_TIMEZONE, Calendar.getInstance().getTimeZone().getDisplayName());
            values.put(CalendarContract.Events.EVENT_LOCATION, editAddress.getText().toString());

            try {
                Uri uri = cr.insert(eventUri, values);
                Long id = Long.parseLong(uri.getLastPathSegment());
                eventIds.add(id);
            } catch (SecurityException se) {
                se.printStackTrace();
            }
        }
        if (!eventIds.isEmpty()) {
            credentials.addEventIds(calName + "Manual", eventIds);
        }
        snackString = "Events successfully added to " + "'" + calName + "'";
        publishProgress(1);
    }

    private void syncImport() {
        String calName = pm.getSelectedCalendar();
        deleteEvents(calName + "Background");
        deleteEvents(calName + "Manual");

        List<Long> eventIds = new ArrayList<>();
        for (Shift shift : shiftList) {
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, shift.getStartTime().getTime());
            values.put(CalendarContract.Events.DTEND, shift.getEndTime().getTime());
            values.put(CalendarContract.Events.TITLE, "Work at Best Buy");
            values.put(CalendarContract.Events.DESCRIPTION, "Departments: " + shift.getCombinedDepts() + "\n" + "Activities: " + shift.getCombinedAct());
            values.put(CalendarContract.Events.CALENDAR_ID, calendarMap.get(calName));
            values.put(CalendarContract.Events.EVENT_TIMEZONE, Calendar.getInstance().getTimeZone().getDisplayName());
            values.put(CalendarContract.Events.EVENT_LOCATION, storeAddress);

            try {
                Uri uri = cr.insert(eventUri, values);
                Long id = Long.parseLong(uri.getLastPathSegment());
                eventIds.add(id);
            } catch (SecurityException se) {
                se.printStackTrace();
            }
        }
        if (!eventIds.isEmpty()) {
            credentials.addEventIds(calName + "Background", eventIds);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        switch (progress[0]) {
            case 1:
                snackBar = Snackbar.make(((Activity) context).findViewById(R.id.coordinatorLayout), snackString, Snackbar.LENGTH_LONG);
                snackBar.show();
                break;

            case 2:
                dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_import, null);

                builder = new AlertDialog.Builder(context);
                builder.setTitle("Customize your events")
                        .setView(dialogView)
                        .setCancelable(false)
                        .setPositiveButton("Import", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                importToCalendar();
                                dialog.dismiss();
                            }
                        })

                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //
                            }
                        });

                builder.create();
                builder.show();

                spinnerCalendar = (Spinner) dialogView.findViewById(R.id.spinner_calendar);
                editEventTitle = (EditText) dialogView.findViewById(R.id.edit_title);
                editAddress = (EditText) dialogView.findViewById(R.id.edit_store_address);
                editStore = (EditText) dialogView.findViewById(R.id.edit_store_number);
                buttonStore = (Button) dialogView.findViewById(R.id.button_store);

                buttonStore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getStoreAddress();
                    }
                });
                editStore.setText(shiftList.get(0).getStoreNumber());

                if (credentials.isGetAddress()) {
                    getStoreAddress();
                }

                checkDelete = (CheckBox) dialogView.findViewById(R.id.check_delete);
                checkDelete.setChecked(true);

                if (calendarNames != null) {
                    ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, calendarNames);
                    spinnerCalendar.setAdapter(adapter);

                    if (credentials.getLastCalName() != null) {
                        Integer calPos = adapter.getPosition(credentials.getLastCalName());
                        spinnerCalendar.setSelection(calPos);
                    }
                }

                break;
        }
    }
}
