package com.icecondor.eaglet.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.icecondor.eaglet.Condor;
import com.icecondor.eaglet.Condor.LocalBinder;
import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.R;
import com.icecondor.eaglet.ui.login.Main;

abstract public class BaseActivity extends ActionBarActivity implements ServiceConnection,
                                                                        UiActions {
    protected SharedPreferences prefs;
    private LocalBinder localBinder;
    protected Condor condor;
    protected Intent condorIntent;
    private Handler handler;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private ListView drawerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        condorIntent = new Intent(this, Condor.class);
        Log.d(Constants.APP_TAG, "BaseActivity: onCreate new Handler "+this);
        handler = new Handler();
        setContentView(R.layout.activity_main);
        drawerSetup();
    }

    public void drawerSetup() {
        ActionBar bar = getSupportActionBar();

        drawerList = (ListView) findViewById(R.id.left_drawer);
        ArrayList<Map<String, ?>> list = new ArrayList<Map<String, ?>>();
        HashMap<String, Object> item = new HashMap<String, Object>();
        item.put("icon",R.drawable.ic_launcher);
        item.put("name", prefs.getString(Main.PREF_KEY_AUTHENTICATED_USER_ID, null));
        list.add(item);
        drawerList.setAdapter(new SimpleAdapter(this, list,
                                       R.layout.drawer_list_item,
                                       new String[]{"icon", "name"},
                                       new int[]{R.id.drawer_row_icon, R.id.drawer_row_name}){

        });

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this,
                                                 drawerLayout,
                                                 R.drawable.ic_navigation_drawer,
                                                 R.string.drawer_open,
                                                 R.string.drawer_closed)
        {

            /** Called when a drawer has settled in a completely closed state. */
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            /** Called when a drawer has settled in a completely open state. */
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(true);
    }

    protected void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.container, fragment).commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Constants.APP_TAG, "BaseActivity("+this.getClass().getSimpleName()+"): onResume");
        bindService(condorIntent, this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(Constants.APP_TAG, "BaseActivity("+this.getClass().getSimpleName()+"): onPause");
        if(condor != null) {
            condor = null;
            unbindService(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
          return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
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
