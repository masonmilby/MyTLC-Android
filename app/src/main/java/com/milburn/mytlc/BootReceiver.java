package com.milburn.mytlc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PrefManager pm = new PrefManager(context, new PrefManager.onPrefChanged() {
            @Override
            public void prefChanged(SharedPreferences sharedPreferences, String s) {
                //
            }
        });

        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") && pm.getSyncBackground()) {
            pm.changeAlarm(1);
        }
    }
}
