package com.milburn.mytlc;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class SettingsFragment extends PreferenceFragment {

    private PrefManager pm;
    private SharedPreferences sharedPref;

    private View primary = null;
    private View accent = null;
    private LinearLayout layoutPrimary;
    private LinearLayout layoutAccent;
    private Credentials credentials;
    private CheckBoxPreference checkPref;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        credentials = new Credentials(getActivity());

        pm = new PrefManager(getActivity(), new PrefManager.onPrefChanged() {
            @Override
            public void prefChanged(SharedPreferences sharedPreferences, String s) {
                setSummary();
            }
        });
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (!sharedPref.contains(pm.key_primary) && !sharedPref.contains(pm.key_accent)) {
            pm.setTheme("Amber", "Grey");
        }

        checkPref = (CheckBoxPreference) findPreference(pm.key_past);
        setSummary();

        findPreference(pm.key_custom).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showColorPicker();
                return false;
            }
        });

        findPreference(pm.key_past).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!checkPref.isChecked() && sharedPref.contains("PastSchedule")) {
                    showConfirmation();
                }
                return false;
            }
        });
    }

    private void showColorPicker() {
        View v  = getActivity().getLayoutInflater().inflate(R.layout.dialog_color, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v);
        builder.setPositiveButton("Select", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (primary != null && accent != null) {
                    pm.setTheme(primary, accent);
                }
            }
        });
        builder.create();
        builder.show();

        layoutPrimary = v.findViewById(R.id.primaryColors);
        layoutAccent = v.findViewById(R.id.accentColors);

        for (int i = 0; i < layoutPrimary.getChildCount(); i++) {
            View pv = layoutPrimary.getChildAt(i);
            if (getSavedColors()[0].equals(pv.getId())) {
                changeSelectedColor(pv);
            }
            pv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    changeSelectedColor(view);
                }
            });
        }

        for (int i = 0; i < layoutAccent.getChildCount(); i++) {
            View av = layoutAccent.getChildAt(i);
            if (getSavedColors()[1].equals(av.getId())) {
                changeSelectedColor(av);
            }
            av.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    changeSelectedColor(view);
                }
            });
        }
    }

    private void setSummary() {
        findPreference(pm.key_pay).setSummary("$" + pm.getPay());
        findPreference(pm.key_tax).setSummary(pm.getTax() + "%");
        findPreference(pm.key_base).setSummary(pm.getBase());
    }

    private void changeSelectedColor(View view) {
        int height_selected = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());
        int height_normal = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
        ViewGroup.LayoutParams params;
        LinearLayout layout = (LinearLayout) view.getParent();
        if (layout.getId() == R.id.primaryColors) {
            if (primary != null) {
                params = primary.getLayoutParams();
                params.height = height_normal;
                primary.setLayoutParams(params);
            }
            primary = view;
            params = primary.getLayoutParams();
            params.height = height_selected;
            primary.setLayoutParams(params);
        } else if (layout.getId() == R.id.accentColors) {
            if (accent != null) {
                params = accent.getLayoutParams();
                params.height = height_normal;
                accent.setLayoutParams(params);
            }
            accent = view;
            params = accent.getLayoutParams();
            params.height = height_selected;
            accent.setLayoutParams(params);
        }
    }

    private Integer[] getSavedColors() {
        Integer primaryId = getActivity().getResources().getIdentifier("id/" + pm.getPrimary(), null, getActivity().getPackageName());
        Integer accentId = getActivity().getResources().getIdentifier("id/" + pm.getAccent() + "_A", null, getActivity().getPackageName());

        return new Integer[]{primaryId, accentId};
    }

    private void showConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Are you sure?");
        builder.setMessage("Disabling past shift saving will clear all currently stored past shifts.");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                credentials.clearPastSchedule();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                pm.setPast(true);
                checkPref.setChecked(true);
            }
        });
        builder.create();
        builder.show();
    }
}