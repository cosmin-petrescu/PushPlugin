package com.innobyte.push;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Cosmin Petrescu on 04/03/15.
 *
 * Copyright 2015 www.innobyte.com
 */
public class PushIntentService extends IntentService {

    /**
     * The TAG of the class
     */
    protected static final String TAG = "PushIntentService";

    /**
     * Constructor with name
     *
     * @param name The name of the service
     */
    public PushIntentService(String name) {
        super(name);
    }

    /**
     * Simple constructor
     */
    public PushIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent - intent: " + intent);

        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        String messageType = gcm.getMessageType(intent);

        if (null != extras
            && !extras.isEmpty()
            && messageType.equals(GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE)
        ) {
        	// if we are in the foreground, just surface the payload, else post it to the status bar
            if (PushPlugin.isInForeground()) {
        	    extras.putBoolean("foreground", true);
                PushPlugin.sendExtras(extras);
        	} else {
        		extras.putBoolean("foreground", false);

                // Send a notification if there is a message
                if (extras.getString("message") != null && extras.getString("message").length() != 0) {
                    createNotification(this.getApplicationContext(), extras);
                }
            }
        }

        PushBroadcastReceiver.completeWakefulIntent(intent);
    }

    public void createNotification(Context context, Bundle extras)
    {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String appName = getAppName(context);

    	Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
    	notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	notificationIntent.putExtra("pushBundle", extras);

    	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    	int defaults = Notification.DEFAULT_ALL;

    	if (extras.getString("defaults") != null) {
    		try {
    			defaults = Integer.parseInt(extras.getString("defaults"));
    		} catch (NumberFormatException e) {}
    	}

    	NotificationCompat.Builder mBuilder =
    		new NotificationCompat.Builder(context)
    			.setDefaults(defaults)
    			.setSmallIcon(context.getApplicationInfo().icon)
    			.setWhen(System.currentTimeMillis())
    			.setContentTitle(extras.getString("title"))
    			.setTicker(extras.getString("title"))
    			.setContentIntent(contentIntent)
    			.setAutoCancel(true);

    	String message = extras.getString("message");
    	if (message != null) {
    		mBuilder.setContentText(message);
    	} else {
    		mBuilder.setContentText("<missing message content>");
    	}

    	String msgcnt = extras.getString("msgcnt");
    	if (msgcnt != null) {
    		mBuilder.setNumber(Integer.parseInt(msgcnt));
    	}

    	int notId = 0;

    	try {
    		notId = Integer.parseInt(extras.getString("notId"));
    	} catch(NumberFormatException e) {
    		Log.e(TAG, "Number format exception - Error parsing Notification ID: " + e.getMessage());
    	} catch(Exception e) {
    		Log.e(TAG, "Number format exception - Error parsing Notification ID" + e.getMessage());
    	}

    	mNotificationManager.notify((String) appName, notId, mBuilder.build());
    }

    private static String getAppName(Context context)
    {
    	CharSequence appName = context
    	    .getPackageManager()
    	    .getApplicationLabel(context.getApplicationInfo());

    	return (String)appName;
    }
}
