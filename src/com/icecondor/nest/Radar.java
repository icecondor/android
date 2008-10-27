package com.icecondor.nest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

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
import android.view.ViewGroup;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class Radar extends MapActivity implements ServiceConnection,
												  Constants {
	static final String appTag = "Radar";
	MapController controller;
	PigeonService pigeon;
	private Timer pigeon_poll_timer = new Timer();
	private Timer service_read_timer = new Timer();
	SharedPreferences settings;
	Overlay nearbys;
	
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences(PREFS_NAME, 0);
        setContentView(R.layout.radar);
        ViewGroup radar_zoom = (ViewGroup)findViewById(R.id.radar_mapview_zoom);
        MapView mapView = (MapView) findViewById(R.id.radar_mapview);
        radar_zoom.addView(mapView.getZoomControls());
        controller = mapView.getController();
        controller.setZoom(15);
        nearbys = new BirdOverlay();
        mapView.getOverlays().add(nearbys);
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
				}, 0, PIGEON_LOCATION_POST_INTERVAL);
		service_read_timer.scheduleAtFixedRate(
				new TimerTask() {
					public void run() {
						getNearbys();
					}
				}, 0, PIGEON_LOCATION_POST_INTERVAL);
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
			startActivity(new Intent(this, Settings.class));
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
			HttpClient client = new DefaultHttpClient();
			String url_with_params = ICECONDOR_WRITE_URL + "?id="
					+ settings.getString("uuid", "");
			Log.i(appTag, "GET " + url_with_params);
			HttpGet get = new HttpGet(url_with_params);
			HttpResponse response;
			response = client.execute(get);
			Log.i(appTag, "http response: " + response.getStatusLine());
			HttpEntity entity = response.getEntity();
			InputStream instream = entity.getContent();
			int length = (int)entity.getContentLength();
			InputStreamReader reader = new InputStreamReader(
					instream);
			Log.i(appTag, "reading "+length);
			char[] buffer = new char[length];
			reader.read(buffer);
			Log.i(appTag, "parsing: "+ new String(buffer));
//			try {
//				JSONArray locations = new JSONArray(buffer.toString());
//				Log.i(appTag, "parsed "+locations.length()+" locations");
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
		} catch (ClientProtocolException e) {
			Log.i(appTag, "client protocol exception " + e);
		} catch (HttpHostConnectException e) {
			Log.i(appTag, "connection failed "+e);
		} catch (IOException e) {
			Log.i(appTag, "IO exception "+e);
			e.printStackTrace();
		}
	}
}