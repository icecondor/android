package com.icecondor.nest.rss;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import com.icecondor.nest.Pigeon;
import com.icecondor.nest.PigeonService;
import com.icecondor.nest.R;
import com.icecondor.nest.Radar;
import com.icecondor.nest.Settings;
import com.icecondor.nest.PigeonService.Stub;
import com.icecondor.nest.R.array;
import com.icecondor.nest.R.id;
import com.icecondor.nest.R.layout;
import com.icecondor.nest.R.string;
import com.icecondor.nest.db.GeoRss;

public class GeoRssList extends ListActivity implements ServiceConnection,
														OnItemSelectedListener {
	static final String appTag = "GeoRssList";
	Intent settingsIntent, radarIntent;
	EditText url_field;
	Spinner service_spinner;
	GeoRss rssdb;
	SQLiteDatabase geoRssDb;
	Cursor feeds;
	View add_url_dialog;
	PigeonService pigeon;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(appTag, "onCreate");
		setContentView(R.layout.georsslist);
		setTitle(R.string.geo_rss_activity_title);
		
        // Jump Points
        settingsIntent = new Intent(this, Settings.class);
        radarIntent = new Intent(this, Radar.class);
        
        // Database
		rssdb = new GeoRss(this);
		rssdb.open();
	}
	
    @Override
    public void onResume() {
    	super.onResume();
        Intent pigeon_service = new Intent(this, Pigeon.class);
        bindService(pigeon_service, this, 0); // 0 = do not auto-start
        
		feeds = rssdb.findFeeds();
        ListAdapter adapter = new SimpleCursorAdapter(
                this, // Context
                android.R.layout.two_line_list_item,  // Specify the row template to use (here, two columns bound to the two retrieved cursor rows)
                feeds,  // Pass in the cursor to bind to.
                new String[] {GeoRss.FEEDS_SERVICENAME, GeoRss.FEEDS_EXTRA}, // Array of cursor columns to bind to.
                new int[] {android.R.id.text1, android.R.id.text2});      // Parallel array of which template objects to bind to those columns.

        // Bind to our new adapter.
        setListAdapter(adapter);
    }
    
    @Override
    public void onPause() {
        feeds.close();
    	super.onPause();
		unbindService(this);
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
		Log.i(appTag, "onCreateDialog "+id);
		LayoutInflater factory = LayoutInflater.from(this);
        add_url_dialog = factory.inflate(R.layout.georssadd, null);
		url_field = (EditText) add_url_dialog.findViewById(R.id.rss_url_edit);
        service_spinner = (Spinner) add_url_dialog.findViewById(R.id.serviceselect);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.location_service_reader_values, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        service_spinner.setAdapter(adapter);
        service_spinner.setOnItemSelectedListener(this);

		return new AlertDialog.Builder(this)
			.setView(add_url_dialog)
			.setTitle(R.string.menu_geo_rss_add)
			.setPositiveButton("Add", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichbutton) {
					String service = (String)service_spinner.getSelectedItem();
					String extra = url_field.getText().toString();
					String title = extra + " - " + service;
					Log.i(appTag, "adding "+title);
					insert_service(service, extra, title);
					try {
						pigeon.addFriend(extra);
						Toast.makeText(GeoRssList.this, "Friending "+extra, Toast.LENGTH_SHORT).show();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					
					// not of the right way to get the list to refresh
					finish();
					Intent refresh = new Intent(GeoRssList.this, GeoRssList.class);
					startActivity(refresh);
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichbutton) {

				}
			})
			.create();
	}

	protected void insert_service(String service, String extra, String title) {
		ContentValues cv = new ContentValues(2);
		cv.put(GeoRss.FEEDS_SERVICENAME, service);
		cv.put(GeoRss.FEEDS_EXTRA, extra);
		cv.put(GeoRss.FEEDS_TITLE, title);
		Log.i(appTag,"adding feed "+service+" "+extra);
		rssdb.addFeed(cv);
	}
	
	protected void onPrepareDialog(int id, Dialog dialog) {
        url_field.setText(""); // initial value
	}
		
	@Override
	public void onListItemClick(ListView l, View v, int position, long id){
		Log.i(appTag, "Item clicked position: "+position);
		Intent itemIntent = new Intent(this, GeoRssDetail.class);
		feeds.moveToPosition(position);
		itemIntent.putExtra(GeoRssDetail.RssDbIdKey, feeds.getInt(feeds.getColumnIndex(GeoRss.FEEDS_ID)));
		startActivity(itemIntent);
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		String service = (String)service_spinner.getSelectedItem();
		TextView title = (TextView) add_url_dialog.findViewById(R.id.rss_field_title);
		if(service.equals("RSS")) {
			title.setText("");
		}else if (service.equals("brightkite.com")) {
			title.setText("Username");			
		}else if (service.equals("shizzow.com")) {
			title.setText("Username");			
		}else if (service.equals("icecondor.com")) {
			title.setText("OpenID or Username");			
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		pigeon = PigeonService.Stub.asInterface(service);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
	}
	
	@Override
	public void onDestroy() {
		rssdb.close();
		super.onDestroy();
	}

}
