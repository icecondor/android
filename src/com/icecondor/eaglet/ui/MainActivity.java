package com.icecondor.eaglet.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.icecondor.eaglet.Condor;
import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.R;

public class MainActivity extends ActionBarActivity implements ServiceConnection {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.APP_TAG, "MainActivity onCreate");
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            switchFragment(new ActivityListFragment());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(Constants.APP_TAG, "MainActivity onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Constants.APP_TAG, "MainActivity onResume");

        Intent bird = new Intent(this, Condor.class);
        startService(bird);
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

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.container, fragment).commit();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(Constants.APP_TAG, "MainActivity onServiceConnected");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(Constants.APP_TAG, "MainActivity onServiceDisconnected");

    }

}
