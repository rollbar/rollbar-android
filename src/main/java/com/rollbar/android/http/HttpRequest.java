package com.rollbar.android.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;

import android.util.Log;

import com.rollbar.android.Rollbar;

public class HttpRequest implements Runnable {
    private URL url;
    private HttpResponseHandler handler;

    private HttpURLConnection connection;
    private HashMap<String, String> requestProperties;

    private String method;
    private byte[] body;
    
    private int attemptNumber;

    public HttpRequest(URL url, String method, HttpResponseHandler handler) {
        this.url = url;
        this.method = method;
        this.handler = handler;

        this.requestProperties = new HashMap<String, String>();
        
        attemptNumber = 1;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setRequestProperty(String key, String value) {
        requestProperties.put(key, value);
    }

    public void setBody(String body) {
        try {
            this.body = body.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(Rollbar.TAG, "Cannot encode body: " + e.toString());
        }
    }

    @Override
    public void run() {
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            handleFailureWithRetries(new HttpResponse(e.toString()));
            return;
        }

        try {
            connection.setRequestMethod(this.method);

            for (Entry<String, String> pair : requestProperties.entrySet()) {
                connection.setRequestProperty(pair.getKey(), pair.getValue());
            }

            if (body != null) {
                connection.setDoOutput(true);

                OutputStream out = new BufferedOutputStream(connection.getOutputStream());
                out.write(this.body);
                out.close();
            }

            int responseCode = connection.getResponseCode();

            InputStream in;
            if (responseCode == 200) {
                in = new BufferedInputStream(connection.getInputStream());
            } else {
                in = new BufferedInputStream(connection.getErrorStream());
            }

            String responseText = getResponseText(in);
            HttpResponse response = new HttpResponse(responseCode, responseText);

            if (responseCode == 200) {
                handler.onSuccess(response);
            } else {
                handleFailureWithRetries(response);
            }
        } catch (IOException e) {
            handleFailureWithRetries(new HttpResponse(e.toString()));
        } finally {
            connection.disconnect();
        }
    }
    
    private void handleFailureWithRetries(HttpResponse response) {
        if (attemptNumber < HttpRequestManager.MAX_RETRIES) {
            attemptNumber++;
            
            HttpRequestManager.getInstance().retryRequest(this);
        } else {
            handler.onFailure(response);
        }
    }
    
    public int getAttemptNumber() {
        return attemptNumber;
    }

    private String getResponseText(InputStream in) throws IOException {
        byte[] contents = new byte[1024];

        int bytesRead = 0;
        String response = "";

        while ((bytesRead = in.read(contents)) != -1) {
            response += new String(contents, 0, bytesRead);
        }

        return response;
    }

}
