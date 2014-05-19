package com.icecondor.eaglet.ui.login;

import java.net.URI;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.R;
import com.icecondor.eaglet.ui.BaseActivity;
import com.icecondor.eaglet.ui.UiActions;

public class Main extends BaseActivity implements UiActions {
    public static String PREF_KEY_AUTHENTICATED_USER_ID = "icecondor_authenticated_user_id";
    LoginFragment loginFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.APP_TAG, "login.Main onCreate");
        setContentView(R.layout.login);

        loginFragment = new LoginFragment();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (savedInstanceState == null) {
            switchFragment(loginFragment);
        }
    }

    @Override
    public void onConnecting(URI uri) {
        Log.d(Constants.APP_TAG, "login.Main onConnecting");
        loginFragment.setStatusText("connecting...");
    }

    @Override
    public void onConnected() {
        Log.d(Constants.APP_TAG, "login.Main onConnected");
        loginFragment.setStatusText("connected.");
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
    public void onTimeout() {
        Log.d(Constants.APP_TAG, "login.Main onTimeout");
    }


}
