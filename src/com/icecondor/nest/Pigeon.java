package com.icecondor.nest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

//look at android.permission.RECEIVE_BOOT_COMPLETED

public class Pigeon extends Service implements Constants, LocationListener {
	private Timer timer = new Timer();
	static final String appTag = "Pigeon";
	boolean on_switch = false;
	private Location last_fix, last_local_fix;
	Notification notification;
	Notification heartbeat_notification;
	NotificationManager notificationManager;
	LocationManager locationManager;
	Pigeon pigeon; // need 'this' for stub
	PendingIntent contentIntent;
	SharedPreferences settings;
	
	public void onCreate() {
		Log.i(appTag, "*** service created.");
		pigeon = this;
		settings = getSharedPreferences(PREFS_NAME, 0);
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		Log.i(appTag, "GPS provider enabled: "+locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
		locationManager.getProvider(LocationManager.GPS_PROVIDER);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		contentIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				Start.class), 0);
		
		on_switch = settings.getBoolean("pigeon_on", true);
		if (on_switch) {
			CharSequence text = getText(R.string.status_transmitting);
			notification = new Notification(R.drawable.statusbar, text, System
					.currentTimeMillis());
			notification.flags = notification.flags
					^ Notification.FLAG_ONGOING_EVENT;
			notification.setLatestEventInfo(this, "IceCondor",
					"Background task started, awating fix.", contentIntent);
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 60000L, 0.0F, pigeon);
		}

		heartbeat_notification = new Notification(R.drawable.statusbar,"Heartbeat On",System.currentTimeMillis());
		heartbeat_notification.flags = heartbeat_notification.flags ^ Notification.FLAG_ONGOING_EVENT;

		timer.scheduleAtFixedRate(
			new TimerTask() {
				public void run() {
					Log.i(appTag, "heartbeat. last_fix is "+last_fix);
					String ago="none";
					String unit="";
					if (last_fix != null) {
						long seconds_ago = (Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis() - last_fix.getTime())/1000;
						unit = "sec.";
						if (seconds_ago > 60) {
							seconds_ago = seconds_ago / 60;
							unit = "min.";
						}
						ago = ""+seconds_ago+" "+unit+" ago";
					}
					String time = Calendar.getInstance().getTime().getHours() + ":" 
					              + Calendar.getInstance().getTime().getMinutes() + ":"
					              + Calendar.getInstance().getTime().getSeconds();
					
					heartbeat_notification.setLatestEventInfo(pigeon, "IceCondor Heartbeat", 
							             "beat "+time+" last GPS fix "+ago, contentIntent);
					notificationManager.notify(2, heartbeat_notification);
				}
//
//				private Location phoneyLocation() {
//					Location fix;
//					fix = new Location("phoney");
//					// simulation
//					Random rand = new Random();
//					fix.setLatitude(45+rand.nextFloat());
//					fix.setLongitude(-122-rand.nextFloat());
//					fix.setTime(Calendar.getInstance().getTimeInMillis());
//					return fix;
//				}
			}, 0, 30000);		
	}
	
	public void onStart() {
		Log.i(appTag, "service started!");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.i(appTag, "onBind for "+intent.getAction());
		return pigeonBinder;
	}
	
	public int pushLocation(Location fix) {
		try {

			Log.i(appTag, "sending id: "+settings.getString("uuid","")+ " fix: " 
					+fix.getLatitude()+" long: "+fix.getLongitude()+
					" alt: "+fix.getAltitude() + " time: " + Util.DateTimeIso8601(fix.getTime()) +
					" meters: "+fix.getAccuracy());
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(ICECONDOR_READ_URL);

			post.addHeader("X_REQUESTED_WITH", "XMLHttpRequest");
			post.setEntity(buildPostParameters(fix, settings.getString("uuid","")));
			HttpResponse response;
			response = client.execute(post);
			Log.i(appTag, "http response: "+response.getStatusLine());
			return response.getStatusLine().getStatusCode();
		} catch (NullPointerException t) {
			Log.i(appTag,"no data in location record "+t);
		} catch (ClientProtocolException e) {
			Log.i(appTag, "client protocol exception "+e);
			e.printStackTrace();
		} catch (HttpHostConnectException e) {
			Log.i(appTag, "connection failed "+e);
		} catch (IOException e) {
			Log.i(appTag, "IO exception "+e);
			e.printStackTrace();
		}
		return 0;
	}
	
	private UrlEncodedFormEntity buildPostParameters(Location fix, String uuid) throws UnsupportedEncodingException {
		ArrayList <NameValuePair> dict = new ArrayList <NameValuePair>();
		dict.add(new BasicNameValuePair("location[latitude]", Double.toString(fix.getLatitude())));
		dict.add(new BasicNameValuePair("location[longitude]", Double.toString(fix.getLongitude())));
		dict.add(new BasicNameValuePair("location[altitude]", Double.toString(fix.getAltitude())));
		dict.add(new BasicNameValuePair("location[guid]", uuid));
		dict.add(new BasicNameValuePair("client[version]", getString(R.string.menu_version)));
		if(fix.hasAccuracy()) {
			dict.add(new BasicNameValuePair("location[accuracy]", Double.toString(fix.getAccuracy())));
		}
		dict.add(new BasicNameValuePair("location[timestamp]", Util.DateTimeIso8601(fix.getTime())));
		return new UrlEncodedFormEntity(dict, HTTP.UTF_8);
	}
	
    private final PigeonService.Stub pigeonBinder = new PigeonService.Stub() {
		public boolean isTransmitting() throws RemoteException {
			Log.i(appTag, "isTransmitting => "+on_switch);
			return on_switch;
		}
		public void startTransmitting() throws RemoteException {
			if (on_switch) {
				Log.i(appTag, "startTransmitting: already transmitting");
			} else {
				Log.i(appTag, "startTransmitting");
				on_switch = true;
				settings.edit().putBoolean("pigeon_on", on_switch).commit();
				locationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, 60000L, 0.0F, pigeon);
				notificationManager.notify(1, notification);
			}
		}
		public void stopTransmitting() throws RemoteException {
			Log.i(appTag, "stopTransmitting");
			on_switch = false;
			settings.edit().putBoolean("pigeon_on",on_switch).commit();
			locationManager.removeUpdates(pigeon);
			notificationManager.cancel(1);
		}
		public Location getLastFix() throws RemoteException {
			return last_fix;
		}
    };

	public void onLocationChanged(Location location) {
		String msg = "onLocationChanged: "+location;
		if(last_local_fix != null) {
			msg = msg + " last location change elapsed time "+(location.getTime()-last_local_fix.getTime())/1000.0;
		}
		Log.i(appTag, msg);
		last_local_fix = location;
		if (on_switch) {
			long last_time = 0;
			if(last_fix != null) { last_time = last_fix.getTime(); }
			long time_since_last_update = location.getTime() - last_time; 
			if(time_since_last_update > PIGEON_LOCATION_POST_INTERVAL) { 
				last_fix = location;
				int result = pushLocation(location); 
				if(result >= 200 && result < 300) {
					notification.setLatestEventInfo(this, "IceCondor", "Location pushed", contentIntent);
				} else {
					notification.setLatestEventInfo(this, "IceCondor", "Location push failed (HTTP error "+result+")", contentIntent);				
				}
				notificationManager.notify(1, notification);
			} else {
				Log.i(appTag, time_since_last_update/1000+" is less than "+
						PIGEON_LOCATION_POST_INTERVAL/1000+ " server push skipped");
			}
		}
	}

	public void onProviderDisabled(String provider) {
		Log.i(appTag, "provider "+provider+" disabled");
	}

	public void onProviderEnabled(String provider) {
		Log.i(appTag, "provider "+provider+" enabled");		
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		String status_msg = "";
		if (status ==  LocationProvider.TEMPORARILY_UNAVAILABLE) {status_msg = "TEMPORARILY_UNAVAILABLE";}
		if (status ==  LocationProvider.OUT_OF_SERVICE) {status_msg = "OUT_OF_SERVICE";}
		if (status ==  LocationProvider.AVAILABLE) {status_msg = "AVAILABLE";}
		Log.i(appTag, "provider "+provider+" status changed to "+status_msg+
				" enabled: "+locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
	}
}
