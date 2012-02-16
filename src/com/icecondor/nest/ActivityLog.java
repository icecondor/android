package com.icecondor.nest;

import java.util.Timer;
import java.util.TimerTask;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.icecondor.nest.db.GeoRss;

public class ActivityLog extends ListActivity implements Constants, 
                                                         SimpleCursorAdapter.ViewBinder,
                                                         ServiceConnection {
	GeoRss rssdb;
	Cursor logs;
	Intent pigeon_intent;
	private boolean pigeon_bound;
	TimerTask refresh_task;
	Timer refresh_timer;
	SimpleCursorAdapter adapter;

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitylog);

        rssdb = new GeoRss(this);
		rssdb.open();		

		logs = rssdb.findActivityLogs();
        adapter = new SimpleCursorAdapter(this,
                R.layout.activitylog_row, logs,
                new String[] {"date", "description"},
                new int[] {R.id.date, R.id.description});
        adapter.setViewBinder(this);
        setListAdapter(adapter);
        pigeon_intent = new Intent(this, Pigeon.class);
	}
	
    @Override
    public void onResume() {
    	super.onResume();
    	Log.i(APP_TAG, "activity_log: onResume");
	    pigeon_bound = bindService(pigeon_intent, this, 0); // 0 = do not auto-start
	    Log.i(APP_TAG, "activity_log: bindService(pigeon)="+pigeon_bound);
	    refresh_task = new TimerTask() {public void run() { 
	    	runOnUiThread(new Runnable() {public void run() {
	    		logs.close();
	    		logs = rssdb.findActivityLogs();
		    	adapter.changeCursor(logs);
	    	}});
	    }};
	    refresh_timer = new Timer("ActivityLog refresh");
	    refresh_timer.scheduleAtFixedRate(refresh_task, 1500, 500);
	    
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	refresh_timer.cancel();
    	logs.close();
    	rssdb.close();
    	if(pigeon_bound) {
    		unbindService(this);
    	}
    }
    
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, 1, Menu.NONE, "Clear").setIcon(android.R.drawable.ic_delete);
		return result;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 1:
			rssdb.clearLog();
			break;
		}
		return false;
	}

	class LogObserver extends DataSetObserver {
		public void onChanged() {
		    Log.i("LogObserver", "changed!");
		}
		public void onInvalidated() {
            Log.i("LogObserver", "invalidated!");			
		}
	}

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        TextView tv = (TextView) view;
        String str = cursor.getString(columnIndex);
        if(columnIndex == cursor.getColumnIndex(GeoRss.ACTIVITY_DATE)) {
            str = str.substring(11)+"\n"+str.substring(0, 10);
        }
        tv.setText(str);
        return true;
    }

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
	}
}


