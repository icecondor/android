package com.icecondor.nest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

//look at android.permission.RECEIVE_BOOT_COMPLETED

public class Pigeon extends Service {
	private static final long UPDATE_INTERVAL = 5000;
	private Timer timer = new Timer();
	static final String appTag = "Pigeon";
	String URL = "http://donpark.org/icecondor/locations"; // use preference
	boolean on_switch;
	
	public void onCreate() {
		Log.i(appTag, "*** service created.");
		final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		timer.scheduleAtFixedRate(
			new TimerTask() {
				public void run() {
					if (on_switch) {
						Location fix;
						fix = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
						fix = new Location("phoney");
						pushLocation(fix);
					}
				}
			}, 0, UPDATE_INTERVAL);		
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
			Log.i(appTag, "sending fix: lat "+fix.getLatitude()+" long "+fix.getLongitude()+" alt "+fix.getAltitude());
			
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(URL);
			post.setEntity(buildPostParameters(fix));
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
	
	private UrlEncodedFormEntity buildPostParameters(Location fix) throws UnsupportedEncodingException {
		ArrayList <NameValuePair> dict = new ArrayList <NameValuePair>();
		dict.add(new BasicNameValuePair("location[latitude]", Double.toString(fix.getLatitude())));
		dict.add(new BasicNameValuePair("location[longitude]", Double.toString(fix.getLongitude())));
		dict.add(new BasicNameValuePair("location[altitude]", Double.toString(fix.getAltitude())));
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
		}
		public void stopTransmitting() throws RemoteException {
			Log.i(appTag, "stopTransmitting");
			on_switch = false;
		}
    };
}
