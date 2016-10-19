package com.rollbar.android;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;
import android.util.Log;

import com.rollbar.android.http.HttpRequestManager;
import com.rollbar.android.http.HttpResponse;
import com.rollbar.android.http.HttpResponseHandler;

public class Notifier {
    private static final String NOTIFIER_VERSION = "0.1.2";
    private static final String DEFAULT_ENDPOINT = "https://api.rollbar.com/api/1/items/";
    private static final String ITEM_DIR_NAME = "rollbar-items";
    private static final String PAYLOAD_ERROR_MSG = "There was an error constructing the JSON payload.";
    private static final String ANDROID = "android";
    private static final String MESSAGE = "message";
    
    
    private static final int DEFAULT_ITEM_SCHEDULE_DELAY = 1;
    private static final int MAX_LOGCAT_SIZE = 100;
    
    private static int itemCounter = 0;
    
    volatile private boolean handlerScheduled;
    
    private final ScheduledExecutorService scheduler;

    private final String accessToken;
    private final String environment;

    private JSONObject personData;
    private String endpoint;
    private boolean reportUncaughtExceptions;
    private boolean includeLogcat;
    private String defaultCaughtExceptionLevel;
    private String uncaughtExceptionLevel;
    private boolean sendOnUncaughtException;
    
    private int versionCode;
    private String versionName;
    
    private final File queuedItemDirectory;
    private final RollbarThread rollbarThread;
    protected final String rollbarThreadName;

    public Notifier(Context context, String accessToken, String environment, boolean registerExceptionHandler) {
        this(context,null,accessToken,environment,registerExceptionHandler);
    }

    public Notifier(Context context,  String rollbarThreadName, String accessToken, String environment, boolean registerExceptionHandler) {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        this.rollbarThreadName = rollbarThreadName;
        this.accessToken = accessToken;
        this.environment = environment;
        
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            
            this.versionCode = info.versionCode;
            this.versionName = info.versionName;
        } catch (NameNotFoundException e) {
            Log.e(Rollbar.TAG, "Error getting package info.");
        }

        
        endpoint = DEFAULT_ENDPOINT;
        reportUncaughtExceptions = true;
        defaultCaughtExceptionLevel = "warning";
        uncaughtExceptionLevel = "error";
        sendOnUncaughtException = false;
        
        handlerScheduled = false;
        
        queuedItemDirectory = new File(context.getCacheDir(), ITEM_DIR_NAME);
        queuedItemDirectory.mkdirs();

        if (registerExceptionHandler) {
            RollbarExceptionHandler.register(this);
        }

        rollbarThread = new RollbarThread(this);
        rollbarThread.start();
        
