package com.icecondor.nest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.client.OAuthClient;
import net.oauth.client.httpclient4.HttpClient4;

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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.icecondor.nest.db.GeoRss;
import com.icecondor.nest.db.LocationStorageProviders;
import com.icecondor.nest.rss.GeoRssList;

public class Radar extends MapActivity implements ServiceConnection,
												  Constants {
	static final String appTag = "Radar";
	MapController mapController;
	PigeonService pigeon;
	private Timer service_read_timer;
	Intent settingsIntent, geoRssIntent, activityLogIntent, pigeonIntent;
	SharedPreferences settings;
	MeOverlay nearbys;
	FlockOverlay flock;
	EditText uuid_field;
	MapView mapView;
	Drawable redMarker, greenMarker;
	boolean first_fix;
	GeoRss rssdb;
	boolean pigeon_connected = false;
	Timer heartbeat_timer;
	Location last_pushed_fix, last_local_fix;
	BroadcastReceiver gps_fix_receiver, bird_fix_receiver;

	
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
		settings = PreferenceManager.getDefaultSharedPreferences(this);
        settingsIntent = new Intent(this, Settings.class);
        geoRssIntent = new Intent(this, GeoRssList.class);
        activityLogIntent = new Intent(this, ActivityLog.class);
        pigeonIntent = new Intent(this, Pigeon.class);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(false);
        
        setTitle(getString(R.string.app_name) + " v" + ICECONDOR_VERSION);

        setContentView(R.layout.radar);
        mapView = (MapView) findViewById(R.id.radar_mapview);
        mapView.setBuiltInZoomControls(true);
        mapController = mapView.getController();
        mapController.setZoom(15);
        nearbys = new MeOverlay();
        mapView.getOverlays().add(nearbys);
        Resources res = getResources();
        redMarker = res.getDrawable(R.drawable.red_dot_12x20);
        greenMarker = res.getDrawable(R.drawable.green_dot_12x20);
        flock = new FlockOverlay(redMarker, this);
        mapView.getOverlays().add(flock);
        
        LinearLayout lg = (LinearLayout)findViewById(R.id.gpsblock);
        lg.setOnClickListener(new GpsClickListener());        		
        LinearLayout lb = (LinearLayout)findViewById(R.id.birdblock);
        lb.setOnClickListener(new BirdClickListener());      		
        
		rssdb = new GeoRss(this);
		rssdb.open();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	Log.i(appTag, "onResume");
        startNeighborReadTimer();
        startHeartbeatTimer();
        gps_fix_receiver = this.new GpsFixReceiver();
        registerReceiver(gps_fix_receiver, new IntentFilter(GPS_FIX_ACTION));
        bird_fix_receiver = this.new BirdFixReceiver();
        registerReceiver(bird_fix_receiver, new IntentFilter(BIRD_FIX_ACTION));
        bindService(pigeonIntent, this, 0); // 0 = do not auto-start
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	if(pigeon_connected) {
    		unbindService(this);
    	}
    	stopNeighborReadTimer();
    	stopHeartbeatTimer();
    	unregisterReceiver(gps_fix_receiver);
    	unregisterReceiver(bird_fix_receiver);
    	Log.i(appTag, "onPause yeah");
    }
    
    public void scrollToLastFix() {
    	scrollToFix(last_local_fix);
    }

    public void scrollToLastPushedFix() {
    	scrollToFix(last_pushed_fix);
    }

    public void scrollToFix(Location fix) {
		mapController = mapView.getController();
		if (fix != null) {
			mapController.animateTo(new GeoPoint(
					(int) (fix.getLatitude() * 1000000), (int) (fix
							.getLongitude() * 1000000)));
		}
    }
    

	private void refreshBirdLocation() {
		nearbys.setLastLocalFix(last_local_fix);
		nearbys.setLastPushedFix(last_pushed_fix);
	}
    
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(appTag, "onCreateOptionsMenu");
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, 1, Menu.NONE, R.string.menu_last_fix).setIcon(android.R.drawable.ic_menu_mylocation);
		menu.add(Menu.NONE, 2, Menu.NONE, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, 3, Menu.NONE, R.string.menu_geo_rss).setIcon(R.drawable.bluerss);
		menu.add(Menu.NONE, 4, Menu.NONE, pigeonStatusTitle()).setIcon(android.R.drawable.presence_invisible);
		menu.add(Menu.NONE, 5, Menu.NONE, R.string.menu_feedback).setIcon(R.drawable.exclamation);
		menu.add(Menu.NONE, 6, Menu.NONE, R.string.menu_exit).setIcon(android.R.drawable.ic_delete);
		menu.add(Menu.NONE, 7, Menu.NONE, R.string.menu_log).setIcon(android.R.drawable.ic_menu_recent_history);
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
		case 5:
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(GETSATISFACTION_URL));
			startActivity(i);
			break;
		case 6:
			unbindService(this);
			pigeon_connected = false;
			stopService(pigeonIntent);
			rssdb.log("Radar: pigeon told to stop");
			finish();
			break;
		case 7:
			startActivity(activityLogIntent);
			break;
		case 8:
			try {
				pigeon.pushFix();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
				if(!LocationStorageProviders.has_access_token(this)) {
					// Alert the user that login is required
					(new AlertDialog.Builder(this)).setMessage(
							"Login to the location storage provider at "
									+ ICECONDOR_URL_SHORTNAME
									+ " to activate position recording.")
							.setPositiveButton("Proceed",
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog,
												int whichButton) {
											Log.i(appTag,"OAUTH request token retrieval");
											Toast.makeText(Radar.this, "contacting server", Toast.LENGTH_SHORT).show();
											// get the OAUTH request token
											OAuthAccessor accessor = LocationStorageProviders
													.defaultAccessor(Radar.this);
											OAuthClient client = new OAuthClient(
													new HttpClient4());
											try {
												client.getRequestToken(accessor);
												String[] token_and_secret = new String[] {
														accessor.requestToken,
														accessor.tokenSecret };
												Log.i(appTag, "request token: "
														+ token_and_secret[0]
														+ " secret:"
														+ token_and_secret[1]);
												LocationStorageProviders
														.setDefaultRequestToken(
																token_and_secret,
																Radar.this);
												Intent i = new Intent(
														Intent.ACTION_VIEW);
												String url = accessor.consumer.serviceProvider.userAuthorizationURL
												+ "?oauth_token="
												+ accessor.requestToken
												+ "&oauth_callback="
												+ accessor.consumer.callbackURL;
												Log.i(appTag, "sending to "+url);
												i.setData(Uri.parse(url));
												startActivity(i);
											} catch (IOException e) {
												Toast.makeText(Radar.this, "server failed", Toast.LENGTH_SHORT).show();
												e.printStackTrace();
											} catch (OAuthException e) {
												Toast.makeText(Radar.this, "server failed", Toast.LENGTH_SHORT).show();
												e.printStackTrace();
											} catch (URISyntaxException e) {
												Toast.makeText(Radar.this, "server failed", Toast.LENGTH_SHORT).show();
												e.printStackTrace();
											}
										}
									}).setNegativeButton("Cancel",
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog,
												int whichButton) {
											/* User clicked Cancel so do some stuff */
										}
									})
	
							.show();
					return false;
				} else {
					pigeon.startTransmitting();	
					return true;
				}
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
		if(pigeon_connected == false) {
			return R.string.status_error;
		}
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
		pigeon_connected = true;
		Log.i(appTag, "onServiceConnected "+service);
		pigeon = PigeonService.Stub.asInterface(service);
		try {
			Location fix = pigeon.getLastFix();
			pigeon.getLastPushedFix(); // triggers UI update
			if(first_fix == false) {
				first_fix = true;
		        if (fix != null) {
		            scrollToLastFix();
		            
		        } else {
		        	Toast.makeText(this, "Waiting for first location fix", Toast.LENGTH_SHORT).show();
		        }
			}
		} catch (RemoteException e) {
		}
	}
	
	public void onServiceDisconnected(ComponentName className) {
		pigeon_connected = false;
		Log.i(appTag, "onServiceDisconnected "+className);
	}

	public void getNearbys() {
		setProgressBarIndeterminateVisibility(true);
		try {
			HttpClient client = new DefaultHttpClient();
			String url_with_params = ICECONDOR_READ_URL + "?id="
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
				//Log.i(appTag, "NeighborReadTimer fired");
				//scrollToLastFix();
				updateBirds();
				//getNearbys();
			}
		}, 0, RADAR_REFRESH_INTERVAL);
	}

	protected void updateBirds() {
		Cursor feeds = rssdb.findFeeds();
		while(feeds.moveToNext()) {
			long url_id = feeds.getLong(feeds.getColumnIndex(GeoRss.FEEDS_ID));
			Log.i(appTag, "reading shouts db for #"+url_id+" "+feeds.getString(feeds.getColumnIndex(GeoRss.FEEDS_EXTRA)));
			Cursor preshouts = rssdb.findPreShouts(url_id, System.currentTimeMillis());
			if(preshouts.getCount() > 0) {
				preshouts.moveToFirst();
				Log.i(appTag, "preshout red "+preshouts.getString(preshouts.getColumnIndex("title")));
				addBird(preshouts, redMarker);
			}
			preshouts.close();

			Cursor postshouts = rssdb.findPostShouts(url_id, System.currentTimeMillis());
			if (postshouts.getCount() > 0) {
				postshouts.moveToFirst();
				Log.i(appTag, "postshout green "+postshouts.getString(postshouts.getColumnIndex("title")));
				addBird(postshouts, greenMarker);
			}
			postshouts.close();
		}
		feeds.close();
	}

	private void addBird(Cursor displayShout, Drawable marker) {
		String guid = displayShout.getString(displayShout.getColumnIndex("guid"));
		if (!flock.contains(guid)) {
			GeoPoint point = new GeoPoint((int) (displayShout
					.getFloat(displayShout.getColumnIndex("lat")) * 1000000),
					(int) (displayShout.getFloat(displayShout
							.getColumnIndex("long")) * 1000000));
			String date_string = displayShout.getString(displayShout.getColumnIndex("date"));
			Date date = Util.DateRfc822(date_string);
			String shout_date = Util.DateToShortDisplay(date);
			String message = displayShout.getString(displayShout.getColumnIndex("title")) + " " + shout_date;
			BirdItem bird = new BirdItem(point, guid, message);
			flock.add(bird, marker);
		}	
	}

	public void stopNeighborReadTimer() {
		service_read_timer.cancel();
	}
	
	@Override
	public void onDestroy() {
		rssdb.close();
		super.onDestroy();
	}
	
	private void startHeartbeatTimer() {
		heartbeat_timer = new Timer("Heartbeat Timer");
		HeartBeatTask heartbeatTask = new HeartBeatTask();
		heartbeat_timer.scheduleAtFixedRate(heartbeatTask, 0, 2000);
	}
	
	private void stopHeartbeatTimer() {
		heartbeat_timer.cancel();
	}

	class HeartBeatTask extends TimerTask {
		public void run() {
			if(last_local_fix != null) {
				runOnUiThread(new UpdateGpsBlock());
			}
			if(last_pushed_fix != null) {
				runOnUiThread(new UpdateBirdBlock());
			}
		}
	}
	
	public class UpdateBirdBlock implements Runnable {
		@Override
		public void run() {
			refreshBirdLocation();
			TextView satl1b = (TextView)findViewById(R.id.topbird3); 
			satl1b.setText(Util.timeAgoInWords(last_pushed_fix.getTime()));
		}		
	}
	
	public class UpdateGpsBlock implements Runnable {
		@Override
		public void run() {
			refreshBirdLocation();
			TextView satl1b = (TextView)findViewById(R.id.satl1b); 
			satl1b.setText(Util.timeAgoInWords(last_local_fix.getTime()));
		}		
	}
	
	public class GpsFixReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Location location = (Location)intent.getExtras().get("location");
			last_local_fix = location;
			runOnUiThread(new UpdateGpsBlock());			
		}		
	}

	public class BirdFixReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Location location = (Location)intent.getExtras().get("location");
			last_pushed_fix = location;
			runOnUiThread(new UpdateBirdBlock());			
		}		
	}
	
	public class GpsClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			scrollToLastFix();			
		}		
	}
	public class BirdClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			scrollToLastPushedFix();			
		}		
	}
}