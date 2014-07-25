package com.icecondor.eaglet.ui.login;

import java.net.URI;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.icecondor.eaglet.Condor;
import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.R;
import com.icecondor.eaglet.ui.BaseActivity;
import com.icecondor.eaglet.ui.UiActions;

public class Main extends BaseActivity implements UiActions, OnEditorActionListener {
    public static String PREF_KEY_AUTHENTICATED_USER_ID = "icecondor_authenticated_user_id";
    private LoginFragment loginFragment;
    private LoginEmailFragment loginEmailFragment;
    private LoginPassFragment loginPassFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.APP_TAG, "login.Main onCreate");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        setContentView(R.layout.login);

        loginFragment = new LoginFragment();
        loginEmailFragment = new LoginEmailFragment();
        loginPassFragment = new LoginPassFragment();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (savedInstanceState == null) {
            switchFragment(loginFragment);
            switchLoginFragment(loginEmailFragment);
        }
    }

    protected void switchLoginFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.login_body_fragment, fragment).commit();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        refreshStatusFromCondor(condor);
    }

    private void refreshStatusFromCondor(Condor condor) {
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
        refreshStatusFromCondor(condor);
    }

    /* Login email field */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if(v.getId() == R.id.login_email_field) {
            if(actionId == EditorInfo.IME_ACTION_SEND) {
                Log.d(Constants.APP_TAG, "LoginFragment: action: "+actionId+" emailField "+v.getText());
                if(condor.isConnected()) {
                    condor.doAccountAuth(v.getText().toString());
                    loginFragment.setStatusText("Email sent. Please check your email and click the login button.");
                }
            }
        }
        return false;
    }

}
