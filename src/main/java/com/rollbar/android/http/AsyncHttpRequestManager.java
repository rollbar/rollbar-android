package main.java.com.rollbar.android.http;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.json.JSONObject;

public class AsyncHttpRequestManager {
	private static AsyncHttpRequestManager instance = null;
	private ThreadPoolExecutor executor;
	
	private AsyncHttpRequestManager() {
		executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
	}
	
	public static AsyncHttpRequestManager getInstance() {
		if (instance == null) {
			instance = new AsyncHttpRequestManager();
		}
		return instance;
	}
	
	public void postJson(String urlString, JSONObject json, AsyncHttpResponseHandler handler) {
		URL url;
		
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			handler.onFailure(null);
			return;
		}
		
		AsyncHttpRequest request = new AsyncHttpRequest(url, "POST", handler);
		
		request.setRequestProperty("Content-Type", "application/json");
		request.setRequestProperty("Accept", "application/json");
		
		request.setBody(json.toString());
		
		executor.execute(request);
	}
}
