package com.icecondor.nest.db;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.icecondor.nest.Util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;


public class GeoRss {
	public static final String appTag = "GeoRSS";
	public static final String DATABASE_NAME = "georss";
	public static final int DATABASE_VERSION = 6;

	public static final String FEEDS_TABLE = "feeds";
	public static final String FEEDS_ID = "_id";
	public static final String FEEDS_SERVICENAME = "service_name";
	public static final String FEEDS_EXTRA = "extra";
	public static final String FEEDS_TITLE = "title";
	public static final String FEEDS_UPDATED_AT = "updated_at";
	
	public static final String SHOUTS_TABLE = "shouts";
	public static final String SHOUTS_TITLE = "title";
	public static final String SHOUTS_DATE = "date";
	public static final String SHOUTS_FEED_ID = "feed_id";
	
	public static final String ACTIVITY_TABLE = "activities";
	public static final String ACTIVITY_DATE = "date";
	public static final String ACTIVITY_DESCRIPTION = "description";
	
	public static final String POSITION_QUEUE_TABLE = "position_queue";
	public static final String POSITION_QUEUE_CREATED_AT = "created_at";
	public static final String POSITION_QUEUE_JSON = "json";
	public static final String POSITION_QUEUE_SENT = "sent";
	
	
	private SQLiteDatabase db;
	private final Context context;
	private GeoRssHelper dbHelper;
	
