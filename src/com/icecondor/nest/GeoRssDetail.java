package com.icecondor.nest;

import com.icecondor.nest.db.GeoRss;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class GeoRssDetail extends Activity {
	private static final String appTag = "GeoRssDetail";
	public static final String RssDbIdKey = "georssid";
	GeoRss rssdb;
	private int row_id;
	
    @Override
    protected void onCreate(Bundle icicle){
        super.onCreate(icicle);
        setContentView(R.layout.georssdetail);

        // Database
		rssdb = new GeoRss(this);
		rssdb.open();
		
        row_id = getIntent().getExtras().getInt(RssDbIdKey);
        Log.i(appTag, "GeoRssDetail onCreate with row id "+row_id);
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	// fill in fields on the screen
		Cursor feed = rssdb.findFeed(row_id);
		feed.moveToFirst();
		String name = feed.getString(feed.getColumnIndex(GeoRss.FEEDS_SERVICENAME));
		String url = feed.getString(feed.getColumnIndex(GeoRss.FEEDS_EXTRA));
		TextView msgTextView = (TextView)findViewById(R.id.georssdetail_header);
		msgTextView.setText(name);
		TextView urlTextView = (TextView)findViewById(R.id.georssdetail_url);
		urlTextView.setText(url);
		
		Cursor last_update = rssdb.findLastShout(row_id);
		String date;
		String title = "";
		if (last_update.getCount() > 0) {
			last_update.moveToFirst();
			date = last_update.getString(last_update.getColumnIndex("date"));
			title = last_update.getString(last_update.getColumnIndex("title"));
		} else {
			date = "no updates";
		}
		TextView dateTextView = (TextView)findViewById(R.id.georssdetail_lastupdate);
		dateTextView.setText(date);
		TextView titleTextView = (TextView)findViewById(R.id.georssdetail_title);
		titleTextView.setText(title);
		last_update.close();
		feed.close();
    }
    
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, 1, Menu.NONE, R.string.menu_geo_rss_detail_delete).setIcon(android.R.drawable.ic_delete);
		return result;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(appTag, "menu:"+item.getItemId());
		
		switch (item.getItemId()) {
		case 1:
			removeServiceFromDatabase();
			break;
		}
		return false;
	}

	private void removeServiceFromDatabase() {
		rssdb.deleteFeed(row_id);
		finish(); // don't come back here
		startActivity(new Intent(this, GeoRssList.class));
	}
	
	@Override
	public void onDestroy() {
		rssdb.close();
		super.onDestroy();
	}

}
