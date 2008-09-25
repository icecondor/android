package com.icecondor.nest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

public class Nest extends Activity implements OnTabChangeListener,
                                              ServiceConnection{
	TabHost myTabHost;
	static final String appTag = "IceNest";
	static final String version = "20080924";
	public static final String PREFS_NAME = "IceNestPrefs";
	Intent pigeon_service;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pigeon_service = new Intent(this, Pigeon.class);
        setContentView(R.layout.main);
        uiSetup();
        restorePreferences();
    }

	private void restorePreferences() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean startPigeon = settings.getBoolean("startPigeon", true);
        if (startPigeon) {
            startPigeon();
        }
	}

	private void startPigeon() {
		// Start the pigeon service
        startService(pigeon_service);
        Log.i(appTag, "in create: Pigeon service start");
        bindService(pigeon_service, this, 0); // do not auto-start
	}

	private void uiSetup() {
		this.myTabHost = (TabHost)this.findViewById(R.id.th_set_menu_tabhost);
		this.myTabHost.setOnTabChangedListener(this);
        this.myTabHost.setup();
        TabSpec ts1 = myTabHost.newTabSpec("TAB1");
        ts1.setIndicator(getString(R.string.tab_title1), null);
        ts1.setContent(R.id.grid_set_menu_radar);
        this.myTabHost.addTab(ts1);
        
        TabSpec ts2 = myTabHost.newTabSpec("TAB2");
        ts2.setIndicator(getString(R.string.tab_title2), null);
        ts2.setContent(R.id.grid_set_menu_settings);
        this.myTabHost.addTab(ts2);
        
        this.myTabHost.setCurrentTab(0);
	}

    @Override
    public void onStart() {
    	super.onStart();
    	Log.i(appTag, "onStart yeah");
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	Log.i(appTag, "onResume yeah");
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	Log.i(appTag, "onPause yeah");
    }

	public void onTabChanged(String tabId) {
		Log.i(appTag, "changed to tab "+tabId);
		if (tabId == "TAB2") {
			updateSettingTabWidgets();
		}
	}

	private void updateSettingTabWidgets() {
		Log.i(appTag, "updateSettingTabWidgets");
		((RadioButton) findViewById(R.id.ibtn_settings_pigeon_on)).setChecked(isPigeonOn());
		
	}

	private boolean isPigeonOn() {
		// better way to do this?
        return bindService(pigeon_service, this, 0); // do not auto-start
	}

	public void onServiceConnected(ComponentName arg0, IBinder arg1) {
		Log.i(appTag, "onServiceConnected "+arg0);
		
	}

	public void onServiceDisconnected(ComponentName name) {
		Log.i(appTag, "onServiceDisconnected "+name);
		
	}
}