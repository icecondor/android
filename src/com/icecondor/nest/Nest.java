package com.icecondor.nest;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class Nest extends Activity {
	TabHost myTabHost;
	static final String appTag = "IceNest";
	static final String version = "20080924";
	public static final String PREFS_NAME = "IceNestPrefs";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        uiSetup();
        
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean startPigeon = settings.getBoolean("startPigeon", true);
        if (startPigeon) {
            startPigeon();
        }
    }

	private void startPigeon() {
		// Start the pigeon service
        Intent pigeon_service = new Intent(this, Pigeon.class);
        startService(pigeon_service);
        Log.i(appTag, "in create: Pigeon service start");
	}

	private void uiSetup() {
		this.myTabHost = (TabHost)this.findViewById(R.id.th_set_menu_tabhost);
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
}