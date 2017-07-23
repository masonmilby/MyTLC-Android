package com.milburn.mytlc;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Shift {
    final private List<Date[]> dateList;
    final private List<String> deptList;
    final private String storeNumber;

    public Shift(List<Date[]> dates, List<String> depts, String store) {
        dateList = dates;
        deptList = depts;
        storeNumber = store;
    }

    public Float getTotalHours() {
        Long start = getStartTime().getTime();
        Long end = getEndTime().getTime();
        Float totalHours = (float)(end-start)/3600000;
        return totalHours;
    }

    public String getStartTime(String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        Date firstDate = dateList.get(0)[0];
        return dateFormat.format(firstDate);
    }

    public Date getStartTime() {
        Date firstDate = dateList.get(0)[0];
        return firstDate;
    }

    public String getEndTime(String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        Date lastDate = dateList.get(dateList.size()-1)[1];
        return dateFormat.format(lastDate);
    }

    public Date getEndTime() {
        Date lastDate = dateList.get(dateList.size()-1)[1];
        return lastDate;
    }

    public String getCombinedTime() {
        if ((getStartTime("aa").contains("AM") && getEndTime("aa").contains("AM")
                || (getStartTime("aa").contains("PM") && getEndTime("aa").contains("PM")))) {
            return getStartTime("h:mm") + "–" + getEndTime("h:mm aa");
        }
        return getStartTime("h:mm aa") + "–" + getEndTime("h:mm aa");
    }

    public String getCombinedTime(Integer index) {
        if ((getDeptSize()-1) < index) {
            return null;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm aa");
        return dateFormat.format(getDate(index)[0]) + "–" + dateFormat.format(getDate(index)[1]);
    }

    public String getDept(Integer index) {
        if ((getDeptSize()-1) < index) {
            return null;
        }
        return deptList.get(index);
    }

    public List<String> getDepts() {
        return deptList;
    }

    public Boolean getDeptDiff() {
        List<String> stringList = new ArrayList<>();
        for (String dept : deptList) {
            if (!dept.contentEquals(deptList.get(0))) {
                stringList.add(dept);
            }
        }
        return stringList.size() == deptList.size();
    }

    public String getCombinedDepts() {
        if (getDeptDiff()) {
            return String.valueOf(getDepts()).replace("[", "").replace("]", "");
        }
        return getDepts().get(0);
    }

    public Integer getDeptSize() {
        return deptList.size();
    }

    public List<Date[]> getDates() {
        return dateList;
    }

    public Date[] getDate(Integer index) {
        if ((getDeptSize()-1) < index) {
            return null;
        }
        return dateList.get(index);
    }

    public Date getSingleDayDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String parsedDate = dateFormat.format(getDate(0)[0]);
        Date newDate = new Date();
        try {
            newDate = dateFormat.parse(parsedDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return newDate;
    }

    public Boolean getScheduledToday() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String parsedDate = dateFormat.format(getDate(0)[0]);
        String currentDate = dateFormat.format(Calendar.getInstance().getTime());

        if (parsedDate.equals(currentDate)) {
            return true;
        }
        return false;
    }

    public String getStoreNumber() {
        return storeNumber;
    }
}