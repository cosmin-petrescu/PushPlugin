package com.innobyte.push;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by Cosmin Petrescu on 04/03/15.
 *
 * Copyright 2015 www.innobyte.com
 */
public class PushHandlerActivity extends Activity
{
    private static String TAG = "PushHandlerActivity";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");

        boolean isPushPluginActive = PushPlugin.isActive();
        processPushBundle(isPushPluginActive);

        finish();

        if (!isPushPluginActive) {
            forceMainActivityReload();
        }
    }

	/**
	 * Takes the pushBundle extras from the intent,
	 * and sends it through to the PushPlugin for processing.
	 */
	private void processPushBundle(boolean isPushPluginActive)
	{
		Bundle extras = getIntent().getExtras();

		if (extras != null)	{
			Bundle originalExtras = extras.getBundle("pushBundle");

            originalExtras.putBoolean("foreground", false);
            originalExtras.putBoolean("coldstart", !isPushPluginActive);

			PushPlugin.sendExtras(originalExtras);
		}
	}

	/**
	 * Forces the main activity to re-launch if it's unloaded.
	 */
	private void forceMainActivityReload()
	{
		PackageManager pm = getPackageManager();
		Intent launchIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
		startActivity(launchIntent);
	}

    @Override
    protected void onResume() {
        super.onResume();

        final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }
}
