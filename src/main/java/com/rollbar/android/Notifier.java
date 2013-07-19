package main.java.com.rollbar.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

public class Notifier {
    private static final String NOTIFIER_VERSION = "0.0.1";
    private static final String DEFAULT_ENDPOINT = "https://api.rollbar.com/api/1/";

    private Configuration configuration;

    private String accessToken;
    private String environment;
    
    private static AsyncHttpClient httpClient;

    public Notifier(String accessToken, String environment, HashMap<String, Object> config) {
        this.accessToken = accessToken;
        this.environment = environment;
        
        httpClient = new AsyncHttpClient();

        configuration = new Configuration();
        configuration.put(Configuration.ENDPOINT, DEFAULT_ENDPOINT);

        if (config != null) {
            configuration.putAll(config);
        }

        RollbarExceptionHandler.register(this);
    }

    private JSONObject buildNotifierData() {
        HashMap<String, Object> notifier = new HashMap<String, Object>();
        notifier.put("name", "rollbar-android");
        notifier.put("version", NOTIFIER_VERSION);

        return new JSONObject(notifier);
    }

    private JSONObject buildClientData() {
        HashMap<String, Object> client = new HashMap<String, Object>();

        client.put("timestamp", System.currentTimeMillis() / 1000);

        HashMap<String, Object> androidData = new HashMap<String, Object>();
        androidData.put("phone_model", android.os.Build.MODEL);
        androidData.put("android_version", android.os.Build.VERSION.RELEASE);
        client.put("android", new JSONObject(androidData));

        return new JSONObject(client);
    }

    private JSONObject buildData(String level, JSONObject body) {
        HashMap<String, Object> data = new HashMap<String, Object>();

        data.put("environment", this.environment);
        data.put("level", level);
        data.put("platform", "android");
        data.put("framework", "android");
        data.put("language", "java");

        data.put("body", body);

        data.put("client", buildClientData());
        data.put("notifier", buildNotifierData());

        return new JSONObject(data);
    }

    private JSONObject buildPayload(String level, JSONObject body) {
        HashMap<String, Object> payload = new HashMap<String, Object>();

        payload.put("access_token", this.accessToken);
        payload.put("data", buildData(level, body));

        return new JSONObject(payload);
    }

    private void postItem(JSONObject payload) {
        HttpEntity entity;
        
        try {
            entity = new StringEntity(payload.toString());
        } catch (UnsupportedEncodingException e) {
            Log.e(Rollbar.TAG, e.toString());
            return;
        }
        
        httpClient.post(null, configuration.get(Configuration.ENDPOINT) + "item/", entity, 
                "application/json", new AsyncHttpResponseHandler()  {
            @Override
            public void onStart() {
                Log.i(Rollbar.TAG, "Sending item payload...");
            }

            @Override
            public void onSuccess(String response) {
                Log.i(Rollbar.TAG, "Success.");
            }
        
            @Override
            public void onFailure(Throwable e, String response) {
                Log.e(Rollbar.TAG, "There was a problem reporting to Rollbar!");
                Log.e(Rollbar.TAG, "Response: " + response);
            }

            @Override
            public void onFinish() {
            }
        });
    }

    public void uncaughtException(Throwable throwable) {
        reportException(throwable, "error");
    }

    public void reportException(Throwable throwable, String level) {
        HashMap<String, Object> body = new HashMap<String, Object>();
        HashMap<String, Object> trace = new HashMap<String, Object>();
        HashMap<String, String> exceptionData = new HashMap<String, String>();

        ArrayList<JSONObject> frames = new ArrayList<JSONObject>();

        StackTraceElement[] elements = throwable.getStackTrace();
        for (int i = elements.length - 1; i >= 0; --i) {
            StackTraceElement element = elements[i];

            HashMap<String, Object> frame = new HashMap<String, Object>();

            frame.put("class_name", element.getClassName());
            frame.put("filename", element.getFileName());
            frame.put("method", element.getMethodName());

            if (element.getLineNumber() > 0) {
                frame.put("lineno", element.getLineNumber());
            }

            frames.add(new JSONObject(frame));
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        
        throwable.printStackTrace(ps);
        
        ps.close();
        try {
            baos.close();
        } catch (IOException e1) {
        }
        
        try {
            trace.put("raw", baos.toString("UTF-8"));
        } catch (UnsupportedEncodingException e) {
        }
        
        exceptionData.put("class", throwable.getClass().getName());
        exceptionData.put("message", throwable.getMessage());

        trace.put("frames", new JSONArray(frames));
        trace.put("exception", new JSONObject(exceptionData));
        body.put("trace", new JSONObject(trace));

        JSONObject payload = buildPayload(level, new JSONObject(body));
        postItem(payload);
    }

    public void reportMessage(String message, String level) {
        HashMap<String, Object> body = new HashMap<String, Object>();
        HashMap<String, String> messageBody = new HashMap<String, String>();

        messageBody.put("body", message);
        body.put("message", new JSONObject(messageBody));

        JSONObject payload = buildPayload(level, new JSONObject(body));
        postItem(payload);
    }

}
