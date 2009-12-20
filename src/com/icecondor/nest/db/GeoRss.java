package com.icecondor.nest.db;

import java.util.Calendar;

import com.icecondor.nest.Util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.widget.Toast;


public class GeoRss {
	public static final String DATABASE_NAME = "georss";
	public static final int DATABASE_VERSION = 5;

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

	public void insertShout(ContentValues cv) {
		db.insert(GeoRss.SHOUTS_TABLE, null, cv);
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

	public Cursor findFeeds() {
		return  db.query(GeoRss.FEEDS_TABLE,null, null, null, null, null, null);
	}

	public Cursor findFeed(int row_id) {
		return db.query(GeoRss.FEEDS_TABLE,null, "_id = ?",
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

	private int count(String table) {
		Cursor c = db.query(table, new String[] {"count(*)"}, null, null, null, null, null);
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
		ContentValues cv = new ContentValues(2);
		cv.put(ACTIVITY_DATE, Util.DateTimeIso8601NowShort());
		cv.put(ACTIVITY_DESCRIPTION, desc);
		db.insert(GeoRss.ACTIVITY_TABLE, null, cv);
		trimLog(200);
	}
	
	private void trimLog(int limit) {
		int count = count(GeoRss.ACTIVITY_TABLE);
		if(count > limit) {
			Cursor zombies = db.query(GeoRss.ACTIVITY_TABLE, new String[] {"_id"}, null,	
					null, null, null, "_id asc", ""+(count - limit));
			zombies.moveToLast();
			int lowwater = zombies.getInt(zombies.getColumnIndex("_id"));
			zombies.close();
			db.execSQL("DELETE from "+GeoRss.ACTIVITY_TABLE+" where _id >= "+lowwater);
		}
	}

	public Cursor findActivityLogs() {
		return  db.query(GeoRss.ACTIVITY_TABLE,null, null, null, null, null, "_id desc");
	}

	public void clearLog() {
		db.execSQL("DELETE from "+GeoRss.ACTIVITY_TABLE);
	}

}
