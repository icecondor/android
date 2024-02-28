package com.icecondor.nest.ui.alist;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.icecondor.nest.Constants;
import com.icecondor.nest.R;
import com.icecondor.nest.db.Activity;
import com.icecondor.nest.db.Database;

public class ActivityListFragment extends Fragment {

    private Database db;
    private ActivityListViewBinder activityListViewBinder;
    private ListView listView;
    private SimpleCursorAdapter sCursorAdapter;
    private int displayRowCount = 100;

    public ActivityListFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(Constants.APP_TAG, "ActivityListFragment onActivityCreated");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(Constants.APP_TAG, "ActivityListFragment onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_activity_list,
                               container, false);
        listView = (ListView)rootView.findViewById(R.id.activity_list_view);
        String[] fromColumns =  {
                                 Database.ROW_CREATED_AT,
                                 Database.ACTIVITIES_VERB,
                                 Database.ACTIVITIES_DESCRIPTION,
                                 Database.ACTIVITIES_UUID,
                                 Database.ACTIVITIES_SYNCED_AT
        };
        int[] toViews = {
                         R.id.activity_row_date,
                         R.id.activity_row_action,
                         R.id.activity_row_description,
                         R.id.activity_row_header};
        db = new Database(getActivity());
        sCursorAdapter = new SimpleCursorAdapter(getActivity(),
                                                      R.layout.activity_row,
                                                      Activity.getAll(db, displayRowCount),
                                                      fromColumns,
                                                      toViews);
        activityListViewBinder = new ActivityListViewBinder();
        sCursorAdapter.setViewBinder(activityListViewBinder);
        listView.setAdapter(sCursorAdapter);
        return rootView;
    }

    public void invalidateView() {
        /* its possible for MainActivity to call this before db is ready */
        if(db != null) {
            Cursor rows = Activity.getAll(db, displayRowCount);
            sCursorAdapter.changeCursor(rows);
        } else {
            Log.d(Constants.APP_TAG, "ActivityListFragment invalidateView skipped, no database");
        }
    }

    private class ActivityListViewBinder implements SimpleCursorAdapter.ViewBinder {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int dbColumnIndex) {
            int createdAtIndex = cursor.getColumnIndex(Database.ROW_CREATED_AT);
            if(dbColumnIndex == createdAtIndex) {
                String dateStr = cursor.getString(createdAtIndex);
                DateTime time = ISODateTimeFormat.dateTime().parseDateTime(dateStr);
                String format = "MMM d h:mm:ssa";
                if(time.isAfter(DateTime.now().minusDays(1))) {
                    format = "h:mm:ssa";
                }
                String displayTime = time.toString(format).toLowerCase();
                ((TextView)view).setText(displayTime);
                return true;
            }

            int categoryIndex = cursor.getColumnIndex(Database.ACTIVITIES_VERB);
            if(dbColumnIndex == categoryIndex) {
                ((TextView)view).setText(cursor.getString(categoryIndex));
                return true;
            }

            int uuidIndex = cursor.getColumnIndex(Database.ACTIVITIES_UUID);
            if(dbColumnIndex == uuidIndex) {
                String shortUuid = "#"+cursor.getString(uuidIndex).substring(32);
                //TextView uuidView = ((TextView)view);
                //uuidView.setText(shortUuid);
                String syncedAt = cursor.getString(cursor.getColumnIndex(Database.ACTIVITIES_SYNCED_AT));
                if(syncedAt == null) {
                    view.setBackgroundColor(Color.rgb(250,250,250));
                } else {
                    view.setBackgroundColor(Color.rgb(200,200,200));
                }
                return true;
            }

            int descriptionIndex = cursor.getColumnIndex(Database.ACTIVITIES_DESCRIPTION);
            if(dbColumnIndex == descriptionIndex) {
                String desc = "";
                desc = cursor.getString(descriptionIndex);
                ((TextView)view).setText(desc);
                return true;
            }

            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
       ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("Activities");
        invalidateView();
    }
}
