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

        Preference prefPay = findPreference(pay);
        prefPay.setSummary("$" + sharedPref.getString(pay, ""));
        Preference prefTax = findPreference(tax);
        prefTax.setSummary(sharedPref.getString(tax, "") + "%");
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
        }
    }
}