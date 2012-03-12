package com.magneticmule.toponimo.client.ui;

import org.apache.http.HttpStatus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.google.gson.Gson;
import com.magneticmule.toponimo.client.ApiKeys;
import com.magneticmule.toponimo.client.Constants;
import com.magneticmule.toponimo.client.R;
import com.magneticmule.toponimo.client.ToponimoApplication;
import com.magneticmule.toponimo.client.structures.userstructure.UserDetails;
import com.magneticmule.toponimo.client.utils.TwoReturnValues;
import com.magneticmule.toponimo.client.utils.http.HttpUtils;

public class LoginActivity extends Activity {

    public static final String TAG = "LoginActivity";
    ToponimoApplication application;

    /*
     * requires the app key generated by the fb api, see
     * http://developers.facebook.com/docs/guides/mobile/
     */
    Facebook facebook = new Facebook(ApiKeys.FACEBOOK_API_KEY);

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.login);

	application = (ToponimoApplication) this.getApplication();

	// Text boxes for user name and password
	final EditText userNameEditText = (EditText) findViewById(R.id.login_activity_username_et);
	final EditText passwordEditText = (EditText) findViewById(R.id.login_activity_password_et);
	final ImageView logoImageView = (ImageView) findViewById(R.id.login_activity_logo_iv);

	logoImageView.setVisibility(2);
	// Image file used as a button for login via facebook
	// final ImageView facebookLoginButton = (ImageView)
	// findViewById(R.id.login_activity_facebook_img);

	// if the Facebook button is pressed
	/*
	 * facebookLoginButton.setOnClickListener(new View.OnClickListener() {
	 * public void onClick(View v) { facebookLogin(); } });
	 */
	// if the Sign in button is pressed
	final Button loginButton = (Button) findViewById(R.id.login_activity_signin_btn);
	loginButton.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View v) {
		String username = userNameEditText.getText().toString();
		String userPassword = passwordEditText.getText().toString();

		if (HttpUtils.isOnline(getApplicationContext())) {
		    if (!(userPassword.length() < 4)) {
			(new ValidateUser(username, userPassword))
				.execute((Object) null);
		    } else {
			Toast toast = Toast.makeText(LoginActivity.this,
				"Please supply both your username and pasword",
				Toast.LENGTH_LONG);
			toast.show();
		    }
		} else { // if no net connection
		    Toast toast = Toast.makeText(LoginActivity.this,
			    R.string.no_connection_message, Toast.LENGTH_LONG);
		    toast.show();
		}
	    }
	});

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
	super.onConfigurationChanged(newConfig);
	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    // Handles the facebook login screen.
    public void facebookLogin() {
	if (facebook.isSessionValid()) {
	    Intent i = new Intent(LoginActivity.this, PlaceListActivity.class);
	    startActivity(i);
	} else {

	    /**
	     * see:
	     * http://developers.facebook.com/docs/authentication/permissions/
	     * for a full list of permissions
	     */
	    facebook.authorize(this, new String[] { "user_about_me",
		    "user_website", "publish_stream" }, new DialogListener() {

		public void onComplete(Bundle values) {

		}

		public void onFacebookError(FacebookError error) {
		    System.out.println("Error: "
			    + error.getMessage().toString());
		}

		public void onError(DialogError e) {

		}

		public void onCancel() {
		    // return to login screen
		}
	    });
	}
    }

    public void startPlaceListActivity(UserDetails _userDetails) {
	switch (_userDetails.getStatus()) {
	case 1: // user exists and is validated
	    Intent i = new Intent(LoginActivity.this, PlaceListActivity.class);
	    startActivity(i);
	    break;
	case 2: // user does not exist
	    Toast toast = Toast.makeText(getApplicationContext(),
		    "Incorrect user name or password", Toast.LENGTH_LONG);
	    toast.show();
	    break;
	case 3:
	    break;
	default:

	}

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
	super.onActivityResult(requestCode, resultCode, data);

	facebook.authorizeCallback(requestCode, resultCode, data);
    }

    /*
     * Enable full color support (non-Javadoc)
     * 
     * @see android.app.Activity#onAttachedToWindow()
     */
    @Override
    public void onAttachedToWindow() {
	super.onAttachedToWindow();
	Window window = getWindow();
	window.setFormat(PixelFormat.RGBA_8888);
	window.addFlags(WindowManager.LayoutParams.FLAG_DITHER);
    }

    private class ValidateUser extends AsyncTask<Object, Void, UserDetails> {

	private String username = "";
	private String password = "";

	ProgressDialog dialog;

	public ValidateUser(String _username, String _password) {
	    username = _username;
	    password = _password;

	}

	@Override
	protected void onPreExecute() {
	    dialog = new ProgressDialog(LoginActivity.this);
	    dialog.setMessage("Signing in");
	    dialog.setIndeterminate(true);
	    dialog.setCancelable(false);
	    dialog.show();
	    super.onPreExecute();
	}

	@Override
	protected UserDetails doInBackground(Object... arg0) {
	    UserDetails userDetails = null;

	    Gson gson = new Gson();
	    TwoReturnValues<Integer, String> authenticateData = HttpUtils
		    .authenticate(ToponimoApplication.getApp().getHttpClient(),
			    username, password, "true", ApiKeys.LOGIN_URL);

	    String jsonData = authenticateData.getSecondVal();
	    userDetails = gson.fromJson(jsonData, UserDetails.class);
	    // Log.i("Firstname", userDetails.getFirstName().toString());
	    // Log.i("Lastname", userDetails.getLastName().toString());
	    // Log.i("ID", userDetails.getUserId().toString());
	    // Log.i("HTTP CODE", Integer.toString(userDetails.getStatus()));

	    int httpCode = authenticateData.getFirstVal();

	    if (httpCode == HttpStatus.SC_OK) {
		application.setUserDetails(userDetails);

		// Write user details to shared preferences file
		SharedPreferences userDetailsPrefs = getSharedPreferences(
			Constants.USER_DETAILS_PREFS, 0);
		SharedPreferences.Editor editor = userDetailsPrefs.edit();
		editor.putBoolean(Constants.IS_LOGGED_IN, true);
		editor.putString(Constants.FIRST_NAME,
			userDetails.getFirstName());
		editor.putString(Constants.LAST_NAME, userDetails.getLastName());
		editor.putString(Constants.USER_ID, userDetails.getUserId());

		editor.commit();

	    } else {
		String errorMessage = HttpUtils.GetHttpStatusMessage(httpCode);
		Log.d(TAG, "Error, server responded with the code: "
			+ errorMessage);
	    }

	    return userDetails;

	}

	@Override
	protected void onPostExecute(UserDetails userDetails) {
	    super.onPostExecute(userDetails);
	    dialog.dismiss();
	    startPlaceListActivity(userDetails);
	}
    }
}
