package com.icecondor.nest;

import java.util.UUID;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class Start extends Activity implements ServiceConnection {
	static final String appTag = "Start";
	public static final String PREFS_NAME = "IceNestPrefs";
	Intent pigeon_intent;
	PigeonService pigeon;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
        pigeon_intent = new Intent(this, Pigeon.class);
        startPigeon();
    }

    private void restorePreferences() {
		Log.i(appTag, "restorePreferences()");
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		Editor editor = settings.edit();

        // Set the unique ID
		String uuid;
		if(settings.contains("uuid")) {
			uuid = settings.getString("uuid", null);
			Log.i(appTag, "retrieved UUID of "+uuid);
		} else {
			uuid = "urn:uuid:"+UUID.randomUUID().toString();
			editor.putString("uuid", uuid);
			Log.i(appTag, "no UUID in preferences. generated "+uuid);
		}
		
		// Start the Pigeon
        boolean startPigeon = settings.getBoolean("startPigeon", true);
        if (startPigeon) {
            try {
				pigeon.startTransmitting();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
	}

	private void startPigeon() {
		// Start the pigeon service
    	Intent pigeon_service = new Intent(this, Pigeon.class);
        startService(pigeon_service);
        bindService(pigeon_intent, this, 0); // 0 = do not auto-start
	}
	
	private void stopPigeon() {
		Log.i(appTag, "stopPigeon");
		unbindService(this);
		stopService(new Intent(this, Pigeon.class));
	}
	
	public void onPause() {
		super.onPause();
		unbindService(this);
		finish();
	}
	public void onServiceConnected(ComponentName className, IBinder service) {
		Log.i(appTag, "onServiceConnected "+service);
		pigeon = PigeonService.Stub.asInterface(service);
        restorePreferences();
        // handoff to the Radar
        startActivity(new Intent(this, Radar.class));
	}

	public void onServiceDisconnected(ComponentName className) {
		Log.i(appTag, "onServiceDisconnected "+className);
		
	}

}
