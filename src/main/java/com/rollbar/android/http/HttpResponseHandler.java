package com.rollbar.android.http;

public interface HttpResponseHandler {
    public void onSuccess(HttpResponse response);
    public void onFailure(HttpResponse response);
}
