package com.milburn.mytlc;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

public class BackgroundSync extends IntentService {

    public BackgroundSync() {
        super("Sync");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        //
    }
}
