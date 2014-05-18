package com.icecondor.eaglet.ui.login;

import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.R;
import com.icecondor.eaglet.ui.BaseActivity;

public class Main extends BaseActivity {
    public static String PREF_KEY_AUTHENTICATED_USER_ID = "icecondor_authenticated_user_id";
    Fragment loginFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.APP_TAG, "MainActivity onCreate");
        setContentView(R.layout.login);

        loginFragment = new LoginFragment();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (savedInstanceState == null) {
            switchFragment(loginFragment);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d(Constants.APP_TAG, "login.Main: handleMessage "+msg.obj);
        if((int)msg.obj == Constants.NEW_ACTIVITY) {
        }
        return false;
    }

}
