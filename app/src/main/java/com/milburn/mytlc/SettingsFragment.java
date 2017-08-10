package com.milburn.mytlc;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String pay = "edit_hourlypay";
        String tax = "edit_tax";
        String base = "base_theme";
        String custom = "custom_colors";

        Preference prefPay = findPreference(pay);
        prefPay.setSummary("$" + sharedPref.getString(pay, ""));
        Preference prefTax = findPreference(tax);
        prefTax.setSummary(sharedPref.getString(tax, "") + "%");
        Preference prefBase = findPreference(base);
        prefBase.setSummary(sharedPref.getString(base, ""));

        Preference prefCustom = findPreference(custom);
        prefCustom.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                System.out.println("Yep, works");
                return false;
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Preference preference = findPreference(s);
        switch (s) {
            case "edit_hourlypay":
                preference.setSummary("$" + sharedPreferences.getString(s, ""));
                break;

            case "edit_tax":
                preference.setSummary(sharedPreferences.getString(s, "") + "%");
                break;

            case "base_theme":
                preference.setSummary(sharedPreferences.getString(s, ""));
                break;
        }
    }
}