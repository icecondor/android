package com.icecondor.nest;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

import com.icecondor.nest.db.GeoRss;

public class ActivityLog extends ListActivity {
	GeoRss rssdb;
	LogObserver logob;

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitylog);

        rssdb = new GeoRss(this);
		rssdb.open();		

		Cursor logs = rssdb.findActivityLogs();
        ListAdapter adapter=new SimpleCursorAdapter(this,
                R.layout.activitylog_row, logs,
                new String[] {"date", "description"},
                new int[] {R.id.date, R.id.description});
        logob = new LogObserver();
        setListAdapter(adapter);
	}
	
    @Override
    public void onPause() {
    	super.onPause();
    	rssdb.close();
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
		}
		public void onInvalidated() {
			
		}
	}
}


