package com.innobyte.push;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * Created by Cosmin Petrescu on 04/03/15.
 *
 * Copyright 2015 www.innobyte.com
 */
public class PushBroadcastReceiver extends WakefulBroadcastReceiver {

    /**
     * The TAG of the class
     */
    protected static final String TAG = "PushBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "PushBroadcastReceiver->onReceive");

        ComponentName comp = new ComponentName(
                context.getPackageName(),
                PushIntentService.class.getName()
        );

        startWakefulService(context, intent.setComponent(comp));
        setResultCode(Activity.RESULT_OK);
    }
}
