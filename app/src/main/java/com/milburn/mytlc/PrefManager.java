package com.milburn.mytlc;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.util.TypedValue;
import android.view.View;

public class PrefManager {

    private SharedPreferences sharedPref;
    private Context con;

    public String key_pay;
    public String key_tax;
    public String key_base;
    public String key_custom;
    public String key_primary;
    public String key_accent;

    public PrefManager(Context context) {
        con = context;
        sharedPref = android.preference.PreferenceManager.getDefaultSharedPreferences(con);

        key_pay = "pay";
        key_tax = "tax";
        key_base = "base_theme";
        key_custom = "custom_colors";
        key_primary = "primaryColor";
        key_accent = "accentColor";
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

    public Integer getColorFromAttribute(Integer attr) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = con.getTheme();
        theme.resolveAttribute(attr, typedValue, true);
        @ColorInt int color = typedValue.data;
        return color;
    }
}