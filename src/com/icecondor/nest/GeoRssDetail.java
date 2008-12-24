package com.icecondor.nest;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class GeoRssDetail extends Activity {
	private final String appTag = "GeoRssDetail";
	private int row_id;
	
    @Override
    protected void onCreate(Bundle icicle){
        super.onCreate(icicle);
        setContentView(R.layout.georssdetail);
        row_id = getIntent().getExtras().getInt(GeoRssSqlite.ID);
        Log.i(appTag, "GeoRssDetail created with row id "+row_id);
        
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
		db.execSQL("DELETE from "+GeoRssSqlite.SERVICES_TABLE+" where "+GeoRssSqlite.ID+ " = "+row_id);
		db.close();
		Log.i(appTag, "removed entry. jumping.");
		finish();
		startActivity(new Intent(this, GeoRssList.class));
	}

}
