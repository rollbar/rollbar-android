package main.java.com.rollbar.android;

import java.util.ArrayList;
import java.util.HashMap;

import main.java.com.rollbar.android.http.AsyncHttpRequestManager;
import main.java.com.rollbar.android.http.AsyncHttpResponseHandler;
import main.java.com.rollbar.android.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class Notifier {
	private static final String NOTIFIER_VERSION = "0.0.1";
	private static final String DEFAULT_ENDPOINT = "https://api.rollbar.com/api/1/";
	
	private Configuration configuration;
	
	private String accessToken;
	private String environment;
	
	public Notifier(String accessToken, String environment, HashMap<String, Object> config) {
		this.accessToken = accessToken;
		this.environment = environment;
		
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
	
	private JSONObject buildBaseData(String level, JSONObject body) {
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
		payload.put("data", buildBaseData(level, body));
		
		return new JSONObject(payload);
	}
	
	private void postItem(JSONObject payload) {
		AsyncHttpRequestManager.getInstance().postJson(configuration.get(Configuration.ENDPOINT) + "item/", payload, new AsyncHttpResponseHandler() {
			
			@Override
			public void onSuccess(HttpResponse response) {
				Log.i(Rollbar.TAG, "Success");
			}
			
			@Override
			public void onFailure(HttpResponse response) {
				Log.e(Rollbar.TAG, "There was a problem reporting to Rollbar");
				if (response != null) {
					Log.e(Rollbar.TAG, "Response: " + response.getResponseText());
				}
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
			
			frame.put("filename", element.getClassName());
			frame.put("method", element.getMethodName());
		
			if (!element.isNativeMethod()) {
				frame.put("lineno", element.getLineNumber());
			}
			
			frames.add(new JSONObject(frame));
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
		
		Log.i(Rollbar.TAG, payload.toString());
	}

}
