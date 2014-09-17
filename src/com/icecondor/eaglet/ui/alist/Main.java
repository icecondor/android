package com.icecondor.eaglet.ui.alist;

import java.net.URI;

import org.json.JSONObject;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.LayoutParams;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.R;
import com.icecondor.eaglet.ui.BaseActivity;
import com.icecondor.eaglet.ui.UiActions;

public class Main extends BaseActivity implements UiActions, CompoundButton.OnCheckedChangeListener {

    CompoundButton onOff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.APP_TAG, "alist.MainActivity onCreate");

        actionBarExtraSetup();

        if (savedInstanceState == null) {
            switchFragment(actListFragment);
        }
    }

    public void actionBarExtraSetup() {
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        View customNav = LayoutInflater.from(this).inflate(R.layout.action_bar_extra, null);
        onOff = (CompoundButton)customNav.findViewById(R.id.actionbar_onoff);
        onOff.setOnCheckedChangeListener(this);
        ActionBar bar = getSupportActionBar();
        bar.setCustomView(customNav, lp);
        bar.setDisplayShowCustomEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Constants.APP_TAG, "alist.MainActivity onResume");
        onOff.setChecked(prefs.isOnOff());
        enableServiceHandler();
        authCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableServiceHandler();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnecting(URI uri) {
        Log.d(Constants.APP_TAG, "alist.MainActivity callback onConnecting");
    }

    @Override
    public void onConnected() {
        Log.d(Constants.APP_TAG, "alist.MainActivity callback onConnected");
    }

    @Override
    public void onDisconnected() {
        Log.d(Constants.APP_TAG, "alist.MainActivity callback onDisconnected");
    }

    @Override
    public void onConnectTimeout() {
        Log.d(Constants.APP_TAG, "alist.MainActivity callback onTimeout");
    }

    @Override
    public void onNewActivity() {
        Log.d(Constants.APP_TAG, "alist.MainActivity: callback onNewActivity");
        actListFragment.invalidateView();
    }

    @Override
    public void onApiResult(String id, JSONObject result) {
        Log.d(Constants.APP_TAG, "alist.Main onApiResult "+id+" "+result);
    }

    @Override
    public void onApiError(String id, JSONObject error) {
        Log.d(Constants.APP_TAG, "alist.Main onApierror "+id+" "+error);
    }

    @Override
    public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
        if(btn.getId() == R.id.actionbar_onoff){
            if(condor != null) {
                condor.setRecording(isChecked);
            }
        }
    }

}
