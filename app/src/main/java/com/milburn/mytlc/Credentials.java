package com.milburn.mytlc;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Credentials {

    private SharedPreferences sharedPreferences;
    private PrefManager pm;

    public Credentials(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        pm = new PrefManager(context, new PrefManager.onPrefChanged() {
            @Override
            public void prefChanged(SharedPreferences sharedPreferences, String s) {
                //
            }
        });
    }

    public HashMap<String, String> getCredentials() {
        HashMap<String, String> mapCreds = new HashMap<>();
        mapCreds.put("Username", sharedPreferences.getString("Username", "DEFAULT"));
        mapCreds.put("Password", sharedPreferences.getString("Password", "DEFAULT"));
        return mapCreds;
    }

    public String getUsername() {
        return sharedPreferences.getString("Username", "DEFAULT");
    }

    public String getPassword() {
        return sharedPreferences.getString("Password", "DEFAULT");
    }

    public void setCredentials(HashMap<String, String> userCreds) {
        sharedPreferences.edit()
                .putString("Username", userCreds.get("Username"))
                .putString("Password", userCreds.get("Password"))
                .apply();
    }

    public void setUsername(String strUser) {
        if (strUser == null) {
            sharedPreferences.edit()
                    .remove("Username")
                    .apply();
        } else {
            sharedPreferences.edit()
                    .putString("Username", strUser)
                    .apply();
        }
    }

    public void setPassword(String strPass) {
        if (strPass == null) {
            sharedPreferences.edit()
                    .remove("Password")
                    .apply();
        } else {
            sharedPreferences.edit()
                    .putString("Password", strPass)
                    .apply();
        }
    }

    public void logout() {
        sharedPreferences.edit()
                .remove("Username")
                .remove("Password")
                .remove("Schedule")
                .remove("ScheduleUpdated")
                .remove("PastSchedule")
                .apply();
        if (pm.getDeleteSettings()) {
            sharedPreferences.edit()
                    .remove("pay")
                    .remove("tax")
                    .remove("getAddress")
                    .remove("past_shifts")
                    .remove("delete_settings")
                    .remove("delete_events")
                    .remove("collapsed")
                    .remove("display_past")
                    .remove("primaryColor")
                    .remove("accentColor")
                    .remove("base_theme")
                    .apply();
        }
    }

    public boolean credsExist() {
        return (sharedPreferences.contains("Username") && sharedPreferences.contains("Password"));
    }

    public boolean userExists() {
        return (sharedPreferences.contains("Username"));
    }

    public void setSchedule(List<Shift> shiftList) {
        if (sharedPreferences.getBoolean("past_shifts", false)) {
            setPastSchedule(getSchedule(), shiftList);
        }

        Gson gson = new Gson();
        String serializedSchedule = gson.toJson(shiftList);
        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat timeFormat = new SimpleDateFormat("'Last refreshed on' E, MMM d yyyy, h:mm aa");

        sharedPreferences.edit()
                .putString("Schedule", serializedSchedule)
                .putString("ScheduleUpdated", timeFormat.format(currentTime))
                .apply();
    }

    public void setPastSchedule(List<Shift> oldShiftList, List<Shift> newShiftList) {
        List<Shift> tempList = new ArrayList<>();
        List<Shift> oldPastShiftList = getPastSchedule();

        Calendar calFirst = Calendar.getInstance();
        calFirst.set(Calendar.DAY_OF_MONTH, 1);

        Calendar calCurrent = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMddyyyy");
        String current = simpleDateFormat.format(calCurrent.getTime());
        Date currentTime = null;
        try {
            currentTime = simpleDateFormat.parse(current);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        for (Shift shift : oldShiftList) {
            if (currentTime != null
                    && shift.getSingleDayDate().getTime() < currentTime.getTime()
                    && shift.getSingleDayDate().getTime() >= calFirst.getTime().getTime()
                    && !doesContain(shift, newShiftList) && !doesContain(shift, oldPastShiftList)) {
                tempList.add(shift);
            }
        }
        if (!tempList.isEmpty()) {
            tempList.addAll(oldPastShiftList);
            Gson gson = new Gson();
            String serializedSchedule = gson.toJson(tempList);
            sharedPreferences.edit()
                    .putString("PastSchedule", serializedSchedule)
                    .apply();
        }
    }

    private Boolean doesContain(Shift shift, List<Shift> shiftList) {
        for (Shift shift1 : shiftList) {
            if (shift1.getSingleDayDate().getTime() == shift.getSingleDayDate().getTime()) {
                return true;
            }
        }
        return false;
    }

    public void clearPastSchedule() {
        sharedPreferences.edit()
                .remove("PastSchedule")
                .apply();
    }

    public List<Shift> getPastSchedule() {
        if (sharedPreferences.contains("PastSchedule")) {
            return getSchedule(sharedPreferences.getString("PastSchedule", "DEFAULT"));
        }
        return new ArrayList<>();
    }

    public String getSerialSchedule(List<Shift> shiftList) {
        Gson gson = new Gson();
        String serializedSchedule = gson.toJson(shiftList);
        return serializedSchedule;
    }

    public List<Shift> getSchedule() {
        if (sharedPreferences.contains("Schedule")) {
            Gson gson = new Gson();
            String scheduleString = sharedPreferences.getString("Schedule", "DEFAULT");
            Type stringType = new TypeToken<ArrayList<Shift>>(){}.getType();

            return gson.fromJson(scheduleString, stringType);
        }
        return new ArrayList<>();
    }

    public void addEventIds(String calName, List<Long> eventIds) {
        HashMap<String, List<Long>> tempMap = getEventIds();
        if (calName != null && eventIds != null && !eventIds.isEmpty()) {
            tempMap.put(calName, eventIds);
            setEventIds(tempMap);
        }
    }

    public void setEventIds(HashMap<String, List<Long>> eventIdMap) {
        Gson gson = new Gson();
        String serializedIds = gson.toJson(eventIdMap);

        sharedPreferences.edit()
                .putString("EventIds", serializedIds)
                .apply();
    }

    public void removeEventIds(String calName) {
        HashMap<String, List<Long>> tempMap = getEventIds();
        if (tempMap.containsKey(calName)) {
            tempMap.remove(calName);
            setEventIds(tempMap);
        }
    }

    public HashMap<String, List<Long>> getEventIds() {
        if (sharedPreferences.contains("EventIds")) {
            Gson gson = new Gson();
            String eventIdsString = sharedPreferences.getString("EventIds", "DEFAULT");
            Type stringType = new TypeToken<HashMap<String, List<Long>>>(){}.getType();

            return gson.fromJson(eventIdsString, stringType);
        }
        return new HashMap<String, List<Long>>();
    }

    public List<Long> getEventIds(String calName) {
        if (getEventIds().containsKey(calName)) {
            return getEventIds().get(calName);
        }
        return null;
    }

    public String getLastCalName() {
        if (getEventIds() != null && !getEventIds().isEmpty()) {
            HashMap<String, List<Long>> tempMap = getEventIds();
            List<String> keyList = new ArrayList<>(tempMap.keySet());

            String lastCalName = keyList.get(keyList.size()-1);

            return lastCalName;
        }
        return null;
    }

    public List<Shift> getSchedule(String serialSchedule) {
        Gson gson = new Gson();
        Type stringType = new TypeToken<ArrayList<Shift>>(){}.getType();

        return gson.fromJson(serialSchedule, stringType);
    }

    public String getScheduleUpdated() {
        if (sharedPreferences.contains("ScheduleUpdated")) {
            return sharedPreferences.getString("ScheduleUpdated", "DEFAULT");
        }
        return null;
    }

    public Boolean isScheduleUpdated(String time) {
        SimpleDateFormat format = new SimpleDateFormat("'Last refreshed on' E, MMM d yyyy, h:mm aa");
        try {
            Date updatedTime = format.parse(time);
            Long updated = updatedTime.getTime();
            Long current = Calendar.getInstance().getTimeInMillis();

            Long hours = (current - updated) / 3600000;
            if (hours > 24) {
                return false;
            }

        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean scheduleExists() {
        return (sharedPreferences.contains("Schedule") && sharedPreferences.contains("ScheduleUpdated"));
    }

    public void setGetAddress(Boolean bool) {
        sharedPreferences.edit()
                .putBoolean("getAddress", bool)
                .apply();
    }

    public boolean isGetAddress() {
        return sharedPreferences.getBoolean("getAddress", true);
    }
}
