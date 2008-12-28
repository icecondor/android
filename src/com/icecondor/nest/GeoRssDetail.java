package com.icecondor.nest;

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
	private final String appTag = "GeoRssDetail";
	GeoRssSqlite rssdb;
	SQLiteDatabase geoRssDb;
	private int row_id;
	
    @Override
    protected void onCreate(Bundle icicle){
        super.onCreate(icicle);
        setContentView(R.layout.georssdetail);
        
		rssdb = new GeoRssSqlite(this, "georss", null, 1);
        row_id = getIntent().getExtras().getInt(GeoRssSqlite.ID);
        Log.i(appTag, "GeoRssDetail created with row id "+row_id);
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	// fill in fields on the screen
		geoRssDb = rssdb.getReadableDatabase();
		Cursor service = geoRssDb.query(GeoRssSqlite.SERVICES_TABLE,null, "_id = ?",
				                     new String[] {""+row_id}, null, null, null);
		service.moveToFirst();
		String name = service.getString(service.getColumnIndex(GeoRssSqlite.NAME));
		String url = service.getString(service.getColumnIndex(GeoRssSqlite.URL));
		TextView msgTextView = (TextView)findViewById(R.id.georssdetail_header);
		msgTextView.setText(name);
		TextView urlTextView = (TextView)findViewById(R.id.georssdetail_url);
		urlTextView.setText(url);
		
		Cursor last_update = geoRssDb.query(GeoRssSqlite.SHOUTS_TABLE,null, "service_id = ?",
                new String[] {""+row_id}, null, null, "date desc", "1");
		String date;
		if (last_update.getCount() > 0) {
			last_update.moveToFirst();
			date = last_update.getString(last_update.getColumnIndex("date"));
		} else {
			date = "no updates";
		}
		TextView dateTextView = (TextView)findViewById(R.id.georssdetail_lastupdate);
		dateTextView.setText(date);
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
		GeoRssSqlite rssdb = new GeoRssSqlite(this, "georss", null, 1);
		SQLiteDatabase db = rssdb.getWritableDatabase();
		// the parameter substitution form of execSQL wasnt working. yuk.
		db.execSQL("DELETE from "+GeoRssSqlite.SERVICES_TABLE+" where "+GeoRssSqlite.ID+ " = "+row_id);
		db.close();
		finish(); // don't come back here
		startActivity(new Intent(this, GeoRssList.class));
	}

}
