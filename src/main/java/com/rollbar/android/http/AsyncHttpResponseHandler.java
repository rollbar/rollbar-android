package main.java.com.rollbar.android.http;

public interface AsyncHttpResponseHandler {
    public void onSuccess(HttpResponse response);

    public void onFailure(HttpResponse response);
}
