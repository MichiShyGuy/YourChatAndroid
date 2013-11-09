package de.deyovi.yourchatandroid;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import de.deyovi.yourchatandroid.objects.ChatUser;
import de.deyovi.yourchatandroid.util.ChatSocketConnector;
import de.deyovi.yourchatandroid.util.ChatSocketConnector.JSONCallback;

public class ChatActivity extends Activity implements JSONCallback {

	private final static String TAG = ChatActivity.class.getName();
	private final JSONCallback refreshCallback = new RefreshCallback();

	private final Map<String, ChatUser> userMap = new TreeMap<String, ChatUser>();
	private ChatSocketConnector socket = null;
	
	private String listenID = null;
	private RelativeLayout chatBg = null;
	private LinearLayout chatView = null;
	private ScrollView chatScroller = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		socket = ChatSocketConnector.getInstance();

        setContentView(R.layout.activity_chat);
        listenID = getIntent().getStringExtra("LISTEN_ID");
        EditText inputText = (EditText) findViewById(R.id.inputtext_chat);
        inputText.setImeActionLabel("Send", KeyEvent.KEYCODE_ENTER);
        final ChatSocketConnector socket = this.socket;
        inputText.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == KeyEvent.KEYCODE_ENTER) {
					try {
						JSONObject request = new JSONObject();
						request.put("message", v.getText());
						socket.send("input", "talk", request, null);
						v.setText("");
						v.requestFocus();
					} catch (JSONException e) {
						Log.e(TAG, "Error while sending Message", e);
					}
					return true;
				} else {
					return false;
				}
			}
		});
        chatBg = (RelativeLayout) findViewById(R.id.chat);
        chatView = (LinearLayout) findViewById(R.id.chat_screen);
        chatScroller = (ScrollView) findViewById(R.id.scrollview_chat);
        socket.registerAsyncListener(this);
        startListening();
    }
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    private void startListening() {
        JSONObject json = new JSONObject();
        try {
			refresh();
			json.put("listenid", listenID);
			socket.send("listen", "start", json, this);
		} catch (JSONException e) {
			e.printStackTrace();
		}
    }
    
    private void refresh() {
 		socket.send("listen", "refresh", null, refreshCallback);
    }


	private class RefreshCallback implements JSONCallback {
		
		@Override
		public void answer(JSONObject result) {
			try {
				boolean success = result.getBoolean("success");
				if (success) {
					Log.d(TAG, result.toString());
					JSONObject data = result.getJSONObject("data");
					JSONArray users = data.getJSONArray("users");
					for (int i = 0; i < users.length(); i++) {
						JSONObject user = users.getJSONObject(i);
						ChatUser chatUser = new ChatUser();
						chatUser.setName(user.getString("username"));
						String alias;
						if (user.has("alias")) {
							alias = user.getString("alias");
						} else {
							alias = null;
						}
						chatUser.setAlias(alias);
						chatUser.setColor(user.getString("color"));
						userMap.put(chatUser.getName(), chatUser);
					}
					if (data.has("backgroundimage")) {
						String bgURL = data.getString("backgroundimage");
						URL url = new URL("http://" + socket.getBaseUrl() + bgURL);
						AsyncTask<URL,Integer,Drawable> task = new BackgroundImageSetter().execute(url);
					}
					if (data.has("background")) {
						String bgColor = data.getString("background");
						if (bgColor != null) {
							ColorDrawable background = new ColorDrawable(Color.parseColor('#' + bgColor));
							chatBg.setBackgroundDrawable(background);
						}
					}
					new JSONObject().put("foo", "foo");
				}
			} catch (JSONException ex) {
				Log.e(TAG, "Error while reading refresh-data", ex);
			} catch (MalformedURLException ex) {
				Log.e(TAG, "Error while reading refresh-data", ex);
			}
		}
		
	}
    
	@Override
	public void answer(JSONObject result) {
		try {
			if (result.has("listenid") && result.getString("listenid").equals(listenID)) {
				JSONObject message = result.getJSONObject("message");
				if (message.has("refresh") && message.getBoolean("refresh")) {
					refresh();
				}
				String hash = result.getString("hash");
				JSONArray messages = message.getJSONArray("messages");
				for (int i = 0; i < messages.length(); i++) {
					JSONObject msg = messages.getJSONObject(i);
					JSONArray segments = msg.getJSONArray("segments");
					StringBuilder builder = new StringBuilder(1024);
					for (int j = 0; j < segments.length(); j++) {
						JSONObject segment = segments.getJSONObject(j);
						Log.d(TAG, segment.toString());
						builder.append(segment.getString("content"));
						builder.append(' ');
					}
					TextView line = new TextView(this);
					String user = null;
					if (msg.has("user")) {
						user = msg.getString("user");
						ChatUser chatUser = userMap.get(user);
						if (chatUser != null) {
							String color = chatUser.getColor();
							ColorDrawable background = new ColorDrawable(Color.parseColor('#' + color));
							background.setAlpha(100);
							line.setBackgroundDrawable(background);
						}
						builder.insert(0, user + ": ");
					}
					
					line.setText(builder.toString());
					chatView.addView(line);
				}
				chatScroller.fullScroll(View.FOCUS_DOWN);
				JSONObject response = new JSONObject();
				response.put("hash", hash);
				socket.send("listen", "acknowledge", response, this);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
    
	private class BackgroundImageSetter extends AsyncTask<URL, Integer, Drawable> {
		
		@Override
		protected Drawable doInBackground(URL... params) {
			try {
				InputStream is = (InputStream) params[0].getContent();
				Drawable bgImage = Drawable.createFromStream(is, params[0].getFile());
				return bgImage;
			} catch (IOException e) {
				Log.e(TAG, "error getting bgImage", e);
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(Drawable result) {
			chatBg.setBackgroundDrawable(result);
		}
		
	}
	
}
