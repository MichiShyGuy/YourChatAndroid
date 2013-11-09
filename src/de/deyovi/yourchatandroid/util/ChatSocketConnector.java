package de.deyovi.yourchatandroid.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;

public class ChatSocketConnector {

	private static volatile ChatSocketConnector instance;
	
	private final static String TAG = ChatSocketConnector.class.getSimpleName();

	private final AtomicLong longIds = new AtomicLong();
	
	private final Map<String, JSONCallback> waitMap = new HashMap<String, JSONCallback>();

	private final WebSocketConnection mConnection = new WebSocketConnection();
	
	private String baseUrl = null;
	
	private JSONCallback asyncListener = null;

	private ChatSocketConnector() {
		// hidden
	}
	
	public static ChatSocketConnector getInstance() {
		if (instance == null) {
			createInstance();
		}
		return instance;
	}
	
	private static synchronized void createInstance() {
		if (instance == null) {
			instance = new ChatSocketConnector();
		}
	}
	
	public void registerAsyncListener(JSONCallback callback) {
		this.asyncListener = callback;
	}
	
	public void start(final String wsuri) {
		try {
			baseUrl = wsuri;
			if (!baseUrl.endsWith("/")) {
				baseUrl += "/";
			}
			mConnection.connect("ws://" + baseUrl + "socket", new WebSocketHandler() {

				@Override
				public void onOpen() {
					Log.d(TAG, "Status: Connected to " + wsuri);
				}

				@Override
				public void onTextMessage(String payload) {
					Log.d(TAG, payload);
					try {
						JSONObject jsonObject = new JSONObject(payload);
						JSONCallback callback;
						if (jsonObject.has("messageid")) {
							callback = waitMap.get(jsonObject.getString("messageid"));
						} else {
							callback = asyncListener;
						}
						if (callback != null) {
							callback.answer(jsonObject);
						}
					} catch (JSONException e) {
						Log.e(TAG, "couldn't parse answer from server", e);
					}
				}

				@Override
				public void onClose(int code, String reason) {
					Log.d(TAG, "Connection lost: " +  reason);
				}
			});
		} catch (WebSocketException e) {
			Log.d(TAG, e.toString());
		}
	}
	
	public void send(String context, String action, JSONObject data, JSONCallback callback) {
		JSONObject json = new JSONObject();
		String messageId = Long.toString(longIds.incrementAndGet());
		try {
			json.put("context", context);
			json.put("action", action);
			json.put("messageid", messageId);
			json.put("body", data);
			waitMap.put(messageId, callback);
			mConnection.sendTextMessage(json.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public String getBaseUrl() {
		return baseUrl;
	}

	public static interface JSONCallback {
		
		public void answer(JSONObject result);
		
	}
	
}
