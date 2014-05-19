package com.icecondor.eaglet.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.icecondor.eaglet.Condor;
import com.icecondor.eaglet.Condor.LocalBinder;
import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.R;

abstract public class BaseActivity extends ActionBarActivity implements ServiceConnection,
                                                                        UiActions {
    protected SharedPreferences prefs;
    private LocalBinder localBinder;
    protected Condor condor;
    protected Intent condorIntent;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        condorIntent = new Intent(this, Condor.class);
        Log.d(Constants.APP_TAG, "BaseActivity: onCreate new Handler "+this);
        handler = new Handler();
    }

    protected void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.container, fragment).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(condorIntent, this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(condor != null) {
            condor = null;
            unbindService(this);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(Constants.APP_TAG, "BaseActivity: onServiceConnected "+name.flattenToShortString());
        localBinder = (Condor.LocalBinder)service;
        localBinder.setHandler(handler, this);
        condor = localBinder.getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(Constants.APP_TAG, "BaseActivity: onServiceDisconnected "+name.flattenToShortString());
    }
}
