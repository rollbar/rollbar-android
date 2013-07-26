package com.rollbar.android.http;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

public class HttpRequestManager {
    public static final int MAX_RETRIES = 5;
    
    private static HttpRequestManager instance = null;
    private ThreadPoolExecutor executor;
    private ScheduledExecutorService service;

    private HttpRequestManager() {
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        service = Executors.newSingleThreadScheduledExecutor();
    }

    public static HttpRequestManager getInstance() {
        if (instance == null) {
            instance = new HttpRequestManager();
        }
        return instance;
    }

    public void postJson(String urlString, JSONObject json, HttpResponseHandler handler) {
        URL url;

        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            handler.onFailure(new HttpResponse(e.toString()));
            return;
        }

        HttpRequest request = new HttpRequest(url, "POST", handler);

        request.setRequestProperty("Content-Type", "application/json");
        request.setRequestProperty("Accept", "application/json");

        request.setBody(json.toString());

        executor.execute(request);
    }
    
    public void retryRequest(final HttpRequest request) {
        int retryDelay = request.getAttempt();
        
        service.schedule(new Runnable() {
            
            @Override
            public void run() {
                executor.execute(request);
            }
        }, retryDelay, TimeUnit.SECONDS);
    }
}
