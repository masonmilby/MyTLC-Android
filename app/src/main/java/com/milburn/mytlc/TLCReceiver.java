package com.milburn.mytlc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

public class TLCReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PrefManager pm = new PrefManager(context, new PrefManager.onPrefChanged() {
            @Override
            public void prefChanged(SharedPreferences sharedPreferences, String s) {
                //
            }
        });

        Toast toast = Toast.makeText(context, "Received", Toast.LENGTH_LONG);
        toast.show();

        if (intent.getAction() != null && intent.getAction().equals("android.intent.action.BOOT_COMPLETED") && pm.getSyncBackground()) {
            pm.changeAlarm(1);
        } else if (pm.getSyncBackground()) {
            Intent service = new Intent(context, BackgroundSync.class);
            context.startService(service);
        }
    }
}
