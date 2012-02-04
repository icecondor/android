package com.icecondor.nest;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.icecondor.nest.db.GeoRss;

public class ActivityLog extends ListActivity implements Constants, 
                                                         SimpleCursorAdapter.ViewBinder {
	GeoRss rssdb;
	LogObserver logob;
	Cursor logs;

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitylog);

        rssdb = new GeoRss(this);
		rssdb.open();		

		logs = rssdb.findActivityLogs();
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                R.layout.activitylog_row, logs,
                new String[] {"date", "description"},
                new int[] {R.id.date, R.id.description});
        logob = new LogObserver();
        adapter.setViewBinder(this);
        setListAdapter(adapter);
	}
	
    @Override
    public void onPause() {
    	super.onPause();
    	logs.close();
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
}


