package com.rollbar.android.http;

public class HttpResponse {
    private String result;
    
    private int statusCode;
    private String responseText;
    
    public HttpResponse(String result) {
        this.result = result;
    }

    public HttpResponse(int statusCode, String responseText) {
        this.statusCode = statusCode;
        this.responseText = responseText;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseText() {
        return responseText;
    }

    @Override
    public String toString() {
        if (responseText != null) {
            return responseText;
        }
        
        return result;
    }
}
