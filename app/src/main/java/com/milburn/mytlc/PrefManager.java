package com.milburn.mytlc;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.ColorInt;
import android.util.TypedValue;
import android.view.View;

public class PrefManager implements SharedPreferences.OnSharedPreferenceChangeListener{

    private SharedPreferences sharedPref;
    private Context con;
    public onPrefChanged changeInterface;

    public String key_pay;
    public String key_tax;
    public String key_base;
    public String key_custom;
    public String key_primary;
    public String key_accent;
    public String key_past;
    public String key_display;

    public PrefManager(Context context, onPrefChanged onChanged) {
        changeInterface = onChanged;
        con = context;
        sharedPref = android.preference.PreferenceManager.getDefaultSharedPreferences(con);
        sharedPref.registerOnSharedPreferenceChangeListener(this);

        key_pay = "pay";
        key_tax = "tax";
        key_base = "base_theme";
        key_custom = "custom_colors";
        key_primary = "primaryColor";
        key_accent = "accentColor";
        key_past = "past_shifts";
        key_display = "display_past";
    }

    public interface onPrefChanged {
        void prefChanged(SharedPreferences sharedPreferences, String s);
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

    public void setTheme(String primary, String accent) {
        sharedPref.edit()
                .putString(key_primary, primary)
                .putString(key_accent, accent)
                .apply();
    }

    public Integer getColorFromAttribute(Integer attr) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = con.getTheme();
        theme.resolveAttribute(attr, typedValue, true);
        @ColorInt int color = typedValue.data;
        return color;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        changeInterface.prefChanged(sharedPreferences, s);
    }

    public Boolean isCriticalAttr(String s) {
        return s.contentEquals(key_base) || s.contentEquals(key_primary) || s.contentEquals(key_accent) || s.contentEquals(key_pay) || s.contentEquals(key_tax);
    }

    public Boolean isThemeChange(String s) {
        return s.contentEquals(key_base) || s.contentEquals(key_primary) || s.contentEquals(key_accent);
    }

    public String getPay() {
        return sharedPref.getString(key_pay, "0");
    }

    public String getTax() {
        return sharedPref.getString(key_tax, "0");
    }

    public String getBase() {
        return sharedPref.getString(key_base, "Light");
    }

    public String getPrimary() {
        return sharedPref.getString(key_primary, "Amber");
    }

    public String getAccent() {
        return sharedPref.getString(key_accent, "Grey");
    }

    public Boolean getPast() {
        return sharedPref.getBoolean(key_past, false);
    }

    public void setPast(Boolean bool) {
        sharedPref.edit()
                .putBoolean(key_past, bool)
                .apply();
    }

    public Boolean getDisplay() {
        return sharedPref.getBoolean(key_display, false);
    }

    public void setDisplay(Boolean bool) {
        sharedPref.edit()
                .putBoolean(key_display, bool)
                .apply();
    }
}