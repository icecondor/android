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
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

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
	EditText uuid_field;
	
    public void onCreate(Bundle savedInstanceState) {
    	setTitle(getString(R.string.app_name) + " " + getString(R.string.menu_version));
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
        //mapView.getOverlays().add(nearbys);
		pigeon_poll_timer.scheduleAtFixedRate(
				new TimerTask() {
					public void run() {
						scrollToLastFix();
					}
				}, 0, PIGEON_LOCATION_POST_INTERVAL);
		service_read_timer.scheduleAtFixedRate(
				new TimerTask() {
					public void run() {
						getNearbys();
					}
				}, 0, PIGEON_LOCATION_POST_INTERVAL);
    }
    
    public void scrollToLastFix() {
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
		menu.add(Menu.NONE, 1, Menu.NONE, R.string.menu_last_fix).setIcon(android.R.drawable.ic_menu_mylocation);
		menu.add(Menu.NONE, 2, Menu.NONE, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, 3, Menu.NONE, pigeonStatusTitle()).setIcon(android.R.drawable.presence_invisible);
		return result;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(appTag, "menu:"+item.getItemId());
		
		switch (item.getItemId()) {
		case 1:
			scrollToLastFix();
			break;
		case 2:
			showDialog(1);
			break;
		case 3:
			togglePigeon();
			item.setIcon(pigeonStatusIcon()).setTitle(pigeonStatusTitle());
			break;
		}
		
		return false;
	}
	
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean result = super.onPrepareOptionsMenu(menu);
		menu.findItem(3).setIcon(pigeonStatusIcon()).setTitle(pigeonStatusTitle());
		return result;
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		LayoutInflater factory = LayoutInflater.from(this);
        View settings_view = factory.inflate(R.layout.settings, null);
        
		return new AlertDialog.Builder(this)
			.setView(settings_view)
			.setTitle(R.string.menu_settings)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichbutton) {
					settings.edit().putString("uuid",uuid_field.getText().toString()).commit();
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichbutton) {
					
				}
			})
			.create();
	}
	
	protected void onPrepareDialog(int id, Dialog dialog) {
		uuid_field = (EditText) dialog.findViewById(R.id.settings_uuid_edit);
        uuid_field.setText(settings.getString("uuid", "n/a"));
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
			String json = EntityUtils.toString(entity);
			try {
				JSONArray locations = new JSONArray(json);
				Log.i(appTag, "parsed "+locations.length()+" locations");
				for(int i=0; i < locations.length(); i++) {
					JSONObject location = (JSONObject)locations.getJSONObject(i).get("location");
					String timestamp = location.getString("timestamp");
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
	}
}