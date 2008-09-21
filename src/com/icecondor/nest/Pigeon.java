package com.icecondor.nest;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.IBinder;
import android.util.Log;

//look at android.permission.RECEIVE_BOOT_COMPLETED

public class Pigeon extends Service {
	private static final long UPDATE_INTERVAL = 5000;
	private Timer timer = new Timer();
	static final String appTag = "IcePigeon";
	boolean on_switch = true;
	
	public void onCreate() {
		Log.i(appTag, "*** service created.");
		final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		timer.scheduleAtFixedRate(
			new TimerTask() {
				public void run() {
					Location fix;
					fix = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					fix = new Location("phoney");
					pushLocation(fix);		
				}
			}, 0, UPDATE_INTERVAL);		
	}
	
	public void onStart() {
		Log.i(appTag, "service started!");
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void pushLocation(Location fix) {
		String URL = "http://10.0.2.2/icecondor/locations"; // use preference
		try {
			Log.i(appTag, "sending fix: "+fix.getLatitude()+" "+fix.getLongitude());
			
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(URL);
			ArrayList <NameValuePair> dict = new ArrayList <NameValuePair>();
			dict.add(new BasicNameValuePair("location[latitude]", Double.toString(fix.getLatitude())));
			dict.add(new BasicNameValuePair("location[longitude]", Double.toString(fix.getLongitude())));
			dict.add(new BasicNameValuePair("location[altitude]", Double.toString(fix.getAltitude())));
			post.setEntity(new UrlEncodedFormEntity(dict, HTTP.UTF_8));
			HttpResponse response;
			response = client.execute(post);
			Log.i(appTag, "http response: "+response.getStatusLine());
		} catch (NullPointerException t) {
			Log.i(appTag,"no data in location record"+t);
		} catch (ClientProtocolException e) {
			Log.i(appTag, "client protocol exception");
			e.printStackTrace();
		} catch (HttpHostConnectException e) {
			Log.i(appTag, "connection failed");
		} catch (IOException e) {
			Log.i(appTag, "IO exception");
			e.printStackTrace();
		}

	}

}