	public GeoRss(Context context){
		this.context = context;
		dbHelper = new GeoRssHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public GeoRss open() throws SQLException {
		db = dbHelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		db.close();
	}

	private static class GeoRssHelper extends SQLiteOpenHelper {
		private final Context context;
		
		public GeoRssHelper(Context context, String name, CursorFactory factory,
				int version) {
			super(context, name, factory, version);
			this.context = context;
		}
	
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE "+FEEDS_TABLE+" (_id integer primary key, service_name text, title text, extra text, username text, password text, "+FEEDS_UPDATED_AT+" datetime)");
			db.execSQL("CREATE TABLE "+SHOUTS_TABLE+" (_id integer primary key, guid text unique on conflict replace, title text, lat float, long float, date datetime, feed_id integer)");
			db.execSQL("CREATE TABLE "+ACTIVITY_TABLE+" (_id integer primary key, date datetime, type text, description text)");
			db.execSQL("CREATE TABLE "+POSITION_QUEUE_TABLE+" (_id integer primary key, "+POSITION_QUEUE_JSON+" text, "+POSITION_QUEUE_CREATED_AT+" datetime, "+POSITION_QUEUE_SENT+" datetime)");
		}
	
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Toast.makeText(context, "Version is too old! Uninstall and reinstall.", Toast.LENGTH_LONG).show();
		}
	}

	public Cursor findPreShouts(long url_id, long currentTimeMillis) {
		return db.query(GeoRss.SHOUTS_TABLE, null, SHOUTS_FEED_ID+" = ? and " +
				"date <= ?", 
				new String[] {String.valueOf(url_id), Util.DateTimeIso8601(currentTimeMillis)},
				null, null, "date desc", "1");
	}

	public Cursor findPostShouts(long url_id, long currentTimeMillis) {
		return db.query(GeoRss.SHOUTS_TABLE, null, SHOUTS_FEED_ID+" = ? and " +
				"date > ?", 
				new String[] {String.valueOf(url_id), Util.DateTimeIso8601(currentTimeMillis)},
				null, null, "date asc", "1");
	}

	public Cursor findShouts(long url_id) {
		return db.query(GeoRss.SHOUTS_TABLE, null, SHOUTS_FEED_ID+" = ?",
				new String[] {String.valueOf(url_id)},
				null, null, "date desc", null);
	}

	public long insertShout(ContentValues cv) {
		return db.insert(GeoRss.SHOUTS_TABLE, null, cv);
	}

	public String urlFor(String serviceName, String extra) {
		String url = extra; // if serviceName is blank or RSS
		if (serviceName.equals("brightkite.com")) {
			url = "http://brightkite.com/people/"+extra+"/objects.rss";
		}else if (serviceName.equals("shizzow.com")) {
			url = "http://shizzow.com/people/"+extra+"/rss";
		}else if (serviceName.equals("icecondor.com")) {
			url = "http://icecondor.com/locations.rss?id="+extra;
		}
		return url;
	}

	public void addFeed(ContentValues cv) {
		db.insert(GeoRss.FEEDS_TABLE, null, cv);
	}

	public long addPosition(String locationJson) {
		ContentValues cv = new ContentValues(2);
		cv.put(POSITION_QUEUE_CREATED_AT, Util.DateTimeIso8601Now());
		cv.put(POSITION_QUEUE_JSON, locationJson);
		return db.insert(GeoRss.POSITION_QUEUE_TABLE, null, cv);		
	}

	public Cursor findFeeds() {
		return  db.query(GeoRss.FEEDS_TABLE,null, null, null, null, null, null);
	}


	public Cursor findFeed(int row_id) {
		return findRow(FEEDS_TABLE, row_id);
	}
	
	public Cursor findRow(String table, int row_id) {
		return db.query(table,null, "_id = ?",
                new String[] {""+row_id}, null, null, null);
	}

	public void deleteFeed(int row_id) {
		// the parameter substitution form of execSQL wasnt working. yuk.
		db.execSQL("DELETE from "+GeoRss.FEEDS_TABLE+" where "+GeoRss.FEEDS_ID+ " = "+row_id);
		db.execSQL("DELETE from "+GeoRss.SHOUTS_TABLE+" where "+GeoRss.SHOUTS_FEED_ID+ " = "+row_id);
	}

	public Cursor findLastShout(int feed_id) {
		return db.query(GeoRss.SHOUTS_TABLE,null, SHOUTS_FEED_ID+" = ?",
                new String[] {""+feed_id}, null, null, SHOUTS_DATE+" desc", "1");
	}
	
	public int countFeeds() {
		int count = count(GeoRss.FEEDS_TABLE);
		return count;
	}
	
	public int countPositionQueue() {
		return count(GeoRss.POSITION_QUEUE_TABLE);
	}

	private int count(String table) {
		return countCondition(table, null);
	}
	
	public int countPositionQueueRemaining() {
		return countCondition(GeoRss.POSITION_QUEUE_TABLE, POSITION_QUEUE_SENT+" IS NULL");
	}

	private int countCondition(String table, String selection) {
		Cursor c = db.query(table, new String[] {"count(*)"}, selection, null, null, null, null);
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		return count;
	}
	

	public void touch(int feed_id) {
		ContentValues cv = new ContentValues(2);
		cv.put(FEEDS_UPDATED_AT, Util.DateTimeIso8601Now());
		db.update(FEEDS_TABLE, cv, FEEDS_ID+" = ?", new String[] {""+feed_id});
		
	}
	
	public void log(String desc) {
		Log.d(appTag, desc);
		ContentValues cv = new ContentValues(2);
		cv.put(ACTIVITY_DATE, Util.dateTimeIso8601NowLocalShort());
		cv.put(ACTIVITY_DESCRIPTION, desc);
		db.insert(GeoRss.ACTIVITY_TABLE, null, cv);
		trimLog(400);
	}
	
	private void trimLog(int limit) {
		int count = count(GeoRss.ACTIVITY_TABLE);
		int excess_rows = count - limit;
		if(excess_rows > 0) {
			Cursor zombies = db.query(GeoRss.ACTIVITY_TABLE, new String[] {"_id"}, null,	
					null, null, null, "_id asc", ""+excess_rows);
			if(zombies.moveToLast()) {
				int lowwater = zombies.getInt(zombies.getColumnIndex("_id"));
				db.execSQL("DELETE from "+GeoRss.ACTIVITY_TABLE+" where _id > "+lowwater);
			}
			zombies.close();
		}
	}

	public Cursor findActivityLogs() {
		return  db.query(GeoRss.ACTIVITY_TABLE,null, null, null, null, null, "_id desc");
	}

	public void clearLog() {
		db.execSQL("DELETE from "+GeoRss.ACTIVITY_TABLE);
	}

	public void readGeoRss(Cursor geoRssRow) throws ClientProtocolException, IOException {
		String urlString = urlFor(geoRssRow.getString(geoRssRow.getColumnIndex(GeoRss.FEEDS_SERVICENAME)),
			                            geoRssRow.getString(geoRssRow.getColumnIndex(GeoRss.FEEDS_EXTRA)));
		Log.i(appTag, "readGeoRss "+urlString);
		int service_id = geoRssRow.getInt(geoRssRow.getColumnIndex("_id"));
		processRssFeed(urlString, service_id);	
	}
	
	public void processRssFeed(String urlString, int service_id)
			throws MalformedURLException, IOException {
		URL url = new URL(urlString);
		URLConnection urlConn = url.openConnection();
		urlConn.setReadTimeout(15000);

		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			Document doc = db.parse(urlConn.getInputStream());
			NodeList items = doc.getElementsByTagName("item");
			if (items.getLength() == 0) {
				// try for ATOM
				items = doc.getElementsByTagName("entry");
			}

			Log.i(appTag, "" + items.getLength() + " items in "+urlString);
			for (int i = 0; i < items.getLength(); i++) {
				String guid = null, title = null, date = null;
				float latitude = -100, longitude = -200;
				Date pubDate = null, dtstart = null;
				int pubDateTZ = 0;
				NodeList item_elements = items.item(i).getChildNodes();
				for (int j = 0; j < item_elements.getLength(); j++) {
					Node sub_item = item_elements.item(j);
					if (sub_item.getNodeName().equals("guid")) {
						guid = sub_item.getFirstChild().getNodeValue();
					}
					if (sub_item.getNodeName().equals("title")) {
						title = sub_item.getFirstChild().getNodeValue();
					}
					if (sub_item.getNodeName().equals("pubDate")) {
						String pubDateStr = sub_item.getFirstChild()
								.getNodeValue();
						pubDate = Util.DateRfc822(pubDateStr);
						// SimpleDateFormat adjusts the date into GMT instead of
						// returning the TZ
						try {
							// try to extract the timezone from pubdate directly
							// (for upcoming.org dtstart adjustment)
							pubDateTZ = Integer.parseInt(pubDateStr.substring(
									pubDateStr.length() - 5, pubDateStr
											.length() - 2));
						} catch (NumberFormatException e) {
							// pubDate does not end in +/-HHMM
						}
						date = Util.DateTimeIso8601(pubDate.getTime());
					}
					if (sub_item.getNodeName().equals("xCal:dtstart")) {
						dtstart = Util.DateRfc822(sub_item.getFirstChild()
								.getNodeValue());
					}
					if (sub_item.getNodeName().equals("geo:lat")) {
						latitude = Float.parseFloat(sub_item.getFirstChild()
								.getNodeValue());
					}
					if (sub_item.getNodeName().equals("geo:long")) {
						longitude = Float.parseFloat(sub_item.getFirstChild()
								.getNodeValue());
					}
					if (sub_item.getNodeName().equals("georss:point")) {
						String latLong = sub_item.getFirstChild()
								.getNodeValue();
						int spacePos = latLong.indexOf(' ');
						String lat = latLong.substring(0, spacePos);
						String lng = latLong.substring(spacePos);
						latitude = Float.parseFloat(lat);
						longitude = Float.parseFloat(lng);
					}
					// ATOM hack
					if (sub_item.getNodeName().equals("published")) {
						date = sub_item.getFirstChild().getNodeValue();
					}
					// ATOM hack
					if (sub_item.getNodeName().equals("id")) {
						guid = sub_item.getFirstChild().getNodeValue();
					}
				}

				Log.i(appTag, "item #" + i + " guid:" + guid + " lat:"
						+ latitude + " long:" + longitude + " date:" + pubDate);
				if (dtstart != null) {
					// xcal dtstart has no timezone. use the timezone from the
					// entry's pubdate
					Log.i(appTag, "tzfix "
							+ Util.DateTimeIso8601(dtstart.getTime()) + " - "
							+ (dtstart.getTimezoneOffset() * 60000)
							+ " + pubDateTZ:" + pubDateTZ);
					date = Util.DateTimeIso8601(dtstart.getTime()
							- (dtstart.getTimezoneOffset() * 60000)
							+ (pubDateTZ * 60 * -60000));
					Log.i(appTag, "tzfix date " + date);
				}

				ContentValues cv = new ContentValues(2);
				cv.put("guid", guid);
				cv.put("lat", latitude);
				cv.put("long", longitude);
				cv.put(GeoRss.SHOUTS_DATE, date);
				cv.put(GeoRss.SHOUTS_TITLE, title);
				cv.put(GeoRss.SHOUTS_FEED_ID, service_id);
				insertShout(cv);
			}
			touch(service_id);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	public Cursor oldestUnpushedLocationQueue() {
		Cursor c = db.query(GeoRss.POSITION_QUEUE_TABLE, null, POSITION_QUEUE_SENT+" IS NULL",
                null, null, null, "_id desc", "1");
		c.moveToFirst();
		return c;
	}

	public void mark_as_pushed(int id) {
		ContentValues cv = new ContentValues(2);
		cv.put(POSITION_QUEUE_SENT, Util.DateTimeIso8601Now());
		db.update(POSITION_QUEUE_TABLE, cv, "_id = ?", new String[] {""+id});
	}
}
