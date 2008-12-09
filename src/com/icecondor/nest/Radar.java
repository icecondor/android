package com.icecondor.nest;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class Radar extends MapActivity implements ServiceConnection,
												  Constants {
	static final String appTag = "Radar";
	MapController mapController;
	PigeonService pigeon;
	private Timer service_read_timer;
	Intent settingsIntent, geoRssIntent;
	SharedPreferences settings;
	BirdOverlay nearbys;
	EditText uuid_field;
	MapView mapView;
	
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
		settings = PreferenceManager.getDefaultSharedPreferences(this);
        settingsIntent = new Intent(this, Settings.class);
        geoRssIntent = new Intent(this, GeoRssList.class);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(false);
        
        setTitle(getString(R.string.app_name) + " v" + ICECONDOR_VERSION);

        setContentView(R.layout.radar);
        ViewGroup radar_zoom = (ViewGroup)findViewById(R.id.radar_mapview_zoom);
        mapView = (MapView) findViewById(R.id.radar_mapview);
        radar_zoom.addView(mapView.getZoomControls());
        mapController = mapView.getController();
        mapController.setZoom(15);
        nearbys = new BirdOverlay();
        mapView.getOverlays().add(nearbys);
    }
    
    public void scrollToLastFix() {
    	try {
    		if (pigeon != null) {
				mapController = mapView.getController();
				Location fix = pigeon.getLastFix();
				Log.i(appTag, "pigeon says last fix is " + fix);
				refreshBirdLocation();
				if (fix != null) {
					mapController.animateTo(new GeoPoint((int) (fix
							.getLatitude() * 1000000), (int) (fix
							.getLongitude() * 1000000)));
				}
			}
		} catch (RemoteException e) {
			Log.e(appTag, "error reading fix from pigeon.");
			e.printStackTrace();
		}
    }

	private void refreshBirdLocation() {
		try {
			if (pigeon!=null) {
				nearbys.setLast_fix(pigeon.getLastFix());
			}
		} catch (RemoteException e) {
			nearbys.setLast_fix(null);
		}
	}
    
    @Override
    public void onResume() {
    	super.onResume();
    	Log.i(appTag, "onResume yeah");
        Intent pigeon_service = new Intent(this, Pigeon.class);
        boolean result = bindService(pigeon_service, this, 0); // 0 = do not auto-start
        Log.i(appTag, "pigeon bind result="+result);
        scrollToLastFix();
        //startNeighborReadTimer();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
		unbindService(this);
    	//stopNeighborReadTimer();
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
		menu.add(Menu.NONE, 1, Menu.NONE, R.string.menu_last_fix).setIcon(android.R.drawable.ic_menu_mylocation);
		menu.add(Menu.NONE, 2, Menu.NONE, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, 3, Menu.NONE, R.string.menu_geo_rss).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, 4, Menu.NONE, pigeonStatusTitle()).setIcon(android.R.drawable.presence_invisible);
		return result;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(appTag, "menu:"+item.getItemId());
		
		switch (item.getItemId()) {
		case 1:
			scrollToLastFix();
			break;
		case 2:
			startActivity(settingsIntent);
			break;
		case 3:
			startActivity(geoRssIntent);
			break;
		case 4:
			togglePigeon();
			item.setIcon(pigeonStatusIcon()).setTitle(pigeonStatusTitle());
			break;
		}
		
		return false;
	}
	
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean result = super.onPrepareOptionsMenu(menu);
		menu.findItem(4).setIcon(pigeonStatusIcon()).setTitle(pigeonStatusTitle());
		return result;
	}
	
	public boolean togglePigeon() {
		try {
			if (pigeon.isTransmitting()) {
				pigeon.stopTransmitting();
				return false;
			} else {
				pigeon.startTransmitting();
				return true;
			}
		} catch (RemoteException e) {
			Log.e(appTag, "togglePigeon: pigeon communication error");
			return false;
		}
	}
	
	public int pigeonStatusIcon() {
		try {
			if(pigeon.isTransmitting()) {
				return android.R.drawable.presence_online;
			} else {
				return android.R.drawable.presence_invisible;
			}
		} catch (RemoteException e) {
			return android.R.drawable.presence_offline;
		}
	}
	
	public int pigeonStatusTitle() {
		try {
			if(pigeon.isTransmitting()) {
				return R.string.status_transmitting;
			} else {
				return R.string.status_not_transmitting;
			}
		} catch (RemoteException e) {
			return R.string.status_error;
		}
	}
	
	public void onServiceConnected(ComponentName className, IBinder service) {
		Log.i(appTag, "onServiceConnected "+service);
		pigeon = PigeonService.Stub.asInterface(service);
		scrollToLastFix();
	}

	public void onServiceDisconnected(ComponentName className) {
		Log.i(appTag, "onServiceDisconnected "+className);
	}

	public void getNearbys() {
		setProgressBarIndeterminateVisibility(true);
		try {
			HttpClient client = new DefaultHttpClient();
			String url_with_params = ICECONDOR_WRITE_URL + "?id="
					+ settings.getString(SETTING_OPENID, "");
			Log.i(appTag, "GET " + url_with_params);
			HttpGet get = new HttpGet(url_with_params);
			get.getParams().setIntParameter("http.socket.timeout", 10000);
			HttpResponse response;
			response = client.execute(get);
			Log.i(appTag, "http response: " + response.getStatusLine());
			HttpEntity entity = response.getEntity();
			String json = EntityUtils.toString(entity);
			try {
				JSONArray locations = new JSONArray(json);
				Log.i(appTag, "parsed "+locations.length()+" locations");
				for(int i=0; i < locations.length(); i++) {
					JSONObject location = (JSONObject)locations.getJSONObject(i).get("location");
					double longitude = location.getJSONObject("geom").getDouble("x");
					double latitude = location.getJSONObject("geom").getDouble("y");
					Log.i(appTag, "#"+i+" longititude: "+longitude+" latitude: "+latitude);
				}
			} catch (JSONException e) {
				Log.i(appTag,"JSON exception: "+e);
			}
			
		} catch (ClientProtocolException e) {
			Log.i(appTag, "client protocol exception " + e);
		} catch (HttpHostConnectException e) {
			Log.i(appTag, "connection failed "+e);
		} catch (IOException e) {
			Log.i(appTag, "IO exception "+e);
			e.printStackTrace();
		}
		setProgressBarIndeterminateVisibility(false);
	}
	
	public void startNeighborReadTimer() {
		service_read_timer = new Timer();
		service_read_timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				Log.i(appTag, "NeighborReadTimer fired");
				scrollToLastFix();
				getNearbys();
			}
		}, 0, ICECONDOR_READ_INTERVAL);
	}

	public void stopNeighborReadTimer() {
		service_read_timer.cancel();
	}
}