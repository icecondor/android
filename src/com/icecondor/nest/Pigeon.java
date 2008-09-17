package com.icecondor.nest;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;

//look at android.permission.RECEIVE_BOOT_COMPLETED

public class Pigeon extends Service {
	private static final long UPDATE_INTERVAL = 5000;
	private Timer timer = new Timer();
	static final String appTag = "IcePigeon";
	
	public void onCreate() {
		Log.i("Pigeon", "service created.");
		timer.scheduleAtFixedRate(
			new TimerTask() {
				public void run() {
					Location fix;
					Log.i(appTag, "getting locationservice");
					LocationManager location_service = (LocationManager) getSystemService(LOCATION_SERVICE);
					Log.i(appTag, "getting fix");
					fix = location_service.getLastKnownLocation("gps");
					Log.i(appTag, "got fix - ");
					pushLocation(fix);					
				}
			}, 0, UPDATE_INTERVAL);		
	}
	
	public void onStart() {
		Log.i(appTag, "service started!");
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void pushLocation(Location fix) {
		Log.i(appTag, "sending fix: "+fix.getLatitude()+" "+fix.getLongitude());
	}

}
