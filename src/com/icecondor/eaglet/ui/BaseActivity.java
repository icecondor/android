package com.icecondor.eaglet.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.icecondor.eaglet.Condor;
import com.icecondor.eaglet.Condor.LocalBinder;
import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.Prefs;
import com.icecondor.eaglet.R;
import com.icecondor.eaglet.ui.alist.ActivityListFragment;
import com.icecondor.eaglet.ui.alist.SettingsFragment;
import com.icecondor.eaglet.ui.login.Main;

abstract public class BaseActivity extends ActionBarActivity
                                   implements ServiceConnection,
                                              UiActions, OnItemClickListener {
    protected Prefs prefs;
    private LocalBinder localBinder;
    protected Condor condor;
    protected Intent condorIntent;
    private Handler handler;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private ListView drawerList;
    private SettingsFragment settingsFragment;
    protected ActivityListFragment actListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.APP_TAG, "BaseActivity("+this.getClass().getName()+"): onCreate");
        Log.d(Constants.APP_TAG, "BaseActivity("+this.getClass().getName()+"): data "+getIntent().getData());
        prefs = new Prefs(this);
        condorIntent = new Intent(this, Condor.class);
        handler = new Handler();
        setContentView(R.layout.activity_main);
        ActionBar bar = getSupportActionBar();
        drawerSetup(bar);
        settingsFragment = new SettingsFragment(handler);
        actListFragment = new ActivityListFragment(handler);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(Constants.APP_TAG, "BaseActivity("+this.getClass().getName()+"): onStart");
        startService(condorIntent); // keep this for STICKY result
    }

    public void drawerSetup(ActionBar bar) {
        drawerList = (ListView) findViewById(R.id.left_drawer);
        ArrayList<Map<String, ?>> list = new ArrayList<Map<String, ?>>();
        populateDrawer(list);
        drawerList.setAdapter(new SimpleAdapter(this, list,
                                       R.layout.drawer_list_item,
                                       new String[]{"icon", "name"},
                                       new int[]{R.id.drawer_row_icon, R.id.drawer_row_name}){

        });
        drawerList.setOnItemClickListener(BaseActivity.this);
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

    public void populateDrawer(ArrayList<Map<String, ?>> list) {
        HashMap<String, Object> item;
        item = new HashMap<String, Object>();
        item.put("icon",R.drawable.ic_launcher);
        item.put("name", prefs.getAuthenticatedUsername());
        list.add(item);
        item = new HashMap<String, Object>();
        item.put("icon",R.drawable.ic_launcher);
        item.put("name", "Activity");
        list.add(item);
        item = new HashMap<String, Object>();
        item.put("icon",R.drawable.ic_launcher);
        item.put("name", "Settings");
        list.add(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(Constants.APP_TAG, "BaseActivity: onItemClick position:"+position+" id:"+id);
        if(position == 0) {
            // User
        }
        if(position == 1) {
            // Activity List
            switchFragment(actListFragment);
            drawerLayout.closeDrawers();
        }
        if(position == 2) {
            // Settings
            switchFragment(settingsFragment);
            drawerLayout.closeDrawers();
        }
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
        Log.d(Constants.APP_TAG, "BaseActivity("+this.getClass().getName()+"): onResume");
        bindService(condorIntent, this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(Constants.APP_TAG, "BaseActivity("+this.getClass().getName()+"): onPause");
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
        enableServiceHandler();
        condor = localBinder.getService();
    }

    protected void enableServiceHandler() {
        if(localBinder != null) {
            localBinder.setHandler(handler, this);
        }
    }

    protected void disableServiceHandler() {
        if(localBinder != null) {
            localBinder.clearHandler();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(Constants.APP_TAG, "BaseActivity: onServiceDisconnected "+name.flattenToShortString());
    }

    public void authCheck() {
        if(prefs.getAuthenticatedUserId() == null) {
            startActivity(new Intent(this, Main.class));
            return;
        }
    }
}
