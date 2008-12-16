package com.icecondor.nest;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemSelectedListener;

public class GeoRssList extends ListActivity implements OnItemSelectedListener {
	static final String appTag = "GeoRssList";
	Intent settingsIntent, radarIntent;
	EditText url_field;
	SQLiteDatabase geoRssDb;
	GeoRssList me;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getListView().setOnItemSelectedListener(this);
		me=this;

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
		menu.add(Menu.NONE, 1, Menu.NONE, R.string.menu_geo_rss_add).setIcon(android.R.drawable.ic_menu_add);
		menu.add(Menu.NONE, 2, Menu.NONE, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, 3, Menu.NONE, R.string.menu_radar).setIcon(android.R.drawable.ic_menu_compass);
		return result;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(appTag, "menu:"+item.getItemId());
		
		switch (item.getItemId()) {
		case 1:
			showDialog(1);
			break;
		case 2:
			startActivity(settingsIntent);
			break;
		case 3:
			startActivity(radarIntent);
			break;
		}
		
		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		LayoutInflater factory = LayoutInflater.from(this);
        View settings_view = factory.inflate(R.layout.georsslist, null);

		return new AlertDialog.Builder(this)
			.setView(settings_view)
			.setTitle(R.string.menu_geo_rss_add)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichbutton) {
					insert_service(url_field.getText().toString());
					me.onContentChanged();
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichbutton) {

				}
			})
			.create();
	}

	protected void insert_service(String url) {
		// GeoRSS Database
		GeoRssSqlite rssdb = new GeoRssSqlite(this, "georss", null, 1);
		geoRssDb = rssdb.getWritableDatabase();
		ContentValues cv = new ContentValues(2);
		cv.put("name", "Service");
		cv.put("url", url);
		geoRssDb.insert(GeoRssSqlite.SERVICES_TABLE, null, cv);
		geoRssDb.close();
		rssdb.close();

	}
	protected void onPrepareDialog(int id, Dialog dialog) {
		url_field = (EditText) dialog.findViewById(R.id.url_edit);
        url_field.setText(""); // initial value
	}
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		Log.i(appTag, "position:"+position);
		
	}
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
}
