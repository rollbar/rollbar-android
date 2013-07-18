package main.java.com.rollbar.android;

import java.util.HashMap;

import android.util.Log;

public class Rollbar {
	public static final String TAG = "Rollbar";
	
	private static Notifier notifier;
	
	public static void init(String accessToken, String environment) {
		notifier = new Notifier(accessToken, environment, null);
	}

	public static void init(String accessToken, String environment, HashMap<String, Object> config) {
		notifier = new Notifier(accessToken, environment, config);
	}
	
	public static void reportException(Throwable throwable, String level) {
		if (notifier == null) {
			Log.e(TAG, "Rollber not initialized with an access token!");
		} else {
			try {
				notifier.reportException(throwable, level);
			} catch (Exception e) {
				Log.e(TAG, "Exception when trying to report exception to Rollbar: " + e.toString());
			}
		}
	}
	
	public static void reportException(Throwable throwable) {
		reportException(throwable, "error");
	}
	
	public static void reportMessage(String message, String level) {
		if (notifier == null) {
			Log.e(TAG, "Rollber not initialized with an access token!");
		} else {
			try {
				notifier.reportMessage(message, level);
			} catch (Exception e) {
				Log.e(TAG, "Exception when trying to report message to Rollbar: " + e.toString());
			}
		}
	}
	
	public static void reportMessage(String message) {
		reportMessage(message, "error");
	}
}
