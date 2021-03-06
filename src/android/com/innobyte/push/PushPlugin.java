package com.innobyte.push;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gcm.GCMRegistrar;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import java.io.IOException;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * Created by Cosmin Petrescu on 04/03/15.
 *
 * Copyright 2015 www.innobyte.com
 */
public class PushPlugin extends CordovaPlugin {
	public static final String TAG = "PushPlugin";

	public static final String REGISTER = "register";
	public static final String UNREGISTER = "unregister";
	public static final String EXIT = "exit";

	private static CordovaWebView gWebView;
	private static String gECB;
	private static String gSenderID;
	private static Bundle gCachedExtras = null;
    private static boolean gForeground = false;

    /**
     * The Google Cloud Messaging API
     */
    protected GoogleCloudMessaging googleCloudMessaging = null;

	/**
	 * Gets the application context from cordova 's main activity.
	 * @return the application context
	 */
	private Context getApplicationContext() {
		return this.cordova.getActivity().getApplicationContext();
	}

	@Override
	public boolean execute(final String action, final JSONArray data, final CallbackContext callbackContext) {
		boolean result = true;

		Log.v(TAG, "execute: action=" + action);

		if (REGISTER.equals(action)) {

		    gWebView = this.webView;

		    cordova.getThreadPool().execute(new Runnable() {
                public void run() {

        			Log.v(TAG, "execute: data=" + data.toString());

                    try {
                        JSONObject jo = data.getJSONObject(0);

                        Log.v(TAG, "execute: jo=" + jo.toString());

                        gECB = (String) jo.get("ecb");
                        gSenderID = (String) jo.get("senderID");

                        if (googleCloudMessaging == null) {
                            googleCloudMessaging = GoogleCloudMessaging.getInstance(getApplicationContext());
                        }

                        Log.v(TAG, "execute: ECB=" + gECB + " senderID=" + gSenderID);

                        String registrationId = googleCloudMessaging.register(gSenderID);

                        Log.v(TAG, "execute: registrationId=" + data.toString());

                        cordova.getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                callbackContext.success();
                            }
                        });

                        // wait for 100 ms
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // send registered
			            JSONObject registerJson = new JSONObject().put("event", "registered");
                        registerJson.put("regid", registrationId);

                        Log.v(TAG, "onRegistered: " + registerJson.toString());

                        // Send this JSON data to the JavaScript application above EVENT should be set to the msg type
                        // In this case this is the registration ID
                        sendJavascript(registerJson);

                    } catch (IOException e) {
                        Log.e(TAG, "execute: Got IO Exception " + e.getMessage());
                        final String exceptionMessage = e.getMessage();

                        cordova.getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                callbackContext.error(exceptionMessage);
                            }
                        });
                    } catch (JSONException e) {
                        Log.e(TAG, "execute: Got JSON Exception " + e.getMessage());
                        final String exceptionMessage = e.getMessage();

                        cordova.getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                callbackContext.error(exceptionMessage);
                            }
                        });
                    }

                    if ( gCachedExtras != null) {
                        Log.v(TAG, "sending cached extras");
                        sendExtras(gCachedExtras);
                        gCachedExtras = null;
                    }
			    }
            });

		} else if (UNREGISTER.equals(action)) {

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        if (googleCloudMessaging == null) {
                            googleCloudMessaging = GoogleCloudMessaging.getInstance(getApplicationContext());
                        }

                        googleCloudMessaging.unregister();
                    } catch (IOException e) {
                        Log.e(TAG, "execute: Got IO Exception " + e.getMessage());
                        final String exceptionMessage = e.getMessage();

                        cordova.getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                callbackContext.error(exceptionMessage);
                            }
                        });
                    }

                    Log.v(TAG, "UNREGISTER");
                    cordova.getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            callbackContext.success();
                        }
                    });
                }
            });

		} else {
			result = false;
			Log.e(TAG, "Invalid action : " + action);
			callbackContext.error("Invalid action : " + action);
		}

		return result;
	}

	/*
	 * Sends a json object to the client as parameter to a method which is defined in gECB.
	 */
	public static void sendJavascript(JSONObject _json) {
		String _d = "javascript:" + gECB + "(" + _json.toString() + ")";
		Log.v(TAG, "sendJavascript: " + _d);

		if (gECB != null && gWebView != null) {
			gWebView.sendJavascript(_d);
		}
	}

	/*
	 * Sends the push bundle extras to the client application.
	 * If the client application isn't currently active, it is cached for later processing.
	 */
	public static void sendExtras(Bundle extras)
	{
		if (extras != null) {
			if (gECB != null && gWebView != null) {
				sendJavascript(convertBundleToJson(extras));
			} else {
				Log.v(TAG, "sendExtras: caching extras to send at a later time.");
				gCachedExtras = extras;
			}
		}
	}

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        gForeground = true;
    }

	@Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        gForeground = false;
        final NotificationManager notificationManager = (NotificationManager) cordova.getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        gForeground = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        gForeground = false;
		gECB = null;
		gWebView = null;
    }

    /*
     * serializes a bundle to JSON.
     */
    private static JSONObject convertBundleToJson(Bundle extras)
    {
		try
		{
		    int foreground = 0;
			JSONObject json;
			json = new JSONObject().put("event", "message");

			JSONObject jsondata = new JSONObject();
			Iterator<String> it = extras.keySet().iterator();
			while (it.hasNext())
			{
				String key = it.next();
				Object value = extras.get(key);

				// System data from Android
				if (key.equals("from") || key.equals("collapse_key"))
				{
					json.put(key, value);
				}
				else if (key.equals("foreground"))
				{
				    foreground = extras.getInt("foreground");
					json.put(key, foreground);
				}
				else if (key.equals("coldstart"))
				{
					json.put(key, extras.getBoolean("coldstart"));
				}
				else
				{
					// Maintain backwards compatibility
					if (key.equals("message") || key.equals("msgcnt") || key.equals("soundname"))
					{
						json.put(key, value);
					}

					if ( value instanceof String ) {
					// Try to figure out if the value is another JSON object

						String strValue = (String)value;
						if (strValue.startsWith("{")) {
							try {
								JSONObject json2 = new JSONObject(strValue);
								jsondata.put(key, json2);
							}
							catch (Exception e) {
								jsondata.put(key, value);
							}
							// Try to figure out if the value is another JSON array
						}
						else if (strValue.startsWith("["))
						{
							try
							{
								JSONArray json2 = new JSONArray(strValue);
								jsondata.put(key, json2);
							}
							catch (Exception e)
							{
								jsondata.put(key, value);
							}
						}
						else
						{
							jsondata.put(key, value);
						}
					}
				}
			} // while
			jsondata.put("foreground", foreground);
			json.put("payload", jsondata);

			Log.v(TAG, "extrasToJSON: " + json.toString());

			return json;
		}
		catch( JSONException e)
		{
			Log.e(TAG, "extrasToJSON: JSON exception");
		}
		return null;
    }

    public static boolean isInForeground()
    {
        return gForeground;
    }

    public static boolean isActive()
    {
    	return gWebView != null;
    }
}
