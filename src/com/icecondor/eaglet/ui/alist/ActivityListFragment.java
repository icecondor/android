package com.icecondor.eaglet.ui.alist;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.R;
import com.icecondor.eaglet.db.Database;
import com.icecondor.eaglet.db.DbActivity;

public class ActivityListFragment extends Fragment {

    private Database db;
    private ActivityListViewBinder activityListViewBinder;
    private ListView listView;
    private SimpleCursorAdapter sCursorAdapter;

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
        String[] fromColumns =  {Database.ROW_CREATED_AT,
                                 Database.ACTIVITIES_VERB,
                                 Database.ACTIVITIES_DESCRIPTION,
                                 Database.ACTIVITIES_UUID};
        int[] toViews = {R.id.activity_row_date,
                         R.id.activity_row_action,
                         R.id.activity_row_description,
                         R.id.activity_row_uuid};
        db = new Database(getActivity());
        sCursorAdapter = new SimpleCursorAdapter(getActivity(),
                                                      R.layout.activity_row,
                                                      DbActivity.getAll(db),
                                                      fromColumns,
                                                      toViews);
        activityListViewBinder = new ActivityListViewBinder();
        sCursorAdapter.setViewBinder(activityListViewBinder);
        listView.setAdapter(sCursorAdapter);
        return rootView;
    }

    public void invalidateView() {
        Cursor rows = DbActivity.getAll(db);
        sCursorAdapter.changeCursor(rows);
    }

    private class ActivityListViewBinder implements SimpleCursorAdapter.ViewBinder {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int dbColumnIndex) {
            int createdAtIndex = cursor.getColumnIndex(Database.ROW_CREATED_AT);
            if(dbColumnIndex == createdAtIndex) {
                String dateStr = cursor.getString(createdAtIndex);
                DateTime time = ISODateTimeFormat.dateTime().parseDateTime(dateStr);
                String displayTime = time.toString("MMM d h:mma");
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
                ((TextView)view).setText(cursor.getString(uuidIndex).substring(32));
                return true;
            }

            int descriptionIndex = cursor.getColumnIndex(Database.ACTIVITIES_DESCRIPTION);
            if(dbColumnIndex == descriptionIndex) {
                ((TextView)view).setText(cursor.getString(descriptionIndex));
                return true;
            }

            return false;
        }
    }
}