        scheduleItemFileHandler();
    }
    
    private JSONArray getLogcatInfo() {
        JSONArray log = null;
        
        int pid = android.os.Process.myPid();

        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            
            InputStreamReader isr = new InputStreamReader(process.getInputStream());
            BufferedReader br = new BufferedReader(isr, 8192);
            
            List<String> lines = new ArrayList<String>();
            
            String line;
            while ((line = br.readLine()) != null) {
                // Only include the line if the current process's pid is present
                if (line.contains(String.valueOf(pid))) {
                    lines.add(line);
                    if (lines.size() > MAX_LOGCAT_SIZE) {
                        lines.remove(0);
                    }
                }
            }
            
            log = new JSONArray(lines);
        } catch (IOException e) {
            Log.e(Rollbar.TAG, "Unable to collect logcat info.", e);
        }
        
        return log;
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
        // NOTE use versionName instead of versionCode in order to fix
        //      'Resolving Rollbar Items in Versions' feature https://rollbar.com/blog/post/2013/09/17/resolving-rollbar-items-in-versions
        androidData.put("code_version", this.versionName);
        // NOTE keep original versionCode value with different name
        androidData.put("version_code", this.versionCode);
        androidData.put("version_name", this.versionName);

        if (includeLogcat) {
            androidData.put("logs", getLogcatInfo());
        }
        
        client.put(ANDROID, androidData);
        client.put("user_ip", "$remote_ip");

        return client;
    }

    private JSONObject buildData(String level, JSONObject body) throws JSONException {
        JSONObject data = new JSONObject();

        data.put("environment", this.environment);
        data.put("level", level);
        data.put("platform", ANDROID);
        data.put("framework", ANDROID);
        data.put("language", "java");

        data.put("body", body);

        if (personData != null) {
            data.put("person", personData);
        }
        data.put("client", buildClientData());
        data.put("notifier", buildNotifierData());

        return data;
    }

    private JSONObject buildPayload(JSONArray data) throws JSONException {
        JSONObject payload = new JSONObject();

        payload.put("access_token", this.accessToken);
        payload.put("data", data);

        return payload;
    }

    private JSONArray loadItems(File file) {
        Log.d(Rollbar.TAG, "Loading items...");
        
        JSONArray items = null;
        
        try {
            FileInputStream in = new FileInputStream(file);
            
            StringBuilder content = new StringBuilder();
            
            byte[] buffer = new byte[1024];
            while (in.read(buffer) != -1) {
                content.append(new String(buffer));
            }
            
            in.close();
            
            items = new JSONArray(content.toString());

            Log.d(Rollbar.TAG, "Items loaded.");
        } catch (FileNotFoundException e) {
            Log.e(Rollbar.TAG, "Unable to read item file.", e);
        } catch (IOException e) {
            Log.e(Rollbar.TAG, "Unable to read item file.", e);
        } catch (JSONException e) {
            Log.e(Rollbar.TAG, "Invalid item data. Deleting file.", e);
            file.delete();
        }
        
        return items;
    }
    
    public File writeItems(JSONArray items) {
        Log.d(Rollbar.TAG, "Writing items...");
        
        try {
            String filename = itemCounter++ + "." + System.currentTimeMillis();
            File file = new File(queuedItemDirectory, filename);
            FileWriter writer = new FileWriter(file);
            
            writer.write(items.toString());
            writer.close();

            Log.d(Rollbar.TAG, "Items written");
            return file;
        } catch (IOException e) {
            Log.e(Rollbar.TAG, "Unable to write items.", e);
            return null;
        }
    }
    
    public void postItems(final JSONArray items, final File file) {
        Log.i(Rollbar.TAG, "Sending item batch...");
        
        JSONObject payload;
        try {
            payload = buildPayload(items);
        } catch (JSONException e) {
            Log.e(Rollbar.TAG, PAYLOAD_ERROR_MSG, e);
            return;
        }
        
        HttpRequestManager.getInstance().postJson(this.endpoint, payload, false,
                new HttpResponseHandler() {
            
            @Override
            public void onSuccess(HttpResponse response) {
                Log.i(Rollbar.TAG, "Success");
                
                if (file != null) {
                    file.delete();
                }
            }
            
            @Override
            public void onFailure(HttpResponse response) {
                Log.e(Rollbar.TAG, "There was a problem reporting to Rollbar.");
                Log.e(Rollbar.TAG, "Response: " + response);

                if (file == null) {
                    if (!response.hasStatusCode()) {
                        writeItems(items);
                    }
                } else {
                    // Give up if there is a server error
                    if (response.hasStatusCode()) {
                        file.delete();
                    }
                }
            }
        });
    }
    
    private void scheduleItemFileHandler() {
        if (!handlerScheduled) {
            handlerScheduled = true;
            
            Log.d(Rollbar.TAG, "Scheheduling item file handler...");
            
            scheduler.schedule(new Runnable() {
                
                @Override
                public void run() {
                    Log.d(Rollbar.TAG, "Item file handler running...");
                    
                    File[] files = queuedItemDirectory.listFiles();
                    
                    for (File file : files) {
                        JSONArray items = loadItems(file);
                        postItems(items, file);
                    }
                    
                    handlerScheduled = false;
                    Log.d(Rollbar.TAG, "Item file handler finished.");
                }
            }, DEFAULT_ITEM_SCHEDULE_DELAY, TimeUnit.SECONDS);
        }
    }

    private JSONObject createTrace(Throwable throwable, String description) throws JSONException {
        JSONObject trace = new JSONObject();

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
            Log.e(Rollbar.TAG, "Exception printing stack trace.", e);
        }

        JSONObject exceptionData = new JSONObject();
        exceptionData.put("class", throwable.getClass().getName());
        exceptionData.put(MESSAGE, throwable.getMessage());

        if (!TextUtils.isEmpty(description)) {
        	exceptionData.put("description", description);
        }            
        
        trace.put("frames", frames);
        trace.put("exception", exceptionData);

        return trace;
    }

    public void uncaughtException(Throwable throwable) {
        if (reportUncaughtExceptions) {
            Log.d(Rollbar.TAG, "Handling uncaught exception...");

            // Finish rollbar thread
            rollbarThread.interrupt();

            JSONObject item = buildItemPayload(throwable, uncaughtExceptionLevel, null);

            if (item != null) {
                JSONArray items = new JSONArray();
                items.put(item);

                if (sendOnUncaughtException) {
                    postItems(items, null);
                } else {
                    writeItems(items);
                }
            }

            try {
                // Try finishing up any items currently being sent
                rollbarThread.join();
            } catch (InterruptedException e) {
                Log.d(Rollbar.TAG, "Couldn't join rollbar thread", e);
            }
        }
    }

    private JSONObject buildItemPayload(Throwable throwable, String level, String description) {
        return buildItemPayload(throwable, level, description, new HashMap<String, String>());
    }

    private JSONObject buildItemPayload(Throwable throwable, String level, String description, Map<String, String> params) {
        try {
            JSONObject body = new JSONObject();

            List<JSONObject> traces = new ArrayList<JSONObject>();
            do {
                traces.add(0, createTrace(throwable, description));
                throwable = throwable.getCause();
            } while (throwable != null);
            JSONArray jsonArray = new JSONArray(traces);

            body.put("trace_chain", jsonArray);

            if (level == null) {
                level = defaultCaughtExceptionLevel;
            }

            for(Map.Entry<String, String> stringEntry : params.entrySet()){
                body.put(stringEntry.getKey(), stringEntry.getValue());
            }

            return buildData(level, body);
        } catch (JSONException e) {
            Log.e(Rollbar.TAG, PAYLOAD_ERROR_MSG, e);
            return null;
        }
    }

    private JSONObject buildItemPayload(String message, String level) {
        return buildItemPayload(message, level, new HashMap<String, String>());
    }

    private JSONObject buildItemPayload(String message, String level, Map<String, String> params) {
        try {
            JSONObject body = new JSONObject();
            JSONObject messageBody = new JSONObject();
            
            messageBody.put("body", message);
            body.put(MESSAGE, messageBody);
            
            for(Map.Entry<String, String> stringEntry : params.entrySet()){
                messageBody.put(stringEntry.getKey(), stringEntry.getValue());
            }
            return buildData(level, body);
        } catch (JSONException e) {
            Log.e(Rollbar.TAG, PAYLOAD_ERROR_MSG, e);
            return null;
        }
    }

    public void reportException(Throwable throwable, String level, String description) {
        JSONObject item = buildItemPayload(throwable, level, description);
        
        if (item != null) {
            rollbarThread.queueItem(item);
        }
    }

    public void reportException(Throwable throwable, String level, String description, Map<String, String> params) {
        JSONObject item = buildItemPayload(throwable, level, description, params);

        if (item != null) {
            rollbarThread.queueItem(item);
        }
    }

    public void reportMessage(String message, String level) {
        JSONObject item = buildItemPayload(message, level);

        if (item != null) {
            rollbarThread.queueItem(item);
        }
    }

    public void reportMessage(String message, String level, Map<String, String> params) {
        JSONObject item = buildItemPayload(message, level, params);

        if (item != null) {
            rollbarThread.queueItem(item);
        }
    }

    public void setPersonData(JSONObject personData) {
        this.personData = personData;
    }

    public void setPersonData(String id, String username, String email) {
        JSONObject personData = new JSONObject();
        
        try {
            personData.put("id", id);
            
            if (username != null) {
                personData.put("username", username);
            }
            if (email != null) {
                personData.put("email", email);
            }
            
            this.personData = personData;
        } catch (JSONException e) {
            Log.e(Rollbar.TAG, "JSON error creating person data.", e);
        }
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setReportUncaughtExceptions(boolean reportUncaughtExceptions) {
        this.reportUncaughtExceptions = reportUncaughtExceptions;
    }
    
    public void setIncludeLogcat(boolean includeLogcat) {
        this.includeLogcat = includeLogcat;
    }

    public void setDefaultCaughtExceptionLevel(String level) {
        this.defaultCaughtExceptionLevel = level;
    }

    public void setUncaughtExceptionLevel(String level) {
        this.uncaughtExceptionLevel = level;
    }

    public void setSendOnUncaughtException(boolean sendOnUncaughtException) {
        this.sendOnUncaughtException = sendOnUncaughtException;
    }
}
