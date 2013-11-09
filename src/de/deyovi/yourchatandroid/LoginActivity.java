package de.deyovi.yourchatandroid;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import de.deyovi.yourchatandroid.util.Authenticator;
import de.deyovi.yourchatandroid.util.ChatSocketConnector;
import de.deyovi.yourchatandroid.util.ChatSocketConnector.JSONCallback;

public class LoginActivity extends Activity implements JSONCallback {

	private final static String TAG = LoginActivity.class.getName();
	
	private EditText inputPass;
	private EditText inputUser;
	
	private ChatSocketConnector socket = null;

	private Step step = Step.START;
	
	private enum Step {
		START, SUGAR, LOGIN
	}
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		socket = ChatSocketConnector.getInstance();
		socket.start("aurora:8080/YourChatWeb/");
        setContentView(R.layout.activity_login);
        inputUser = (EditText) findViewById(R.id.text_login_user);
        inputPass = (EditText) findViewById(R.id.text_login_pass);
        final Button button = (Button) findViewById(R.id.button_login);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               login();
            }
        });
        
    }
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }
 
    public void login() {
		JSONObject request = new JSONObject();
		try {
			request.put("username", readUsername());
		} catch (JSONException e) {
			Log.e(TAG, "Error building Sugar request", e);
		}
		step = Step.SUGAR;
		socket.send("session", "sugar", request, this);
	}


	private String readUsername() {
		return inputUser.getEditableText().toString();
	}
	

	private String readPassword() {
		return inputPass.getEditableText().toString();
	}

	@Override
	public void answer(JSONObject result) {
		try {
			switch (step) {
			case SUGAR:
				if (!result.has("sugar")) {
//					callback.failure(this.getString(R.string.handshake_failed));
				} else {
					String sugar = result.getString("sugar");
					JSONObject request = new JSONObject();
					try {
						request.put("username", readUsername());
						String pwHashed = Authenticator.encrypt(readPassword(), null);
						request.put("password", Authenticator.encrypt(pwHashed, sugar));
					} catch (JSONException e) {
						Log.e(TAG, "Error building Sugar request", e);
					}
					step = Step.LOGIN;
					socket.send("session", "login", request, this);
				}
				break;
			case LOGIN:
				if (!result.has("listenId")) {
//					callback.failure(this.getString(R.string.credentials_wrong));
				} else {
					String listenId = result.getString("listenId");
					Log.i(TAG, "Successfully logged in with listenId: " + listenId);
//					callback.success(listenId);
					Intent myIntent = new Intent(this, ChatActivity.class);
					myIntent.putExtra("LISTEN_ID", listenId);
					startActivity(myIntent);
				}
				break;
			}
		} catch (JSONException e) {
			Log.e(TAG, "Error on Step " + step, e);
		}
	}
    
    
}
