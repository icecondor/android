package com.icecondor.nest;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TabHost;
import android.widget.TextView;

public class Settings extends PreferenceActivity implements ServiceConnection,    
                                                            Constants {
	TabHost myTabHost;
	static final String appTag = "Settings";
	PigeonService pigeon;
	SharedPreferences settings;
	LinearLayout settings_layout;
	int identity_url_id;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		addPreferencesFromResource(R.layout.settings);
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
    	Preference auth_pref = getPreferenceScreen().findPreference("authentication");
    	if (LocationRepositoriesSqlite.has_access_token(this)) {
    		String openid = settings.getString(SETTING_OPENID, null);
    		auth_pref.setSummary(openid);
    	} else {
    		auth_pref.setSummary("Access token missing.");
    	}
    	Preference xmit_pref = getPreferenceScreen().findPreference("transmission frequency");
    	String minutes = Util.millisecondsToWords(Long.parseLong(settings.getString(SETTING_TRANSMISSION_FREQUENCY, "")));
    	xmit_pref.setSummary("every "+minutes);    	
    	Preference rss_pref = getPreferenceScreen().findPreference("rss read frequency");
    	minutes = Util.millisecondsToWords(Long.parseLong(settings.getString(SETTING_TRANSMISSION_FREQUENCY, "")));
    	rss_pref.setSummary("every "+minutes);    	
    }
    
    @Override
    public void onPause() {
    	super.onPause();
		unbindService(this);
    	Log.i(appTag, "onPause yeah");
    }

	public void onServiceConnected(ComponentName className, IBinder service) {
		Log.i(appTag, "onServiceConnected "+service);
		pigeon = PigeonService.Stub.asInterface(service);
	}

	public void onServiceDisconnected(ComponentName className) {
		Log.i(appTag, "onServiceDisconnected "+className);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(appTag, "onCreateOptionsMenu");
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, Menu.FIRST, 0, "About").setIcon(android.R.drawable.star_on);
		menu.add(0, R.string.menu_radar, 0, R.string.menu_radar).setIcon(android.R.drawable.ic_menu_compass);
		return result;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(appTag, "menu:"+item.getItemId());
		if (item.getItemId() == R.string.menu_radar) {
			startActivity(new Intent(this, Radar.class));
		}
		if (item.getItemId() == Menu.FIRST) {
			showDialog(1);
		}
		return false;
	}

	public void onFocusChange(View arg0, boolean arg1) {
		// TODO Auto-generated method stub
		
	}
	
	protected Dialog onCreateDialog(int id) {
		return new AlertDialog.Builder(this)
		.setTitle("About IceCondor ")
		.setMessage(R.string.about)
		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichbutton) {

				}})
		.create();
	}
}