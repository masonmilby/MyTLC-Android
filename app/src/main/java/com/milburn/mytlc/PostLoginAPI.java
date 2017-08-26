package com.milburn.mytlc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;

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
    private ProgressDialog mProgressAlert = null;
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
    public FirebaseAnalytics firebaseAnalytics;

    private Boolean errorStatus = true;
    private List<String[]> htmlList = new ArrayList<>();

    public PostLoginAPI(Context context, AsyncResponse asyncResponse) {
        mContext = context;
        delegate = asyncResponse;
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);
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
            tokenValue = mainResponse.parse().select("input[name=url_login_token]").first().attr("value");
            FirebaseCrash.log("MyTLC loaded");
            htmlList.add(new String[]{"Main.html", mainResponse.parse().toString()});
        } catch (Exception e) {
            FirebaseCrash.log("MyTLC loading failed");
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
            htmlList.add(new String[]{"Login.html", loginDoc.toString()});
        } catch (Exception e) {
            FirebaseCrash.log("Login failed");
            publishProgress(102);
            e.printStackTrace();
            return false;
        }

        if (loginDoc.title().contentEquals("Infor HCM Workforce Management - ETM Login")) {
            if (loginDoc.getElementsByClass("errorText").first() != null) {
                FirebaseCrash.log("Login failed: " + loginDoc.getElementsByClass("errorText").first().text());
                publishProgress(101);
                return false;
            }
        } else if (loginDoc.getElementsByTag("font").first() != null && loginDoc.getElementsByTag("font").first().text().contains("updating schedule information")) {
            FirebaseCrash.log("Login failed: updating schedule");
            publishProgress(100);
            return false;
        }
        FirebaseCrash.log("Login completed");

        try {
            String secureToken = loginDoc.select("input[name=secureToken]").first().attr("value");
            String newMonthYear = loginDoc.getElementsByClass("imageButton").last().attr("onclick").split("'")[1];
            String lastServerTime = loginDoc.select("input[name=lastServerTime]").first().attr("value");
            String currentTime = loginDoc.select("input[name=initialCurrentTime]").first().attr("value");

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
            htmlList.add(new String[]{"ShiftNextPage.html", shiftPageNextDoc.toString()});
        } catch (Exception e) {
            FirebaseCrash.log("Next page loading failed");
            FirebaseCrash.report(e);
            publishProgress(102);
            e.printStackTrace();
            return false;
        }
        FirebaseCrash.log("Next page loaded");

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
            int i = -1;
            for (Element shift : shiftsFuture) {
                i++;
                String shiftUrl = shift.children().first().attr("onclick");
                shiftUrl = shiftUrl.replace("window.location='", "").replace("';return false", "");
                try {
                    shiftDoc = Jsoup.connect("https://mytlc.bestbuy.com" + shiftUrl)
                            .method(Connection.Method.GET)
                            .maxBodySize(0)
                            .cookies(loginResponse.cookies())
                            .header("cache-control", "no-cache")
                            .get();
                    FirebaseCrash.log("Shift" + i + " loaded");
                    htmlList.add(new String[]{"Shift" + i + ".html", shiftDoc.toString()});
                } catch (Exception e) {
                    FirebaseCrash.log("Shift" + i + " load failed");
                    FirebaseCrash.report(e);
                    publishProgress(102);
                    e.printStackTrace();
                    return false;
                }

                setCurrentDay();

                if (currentDay != null) {
                    List<List<String>> deptActList = getDeptsActivities();
                    List<String> parsedDepts = deptActList.get(0);
                    List<Date[]> parsedTimeDate = getTimeDate();
                    String storeNumber = currentDay.select("div.calendarTextSchedDtl").first().text().split(",")[3].split("-")[1].replaceFirst("^0+(?!$)", "");
                    List<String> activityList = deptActList.get(1);
                    if (deptActList != null && parsedDepts != null && parsedTimeDate != null && storeNumber != null && activityList != null) {
                        shiftList.add(new Shift(parsedTimeDate, parsedDepts, storeNumber, activityList));
                        FirebaseCrash.log("Shift" + i + " added");
                    } else {
                        FirebaseCrash.log("Shift" + i + " failed to add");
                        publishProgress(102);
                        return false;
                    }
                }
                publishProgress(shiftList.size(), shiftsFuture.select("table.etmCursor").size());
            }
            return true;
        }
        FirebaseCrash.log("Shift list null");
        publishProgress(102);
        return false;
    }

    private void setCurrentDay() {
        currentDay = null;
        if (!shiftDoc.getElementsByClass(" calendarColWeekday currentDay").isEmpty()) {
            currentDay = shiftDoc.getElementsByClass(" calendarColWeekday currentDay").first().children().select("div.calendarShift").first().children();
        } else if (!shiftDoc.getElementsByClass(" calendarColWeekend currentDay").isEmpty()) {
            currentDay = shiftDoc.getElementsByClass(" calendarColWeekend currentDay").first().children().select("div.calendarShift").first().children();
        } else if (!shiftDoc.getElementsByClass("calendarCurrentDay calendarColWeekday currentDay").isEmpty()) {
            currentDay = shiftDoc.getElementsByClass("calendarCurrentDay calendarColWeekday currentDay").first().children().select("div.calendarShift").first().children();
        } else if (!shiftDoc.getElementsByClass("calendarCurrentDay calendarColWeekend currentDay").isEmpty()) {
            currentDay = shiftDoc.getElementsByClass("calendarCurrentDay calendarColWeekend currentDay").first().children().select("div.calendarShift").first().children();
        }
    }

    private List<Date[]> getTimeDate() {
        List<Date[]> timesList = new ArrayList<>();
        if (!currentDay.hasClass("calendarTextSchedDtlTime")) {
            String[] dateTimes = currentDay.select("div.calendarTextShiftTime").text().split(" - ");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HHmmss");
            try {
                Date start = dateFormat.parse(dateTimes[0]);
                Date end = dateFormat.parse(dateTimes[1]);
                timesList.add(new Date[]{start, end});
            } catch (ParseException e) {
                FirebaseCrash.log("Get times failed");
                FirebaseCrash.report(e);
                e.printStackTrace();
                return null;
            }
        } else {
            String dateString = currentDay.select("div.calendarTextShiftTime").text().split(" ")[0];
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
                    FirebaseCrash.log("Get times failed");
                    FirebaseCrash.report(e);
                    e.printStackTrace();
                    return null;
                }
            }
        }
        if (!timesList.isEmpty()) {
            FirebaseCrash.log("Get times completed");
            return timesList;
        }
        return null;
    }

    private List<List<String>> getDeptsActivities() {
        Elements depts = currentDay.select("div.calendarTextSchedDtl");
        List<String> deptList = new ArrayList<>();
        List<String> actList = new ArrayList<>();
        DepartmentMap deptMap = new DepartmentMap();
        for (Element dept : depts) {
            String[] deptSplit = dept.text().split(",");
            actList.add(deptSplit[4]);
            deptSplit = deptSplit[3].split("-");
            String deptNum = deptSplit[2].replace("DEPT", "");
            deptList.add(deptMap.getDeptName(deptNum));
        }

        List<List<String>> finalList = new ArrayList<>();
        finalList.add(deptList);
        finalList.add(actList);

        return finalList;
    }

    private void createSnack(String notice) {
        mSnackBar = Snackbar.make(((Activity) mContext).findViewById(R.id.coordinatorLayout), notice, Snackbar.LENGTH_LONG);
        mSnackBar.show();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        FirebaseHelper firebaseHelper = new FirebaseHelper(mContext);
        if (firebaseHelper.getReport() && shiftList != null && !shiftList.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString("UUID", firebaseHelper.getUUID());
            firebaseAnalytics.logEvent("report_issue", bundle);
            firebaseHelper.sendIssue(htmlList, shiftList);
        }
        firebaseHelper.setReport(false);

        if (!errorStatus) {
            delegate.processFinish(shiftList);
        } else {
            delegate.processFinish(new ArrayList<Shift>());
        }
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
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }

                if (mProgressAlert == null) {
                    mProgressAlert = new ProgressDialog(mContext);
                    mProgressAlert.setIndeterminate(false);
                    mProgressAlert.setCancelable(false);
                    mProgressAlert.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    mProgressAlert.setTitle("Parsing shifts");
                }

                if (!progress[0].equals(progress[1])) {
                    mProgressAlert.setMax(progress[1]);
                    mProgressAlert.setProgress(progress[0]);
                    mProgressAlert.show();
                } else {
                    mProgressAlert.dismiss();
                    errorStatus = false;
                }
                break;
        }
    }
}
