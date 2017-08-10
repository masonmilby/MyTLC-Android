package com.milburn.mytlc;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {

    SharedPreferences sharedPref;

    public PrefManager(Context con) {
        sharedPref = android.preference.PreferenceManager.getDefaultSharedPreferences(con);
    }

    public Integer getTheme() {
        switch (getThemeName()) {
            case "Light":
                return R.style.CustomLight;

            case "Dark":
                return R.style.CustomDark;

            default:
                return R.style.CustomLight;
        }
    }

    public String getThemeName() {
        return sharedPref.getString("base_theme", "");
    }
}
