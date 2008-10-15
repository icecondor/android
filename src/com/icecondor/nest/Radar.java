package com.icecondor.nest;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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

public class Radar extends MapActivity implements ServiceConnection,
												  Constants {
	static final String appTag = "Radar";
	MapController controller;
	PigeonService pigeon;
	private static final long UPDATE_INTERVAL = 5000;
	private Timer pigeon_poll_timer = new Timer();
	private Timer service_read_timer = new Timer();
	SharedPreferences settings;
	String URL = "http://icecondor.com/locations"; // use preference
	
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences(PREFS_NAME, 0);
        setContentView(R.layout.radar);
        Intent pigeon_service = new Intent(this, Pigeon.class);
        bindService(pigeon_service, this, 0); // 0 = do not auto-start
        MapView mapView = (MapView) findViewById(R.id.radar_mapview);
        controller = mapView.getController();
        controller.setZoom(9);
		pigeon_poll_timer.scheduleAtFixedRate(
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
		service_read_timer.scheduleAtFixedRate(
				new TimerTask() {
					public void run() {
						getNearbys();
					}
				}, 0, UPDATE_INTERVAL);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	Log.i(appTag, "onResume yeah");
    }
    
    @Override
    public void onPause() {
    	super.onPause();
		unbindService(this);
    	pigeon_poll_timer.cancel();
    	service_read_timer.cancel();
    	Log.i(appTag, "onPause yeah");
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

	public void getNearbys() {
		try {
			Log.i(appTag, "send read request");
			HttpClient client = new DefaultHttpClient();
			URL = URL + "?id=wtf";
			HttpGet get = new HttpGet(URL);

			get.addHeader("X_REQUESTED_WITH", "XMLHttpRequest");
			HttpResponse response;
			response = client.execute(get);
			Log.i(appTag, "http response: "+response.getStatusLine());
		} catch (ClientProtocolException e) {
			Log.i(appTag, "client protocol exception "+e);
		} catch (HttpHostConnectException e) {
			Log.i(appTag, "connection failed "+e);
		} catch (IOException e) {
			Log.i(appTag, "IO exception "+e);
			e.printStackTrace();
		}
	}
}
