package com.icecondor.nest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

public class Nest extends Activity implements OnTabChangeListener,
                                              ServiceConnection, 
                                              OnClickListener,
                                              Constants {
	TabHost myTabHost;
	static final String appTag = "Nest";
	Intent pigeon_service;
	PigeonService pigeon;
	SharedPreferences settings;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
        pigeon_service = new Intent(this, Pigeon.class);
        settings = getSharedPreferences(PREFS_NAME, 0);
        uiSetup();       
        // sideeffect: sets pigeon_service
        bindService(pigeon_service, this, 0); // 0 = do not auto-start
    }

	private void uiSetup() {
        setContentView(R.layout.main);
		this.myTabHost = (TabHost)this.findViewById(R.id.th_set_menu_tabhost);
		this.myTabHost.setOnTabChangedListener(this);
        this.myTabHost.setup();
        ((RadioButton) findViewById(R.id.ibtn_settings_pigeon_on)).setOnClickListener(this);
        TabSpec ts1 = myTabHost.newTabSpec("TAB1");
        ts1.setIndicator(getString(R.string.tab_title1), null);
        ts1.setContent(R.id.grid_set_menu_radar);
        this.myTabHost.addTab(ts1);
        
        ((EditText) findViewById(R.id.settings_uuid)).setText(settings.getString("uuid", "n/a"));
        
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
		boolean checked;
		try {
			checked = isPigeonOn();
		} catch (RemoteException e) {
			checked = false;
			Log.i(appTag, "cannot reach pigeon service to display in settings tab");
		}
		((RadioButton) findViewById(R.id.ibtn_settings_pigeon_on)).setChecked(checked);
	}

	private boolean isPigeonOn() throws RemoteException {
		boolean result = pigeon.isTransmitting();
        return result;
	}

	public void onServiceConnected(ComponentName className, IBinder service) {
		Log.i(appTag, "onServiceConnected "+service);
		pigeon = PigeonService.Stub.asInterface(service);
	}

	public void onServiceDisconnected(ComponentName className) {
		Log.i(appTag, "onServiceDisconnected "+className);
		
	}

	public void onClick(View arg0) {
		if (arg0.getId() == R.id.ibtn_settings_pigeon_on) {
			try {
				if(isPigeonOn() == true) {
					pigeon.stopTransmitting();
				} else {
					pigeon.startTransmitting();
				}
				((RadioButton) findViewById(R.id.ibtn_settings_pigeon_on)).setChecked(isPigeonOn());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(appTag, "onCreateOptionsMenu");
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, Menu.FIRST, 0, R.string.menu_version);
		menu.add(0, R.string.menu_first, 0, R.string.menu_first);
		menu.add(0, R.string.menu_second, 0, R.string.menu_second);
		return result;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(appTag, "menu:"+item.getItemId());
		if (item.getItemId() == R.string.menu_first) {
			startActivity(new Intent(this, Radar.class));
		}
		return false;
	}
}