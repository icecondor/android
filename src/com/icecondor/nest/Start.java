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

public class Start extends Activity implements ServiceConnection {
	static final String appTag = "Start";
	public static final String PREFS_NAME = "IceNestPrefs";
	PigeonService pigeon;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
        startPigeon();
        bindService(new Intent(this, Pigeon.class), this, 0); // 0 = do not auto-start
    }

    private void restorePreferences() {
		Log.i(appTag, "restorePreferences()");
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
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
	}
	
	private void stopPigeon() {
		Log.i(appTag, "stopPigeon");
		//stopService(pigeon_service);
	}
	
	public void onServiceConnected(ComponentName className, IBinder service) {
		Log.i(appTag, "onServiceConnected "+service);
		pigeon = PigeonService.Stub.asInterface(service);
        restorePreferences();
        // handoff to the Radar
        startActivity(new Intent(this, Radar.class));
        finish();
	}

	public void onServiceDisconnected(ComponentName className) {
		Log.i(appTag, "onServiceDisconnected "+className);
		
	}

}
