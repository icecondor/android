package com.icecondor.nest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
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
	boolean on_switch;
	private Location last_fix;
	Notification notification;
	NotificationManager notificationManager;
	LocationManager locationManager;
	Pigeon pigeon; // need 'this' for stub
	
	public void onCreate() {
		Log.i(appTag, "*** service created.");
		pigeon = this;
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		Log.i(appTag, "GPS provider enabled: "+locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
		locationManager.getProvider(LocationManager.GPS_PROVIDER);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		CharSequence text = getText(R.string.status_transmitting);
		notification = new Notification(R.drawable.statusbar,text,System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, Start.class), 0);
		notification.setLatestEventInfo(this, "event title", "event text", contentIntent);

//		timer.scheduleAtFixedRate(
//			new TimerTask() {
//				public void run() {
//					if (on_switch) {
//						Location fix;
//						fix = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//						//fix = phoneyLocation();
//						last_fix = fix;
//						pushLocation(fix);
//					}
//				}
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
//			}, 0, ICECONDOR_READ_INTERVAL);		
		on_switch = true;
	}
	
	public void onStart() {
		Log.i(appTag, "service started!");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.i(appTag, "onBind for "+intent.getAction());
		return pigeonBinder;
	}
	
	public void pushLocation(Location fix) {
		try {
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
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
			Log.i(appTag, "startTransmitting");
			on_switch = true;
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 6000L, 0.0F, pigeon);
			notificationManager.notify(1, notification);
		}
		public void stopTransmitting() throws RemoteException {
			Log.i(appTag, "stopTransmitting");
			on_switch = false;
			locationManager.removeUpdates(pigeon);
			notificationManager.cancel(1);
		}
		public Location getLastFix() throws RemoteException {
			return last_fix;
		}
    };

	public void onLocationChanged(Location location) {
		last_fix = location;
		Log.i(appTag, "onLocationChanged: "+location);
		//if (on_switch) { pushLocation(location); }
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
		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Log.i(appTag, "location anyways: "+location+ " msec"+location.getTime()+" time:"+Util.DateTimeIso8601(location.getTime()-(60*60*7))); 
	}
}
