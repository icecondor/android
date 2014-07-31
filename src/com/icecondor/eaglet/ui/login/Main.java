package com.icecondor.eaglet.ui.login;

import java.net.URI;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.icecondor.eaglet.Condor;
import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.R;
import com.icecondor.eaglet.Start;
import com.icecondor.eaglet.ui.BaseActivity;
import com.icecondor.eaglet.ui.UiActions;

public class Main extends BaseActivity implements UiActions, OnEditorActionListener {
    public static final String PREF_KEY_UNVERIFIED_TOKEN = "icecondor_unverified_token";
    public static String PREF_KEY_AUTHENTICATED_USER_ID = "icecondor_authenticated_user_id";
    private LoginFragment loginFragment;
    private LoginEmailFragment loginEmailFragment;
    private LoginPassFragment loginPassFragment;
    private TokenValidateFragment tokenValidateFragment;
    private Fragment currentLoginFragment;
    private String token;
    private String userDetailApiId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.APP_TAG, "login.Main onCreate");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        setContentView(R.layout.login);

        loginFragment = new LoginFragment();
        loginEmailFragment = new LoginEmailFragment();
        loginPassFragment = new LoginPassFragment();
        tokenValidateFragment = new TokenValidateFragment();

        switchFragment(loginFragment);
    }

    @Override
    protected void onStart() {
        super.onStart();
        token = prefs.getUnvalidatedToken();
        Log.d(Constants.APP_TAG, "login.Main onStart token: "+token);

        if(token == null) {
            switchLoginFragment(loginEmailFragment);
        } else {
            prefs.clearUnvalidatedToken();
            switchLoginFragment(tokenValidateFragment);
        }
    }

    protected void switchLoginFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.login_body_fragment, fragment).commit();
        currentLoginFragment = fragment;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        refreshStatusFromCondor(condor);
        if(token != null && condor.isConnected()) {
            Log.d(Constants.APP_TAG, "login.Main onStart condor connected!");
            processToken();
        }
    }

    private void refreshStatusFromCondor(Condor condor) {
        if(condor != null) {
            switch (condor.getNetworkState()) {
            case CONNECTED:
                loginIsOk();
                break;
            case CONNECTING:
                loginFragment.setStatusText("connecting...");
                break;
            case WAITING:
                loginFragment.setStatusText("waiting...");
                break;
            }
        }
    }

    private void loginIsOk() {
        loginFragment.setStatusText("ready.");
        loginEmailFragment.enableLoginField();
    }

    /* UiActions */
    @Override
    public void onConnecting(URI uri) {
        Log.d(Constants.APP_TAG, "login.Main onConnecting");
        refreshStatusFromCondor(condor);
    }

    @Override
    public void onConnected() {
        Log.d(Constants.APP_TAG, "login.Main onConnected");
        refreshStatusFromCondor(condor);
        if(currentLoginFragment == tokenValidateFragment) {
            Log.d(Constants.APP_TAG, "login.Main onConnected processToken");
            processToken();
        }
    }

    public void processToken() {
        Log.d(Constants.APP_TAG, "login.Main processToken");
        tokenValidateFragment.indicateProcessToken();
        userDetailApiId = condor.doUserDetail();
        Log.d(Constants.APP_TAG, "login.Main processToken doUserDetail "+userDetailApiId);
    }

    public void goodUser(JSONObject user) {
        Log.d(Constants.APP_TAG, "login.Main processToken");
        String userId;
        try {
            userId = user.getString("id");
            prefs.setAuthenticatedUserId(userId);
            tokenValidateFragment.indicateSuccess();
            Intent start = new Intent(this, Start.class);
            startActivity(start);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnected() {
        Log.d(Constants.APP_TAG, "login.Main onDisconnected");
        loginFragment.setStatusText("disconnected!");
    }

    @Override
    public void onNewActivity() {
    }

    @Override
    public void onApiResult(String id, JSONObject result) {
        Log.d(Constants.APP_TAG, "login.Main onApiResult "+id+" "+result);
        if(id.equals(userDetailApiId)) {
            goodUser(result);
        }
    }

    @Override
    public void onTimeout() {
        Log.d(Constants.APP_TAG, "login.Main onTimeout");
        refreshStatusFromCondor(condor);
    }

    /* Login email field */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if(v.getId() == R.id.login_email_field) {
            if(actionId == EditorInfo.IME_ACTION_SEND) {
                Log.d(Constants.APP_TAG, "LoginFragment: action: "+actionId+" emailField "+v.getText());
                emailFieldReady(v.getText().toString());
            }
        }
        return false;
    }

    private void emailFieldReady(String email) {
        if(condor.isConnected()) {
            condor.doAccountAuth(email);
            emailSent();
        }
    }

    private void emailSent() {
        loginFragment.setStatusText("Email sent. Please check your email and click the login button.");
        loginEmailFragment.disableLoginField();
    }

}
