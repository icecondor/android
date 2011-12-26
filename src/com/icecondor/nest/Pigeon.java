package com.icecondor.nest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.client.OAuthClient;
import net.oauth.client.httpclient4.HttpClient4;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;

import com.icecondor.nest.db.GeoRss;
import com.icecondor.nest.db.LocationStorageProviders;
import com.icecondor.nest.types.Gps;

//look at android.permission.RECEIVE_BOOT_COMPLETED

public class Pigeon extends Service implements Constants, LocationListener,
                                               SharedPreferences.OnSharedPreferenceChangeListener,
                                               Handler.Callback {
	private Timer heartbeat_timer;
	private Timer rss_timer;
	private Timer push_queue_timer;
	//private Timer wifi_scan_timer = new Timer();
	boolean on_switch = false;
	private Location last_recorded_fix, last_pushed_fix, last_local_fix;
	long last_pushed_time;
	int last_fix_http_status;
	Notification ongoing_notification;
	NotificationManager notificationManager;
	LocationManager locationManager;
	WifiManager wifiManager;
	PendingIntent contentIntent;
	SharedPreferences settings;
	GeoRss rssdb;
	MediaPlayer mp;
	private TimerTask heartbeatTask;
	DefaultHttpClient httpClient;
	OAuthClient oclient;
	private int last_battery_level;
	BatteryReceiver battery_receiver;
	WidgetReceiver widget_receiver;
	private boolean ac_power;
	ApiSocket apiSocket;
	Handler pigeonHandler;
	long reconnectLastTry;
	
	public void onCreate() {
		Log.i(APP_TAG, "*** Pigeon service created. "+
				"\""+Thread.currentThread().getName()+"\""+" tid:"+Thread.currentThread().getId()+
				" uid:"+Process.myUid());
		super.onCreate();
		
		/* Database */
		rssdb = new GeoRss(this);
		rssdb.open();
		rssdb.log("Pigon created");

		/* GPS */
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		rssdb.log("GPS provider enabled: "+locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
		last_local_fix = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		rssdb.log("NETWORK provider enabled: "+locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
		Location last_network_fix = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (last_local_fix == null) { 
			if(last_network_fix != null) {
				last_local_fix = last_network_fix; // fall back onto the network location
				rssdb.log("Last known NETWORK fix: "+last_network_fix+" "+
						Util.DateTimeIso8601(last_network_fix.getTime()));
			}
		} else {
			rssdb.log("Last known GPS fix: "+last_local_fix+" "+
					Util.DateTimeIso8601(last_local_fix.getTime()));			
		}
		Cursor oldest;
		if ((oldest = rssdb.oldestPushedLocationQueue()).getCount() > 0) {
			rssdb.log("Oldest pushed fix found");
			last_pushed_fix =  Gps.fromJson(oldest.getString(
			                    oldest.getColumnIndex(GeoRss.POSITION_QUEUE_JSON))).getLocation();
		}
		oldest.close();

		
		/* WIFI */
		wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		
		/* Notifications */
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		contentIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				Start.class), 0);
		CharSequence text = getText(R.string.status_started);
		ongoing_notification = new Notification(R.drawable.condorhead_statusbar, text, System
				.currentTimeMillis());
		ongoing_notification.flags = ongoing_notification.flags ^ Notification.FLAG_ONGOING_EVENT;
		ongoing_notification.setLatestEventInfo(this, "IceCondor", "", contentIntent);


		/* Preferences */
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		settings.registerOnSharedPreferenceChangeListener(this);
		on_switch = settings.getBoolean(SETTING_PIGEON_TRANSMITTING, false);
		if (on_switch) {
			//startForeground(1, ongoing_notification);
			startLocationUpdates();
			sendBroadcast(new Intent("com.icecondor.nest.WIDGET_ON"));
		}

		/* Sound */
		mp = MediaPlayer.create(this, R.raw.beep);	
		
		/* Timers */
		startHeartbeatTimer();
		//startRssTimer();
		startPushQueueTimer();
		
		/* Apache HTTP Monstrosity*/
		httpClient =  new DefaultHttpClient();
		httpClient.getParams().setIntParameter(AllClientPNames.CONNECTION_TIMEOUT, 15 *1000);
		httpClient.getParams().setIntParameter(AllClientPNames.SO_TIMEOUT, 30 *1000);
		oclient = new OAuthClient(new HttpClient4());
		
		/* Battery */
		battery_receiver = new BatteryReceiver();
		registerReceiver(battery_receiver, 
		         new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		registerReceiver(battery_receiver, 
		         new IntentFilter(Intent.ACTION_POWER_CONNECTED));
		registerReceiver(battery_receiver, 
		         new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
		
		widget_receiver = new WidgetReceiver();
		registerReceiver(widget_receiver,
				new IntentFilter("com.icecondor.nest.PIGEON_OFF"));
		registerReceiver(widget_receiver,
				new IntentFilter("com.icecondor.nest.PIGEON_ON"));
		registerReceiver(widget_receiver,
				new IntentFilter("com.icecondor.nest.PIGEON_INQUIRE"));
		
		/* Callbacks from the API Communication Thread */
		pigeonHandler = new Handler(this);
	}

	protected void apiReconnect() {
		if (reconnectLastTry < (System.currentTimeMillis()-(30*1000))) {
			reconnectLastTry = System.currentTimeMillis();
			rssdb.log("apiReconnect() "+
					"\""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId() );
			if(apiSocket != null && apiSocket.isConnected()) {
				try {
					apiSocket.close();
				} catch (IOException e) {
				}
				rssdb.log("Warning: Closing connected apiSocket");
			}
			try {
				apiSocket = new ApiSocket(ICECONDOR_API_URL, pigeonHandler, "token");
				apiSocket.connect();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		} else {
			rssdb.log("apiReconnect() ignored. last try is "+ 
					(System.currentTimeMillis()-reconnectLastTry)/1000+" sec ago "+
					"\""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId() );
		}
	}

	public void onStart(Intent start, int key) {
		super.onStart(start,key);
		apiReconnect();
		rssdb.log("Pigon started");
		broadcastGpsFix(last_local_fix);
	}
	
	public void onDestroy() {
		unregisterReceiver(battery_receiver);
		unregisterReceiver(widget_receiver);
        stop_background();
		rssdb.log("Pigon destroyed");
		rssdb.close();
	}
	
	private void notificationStatusUpdate(String msg) {
		ongoing_notification.setLatestEventInfo(this, "IceCondor",
				msg, contentIntent);
		ongoing_notification.when = System.currentTimeMillis();
		notificationManager.notify(1, ongoing_notification);
	}
	
	private void notification(String msg) {
		Notification notification = new Notification(R.drawable.condorhead_statusbar, msg,
				System.currentTimeMillis());
		// a contentView error is thrown if this line is not here
		notification.setLatestEventInfo(this, "IceCondor Notice", msg, contentIntent);
		notificationManager.notify(2, notification);
	}
	
	private void notificationFlash(String msg) {
		notification(msg);
		notificationManager.cancel(2);
	}

	private void startLocationUpdates() {
		long record_frequency = Long.decode(settings.getString(SETTING_TRANSMISSION_FREQUENCY, "300000"));
		rssdb.log("requesting Location Updates every "+ Util.millisecondsToWords(record_frequency));
		locationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER, 
				record_frequency, 
				0.0F, this);
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 
				record_frequency, 
				0.0F, this);
//		Log.i(APP_TAG, "kicking off wifi scan timer");
//		wifi_scan_timer.scheduleAtFixedRate(
//				new TimerTask() {
//					public void run() {
//						Log.i(APP_TAG, "wifi: start scan (enabled:"+wifiManager.isWifiEnabled()+")");
//						wifiManager.startScan();
//					}
//				}, 0, 60000);		
	}
	
	private void stopLocationUpdates() {
		rssdb.log("pigeon: stopping location updates");		
		locationManager.removeUpdates(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(APP_TAG,"onBind for "+intent.getExtras());
		return pigeonBinder;
	}
	
	@Override
	public void onRebind(Intent intent) {
		Log.i(APP_TAG, "onReBind for "+intent.toString());
	}
	
	@Override
	public void onLowMemory() {
		rssdb.log("Pigeon: onLowMemory");
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(APP_TAG, "Pigeon: onUnbind for "+intent.toString());
		return false;
	}
	
	public void pushQueue() {
		Timer push_queue_timer_single = new Timer("Push Queue Single Timer");
		push_queue_timer_single.schedule(new PushQueueTask(), 0);
	}

	class PushQueueTask extends TimerTask {
		public void run() {
			Cursor oldest;
			rssdb.log("** queue push size "+rssdb.countPositionQueueRemaining()+" \""+Thread.currentThread().getName()+"\""+" tid:"+Thread.currentThread().getId() );
			if ((oldest = rssdb.oldestUnpushedLocationQueue()).getCount() > 0) {
				int id = oldest.getInt(oldest.getColumnIndex("_id"));
				rssdb.log("PushQueueTask oldest unpushed id "+id);
				Gps fix =  Gps.fromJson(oldest.getString(
				                    oldest.getColumnIndex(GeoRss.POSITION_QUEUE_JSON)));
				rssdb.log("PushQueueTask after Gps.fromJson");
				boolean status = pushLocationApi(fix);
				if (status == true) {
					rssdb.log("queue push #"+id+" OK");
					rssdb.mark_as_pushed(id);
					last_pushed_fix = fix.getLocation();
					last_pushed_time = System.currentTimeMillis();
					broadcastBirdFix(fix.getLocation());
				} else {
					rssdb.log("queue push #"+id+" FAIL "+status);
				}
			} 
			oldest.close();
		}
	}
	
	public boolean pushLocationApi(Gps gps) {
		rssdb.log("pushLocationApi: loading access token");
		String[] token_and_secret = LocationStorageProviders.getDefaultAccessToken(this);
		JSONObject json = gps.toJson();
		try {
			json.put("oauth", token_and_secret[0]);
			rssdb.log("pushLocationApi: "+json.toString()+" isConnected():"+apiSocket.isConnected());
			boolean pass = apiSocket.emit(json.toString());
			if(pass == false) {
				apiReconnect();
			}
			return pass;
		} catch (JSONException e) {
			rssdb.log("pushLocationApi: JSONException "+e);
		}
		return false;
	}
	
	public int pushLocationRest(Gps gps) {
		Location fix = gps.getLocation();
		Log.i(APP_TAG, "sending id: "+settings.getString(SETTING_OPENID,"")+ " fix: " 
				+fix.getLatitude()+" long: "+fix.getLongitude()+
				" alt: "+fix.getAltitude() + " time: " + Util.DateTimeIso8601(fix.getTime()) +
				" acc: "+fix.getAccuracy());
		rssdb.log("pushing fix "+" time: " + Util.DateTimeIso8601(fix.getTime()) +
				"("+fix.getTime()+") acc: "+fix.getAccuracy());
		if (settings.getBoolean(SETTING_BEEP_ON_FIX, false)) {
			play_fix_beep();
		}
		//ArrayList <NameValuePair> params = new ArrayList <NameValuePair>();
		ArrayList<Map.Entry<String, String>> params = new ArrayList<Map.Entry<String, String>>();
		addPostParameters(params, fix, gps.getBattery());

		OAuthAccessor accessor = LocationStorageProviders.defaultAccessor(this);
		String[] token_and_secret = LocationStorageProviders.getDefaultAccessToken(this);
		params.add(new OAuth.Parameter("oauth_token", token_and_secret[0]));
		accessor.tokenSecret = token_and_secret[1];
		try {
			OAuthMessage omessage;
			Log.d(APP_TAG, "invoke("+accessor+", POST, "+ICECONDOR_WRITE_URL+", "+params);
			omessage = oclient.invoke(accessor, "POST",  ICECONDOR_WRITE_URL, params);
			omessage.getHeader("Result");
			last_fix_http_status = 200;
			return last_fix_http_status;
		} catch (OAuthException e) {
			rssdb.log("push OAuthException "+e);
		} catch (URISyntaxException e) {
			rssdb.log("push URISyntaxException "+e);
		} catch (UnknownHostException e) {
			rssdb.log("push UnknownHostException "+e);
		} catch (IOException e) {
			// includes host not found
			rssdb.log("push IOException "+e);
		}
		last_fix_http_status = 500;
		return last_fix_http_status; // something went wrong
	}
	
	private void addPostParameters(ArrayList<Map.Entry<String, String>> dict, Location fix, int lastBatteryLevel) {
		dict.add(new Util.Parameter("location[provider]", fix.getProvider()));
		dict.add(new Util.Parameter("location[latitude]", Double.toString(fix.getLatitude())));
		dict.add(new Util.Parameter("location[longitude]", Double.toString(fix.getLongitude())));
		dict.add(new Util.Parameter("location[altitude]", Double.toString(fix.getAltitude())));
		dict.add(new Util.Parameter("client[version]", ""+ICECONDOR_VERSION));
		if(fix.hasAccuracy()) {
			dict.add(new Util.Parameter("location[accuracy]", Double.toString(fix.getAccuracy())));
		}
		if(fix.hasBearing()) {
			dict.add(new Util.Parameter("location[heading]", Double.toString(fix.getBearing())));
		}
		if(fix.hasSpeed()) {
			dict.add(new Util.Parameter("location[velocity]", Double.toString(fix.getSpeed())));
		}
		
		dict.add(new Util.Parameter("location[timestamp]", Util.DateTimeIso8601(fix.getTime())));
		dict.add(new Util.Parameter("location[batterylevel]", Integer.toString(lastBatteryLevel)));
	}
				
	public void onLocationChanged(Location location) {
		Log.i(APP_TAG, "onLocationChanged: Thread \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
		last_local_fix = location;
		long time_since_last_update = last_local_fix.getTime() - (last_recorded_fix == null?0:last_recorded_fix.getTime()); 
		long record_frequency = Long.decode(settings.getString(SETTING_TRANSMISSION_FREQUENCY, "180000"));
		rssdb.log("pigeon onLocationChanged: lat:"+location.getLatitude()+
				  " long:"+location.getLongitude() + " acc:"+
			       location.getAccuracy()+" "+ 
			       (time_since_last_update/1000)+" seconds since last update");

		if (on_switch) {
			if((last_local_fix.getAccuracy() < (last_recorded_fix == null?
					                            500000:last_recorded_fix.getAccuracy())) ||
					time_since_last_update > record_frequency ) {
				last_recorded_fix = last_local_fix;
				last_fix_http_status = 200;
				Gps gps = new Gps();
				gps.setLocation(last_local_fix);
				gps.setBattery(last_battery_level);
				long id = rssdb.addPosition(gps.toJson().toString());
				rssdb.log("Pigeon location queued. location #"+id);
				pushQueue();
				broadcastGpsFix(location);
			}
		}
	}

	private void broadcastGpsFix(Location location) {
		Intent intent = new Intent(GPS_FIX_ACTION);
		intent.putExtra("location", location);
		sendBroadcast(intent);	
	}

	private void broadcastBirdFix(Location location) {
		Intent intent = new Intent(BIRD_FIX_ACTION);
		intent.putExtra("location", location);
		sendBroadcast(intent);	
	}

	private void play_fix_beep() {
		mp.start();
	}

	public void onProviderDisabled(String provider) {
		rssdb.log("provider "+provider+" disabled");
	}

	public void onProviderEnabled(String provider) {
		rssdb.log("provider "+provider+" enabled");
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		String status_msg = "";
		if (status ==  LocationProvider.TEMPORARILY_UNAVAILABLE) {status_msg = "TEMPORARILY_UNAVAILABLE";}
		if (status ==  LocationProvider.OUT_OF_SERVICE) {status_msg = "OUT_OF_SERVICE";}
		if (status ==  LocationProvider.AVAILABLE) {status_msg = "AVAILABLE";}
		Log.i(APP_TAG, "provider "+provider+" status changed to "+status_msg);
		rssdb.log("GPS "+status_msg);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String pref_name) {
		Log.i(APP_TAG, "shared preference changed: "+pref_name);		
		if (pref_name.equals(SETTING_TRANSMISSION_FREQUENCY)) {
			if (on_switch) {
				stopLocationUpdates();
				startLocationUpdates();
				notificationFlash("Position reporting frequency now "+Util.millisecondsToWords(
						Long.parseLong(prefs.getString(pref_name, "N/A"))));
			}
		}
		if (pref_name.equals(SETTING_RSS_READ_FREQUENCY)) {
			stopRssTimer();
			startRssTimer();
			notificationFlash("RSS Read frequency now "+Util.millisecondsToWords(
						Long.parseLong(prefs.getString(pref_name, "N/A"))));
		}
	}
	
	private void startHeartbeatTimer() {
		heartbeat_timer = new Timer("Heartbeat Timer");
		heartbeatTask = new HeartBeatTask();
		heartbeat_timer.scheduleAtFixedRate(heartbeatTask, 0, 20000);
	}

	private void stopHeartbeatTimer() {
		heartbeat_timer.cancel();
	}
	private void startRssTimer() {
		rss_timer = new Timer("RSS Reader Timer");
		long rss_read_frequency = Long.decode(settings.getString(SETTING_RSS_READ_FREQUENCY, "60000"));
		Log.i(APP_TAG, "starting rss timer at frequency "+rss_read_frequency);
		rss_timer.scheduleAtFixedRate(
				new TimerTask() {
					public void run() {
						Log.i(APP_TAG, "rss_timer fired");
						updateRSS();
					}
				}, 0, rss_read_frequency);
	}
	
	private void stopRssTimer() {
		rss_timer.cancel();
	}
	
	private void startPushQueueTimer() {
		push_queue_timer = new Timer("Push Queue Timer");
		push_queue_timer.scheduleAtFixedRate(new PushQueueTask(), 0, 30000);
	}

	private void stopPushQueueTimer() {
		push_queue_timer.cancel();
	}
	
	protected void updateRSS() {
		new Timer().schedule(
				new TimerTask() {
					public void run() {
						Log.i(APP_TAG, "rss_timer fired");
						Cursor geoRssUrls = rssdb.findFeeds();
						while (geoRssUrls.moveToNext()) {
							try {
								rssdb.readGeoRss(geoRssUrls);
							} catch (ClientProtocolException e) {
								Log.i(APP_TAG, "http protocol exception "+e);
							} catch (IOException e) {
								Log.i(APP_TAG, "io error "+e);
							}
						}
						geoRssUrls.close();						
					}
				}, 0);
	}
	
	protected void start_background() {
		on_switch = true;
		settings.edit().putBoolean(SETTING_PIGEON_TRANSMITTING, on_switch).commit();
		startLocationUpdates();
		notificationStatusUpdate("Waiting for fix.");				
		notificationFlash("Location reporting ON.");
		Intent intent = new Intent("com.icecondor.nest.WIDGET_ON");
		sendBroadcast(intent);
	}
	
	protected void stop_background() {
		on_switch = false;
		stopRssTimer();
		stopLocationUpdates();
		stopPushQueueTimer();
		stopHeartbeatTimer();
		settings.edit().putBoolean(SETTING_PIGEON_TRANSMITTING,on_switch).commit();
		notificationManager.cancel(1);
		Intent intent = new Intent("com.icecondor.nest.WIDGET_OFF");
		sendBroadcast(intent);
	}
	
	class HeartBeatTask extends TimerTask {
		public void run() {
			String fix_part = "";
			if (on_switch) {
				if (last_pushed_fix != null) {
					String ago = Util.timeAgoInWords(last_pushed_time);
					String fago = Util.timeAgoInWords(last_pushed_fix.getTime());
					if (last_fix_http_status != 200) {
						ago = "err.";
					}
					fix_part = "push "+ ago+"/"+fago+".";
			    }
				if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					fix_part = "Warning: GPS set to disabled";
				}
			} else {
				fix_part = "Location reporting is off.";
			}
			String queue_part = ""+rssdb.countPositionQueueRemaining()+" queued.";
		    String beat_part = "";
		    if (last_local_fix != null) {
		    	String ago = Util.timeAgoInWords(last_local_fix.getTime());
		    	beat_part = "fix "+ago+".";
		    }
		    String msg = fix_part+" "+beat_part+" "+queue_part;
			notificationStatusUpdate(msg); 
		}
	};

	private class BatteryReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	String action = intent.getAction();
	    	if (action.equals("android.intent.action.BATTERY_CHANGED")) {
	  	      last_battery_level = intent.getIntExtra("level", 0);
	    	}
	    	if (action.equals("android.intent.action.ACTION_POWER_CONNECTED")) {
		    	ac_power = true;	    		
	    	}
	    	if (action.equals("android.intent.action.ACTION_POWER_DISCONNECTED")) {
		    	ac_power = false;	    		
	    	}
	    }
	  };

	private class WidgetReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	String action = intent.getAction();
	    	if (action.equals("com.icecondor.nest.PIGEON_OFF")) {
	    		stopSelf();
	    	}
	    	if (action.equals("com.icecondor.nest.PIGEON_ON")) {
	    		start_background();
	    	}
	    	if (action.equals("com.icecondor.nest.PIGEON_INQUIRE")) {
	    		if(on_switch) {
	    			sendBroadcast(new Intent("com.icecondor.nest.WIDGET_ON"));
	    		} else {
	    			sendBroadcast(new Intent("com.icecondor.nest.WIDGET_ON"));
	    		}
	    	}
	    }
	  };

    private final PigeonService.Stub pigeonBinder = new PigeonService.Stub() {
		public boolean isTransmitting() throws RemoteException {
			Log.i(APP_TAG, "isTransmitting => "+on_switch);
			return on_switch;
		}
		public void startTransmitting() throws RemoteException {
			if (on_switch) {
				Log.i(APP_TAG, "startTransmitting: already transmitting");
			} else {
				rssdb.log("Pigeon: startTransmitting");
				start_background();
			}
		}
		public void stopTransmitting() throws RemoteException {
			rssdb.log("Pigeon: stopTransmitting");
			stop_background();
		}
		public Location getLastFix() throws RemoteException {
			if (last_local_fix != null) {
				broadcastGpsFix(last_local_fix);
			}
			return last_local_fix;
		}
		@Override
		public Location getLastPushedFix() throws RemoteException {
			if(last_pushed_fix != null) {
			  broadcastBirdFix(last_pushed_fix);
			}
			return last_pushed_fix;		
		}
		@Override
		public void refreshRSS() throws RemoteException {
			updateRSS();
		}
		@Override 
		public void pushFix() throws RemoteException {
			pushQueue();
		}
    };

	@Override
	public boolean handleMessage(Message msg) {
		rssdb.log("handleMessage: "+msg+" \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
		return true;
	}

}
