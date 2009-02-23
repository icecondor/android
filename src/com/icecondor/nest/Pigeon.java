package com.icecondor.nest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.client.OAuthClient;
import net.oauth.client.httpclient4.HttpClient4;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
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
import android.sax.Element;
import android.util.Log;
import android.widget.Toast;

//look at android.permission.RECEIVE_BOOT_COMPLETED

public class Pigeon extends Service implements Constants, LocationListener,
                                               SharedPreferences.OnSharedPreferenceChangeListener {
	private Timer heartbeat_timer = new Timer();
	private Timer rss_timer;
	//private Timer wifi_scan_timer = new Timer();
	static final String appTag = "Pigeon";
	boolean on_switch = false;
	private Location last_fix, last_local_fix;
	int last_fix_http_status;
	Notification ongoing_notification;
	NotificationManager notificationManager;
	LocationManager locationManager;
	WifiManager wifiManager;
	PendingIntent contentIntent;
	SharedPreferences settings;
	SQLiteDatabase geoRssDb;
	
	public void onCreate() {
		Log.i(appTag, "*** service created.");
		super.onCreate();
		
		/* GPS */
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		Log.i(appTag, "GPS provider enabled: "+locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
		last_local_fix = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Log.i(appTag, "Last known GPS fix: "+last_local_fix);
		Log.i(appTag, "NETWORK provider enabled: "+locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
		Log.i(appTag, "Last known NETWORK fix: "+locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
		if (last_local_fix == null) { // fall back onto the network location
			last_local_fix = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
		
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
		
		/* Preferences */
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		settings.registerOnSharedPreferenceChangeListener(this);
		on_switch = settings.getBoolean(SETTING_PIGEON_TRANSMITTING, false);
		if (on_switch) {
			startLocationUpdates();
		}

		heartbeat_timer.scheduleAtFixedRate(
			new TimerTask() {
				public void run() {
					String fix_part = "";
					if (on_switch) {
						if (last_fix != null) {
							String ago = Util.timeAgoInWords(last_fix.getTime());
							String http_status = "";
							if (last_fix_http_status != 200) {
								fix_part = last_fix.getProvider()+" publish error.";
							} else {
								fix_part = last_fix.getProvider()+" push"+http_status+" "+
							           ago+".";
							}
						
						}
						if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
							fix_part = "Warning: GPS set to disabled";
						}
					} else {
						fix_part = "Location reporting is off.";
					}
					String beat_part = "";
					if (last_local_fix != null) {
						String ago = Util.timeAgoInWords(last_local_fix.getTime());
						beat_part = "fix "+ago;
					}
					notificationStatusUpdate(fix_part+" "+beat_part); 
				}
			}, 0, 20000);		

		start_rss_timer();
}
	public void onDestroy() {
		stop_rss_timer();
	}
	
	private void start_rss_timer() {
		rss_timer = new Timer();
		// GeoRSS Database
		GeoRssSqlite rssdb = new GeoRssSqlite(this, "georss", null, 1);
		geoRssDb = rssdb.getWritableDatabase();
		long rss_read_frequency = Long.decode(settings.getString(SETTING_RSS_READ_FREQUENCY, "60000"));
		Log.i(appTag, "starting rss timer at frequency "+rss_read_frequency);
		rss_timer.scheduleAtFixedRate(
				new TimerTask() {
					public void run() {
						Log.i(appTag, "rss_timer fired");
						updateRSS();
					}
				}, 0, rss_read_frequency);
	}
	
	private void stop_rss_timer() {
		rss_timer.cancel();
		geoRssDb.close();
	}
	
	protected void updateRSS() {
		new Timer().schedule(
				new TimerTask() {
					public void run() {
						Log.i(appTag, "rss_timer fired");
						Cursor geoRssUrls = geoRssDb.query(GeoRssSqlite.SERVICES_TABLE,null, null, null, null, null, null);
						while (geoRssUrls.moveToNext()) {
							try {
								readGeoRss(geoRssUrls);
							} catch (ClientProtocolException e) {
								Log.i(appTag, "http protocol exception "+e);
							} catch (IOException e) {
								Log.i(appTag, "io error "+e);
							}
						}
						geoRssUrls.close();						
					}
				}, 0);
		Toast.makeText(this, "Refreshing GeoRSS feeds", Toast.LENGTH_SHORT).show();
	}

	protected void readGeoRss(Cursor geoRssUrls) throws ClientProtocolException, IOException {
		String urlString = geoRssUrls.getString(geoRssUrls.getColumnIndex(GeoRssSqlite.URL));
		Log.i(appTag, "readGeoRss "+urlString);
		URL url = new URL(urlString);
		URLConnection urlConn = url.openConnection();
		urlConn.setReadTimeout(15000);
		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			Document doc = db.parse(urlConn.getInputStream());
			NodeList items = doc.getElementsByTagName("item");

			Log.i(appTag, "i read "+items.getLength()+" shouts");
			for (int i = 0; i < items.getLength(); i++) {
				String guid = null, title = null, date =null;
				float latitude = -100, longitude = -200;
				NodeList item_elements = items.item(i).getChildNodes();
				for(int j=0; j < item_elements.getLength(); j++) {
					Node sub_item = item_elements.item(j);
					if(sub_item.getNodeName().equals("guid")) {
						guid = sub_item.getFirstChild().getNodeValue();
					}
					if(sub_item.getNodeName().equals("title")) {
						title = sub_item.getFirstChild().getNodeValue();
					}
					if(sub_item.getNodeName().equals("pubDate")) {
						date = Util.DateTimeIso8601(Util.DateRfc822(sub_item.getFirstChild().getNodeValue()));
					}
					if(sub_item.getNodeName().equals("geo:lat")) {
						latitude = Float.parseFloat(sub_item.getFirstChild().getNodeValue());
					}
					if(sub_item.getNodeName().equals("geo:long")) {
						DateFormat date_format = DateFormat.getInstance();
						longitude = Float.parseFloat(sub_item.getFirstChild().getNodeValue());
					}
				}
				Log.i(appTag, "item #"+i+" guid:"+ guid+" lat:"+
						latitude + " long:"+longitude +" date:"+date);
				ContentValues cv = new ContentValues(2);
				cv.put("guid", guid);
				cv.put("lat", latitude);
				cv.put("long", longitude);
				cv.put("date", date);
				cv.put("title", title);
				cv.put("service_id", geoRssUrls.getInt(geoRssUrls.getColumnIndex("_id")));
				geoRssDb.insert(GeoRssSqlite.SHOUTS_TABLE, null, cv);
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}	
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
		long record_frequency = Long.decode(settings.getString(SETTING_TRANSMISSION_FREQUENCY, "60000"));
		Log.i(appTag, "requesting GPS updates with frequency "+record_frequency);
		locationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER, 
				record_frequency, 
				0.0F, this);
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
		locationManager.removeUpdates(this);
	}

	public void onStart(Intent start, int key) {
		super.onStart(start,key);
		Log.i(appTag, "pigeon service started!");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.i(appTag, "onBind for "+intent.getAction());
		return pigeonBinder;
	}
	
	public int pushLocation(Location fix) {
		Log.i(appTag, "sending id: "+settings.getString(SETTING_OPENID,"")+ " fix: " 
				+fix.getLatitude()+" long: "+fix.getLongitude()+
				" alt: "+fix.getAltitude() + " time: " + Util.DateTimeIso8601(fix.getTime()) +
				" acc: "+fix.getAccuracy());
		//ArrayList <NameValuePair> params = new ArrayList <NameValuePair>();
		ArrayList<Map.Entry<String, String>> params = new ArrayList<Map.Entry<String, String>>();
		addPostParameters(params, fix);
		OAuthClient oclient = new OAuthClient(new HttpClient4());
		OAuthAccessor accessor = LocationRepositoriesSqlite.defaultAccessor(this);
		String[] token_and_secret = LocationRepositoriesSqlite.getDefaultAccessToken(this);
		params.add(new OAuth.Parameter("oauth_token", token_and_secret[0]));
		accessor.tokenSecret = token_and_secret[1];
		try {
			OAuthMessage omessage;
			Log.d(appTag, "invoke("+accessor+", POST, "+ICECONDOR_WRITE_URL+", "+params);
			omessage = oclient.invoke(accessor, "POST",  ICECONDOR_WRITE_URL, params);
			omessage.getHeader("Result");
			return 200;
		} catch (OAuthException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// includes host not found
			e.printStackTrace();
		}
		return 500; // something went wrong
	}
	
	private void addPostParameters(ArrayList<Map.Entry<String, String>> dict, Location fix) {
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
				notificationStatusUpdate("Waiting for fix.");				
				notificationFlash("Location reporting ON.");
			}
		}
		public void stopTransmitting() throws RemoteException {
			Log.i(appTag, "stopTransmitting");
			on_switch = false;
			settings.edit().putBoolean(SETTING_PIGEON_TRANSMITTING,on_switch).commit();
			stopLocationUpdates();
			notificationStatusUpdate("Location reporting is off.");				
			notificationFlash("Location reporting OFF.");
		}
		public Location getLastFix() throws RemoteException {
			return last_local_fix;
		}
		@Override
		public Location getLastPushedFix() throws RemoteException {
			return last_fix;		
		}
		@Override
		public void refreshRSS() throws RemoteException {
			updateRSS();
		}
    };

	public void onLocationChanged(Location location) {
		last_local_fix = location;
		long time_since_last_update = last_local_fix.getTime() - (last_fix == null?0:last_fix.getTime()); 
		long record_frequency = Long.decode(settings.getString(SETTING_TRANSMISSION_FREQUENCY, "180000"));
		Log.i(appTag, "onLocationChanged: at:"+location.getLatitude()+" long:"+location.getLongitude() + " acc:"+
			       location.getAccuracy()+" "+ time_since_last_update+" seconds since last update");

		if (on_switch) {
			if((last_local_fix.getAccuracy() < (last_fix == null?500000:last_fix.getAccuracy())) ||
					time_since_last_update > record_frequency ) {
				last_fix_http_status = pushLocation(last_local_fix);
				if(last_fix_http_status == 200) {
					last_fix = last_local_fix;
				}
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
	public void onSharedPreferenceChanged(SharedPreferences prefs, String pref_name) {
		Log.i(appTag, "shared preference changed: "+pref_name);		
		if (pref_name.equals(SETTING_TRANSMISSION_FREQUENCY)) {
			if (on_switch) {
				stopLocationUpdates();
				startLocationUpdates();
				notificationFlash("Position reporting frequency now "+Util.millisecondsToWords(
						Long.parseLong(prefs.getString(pref_name, "N/A"))));
			}
		}
		if (pref_name.equals(SETTING_RSS_READ_FREQUENCY)) {
			if (on_switch) {
				stop_rss_timer();
				start_rss_timer();
				notificationFlash("RSS Read frequency now "+Util.millisecondsToWords(
						Long.parseLong(prefs.getString(pref_name, "N/A"))));
			}
		}
	}
}
