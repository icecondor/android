package com.icecondor.nest;

import java.util.Timer;
import java.util.TimerTask;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class Radar extends MapActivity implements ServiceConnection {
	static final String appTag = "Radar";
	MapController controller;
	PigeonService pigeon;
	private static final long UPDATE_INTERVAL = 5000;
	
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.radar);
        Intent pigeon_service = new Intent(this, Pigeon.class);
        bindService(pigeon_service, this, 0); // 0 = do not auto-start
        MapView mapView = (MapView) findViewById(R.id.radar_mapview);
        controller = mapView.getController();
        controller.setZoom(9);
    	Timer timer = new Timer();
		timer.scheduleAtFixedRate(
				new TimerTask() {
					public void run() {
						try {
							Location fix = pigeon.getLastFix();
							Log.i(appTag, "radar: pigeon says last fix is "+fix);
							controller.animateTo(new GeoPoint((int)(fix.getLatitude()*1000000),
									                          (int)(fix.getLongitude()*1000000)));
						} catch (RemoteException e) {
							Log.e(appTag, "radar: error reading fix from pigeon.");
							e.printStackTrace();
						}
					}
				}, 0, UPDATE_INTERVAL);
    }
    
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	// copy/pasted from nest.java. extract into superclass?
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
		if (item.getItemId() == R.string.menu_second) {
			startActivity(new Intent(this, Nest.class));
		}
		return false;
	}
	
	public void onServiceConnected(ComponentName className, IBinder service) {
		Log.i(appTag, "onServiceConnected "+service);
		pigeon = PigeonService.Stub.asInterface(service);
	}

	public void onServiceDisconnected(ComponentName className) {
		Log.i(appTag, "onServiceDisconnected "+className);
		
	}


}
