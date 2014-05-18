package com.icecondor.eaglet.ui.alist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.R;
import com.icecondor.eaglet.ui.BaseActivity;
import com.icecondor.eaglet.ui.Preferences;
import com.icecondor.eaglet.ui.login.Main;

public class MainActivity extends BaseActivity {

    private ActivityListFragment aList;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.APP_TAG, "MainActivity onCreate");
        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        aList = new ActivityListFragment();

        if (savedInstanceState == null) {
            switchFragment(aList);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(Constants.APP_TAG, "MainActivity onStart");
        startService(condorIntent); // keep this for STICKY result
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Constants.APP_TAG, "MainActivity onResume");

        if(prefs.getString(Main.PREF_KEY_AUTHENTICATED_USER_ID, null) == null) {
            startActivity(new Intent(this, Main.class));
            return;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent preference = new Intent(this, Preferences.class);
            startActivity(preference);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean handleMessage(Message msg) {
        Log.d(Constants.APP_TAG, "alist.MainActivity: handleMessage "+msg.obj);
        if((int)msg.obj == Constants.NEW_ACTIVITY) {
            aList.invalidateView();
        }
        return false;
    }

}
