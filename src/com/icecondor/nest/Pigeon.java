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

import android.app.AlarmManager;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.icecondor.nest.db.GeoRss;
import com.icecondor.nest.db.LocationStorageProviders;
import com.icecondor.nest.types.Gps;
import com.icecondor.nest.util.GrabAndSavePicture;

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
	TelephonyManager telephonyManager;
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
    private String ongoing_notification_msg;
	private long websocket_last_msg;
	private boolean activity_bound;
	private boolean friends_followed;
	AlarmManager alarmManager;
	AlarmReceiver alarm_receiver;
	private PendingIntent wake_alarm_intent;
	
	
	@Override
	public void onCreate() {
		Log.i(APP_TAG, "*** Pigeon service created. "+
				"\""+Thread.currentThread().getName()+"\""+" tid:"+Thread.currentThread().getId()+
				" uid:"+Process.myUid());
		super.onCreate();
		
		/* Database */
		rssdb = new GeoRss(this);
		rssdb.open();
		rssdb.log("Pigon created v"+ICECONDOR_VERSION);

		alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		
		/* refresh last_local_fix from LocationManager */
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        last_local_fix = getBestLastLocation();
        if (last_local_fix != null) {
            rssdb.log("Last known "+last_local_fix.getProvider()+" fix: "+last_local_fix+" "+
                    Util.DateTimeIso8601(last_local_fix.getTime()));      
        }

        /* refresh last_pushed_fix from db */
		Cursor oldest = rssdb.oldestPushedLocations();
		if (oldest.getCount() > 0) {
			rssdb.log("Oldest pushed fix found");
			last_pushed_fix =  Gps.fromJson(oldest.getString(
			                    oldest.getColumnIndex(GeoRss.POSITION_QUEUE_JSON))).getLocation();
		}
		oldest.close();


		/* Websockets API */
        pigeonHandler = new Handler(this);        
        try {
            apiSocket = new ApiSocket(ICECONDOR_API_URL, pigeonHandler, rssdb);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
		
		/* WIFI */
		wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		
		/* Notifications */
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		contentIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				Start.class), 0);

		/* Preferences */
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		settings.registerOnSharedPreferenceChangeListener(this);
		on_switch = settings.getBoolean(SETTING_PIGEON_TRANSMITTING, false);
		if (on_switch) {
			Log.i(APP_TAG, "switch ON: registering for location updates");
		    //startForeground(1, ongoing_notification);
		    startLocationUpdates();
		    sendBroadcast(new Intent("com.icecondor.nest.WIDGET_ON"));
		} else {
			Log.i(APP_TAG, "switch OFF: not registering for location updates");			
		}

		/* Sound */
		mp = MediaPlayer.create(this, R.raw.beep);	
		
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
		
		alarm_receiver = new AlarmReceiver();
		registerReceiver(alarm_receiver,
				new IntentFilter("com.icecondor.nest.WAKE_ALARM"));
        wake_alarm_intent = PendingIntent.getBroadcast(getApplicationContext(), 
                0,
                new Intent("com.icecondor.nest.WAKE_ALARM"),
                0);
		
		/* Emulator ipv6 issue */
		if ("google_sdk".equals( Build.PRODUCT )) {
		    java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
		    java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
		}
		
		/* telephony callbacks */
		/* needs android.permission.READ_PHONE_STATE
		telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(new PhoneStateListener(), PhoneStateListener.LISTEN_DATA_CONNECTION_STATE); */
	}

	@Override
	public void onStart(Intent start, int key) {
		super.onStart(start,key);
		rssdb.log("Pigon starting v"+ICECONDOR_VERSION);
		startBackground();
		broadcastGpsFix(last_local_fix);
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(battery_receiver);
		unregisterReceiver(widget_receiver);
        stopBackground();
		rssdb.log("Pigeon onDestroy()");
		rssdb.close();
	}
	
    protected Notification buildNotification() {
        int icon;
        if(apiSocket.isConnected()) {
            icon = R.drawable.condorhead_statusbar;
        } else {
            icon = R.drawable.condorhead_statusbar_gray;
        }
        Notification notification = new Notification(icon, null, System
				.currentTimeMillis());
		notification.flags = notification.flags ^ Notification.FLAG_ONGOING_EVENT;
		notification.setLatestEventInfo(this, "IceCondor", "", contentIntent);
		return notification;
    }

	protected Location getBestLastLocation() {
       Location last_gps_fix = getLastLocationFor(LocationManager.GPS_PROVIDER);
       Location last_network_fix = getLastLocationFor(LocationManager.NETWORK_PROVIDER);
       Location winner = null;
        if (last_gps_fix == null) { 
            if(last_network_fix != null) {
                winner = last_network_fix; // fall back onto the network location
            }
        } else {
            winner = last_gps_fix;
        }
        return winner;
	}
	
    protected Location getLastLocationFor(String provider) {
        boolean enabled = locationManager.isProviderEnabled(provider);
        rssdb.log(provider+" provider enabled: "+enabled);
        return locationManager.getLastKnownLocation(provider);
    }

	protected void apiReconnect() {
		Log.i(APP_TAG, "apiReconnect "+
				"\""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId() );
		if(apiSocket.isConnected()) {
			rssdb.log("apiReconnect ignored, already connected.");
		} else {
			if (reconnectLastTry < (System.currentTimeMillis()-(SERVER_CONNECT_TIMEOUT*1000))) {
	            reconnectLastTry = System.currentTimeMillis();
	            rssdb.log("apiReconnect: connecting to "+ICECONDOR_API_URL);
	            try {
	                apiSocket.connect();
	            } catch ( IllegalStateException e) {
	                Log.i(APP_TAG, "apiReconnect: ignored on connect: "+e);
	            }
			} else {
				rssdb.log("apiReconnect ignored. last try is "+ 
						(System.currentTimeMillis()-reconnectLastTry)/1000+" sec ago "+
						"\""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId() );
			}
		}
	}

    protected void apiDisconnect() {
        Log.i(APP_TAG, "apiDisconnect "+
                "\""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId() );
        if(apiSocket != null) {
            try {
                apiSocket.close();
                Log.i(APP_TAG, "apiDisconnect: apiSocket.close called");
            } catch(IllegalStateException e) {
                Log.i(APP_TAG, "apiDisconnect close error (ignored): "+e);
            }
        }
    }

	private void notificationStatusUpdate(String msg) {
	    if(ongoing_notification == null) {
            ongoing_notification = buildNotification();
	    }
	    // preserve the last message to rebuild the notification with a different icon later
        ongoing_notification_msg = msg;
	        
		ongoing_notification.setLatestEventInfo(this, "IceCondor",
				msg, contentIntent);
		ongoing_notification.when = System.currentTimeMillis();
		notificationManager.notify(1, ongoing_notification);
	}
	
	private void notificationRebuild() {
	    notificationCancel();
        ongoing_notification = null;
        notificationStatusUpdate(ongoing_notification_msg);
	}
	
	private void notificationCancel() {
	    notificationManager.cancel(1);
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
		Log.i(APP_TAG,"pigeon: onBind for "+intent.getExtras());
		activity_bound = true;
		if(!friends_followed)
			followFriends();
		return pigeonBinder;
	}
	
	@Override
	public void onRebind(Intent intent) {
		Log.i(APP_TAG, "pigeon: onReBind for "+intent.toString());
		activity_bound = true;
		if(!friends_followed)
			followFriends();
	}
	
	@Override
	public void onLowMemory() {
		rssdb.log("pigeon: onLowMemory");
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(APP_TAG, "pigeon: onUnbind for "+intent.toString());
		activity_bound = false;
		return true;
	}
	
	public void pushQueue() {
		Timer push_queue_timer_single = new Timer("Push Queue Single Timer");
		push_queue_timer_single.schedule(new PushQueueTask(), 0);
	}

	private final class PhoneStateListener extends android.telephony.PhoneStateListener {
	    //@Override
	    //public void onDataConnectionStateChanged(int state, int networkType) {
	    //}
    }

    class PushQueueTask extends TimerTask {
		public void run() {
			pushOldestFix();
		}
	}
	
	public void pushOldestFix() {
		try {
			Cursor oldest;
			rssdb.log("** queue push size "+rssdb.countPositionQueueRemaining()+
			          " \""+Thread.currentThread().getName()+"\""+" tid:"+
			          Thread.currentThread().getId() );
			if ((oldest = rssdb.oldestUnpushedLocationQueue()).getCount() > 0) {
				Gps fix =  Gps.fromJson(oldest.getString(
				                    oldest.getColumnIndex(GeoRss.POSITION_QUEUE_JSON)));
				boolean status = pushLocationApi(fix);
			} 
			oldest.close();
		} catch (IllegalStateException e) {
			Log.i(APP_TAG, ""+e);
		}
	}

	public boolean pushLocationApi(Gps fix) {
		JSONObject json = fix.toJson();
		try {
			json.put("username", getMyUsername());
			rssdb.log("pushLocationApi: "+json.toString());
			boolean pass = apiSocket.emit(json.toString());
			return pass;
		} catch (Exception e) {
			rssdb.log("pushLocationApi: JSONException "+e);
		}
		return false;
	}

    protected String getMyUsername() {
        String[] token_and_secret = LocationStorageProviders.getDefaultAccessToken(this);
        return token_and_secret[1];
    }
	
	public void onDataConnectionChange(int state) {
        rssdb.log("onDataConnectionChange "+state);
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
			       (int)location.getAccuracy()+"m "+ 
			       (time_since_last_update/1000)+" seconds since last update");

		if (on_switch) {
			if(isAccuracyImproved(location, last_recorded_fix) ||
			        time_since_last_update > record_frequency) {
				last_recorded_fix = location;
				last_fix_http_status = 200;
				Gps gps = new Gps();
				gps.setLocation(last_local_fix);
				gps.setBattery(last_battery_level);
				gps.setAC(ac_power);
				long id = rssdb.addToQueue(gps.getId(), gps.toJson().toString());
				pushQueue();
				broadcastGpsFix(location);
			}
		}
	}

    protected boolean isAccuracyImproved(Location recent, Location older) {
        return (recent.getAccuracy() < (older == null?500000:older.getAccuracy()));
    }

	private void broadcastGpsFix(Location location) {
		Intent intent = new Intent(GPS_FIX_ACTION);
		intent.putExtra("location", location);
		sendBroadcast(intent);	
	}

    private void broadcastBirdUpdate(String username) {
        Intent intent = new Intent(BIRD_FIX_ACTION);
        intent.putExtra("username", username);
        sendBroadcast(intent);  
    }
    
    private void broadcastBirdFix(Location location) {
		Intent intent = new Intent(BIRD_FIX_ACTION);
		intent.putExtra("location", location);
		sendBroadcast(intent);	
	}

    private void broadcastAuthOK() {
		Intent intent = new Intent(AUTH_OK_ACTION);
		sendBroadcast(intent);	
	}

	private void play_fix_beep() {
		mp.start();
	}

	public void onProviderDisabled(String provider) {
		rssdb.log("onProviderDisabled: "+provider);
	}

	public void onProviderEnabled(String provider) {
        rssdb.log("onProviderEnabled: "+provider);
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		String status_msg = "";
		if (status ==  LocationProvider.TEMPORARILY_UNAVAILABLE) {status_msg = "TEMPORARILY_UNAVAILABLE";}
		if (status ==  LocationProvider.OUT_OF_SERVICE) {status_msg = "OUT_OF_SERVICE";}
		if (status ==  LocationProvider.AVAILABLE) {status_msg = "AVAILABLE";}
		rssdb.log("onStatusChanged: "+provider+" "+status_msg);
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
			//stopRssTimer();
			//startRssTimer();
			notificationFlash("RSS Read frequency now "+Util.millisecondsToWords(
						Long.parseLong(prefs.getString(pref_name, "N/A"))));
		}
		if (pref_name.equals(SETTING_OPENID)) {
			// have an oauth token now
			doAuth();
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
	    rssdb.log("PushQueue Alarm started at 3 minutes");
	    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 
	    		                  System.currentTimeMillis(), 
	    		                  180000, 
	    						  wake_alarm_intent);
	    //push_queue_timer = new Timer("Push Queue Timer");
	    //push_queue_timer.scheduleAtFixedRate(new PushQueueTask(), 0, 30000);
	}

	private void stopPushQueueTimer() {
		alarmManager.cancel(wake_alarm_intent);
		//push_queue_timer.cancel();
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
	
	protected void startBackground() {
		Log.i(APP_TAG, "pigeon: startBackground()");
		on_switch = true;
		settings.edit().putBoolean(SETTING_PIGEON_TRANSMITTING, on_switch).commit();
		apiReconnect();
        startHeartbeatTimer();
        startPushQueueTimer();
		startLocationUpdates();
		notificationStatusUpdate(notificationStatusLine());				
		Intent intent = new Intent("com.icecondor.nest.WIDGET_ON");
		sendBroadcast(intent);
	}
	
	protected void stopBackground() {
		Log.i(APP_TAG, "pigeon: stopBackground()");
		on_switch = false;
		//stopRssTimer();
		stopLocationUpdates();
		stopPushQueueTimer();
		Log.i(APP_TAG, "pigeon: stopHeartbeatTimer()");
		stopHeartbeatTimer();
		Log.i(APP_TAG, "pigeon: apiDisconnect()");
		apiDisconnect();
		settings.edit().putBoolean(SETTING_PIGEON_TRANSMITTING,on_switch).commit();
		notificationCancel();
		Intent intent = new Intent("com.icecondor.nest.WIDGET_OFF");
		sendBroadcast(intent);
	}
	
	class HeartBeatTask extends TimerTask {
		public void run() {
			String msg = notificationStatusLine();
		    if(ongoing_notification != null) {
		        notificationStatusUpdate(msg); 
		    }
		    if(apiSocket.isConnected()) {
		    	if(!activity_bound && friends_followed) {
			        Log.i(APP_TAG, "heartbeat: activity unbound, unfollowing friends");
		    		unfollowFriends();
		    	}
		    	if(System.currentTimeMillis() - websocket_last_msg > 90*1000) {
			        rssdb.log("heartbeat: missed ping. disconnecting");
		    		apiDisconnect();
		    	}
		    }
		    Log.i(APP_TAG, "heartbeat: apiSocket "+(apiSocket.isConnected()?"connected":"disconnected")+" "+
		    		"activity_bound:"+activity_bound);
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
	    		startBackground();
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

	  private class AlarmReceiver extends BroadcastReceiver {
		  @Override
		  public void onReceive(Context context, Intent intent) {
			  String action = intent.getAction();
			  if (action.equals("com.icecondor.nest.WAKE_ALARM")) {
				  Log.i(APP_TAG, "service, alarm received!");
				  String msg = notificationStatusLine();
				  if(ongoing_notification != null) {
				      notificationStatusUpdate(msg); 
				  }
				  if(!apiSocket.isConnected()) {
					  apiReconnect();
				  }
				  pushOldestFix();
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
				startBackground();
			}
		}
		public void stopTransmitting() throws RemoteException {
			rssdb.log("Pigeon: stopTransmitting");
			stopBackground();
		}
		public Location getLastFix(boolean broadcast) throws RemoteException {
			if (last_local_fix != null && broadcast) {
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
        @Override
        public void addFriend(String username) throws RemoteException {
            apiSocket.addFriend(username);
        }
        @Override
        public void unFriend(String username) throws RemoteException {
            apiSocket.unFriend(username);
        }
        @Override
        public void followFriend(String username) throws RemoteException {
            apiSocket.followFriend(username);
        }
        @Override
        public void unfollowFriend(String username) throws RemoteException {
            apiSocket.unfollowFriend(username);
        }
        @Override
        public void followFriends() throws RemoteException {
            Pigeon.this.followFriends();
        }
        @Override
        public void unfollowFriends() throws RemoteException {
            Pigeon.this.unfollowFriends();
        }
    };

    private void followFriends() {
        /* follow our friends */
        Cursor c = rssdb.findFeedsByService("IceCondor");
        while(c.moveToNext()) {
            String username = c.getString(c.getColumnIndex(GeoRss.FEEDS_EXTRA));
            apiSocket.followFriend(username);
        }
        c.close();
        friends_followed = true;
    }
    
    private void unfollowFriends() {
        /* follow our friends */
        Cursor c = rssdb.findFeedsByService("IceCondor");
        while(c.moveToNext()) {
            String username = c.getString(c.getColumnIndex(GeoRss.FEEDS_EXTRA));
            apiSocket.unfollowFriend(username);
        }
        c.close();
        friends_followed = false;
    }

    private void onApiOpened() {
        notificationRebuild();
        doAuth();
    }

	protected void doAuth() {
		String[] token_and_secret = LocationStorageProviders.getDefaultAccessToken(this);
        if(token_and_secret[0] != null)
        	apiSocket.auth(token_and_secret[0]);
	}

    private void onApiClosed() {
        notificationRebuild();
    }

    private void onApiPing() {
    }

    @Override
	public boolean handleMessage(Message msg) {
        String message_type = msg.getData().getString("type");
        websocket_last_msg = System.currentTimeMillis();

        if(message_type.equals("open")) {
            onApiOpened();
        }
        if(message_type.equals("close")) {
            onApiClosed();
        }
        if(message_type.equals("ping")) {
            onApiPing();
        }
        if(message_type.equals("message")) {
    	    String json_str = msg.getData().getString("json");
    		try {
                JSONObject json = new JSONObject(json_str);
                dispatch(json);
            } catch (JSONException e) {
                rssdb.log("handleMessage json:"+json_str+" err:"+e);
            }
        }
		return true;
	}
	
    void dispatch(JSONObject json) {
        try {
            String type = json.getString("type");

            if(type.equals("location")) {
                doLocation(json);
            }
            
            if(type.equals("auth")) {
                doAuth(json);
            }
            
            if(type.equals("follow")) {
                doFollow(json);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
	}
	
	protected void doAuth(JSONObject json) throws JSONException {
        String status = json.getString("status");
        if(status.equals("OK")) {
            broadcastAuthOK();
            if(activity_bound)
            	followFriends();
            pushQueue();
        }
	}

    protected void doLocation(JSONObject json)
            throws JSONException {
        // change protocol to have txids and respond to specific id
        if(json.has("status")) {
            String status = json.getString("status");
            String id = json.getString("id");
            if(status.equals("OK")) {            	
	            rssdb.log("location "+id+" "+status);
	            Cursor o = rssdb.readLocationQueue(id);
	            Gps gps =  Gps.fromJson(o.getString(
	                                   o.getColumnIndex(GeoRss.POSITION_QUEUE_JSON)));
	            Location pushed_fix = gps.getLocation();
	            if(pushed_fix.getTime() > last_pushed_fix.getTime()) {
	                last_pushed_fix = pushed_fix;
	                last_pushed_time = System.currentTimeMillis();
	                broadcastBirdFix(last_pushed_fix);
	            }
	            o.close();
            }
            rssdb.mark_as_pushed(id);
        }
        if(json.has("username")) {
            String username = json.getString("username");
            int service_id = rssdb.findFeedIdByServicenameAndExtra("IceCondor", username);
            if(service_id >= 0) {
                Gps gps = Gps.fromJson(json);
                rssdb.insertShout(service_id, gps);
                broadcastBirdUpdate(username);
                rssdb.log("pigeon: location updated for "+username);
            } else {
                rssdb.log("pigeon: ignoring location for unknown user "+username);
            }
        }
    }

    protected void doFollow(JSONObject json)
                              throws JSONException {
        String username = json.getString("username");
        if(json.has("mobile_avatar_url")) {
            String profile_url_mobile = json.getString("mobile_avatar_url");
            if(!Util.profilePictureExists(username, this)) {
                GrabAndSavePicture grabUrl = new GrabAndSavePicture(profile_url_mobile,
                                                                    username,
                                                                    httpClient, this);
                (new Thread(grabUrl)).start();
            }
        }
    }
    
    protected String notificationStatusLine() {
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
        String queue_part = "";
        try {
        	queue_part += rssdb.countPositionQueueRemaining()+" queued.";
        } catch (IllegalStateException e) {
        	// a heartbeat can fire after the pigeon has died (closing rssdb)
        	Log.i(APP_TAG, "heartbeat rssdb closed: "+e);
        }
        String beat_part = "";
        if (last_local_fix != null) {
        	String ago = Util.timeAgoInWords(last_local_fix.getTime());
        	beat_part = "fix "+ago+".";
        }
        String msg = fix_part+" "+beat_part+" "+queue_part;
        return msg;
    }

}
