package com.icecondor.nest;

import android.preference.PreferenceActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.LinearLayout;

public class Settings extends PreferenceActivity implements ServiceConnection,    
															OnClickListener,
                                                            Constants {
	TabHost myTabHost;
	static final String appTag = "Nest";
	PigeonService pigeon;
	SharedPreferences settings;
	LinearLayout settings_layout;
	int identity_url_id;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences(PREFS_NAME, 0);
        uiSetup();       
    }

	private void uiSetup() {
		addPreferencesFromResource(R.layout.settings);
        //setContentView(R.layout.main);
	}

    @Override
    public void onStart() {
    	super.onStart();
    	Log.i(appTag, "onStart yeah");
    }
    
    @Override
    public void onResume() {
    	super.onResume();
        Intent pigeon_service = new Intent(this, Pigeon.class);
        boolean result = bindService(pigeon_service, this, 0); // 0 = do not auto-start
        Log.i(appTag, "pigeon bind result="+result);
    	Log.i(appTag, "onResume yeah");
    }
    
    @Override
    public void onPause() {
    	super.onPause();
		unbindService(this);
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
		} else if (arg0.getId() == R.id.settings_uuid) {
			settings_layout.removeView(arg0);
			if(arg0.getClass() == TextView.class) {
				EditText edit_uuid = new EditText(this);
				edit_uuid.setId(R.id.settings_uuid_edit);
				edit_uuid.setText(settings.getString("uuid", "n/a"));
				settings_layout.addView(edit_uuid, 2);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(appTag, "onCreateOptionsMenu");
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, Menu.FIRST, 0, ""+ICECONDOR_VERSION);
		menu.add(0, R.string.menu_radar, 0, R.string.menu_radar).setIcon(android.R.drawable.ic_menu_compass);
		menu.add(0, R.string.menu_settings, 0, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences);
		return result;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(appTag, "menu:"+item.getItemId());
		if (item.getItemId() == R.string.menu_radar) {
			startActivity(new Intent(this, Radar.class));
		}
		return false;
	}

	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if(v.getId() == R.id.settings_uuid_edit) {
			Log.i(appTag, "keycode: "+keyCode);
			if(keyCode == 66) {
				add_uuid_display(v);
			}
		}
		return false;
	}

	private void add_uuid_display(View v) {
		settings.edit().putString("uuid",((EditText)v).getText().toString()).commit();
		settings_layout.removeView(v);
		TextView uuid = new TextView(this);
		uuid.setId(R.id.settings_uuid);
		uuid.setOnClickListener(this);
		uuid.setText(settings.getString("uuid", "n/a"));
		settings_layout.addView(uuid, 2);
	}

	public void onFocusChange(View arg0, boolean arg1) {
		// TODO Auto-generated method stub
		
	}
}