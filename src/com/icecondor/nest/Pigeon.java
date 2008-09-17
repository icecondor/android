package com.icecondor.nest;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
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
					Log.i(appTag, "I am a background process running periodically.");
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

}
