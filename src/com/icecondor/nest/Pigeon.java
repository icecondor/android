package com.icecondor.nest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

//look at android.permission.RECEIVE_BOOT_COMPLETED

public class Pigeon extends Service implements Constants, LocationListener,
                                               SharedPreferences.OnSharedPreferenceChangeListener {
	private Timer heartbeat_timer = new Timer();
	private Timer rss_timer = new Timer();
	//private Timer wifi_scan_timer = new Timer();
	static final String appTag = "Pigeon";
	boolean on_switch = false;
	private Location last_fix, last_local_fix;
	int last_fix_http_status;
	Notification notification;
	NotificationManager notificationManager;
	LocationManager locationManager;
	WifiManager wifiManager;
	Pigeon pigeon; // need 'this' for stub
	PendingIntent contentIntent;
	SharedPreferences settings;
	SQLiteDatabase geoRssDb;
	
	public void onCreate() {
		Log.i(appTag, "*** service created.");
		super.onCreate();
		pigeon = this;
		
		/* GPS */
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		Log.i(appTag, "GPS provider enabled: "+locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
		Log.i(appTag, "NETWORK provider enabled: "+locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
		
		/* WIFI */
		wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		
		/* Notifications */
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		contentIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				Start.class), 0);
		CharSequence text = getText(R.string.status_transmitting);
		notification = new Notification(R.drawable.icecube_statusbar, text, System
				.currentTimeMillis());
		notification.flags = notification.flags ^ Notification.FLAG_ONGOING_EVENT;
		
		/* Preferences */
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		settings.registerOnSharedPreferenceChangeListener(this);
		on_switch = settings.getBoolean(SETTING_PIGEON_TRANSMITTING, true);
		if (on_switch) {
			notificationStatusUpdate("Background task started, awating first fix.");
			startLocationUpdates();
		}

		heartbeat_timer.scheduleAtFixedRate(
			new TimerTask() {
				public void run() {
					Log.i(appTag, "heartbeat. last_fix is "+last_fix);
					String fix_part;
					if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
						fix_part = "No fix yet.";
					} else {
						fix_part = "Warning: GPS set to disabled";
					}
					if (last_fix != null) {
						String ago = Util.timeAgoInWords(last_fix.getTime());
						fix_part = last_fix.getProvider()+" push("+last_fix_http_status+") "+
						           ago;
					}					

					String beat_part = "";
					if (last_local_fix != null) {
						String ago = Util.timeAgoInWords(last_local_fix.getTime());
						beat_part = "fix "+ago;
					}
					notificationStatusUpdate(fix_part+" "+beat_part); 
				}
			}, 0, 30000);		

		// GeoRSS Database
		GeoRssSqlite rssdb = new GeoRssSqlite(this, "georss", null, 1);
		geoRssDb = rssdb.getWritableDatabase();

		rss_timer.scheduleAtFixedRate(
				new TimerTask() {
					public void run() {
						Log.i(appTag, "rss_timer fired");
						Cursor geoRssUrls = geoRssDb.query("urls",null, null, null, null, null, null);
						while (geoRssUrls.moveToNext()) {
							try {
								readGeoRss(geoRssUrls.getString(geoRssUrls.getColumnIndex(GeoRssSqlite.URL)));
							} catch (ClientProtocolException e) {
								Log.i(appTag, "http protocol exception "+e);
							} catch (IOException e) {
								Log.i(appTag, "io error "+e);
							}
						}
					}
				}, 0, ICECONDOR_READ_INTERVAL);
}

	protected void readGeoRss(String urlString) throws ClientProtocolException, IOException {
		Log.i(appTag, "readGeoRss "+urlString);
		URL url = new URL(urlString);
		URLConnection urlConn = url.openConnection();
		urlConn.setReadTimeout(15000);
		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			Document doc = db.parse(urlConn.getInputStream());
			int shoutsSize = doc.getElementsByTagName("item").getLength();
			Log.i(appTag, "i read "+shoutsSize+" shouts");
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	private void notificationStatusUpdate(String msg) {
		notification.setLatestEventInfo(this, "IceCondor",
				msg, contentIntent);
		notification.when = System.currentTimeMillis();
		notificationManager.notify(1, notification);
	}

	private void startLocationUpdates() {
		long record_frequency = Long.decode(settings.getString(SETTING_RECORD_FREQUENCY, "60000"));
		Log.i(appTag, "requesting GPS updates with frequency "+record_frequency);
		locationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER, 
				record_frequency, 
				0.0F, pigeon);
		// Network provider takes no extra power but the accuracy is
		// too low to be useful.
		//locationManager.requestLocationUpdates(
		//		LocationManager.NETWORK_PROVIDER, 60000L, 0.0F, pigeon);
//		Log.i(appTag, "kicking off wifi scan timer");
//		wifi_scan_timer.scheduleAtFixedRate(
//				new TimerTask() {
//					public void run() {
//						Log.i(appTag, "wifi: start scan (enabled:"+wifiManager.isWifiEnabled()+")");
//						wifiManager.startScan();
//					}
//				}, 0, 60000);		
	}
	
	private void stopLocationUpdates() {
		Log.i(appTag, "stopping GPS updates");		
		locationManager.removeUpdates(pigeon);
	}

	public void onStart(Intent start, int key) {
		super.onStart(start,key);
		Log.i(appTag, "service started!");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.i(appTag, "onBind for "+intent.getAction());
		return pigeonBinder;
	}
	
	public int pushLocation(Location fix) {
		try {

			Log.i(appTag, "sending id: "+settings.getString(SETTING_OPENID,"")+ " fix: " 
					+fix.getLatitude()+" long: "+fix.getLongitude()+
					" alt: "+fix.getAltitude() + " time: " + Util.DateTimeIso8601(fix.getTime()) +
					" meters: "+fix.getAccuracy());
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(ICECONDOR_READ_URL);

			post.addHeader("X_REQUESTED_WITH", "XMLHttpRequest");
			post.setEntity(buildPostParameters(fix, settings.getString(SETTING_OPENID,"")));
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
		dict.add(new BasicNameValuePair("client[version]", ""+ICECONDOR_VERSION));
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
				settings.edit().putBoolean(SETTING_PIGEON_TRANSMITTING, on_switch).commit();
				startLocationUpdates();
				notificationStatusUpdate("Background task activated.");
			}
		}
		public void stopTransmitting() throws RemoteException {
			Log.i(appTag, "stopTransmitting");
			on_switch = false;
			settings.edit().putBoolean(SETTING_PIGEON_TRANSMITTING,on_switch).commit();
			stopLocationUpdates();
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
			long record_frequency = Long.decode(settings.getString(SETTING_RECORD_FREQUENCY, "60000"));
			if(time_since_last_update > record_frequency) { 
				last_fix = location;
				last_fix_http_status = pushLocation(location); 
			} else {
				Log.i(appTag, time_since_last_update/1000+" sec. is less than "+
						record_frequency/1000+ " sec. server push skipped");
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
		Log.i(appTag, "provider "+provider+" status changed to "+status_msg);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String pref_name) {
		Log.i(appTag, "shared preference changed: "+pref_name);		
		if (pref_name.equals(SETTING_RECORD_FREQUENCY)) {
			if (on_switch) {
				stopLocationUpdates();
				startLocationUpdates();
			}
		}
	}
}
