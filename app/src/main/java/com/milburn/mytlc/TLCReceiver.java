package com.milburn.mytlc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
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
        Credentials credentials = new Credentials(context);

        if (credentials.credsExist() && intent.getAction() != null && intent.getAction().equals("android.intent.action.BOOT_COMPLETED") && pm.getSyncBackground()) {
            pm.changeAlarm(1);
        } else if (credentials.credsExist() && pm.getSyncBackground()) {
            Intent service = new Intent(context, BackgroundSync.class);

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                context.startForegroundService(service);
            } else{
                context.startService(service);
            }
        }
    }
}
