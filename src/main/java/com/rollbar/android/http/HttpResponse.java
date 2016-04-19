package com.rollbar.android.http;

public class HttpResponse {
    private final String result;
    
    private final int statusCode;
    private final String responseText;
    
    public HttpResponse(String result) {
        this.result = result;
        this.statusCode = 0;
        this.responseText = null;
    }

    public HttpResponse(int statusCode, String responseText) {
        this.result = null;
        this.statusCode = statusCode;
        this.responseText = responseText;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseText() {
        return responseText;
    }
    
    public boolean hasStatusCode() {
        return statusCode > 0;
    }

    @Override
    public String toString() {
        if (responseText != null) {
            return responseText;
        }
        
        return result;
    }
}
