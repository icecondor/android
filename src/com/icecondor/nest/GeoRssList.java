package com.icecondor.nest;

import android.app.ListActivity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

public class GeoRssList extends ListActivity {
	static final String appTag = "GeoRssList";
	Intent settingsIntent, radarIntent;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
		GeoRssSqlite rssdb = new GeoRssSqlite(this, "georss", null, 1);
		SQLiteDatabase db = rssdb.getWritableDatabase();
		//db.execSQL("insert into urls values (null, 'service', 'https://service.com'");
        ListAdapter adapter = new SimpleCursorAdapter(
                this, // Context
                android.R.layout.two_line_list_item,  // Specify the row template to use (here, two columns bound to the two retrieved cursor rows)
                db.query(GeoRssSqlite.SERVICES_TABLE,null, null, null, null, null, null),  // Pass in the cursor to bind to.
                new String[] {GeoRssSqlite.NAME, GeoRssSqlite.URL}, // Array of cursor columns to bind to.
                new int[] {android.R.id.text1, android.R.id.text2});      // Parallel array of which template objects to bind to those columns.

        // Bind to our new adapter.
        setListAdapter(adapter);
        
        // Jump Points
        settingsIntent = new Intent(this, Settings.class);
        radarIntent = new Intent(this, Radar.class);


	}
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(appTag, "onCreateOptionsMenu");
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, 2, Menu.NONE, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, 3, Menu.NONE, R.string.menu_radar).setIcon(android.R.drawable.ic_menu_compass);
		return result;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(appTag, "menu:"+item.getItemId());
		
		switch (item.getItemId()) {
		case 2:
			startActivity(settingsIntent);
			break;
		case 3:
			startActivity(radarIntent);
			break;
		}
		
		return false;
	}


}
