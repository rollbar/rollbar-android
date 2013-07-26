package com.rollbar.android;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.util.Log;

import com.rollbar.android.http.HttpRequestManager;
import com.rollbar.android.http.HttpResponse;
import com.rollbar.android.http.HttpResponseHandler;

public class Notifier {
    private static final String NOTIFIER_VERSION = "0.0.2";
    private static final String DEFAULT_ENDPOINT = "https://api.rollbar.com/api/1/";

    private Context context;
    private String accessToken;
    private String environment;
    
    private String endpoint;
    private boolean reportUncaughtExceptions;
    
    private int versionCode;
    private String versionName;

    public Notifier(Context context, String accessToken, String environment) {
        this.context = context;
        this.accessToken = accessToken;
        this.environment = environment;
        
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            
            this.versionCode = info.versionCode;
            this.versionName = info.versionName;
        } catch (NameNotFoundException e) {
            Log.e(Rollbar.TAG, "Error getting package info!");
        }

        endpoint = DEFAULT_ENDPOINT;
        reportUncaughtExceptions = true;
        
        RollbarExceptionHandler.register(this);
    }

    private JSONObject buildNotifierData() throws JSONException {
        JSONObject notifier = new JSONObject();
        notifier.put("name", "rollbar-android");
        notifier.put("version", NOTIFIER_VERSION);

        return notifier;
    }

    private JSONObject buildClientData() throws JSONException {
        JSONObject client = new JSONObject();

        client.put("timestamp", System.currentTimeMillis() / 1000);

        JSONObject androidData = new JSONObject();
        androidData.put("phone_model", android.os.Build.MODEL);
        androidData.put("android_version", android.os.Build.VERSION.RELEASE);
        androidData.put("code_version", this.versionCode);
        androidData.put("version_name", this.versionName);
        client.put("android", androidData);

        return client;
    }

    private JSONObject buildData(String level, JSONObject body) throws JSONException {
        JSONObject data = new JSONObject();

        data.put("environment", this.environment);
        data.put("level", level);
        data.put("platform", "android");
        data.put("framework", "android");
        data.put("language", "java");

        data.put("body", body);

        data.put("client", buildClientData());
        data.put("notifier", buildNotifierData());

        return data;
    }

    private JSONObject buildPayload(String level, JSONObject body) throws JSONException {
        JSONObject payload = new JSONObject();

        payload.put("access_token", this.accessToken);
        payload.put("data", buildData(level, body));

        return payload;
    }

    private void postItem(JSONObject payload) {
        Log.i(Rollbar.TAG, "Sending item payload...");
        
        HttpRequestManager.getInstance().postJson(this.endpoint + "item/", payload,
            new HttpResponseHandler() {
            
            @Override
            public void onSuccess(HttpResponse response) {
                Log.i(Rollbar.TAG, "Success");
            }
            
            @Override
            public void onFailure(HttpResponse response) {
                Log.e(Rollbar.TAG, "There was a problem reporting to Rollbar!");
                Log.e(Rollbar.TAG, "Response: " + response);
            }
        });
    }

    public void uncaughtException(Throwable throwable) {
        if (reportUncaughtExceptions) {
            reportException(throwable, "error");
        }
    }

    public void reportException(Throwable throwable, String level) {
        try {
            JSONObject body = new JSONObject();
            JSONObject trace = new JSONObject();
            JSONObject exceptionData = new JSONObject();
    
            JSONArray frames = new JSONArray();
    
            StackTraceElement[] elements = throwable.getStackTrace();
            for (int i = elements.length - 1; i >= 0; --i) {
                StackTraceElement element = elements[i];
    
                JSONObject frame = new JSONObject();
    
                frame.put("class_name", element.getClassName());
                frame.put("filename", element.getFileName());
                frame.put("method", element.getMethodName());
    
                if (element.getLineNumber() > 0) {
                    frame.put("lineno", element.getLineNumber());
                }
    
                frames.put(frame);
            }
            
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                
                throwable.printStackTrace(ps);
                ps.close();
                baos.close();
                
                trace.put("raw", baos.toString("UTF-8"));
            } catch (Exception e) {
                Log.e(Rollbar.TAG, "Exception printing stack trace!", e);
            }
            
            exceptionData.put("class", throwable.getClass().getName());
            exceptionData.put("message", throwable.getMessage());
    
            trace.put("frames", frames);
            trace.put("exception", exceptionData);
            body.put("trace", trace);
    
            JSONObject payload = buildPayload(level, body);
            postItem(payload);
        } catch (JSONException e) {
            Log.e(Rollbar.TAG, "There was an error constructing the JSON payload!", e);
        }
    }

    public void reportMessage(String message, String level) {
        try {
            JSONObject body = new JSONObject();
            JSONObject messageBody = new JSONObject();
    
            messageBody.put("body", message);
            body.put("message", messageBody);
    
            JSONObject payload = buildPayload(level, body);
            postItem(payload);
        } catch (JSONException e) {
            Log.e(Rollbar.TAG, "There was an error constructing the JSON payload!", e);
        }
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setReportUncaughtExceptions(boolean reportUncaughtExceptions) {
        this.reportUncaughtExceptions = reportUncaughtExceptions;
    }

}
