package com.icecondor.nest.rss;

import com.icecondor.nest.R;
import com.icecondor.nest.R.id;
import com.icecondor.nest.R.layout;
import com.icecondor.nest.R.string;
import com.icecondor.nest.db.GeoRss;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class GeoRssDetail extends ListActivity {
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
		feed.close();
		
		Cursor shouts = rssdb.findShouts(row_id);
        ListAdapter adapter = new SimpleCursorAdapter(
                this, // Context
                android.R.layout.two_line_list_item,  // Specify the row template to use (here, two columns bound to the two retrieved cursor rows)
                shouts,  // Pass in the cursor to bind to.
                new String[] {GeoRss.SHOUTS_TITLE, GeoRss.SHOUTS_DATE}, // Array of cursor columns to bind to.
                new int[] {android.R.id.text1, android.R.id.text2});      // Parallel array of which template objects to bind to those columns.

        // Bind to our new adapter.
        setListAdapter(adapter);
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
