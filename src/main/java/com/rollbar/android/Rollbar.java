package com.rollbar.android;

import android.content.Context;
import android.util.Log;

public class Rollbar {
    public static final String TAG = "Rollbar";

    private static Notifier notifier;

    public static void init(Context context, String accessToken, String environment) {
        notifier = new Notifier(context, accessToken, environment);
    }

    public static void reportException(final Throwable throwable, final String level) {
        ensureInit(new Runnable() {
            public void run() {
                notifier.reportException(throwable, level);
            }
        });
    }

    public static void reportException(Throwable throwable) {
        reportException(throwable, "error");
    }

    public static void reportMessage(final String message, final String level) {
        ensureInit(new Runnable() {
            public void run() {
                notifier.reportMessage(message, level);
            }
        });
    }

    public static void reportMessage(String message) {
        reportMessage(message, "error");
    }
    
    public static void setEndpoint(final String endpoint) {
        ensureInit(new Runnable() {
            public void run() {
                notifier.setEndpoint(endpoint);
            }
        });
    }
    
    public static void setReportUncaughtExceptions(final boolean report) {
        ensureInit(new Runnable() {
            public void run() {
                notifier.setReportUncaughtExceptions(report);
            }
        });
    }
    
    private static void ensureInit(Runnable runnable) {
        if (notifier == null) {
            Log.e(TAG, "Rollbar not initialized with an access token!");
        } else {
            try {
                runnable.run();
            } catch (Exception e) {
                Log.e(TAG, "Exception when interacting with Rollbar", e);
            }
        }
    }
}
