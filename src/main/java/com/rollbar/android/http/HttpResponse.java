package main.java.com.rollbar.android.http;

public class HttpResponse {
    private int statusCode;
    private String responseText;

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
}
