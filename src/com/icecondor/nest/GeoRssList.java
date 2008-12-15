package com.icecondor.nest;

import android.app.ListActivity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

public class GeoRssList extends ListActivity {
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

	}
}
