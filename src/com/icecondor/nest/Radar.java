package com.icecondor.nest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.TimeZone;
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
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.icecondor.nest.db.GeoRss;

public class Radar extends MapActivity implements ServiceConnection,
												  Constants {
	static final String appTag = "Radar";
	MapController mapController;
	PigeonService pigeon;
	private Timer service_read_timer;
	Intent settingsIntent, geoRssIntent, activityLogIntent;
	SharedPreferences settings;
	MeOverlay nearbys;
	FlockOverlay flock;
	EditText uuid_field;
	MapView mapView;
	Drawable redMarker, greenMarker;
	boolean first_fix;
	GeoRss rssdb;
	
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(appTag, "onCreate");
        super.onCreate(savedInstanceState);
		settings = PreferenceManager.getDefaultSharedPreferences(this);
        settingsIntent = new Intent(this, Settings.class);
        geoRssIntent = new Intent(this, GeoRssList.class);
        activityLogIntent = new Intent(this, ActivityLog.class);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(false);
        
        setTitle(getString(R.string.app_name) + " v" + ICECONDOR_VERSION);

        setContentView(R.layout.radar);
        ViewGroup radar_zoom = (ViewGroup)findViewById(R.id.radar_mapview_zoom);
        mapView = (MapView) findViewById(R.id.radar_mapview);
        radar_zoom.addView(mapView.getZoomControls());
        mapController = mapView.getController();
        mapController.setZoom(15);
        nearbys = new MeOverlay();
        mapView.getOverlays().add(nearbys);
        Resources res = getResources();
        redMarker = res.getDrawable(R.drawable.red_dot_12x20);
        greenMarker = res.getDrawable(R.drawable.green_dot_12x20);
        flock = new FlockOverlay(redMarker, this);
        mapView.getOverlays().add(flock);
        
		rssdb = new GeoRss(this);
		rssdb.open();
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
				nearbys.setLastLocalFix(pigeon.getLastFix());
				nearbys.setLastPushedFix(pigeon.getLastPushedFix());
			}
		} catch (RemoteException e) {
			nearbys.setLastLocalFix(null);
			nearbys.setLastPushedFix(null);
		}
	}
    
    @Override
    public void onResume() {
    	super.onResume();
    	Log.i(appTag, "onResume yeah");
        Intent pigeon_service = new Intent(this, Pigeon.class);
        boolean result = bindService(pigeon_service, this, 0); // 0 = do not auto-start
        Log.i(appTag, "pigeon bind result="+result);
        startNeighborReadTimer();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
		unbindService(this);
    	stopNeighborReadTimer();
    	Log.i(appTag, "onPause yeah");
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
			stopService(new Intent(this, Pigeon.class));
			break;
		case 7:
			startActivity(activityLogIntent);
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
				if(!LocationRepositoriesSqlite.has_access_token(this)) {
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
											OAuthAccessor accessor = LocationRepositoriesSqlite
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
												LocationRepositoriesSqlite
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
		try {
			Location fix = pigeon.getLastFix();
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
}