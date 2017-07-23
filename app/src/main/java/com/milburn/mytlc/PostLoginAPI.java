package com.milburn.mytlc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class PostLoginAPI extends AsyncTask<HashMap<String, String>, Integer, Boolean> {

    private Context mContext;
    private ProgressDialog mProgressDialog;
    private Snackbar mSnackBar;
    private HashMap<String, String> loginMap;
    private List<Shift> shiftList = new ArrayList<>();
    private Connection.Response mainResponse;
    private Connection.Response loginResponse;
    private Connection.Response shiftPageNextResponse;
    private Document loginDoc;
    private Document shiftDoc;
    private Document shiftPageNextDoc;
    private String tokenValue;
    private Elements currentDay;
    public AsyncResponse delegate = null;

    public PostLoginAPI(Context context, AsyncResponse asyncResponse) {
        mContext = context;
        delegate = asyncResponse;
    }

    public interface AsyncResponse {
        void processFinish(List<Shift> shiftList);
    }

    @Override
    protected Boolean doInBackground(HashMap<String, String>... params) {
        publishProgress(103);
        loginMap = params[0];
        return tryLogin() && getSchedule();
    }

    private boolean tryLogin() {
        try {
            mainResponse = Jsoup.connect("https://mytlc.bestbuy.com/etm/")
                    .method(Connection.Method.GET)
                    .execute();
            tokenValue = mainResponse.parse().select("input[name=url_login_token").first().attr("value");
        } catch (Exception e) {
            publishProgress(102);
            e.printStackTrace();
            return false;
        }

        try {
            loginResponse = Jsoup.connect("https://mytlc.bestbuy.com/etm/login.jsp")
                    .method(Connection.Method.POST)
                    .maxBodySize(0)
                    .cookies(mainResponse.cookies())
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("cache-control", "no-cache")
                    .data("wbat", mainResponse.header("wbat"))
                    .data("pageAction", "login")
                    .data("url_login_token", tokenValue)
                    .data("login", loginMap.get("Username"))
                    .data("password", loginMap.get("Password"))
                    .data("localeSelected", "false")
                    .data("wbXpos", "-1")
                    .data("wbYpos", "-1")
                    .execute();
            loginDoc = loginResponse.parse();
        } catch (Exception e) {
            publishProgress(102);
            e.printStackTrace();
            return false;
        }

        if (loginDoc.title().contentEquals("Infor HCM Workforce Management - ETM Login")) {
            if (loginDoc.getElementsByClass("errorText").first() != null) {
                publishProgress(101);
                return false;
            }
        } else if (loginDoc.getElementsByTag("font").first() != null && loginDoc.getElementsByTag("font").first().text().contains("updating schedule information")) {
            publishProgress(100);
            return false;
        }

        try {
            String secureToken = loginDoc.select("input[name=secureToken").first().attr("value");
            String newMonthYear = loginDoc.getElementsByClass("imageButton").last().attr("onclick").split("'")[1];
            String lastServerTime = loginDoc.select("input[name=lastServerTime").first().attr("value");
            String currentTime = loginDoc.select("input[name=initialCurrentTime").first().attr("value");

            shiftPageNextResponse = Jsoup.connect("https://mytlc.bestbuy.com/etm/etmMenu.jsp")
                    .method(Connection.Method.POST)
                    .maxBodySize(0)
                    .cookies(loginResponse.cookies())
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("cache-control", "no-cache")
                    .data("wbat", loginResponse.header("wbat"))
                    .data("pageAction", "")
                    .data("clockTimeFormat", "HH:mm")
                    .data("clockTimeInvalidFormat", "Invalid Time Format")
                    .data("lastServerTime", lastServerTime)
                    .data("initialCurrentTime", currentTime)
                    .data("timeToNextDstTransition", "-1")
                    .data("tran", "-1")
                    .data("tzOffset", "-14400000")
                    .data("rawOffset", "-18000000")
                    .data("rightNowInDST", "T")
                    .data("tzDSTObserved", "T")
                    .data("contextPathParam", "")
                    .data("secureToken", secureToken)
                    .data("selectedTocID", "0")
                    .data("parentID", "0")
                    .data("msg", "")
                    .data("pageFullyLoaded", "")
                    .data("pwdExpDays", "NotInit")
                    .data("pwdAlertMsg", "NotInit")
                    .data("NEW_MONTH_YEAR", newMonthYear)
                    .data("wbXpos", "-1")
                    .data("wbYpos", "-1")
                    .execute();
            shiftPageNextDoc = shiftPageNextResponse.parse();
        } catch (Exception e) {
            publishProgress(102);
            e.printStackTrace();
            return false;
        }

        return !shiftPageNextDoc.title().contentEquals("Infor HCM Workforce Management - ETM Login");
    }

    private boolean getSchedule() {
        Element shiftCurrent = loginDoc.getElementsByClass("calendarCellRegularCurrent").first();
        Elements shiftsFuture = loginDoc.getElementsByClass("calendarCellRegularFuture");
        Elements shiftsNextMonth = shiftPageNextDoc.getElementsByClass("calendarCellRegularFuture");

        if (shiftCurrent != null && !shiftCurrent.text().contains("OFF")) {
            shiftsFuture.add(0, shiftCurrent);
        }

        if (shiftsNextMonth != null && !shiftsNextMonth.isEmpty()) {
            shiftsFuture.addAll(shiftsNextMonth);
        }

        if (shiftsFuture != null) {
            for (Element shift : shiftsFuture) {
                String shiftUrl = shift.children().first().attr("onclick");
                shiftUrl = shiftUrl.replace("window.location='", "").replace("';return false", "");
                try {
                    shiftDoc = Jsoup.connect("https://mytlc.bestbuy.com" + shiftUrl)
                            .method(Connection.Method.GET)
                            .maxBodySize(0)
                            .cookies(loginResponse.cookies())
                            .header("cache-control", "no-cache")
                            .get();
                } catch (Exception e) {
                    publishProgress(102);
                    e.printStackTrace();
                    return false;
                }

                setCurrentDay();

                if (currentDay != null) {
                    List<String> parsedDepts = getDepts();
                    List<Date[]> parsedTimeDate = getTimeDate();
                    String storeNumber = currentDay.select("div.calendarTextSchedDtl").first().text().split(",")[3].split("-")[1].replaceFirst("^0+(?!$)", "");
                    shiftList.add(new Shift(parsedTimeDate, parsedDepts, storeNumber));
                }
                publishProgress(shiftList.size(), shiftsFuture.select("table.etmCursor").size());
            }
            return true;
        }
        return false;
    }

    private void setCurrentDay() {
        currentDay = null;
        if (!shiftDoc.getElementsByClass(" calendarColWeekday currentDay").isEmpty()) {
            currentDay = shiftDoc.getElementsByClass(" calendarColWeekday currentDay").first().child(0).child(0).children();
        } else if (!shiftDoc.getElementsByClass(" calendarColWeekend currentDay").isEmpty()) {
            currentDay = shiftDoc.getElementsByClass(" calendarColWeekend currentDay").first().child(0).child(0).children();
        } else if (!shiftDoc.getElementsByClass("calendarCurrentDay calendarColWeekday currentDay").isEmpty()) {
            currentDay = shiftDoc.getElementsByClass("calendarCurrentDay calendarColWeekday currentDay").first().child(0).child(0).children();
        } else if (!shiftDoc.getElementsByClass("calendarCurrentDay calendarColWeekend currentDay").isEmpty()) {
            currentDay = shiftDoc.getElementsByClass("calendarCurrentDay calendarColWeekend currentDay").first().child(0).child(0).children();
        }
    }

    private List<Date[]> getTimeDate() {
        List<Date[]> timesList = new ArrayList<>();
        if (!currentDay.hasClass("calendarTextSchedDtlTime")) {
            String[] dateTimes = currentDay.get(1).text().split(" - ");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HHmmss");
            try {
                Date start = dateFormat.parse(dateTimes[0]);
                Date end = dateFormat.parse(dateTimes[1]);
                timesList.add(new Date[]{start, end});
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            String dateString = currentDay.get(1).text().split(" ")[0];
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddhh:mmaa");

            Elements timeElements = currentDay.select("div.calendarTextSchedDtlTime");
            for (Element time : timeElements) {
                String[] timeOriginal = time.text().split(" - ");
                String time1 = timeOriginal[0].replace("a", "AM").replace("p", "PM");
                String time2 = timeOriginal[1].replace("a", "AM").replace("p", "PM");

                try {
                    Date start = dateFormat.parse(dateString + time1);
                    Date end = dateFormat.parse(dateString + time2);
                    timesList.add(new Date[]{start, end});
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!timesList.isEmpty()) {
            return timesList;
        }
        return null;
    }

    private List<String> getDepts() {
        Elements depts = currentDay.select("div.calendarTextSchedDtl");
        List<String> deptList = new ArrayList<>();
        DepartmentMap deptMap = new DepartmentMap();
        for (Element dept : depts) {
            String[] deptSplit = dept.text().split(",");
            deptSplit = deptSplit[3].split("-");
            String deptNum = deptSplit[2].replace("DEPT", "");
            deptList.add(deptMap.getDeptName(deptNum));
        }
        return deptList;
    }

    private void createSnack(String notice) {
        mSnackBar = Snackbar.make(((Activity) mContext).findViewById(R.id.coordinatorLayout), notice, Snackbar.LENGTH_LONG);
        mSnackBar.show();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        delegate.processFinish(shiftList);
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setCancelable(false);
        }

        switch (progress[0]) {
            case 100:
                mProgressDialog.dismiss();
                createSnack("MyTLC is currently updating. " + loginDoc.getElementsByTag("font").first().text().replace("MyTLC is currently updating schedule information and viewing schedules is unavailable. ", ""));
                break;

            case 101:
                mProgressDialog.dismiss();
                createSnack(loginDoc.getElementsByClass("errorText").first().text());
                break;

            case 102:
                mProgressDialog.dismiss();
                createSnack("Error retrieving schedule");
                break;

            case 103:
                mProgressDialog.setMessage("Authenticating...");
                mProgressDialog.show();
                break;

            default:
                mProgressDialog.setMessage("Parsing shift "+Integer.toString(progress[0])+"/"+Integer.toString(progress[1]));
                if (!progress[0].equals(progress[1])) {
                    mProgressDialog.show();
                } else {
                    mProgressDialog.dismiss();
                }
                break;
        }
    }
}
