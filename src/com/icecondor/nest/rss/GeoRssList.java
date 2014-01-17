package com.icecondor.nest.rss;

import java.util.Date;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.icecondor.nest.Constants;
import com.icecondor.nest.Pigeon;
import com.icecondor.nest.PigeonService;
import com.icecondor.nest.R;
import com.icecondor.nest.Radar;
import com.icecondor.nest.Settings;
import com.icecondor.nest.Util;
import com.icecondor.nest.db.GeoRss;

public class GeoRssList extends ListActivity implements ServiceConnection,
														OnItemSelectedListener,
														Constants {
	Intent settingsIntent, radarIntent;
	EditText url_field;
	Spinner service_spinner;
	GeoRss rssdb;
	SQLiteDatabase geoRssDb;
	Cursor feeds;
	View add_url_dialog;
	PigeonService pigeon;
	BirdFixReceiver bird_fix_receiver, profile_update_receiver;
	GpsFixReceiver gps_fix_receiver;
	SimpleCursorAdapter adapter;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(APP_TAG, "GeoRssList: onCreate");
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
		Log.i(APP_TAG, "GeoRssList: onResume");
        Intent pigeon_service = new Intent(this, Pigeon.class);
        bindService(pigeon_service, this, 0); // 0 = do not auto-start
        
        bird_fix_receiver = this.new BirdFixReceiver();
        registerReceiver(bird_fix_receiver, new IntentFilter(BIRD_FIX_ACTION));
        profile_update_receiver = this.new BirdFixReceiver();
        registerReceiver(profile_update_receiver, new IntentFilter(USER_PROFILE_UPDATE_ACTION));
        gps_fix_receiver = this.new GpsFixReceiver();
        registerReceiver(gps_fix_receiver, new IntentFilter(GPS_FIX_ACTION));
    }
    
    @Override
    public void onPause() {
        feeds.close();
    	super.onPause();
    	unregisterReceiver(bird_fix_receiver);
    	unregisterReceiver(profile_update_receiver);
    	unregisterReceiver(gps_fix_receiver);
		unbindService(this);
    }

	
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(APP_TAG, "onCreateOptionsMenu");
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, 1, Menu.NONE, R.string.menu_geo_rss_add).setIcon(android.R.drawable.ic_menu_add);
		menu.add(Menu.NONE, 2, Menu.NONE, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, 3, Menu.NONE, R.string.menu_radar).setIcon(android.R.drawable.ic_menu_compass);
		return result;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(APP_TAG, "menu:"+item.getItemId());
		
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
		Log.i(APP_TAG, "onCreateDialog "+id);
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
					Log.i(APP_TAG, "adding "+title);
					insert_service(service, extra, title);
					try {
						pigeon.addFriend(extra);
						pigeon.followFriend(extra);
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
		Log.i(APP_TAG,"adding feed "+service+" "+extra);
		rssdb.addFeed(cv);
	}
	
	protected void onPrepareDialog(int id, Dialog dialog) {
        url_field.setText(""); // initial value
	}
		
	@Override
	public void onListItemClick(ListView l, View v, int position, long id){
		Log.i(APP_TAG, "Item clicked position: "+position);
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
        feeds = rssdb.findFeeds();
        adapter = new SimpleCursorAdapter(
                this, // Context
                R.layout.georssrow,
                feeds,  // Pass in the cursor to bind to.
                new String[] {GeoRss.FEEDS_EXTRA, GeoRss.FEEDS_EXTRA, GeoRss.FEEDS_UPDATED_AT}, // Array of cursor columns to bind to.
                new int[] {R.id.row_gravatar, R.id.row_username, R.id.row_date});      // Parallel array of which template objects to bind to those columns.
        adapter.setViewBinder(new MyBinder());
        setListAdapter(adapter);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
	}
	
	@Override
	public void onDestroy() {
		rssdb.close();
		super.onDestroy();
	}

	class MyBinder implements SimpleCursorAdapter.ViewBinder {
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			String username = cursor.getString(cursor.getColumnIndex(GeoRss.FEEDS_EXTRA));
			if(view.getClass().getName().equals("android.widget.ImageView") &&
					columnIndex == cursor.getColumnIndex(GeoRss.FEEDS_EXTRA)) {
				if(Util.profilePictureExists(username, GeoRssList.this)) {
					((ImageView)view).setImageDrawable(
							Util.drawableGravatarFromUsername(username, GeoRssList.this));
				}
			}
			if(view.getClass().getName().equals("android.widget.TextView") &&
					columnIndex == cursor.getColumnIndex(GeoRss.FEEDS_EXTRA)) {
				((TextView)view).setText(username);
			}
			if(columnIndex == cursor.getColumnIndex(GeoRss.FEEDS_UPDATED_AT) ) {
				int feed_id = cursor.getInt(cursor.getColumnIndex(GeoRss.FEEDS_ID));
				Cursor c = rssdb.findLastShout(feed_id);
		        if(c.getCount() > 0) {
		        	c.moveToFirst();
		        	String date = c.getString(c.getColumnIndex(GeoRss.SHOUTS_DATE));
		        	Date mark = Util.DateRfc822(date);
		        	String msg = Util.DateToShortDisplay(mark);
		        	
		        	float lat = c.getFloat(c.getColumnIndex(GeoRss.SHOUTS_LAT));
		        	float lng = c.getFloat(c.getColumnIndex(GeoRss.SHOUTS_LNG));
		        	Location l = new Location("db");
		        	l.setLatitude(lat);
		        	l.setLongitude(lng);
					try {
						Location last = pigeon.getLastFix(false);
						Log.i(APP_TAG, "georsslist: last fix lat "+last.getLatitude()+" lng "+last.getLongitude());
			        	if(last != null) {
			        		float distance = l.distanceTo(last);
			        		msg += " "+Util.short_distance(distance, false);
			        	}
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					((TextView)view).setText(msg);

		        }
				c.close();
			}
			return true;
		}
		
	}

	public class BirdFixReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String username = (String)intent.getExtras().get("username");
			if(username != null) {
				RefreshRow rrow = new RefreshRow();
				rrow.setUsername(username);
				runOnUiThread(rrow);
			}
		}		
	}

	public class GpsFixReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(APP_TAG, "GPS_FIX received");
			RefreshRow rrow = new RefreshRow();
			runOnUiThread(rrow);
		}		
	}

	public class RefreshRow implements Runnable {
	    String username;
	    
	    public void setUsername(String username) {
	    	this.username = username;
	    }
	    
		@Override
		public void run() {
			Log.i(APP_TAG, "GeoRssList: refreshing "+username);
			feeds = rssdb.findFeeds();
			adapter.changeCursor(feeds);
		}
	}
}
