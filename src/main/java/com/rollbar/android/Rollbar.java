package com.rollbar.android;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.util.IllegalFormatCodePointException;
import java.util.Map;

public class Rollbar {
    public static final String TAG = "Rollbar";

    private static Notifier notifier;

    // prevent user pointless object initialization
    private Rollbar() {
        throw new IllegalStateException("This class has static initializer and can't be initialized using this constructor!");
    }

    public static void init(Context context, String accessToken, String environment) {
        init(context, null, accessToken, environment, true);
    }

    public static void init(Context context, String rollbarThreadName, String accessToken, String environment) {
        init(context, rollbarThreadName, accessToken, environment, true);
    }

    public static void init(Context context, String accessToken, String environment,  boolean registerExceptionHandle) {
        init(context, null, accessToken, environment, registerExceptionHandle);
    }

    public static void init(Context context, String rollbarThreadName, String accessToken, String environment, boolean registerExceptionHandler) {
        if (isInit()) {
            Log.w(TAG, "Rollbar.init() called when it was already initialized.");
        } else {
            notifier = new Notifier(context, rollbarThreadName, accessToken, environment, registerExceptionHandler);
        }
    }

    public static boolean isInit() {
        return notifier != null;
    }

    public static void reportException(final Throwable throwable, final String level, final String description) {
        ensureInit(new Runnable() {
            public void run() {
                notifier.reportException(throwable, level, description);
            }
        });
    }

    public static void reportException(final Throwable throwable, final String level, final String description, final Map<String, String> params) {
        ensureInit(new Runnable() {
            public void run() {
                notifier.reportException(throwable, level, description, params);
            }
        });
    }

    public static void reportException(final Throwable throwable, final String level) {
        reportException(throwable, level, null);
    }

    public static void reportException(Throwable throwable) {
        reportException(throwable, null, null);
    }

    public static void reportMessage(final String message, final String level) {
        ensureInit(new Runnable() {
            public void run() {
                notifier.reportMessage(message, level);
            }
        });
    }

    public static void reportMessage(final String message, final String level, final Map<String, String> params) {
        ensureInit(new Runnable() {
            public void run() {
                notifier.reportMessage(message, level, params);
            }
        });
    }

    public static void reportMessage(String message) {
        reportMessage(message, "info");
    }
    
    public static void setPersonData(final JSONObject personData) {
        ensureInit(new Runnable() {
            public void run() {
                notifier.setPersonData(personData);
            }
        });
    }
    
    public static void setPersonData(final String id, final String username, final String email) {
        ensureInit(new Runnable() {
            public void run() {
                notifier.setPersonData(id, username, email);
            }
        });
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
    
    public static void setIncludeLogcat(final boolean includeLogcat) {
        ensureInit(new Runnable() {
            public void run() {
                notifier.setIncludeLogcat(includeLogcat);
            }
        });
    }
    
    public static void setDefaultCaughtExceptionLevel(final String level) {
        ensureInit(new Runnable() {
            public void run() {
                notifier.setDefaultCaughtExceptionLevel(level);
            }
        });
    }
    
    public static void setUncaughtExceptionLevel(final String level) {
        ensureInit(new Runnable() {
            public void run() {
                notifier.setUncaughtExceptionLevel(level);
            }
        });
    }

    public static void setSendOnUncaughtException(final boolean send) {
        ensureInit(new Runnable() {
            public void run() {
                notifier.setSendOnUncaughtException(send);
            }
        });
    }
    
    private static void ensureInit(Runnable runnable) {
        if (isInit()) {
            try {
                runnable.run();
            } catch (Exception e) {
                Log.e(TAG, "Exception when interacting with Rollbar", e);
            }
        } else {
            Log.e(TAG, "Rollbar not initialized with an access token!");
        }
    }
}
