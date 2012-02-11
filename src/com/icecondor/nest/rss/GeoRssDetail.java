package com.icecondor.nest.rss;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.icecondor.nest.Pigeon;
import com.icecondor.nest.PigeonService;
import com.icecondor.nest.R;
import com.icecondor.nest.Util;
import com.icecondor.nest.db.GeoRss;

public class GeoRssDetail extends ListActivity implements ServiceConnection {
	private static final String appTag = "GeoRssDetail";
	public static final String RssDbIdKey = "georssid";
	GeoRss rssdb;
	private int row_id;
	PigeonService pigeon;
	String service_name, service_extra;
	Cursor shouts;
	
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
        Intent pigeon_service = new Intent(this, Pigeon.class);
        bindService(pigeon_service, this, 0); // 0 = do not auto-start

    	// fill in fields on the screen
		Cursor feed = rssdb.findFeed(row_id);
		feed.moveToFirst();
		service_name = feed.getString(feed.getColumnIndex(GeoRss.FEEDS_SERVICENAME));
		service_extra = feed.getString(feed.getColumnIndex(GeoRss.FEEDS_EXTRA));
		TextView msgTextView = (TextView)findViewById(R.id.georssdetail_header);
		msgTextView.setText(service_name);
		TextView urlTextView = (TextView)findViewById(R.id.georssdetail_url);
		urlTextView.setText(service_extra);
		feed.close();
		
		shouts = rssdb.findShouts(row_id);
        ListAdapter adapter = new SimpleCursorAdapter(
                this, // Context
                android.R.layout.two_line_list_item,  // Specify the row template to use (here, two columns bound to the two retrieved cursor rows)
                shouts,  // Pass in the cursor to bind to.
                new String[] {GeoRss.SHOUTS_TITLE, GeoRss.SHOUTS_DATE}, // Array of cursor columns to bind to.
                new int[] {android.R.id.text1, android.R.id.text2});      // Parallel array of which template objects to bind to those columns.

        // Bind to our new adapter.
        setListAdapter(adapter);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        shouts.close();
        unbindService(this);
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
		try {
            pigeon.unFriend(service_extra);
            Util.profilePictureDelete(service_extra, this);
            Toast.makeText(GeoRssDetail.this, "Unfriending "+service_extra, Toast.LENGTH_SHORT).show();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
		finish(); // don't come back here
		startActivity(new Intent(this, GeoRssList.class));
	}
	
	@Override
	public void onDestroy() {
		rssdb.close();
		super.onDestroy();
	}

	@Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        pigeon = PigeonService.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }    
}
