package com.icecondor.eaglet.ui;

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
import com.icecondor.eaglet.db.Activity;
import com.icecondor.eaglet.db.Database;

public class ActivityListFragment extends Fragment {

    private Database db;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(Constants.APP_TAG, "ActivityListFragment onActivityCreated");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(Constants.APP_TAG, "ActivityListFragment onActivityCreated");
        View rootView = inflater.inflate(R.layout.activity_list_fragment_main,
                               container, false);
        ListView listView = (ListView)rootView.findViewById(R.id.activity_list_view);
        String[] fromColumns =  {Database.ROW_CREATED_AT,
                                  Database.ACTIVITIES_UUID};
        int[] toViews = {R.id.activity_row_date,
                          R.id.activity_row_description};
        db = new Database(getActivity());
        SimpleCursorAdapter sCursorAdapter = new SimpleCursorAdapter(getActivity(),
                                                      R.layout.activity_row,
                                                      Activity.getAll(db),
                                                      fromColumns,
                                                      toViews);
        sCursorAdapter.setViewBinder(new ActivityListViewBinder());
        listView.setAdapter(sCursorAdapter);
        return rootView;
    }

    private class ActivityListViewBinder implements SimpleCursorAdapter.ViewBinder {

        @Override
        public boolean setViewValue(View view, Cursor cursor, int dbColumnIndex) {
            int uuidIndex = cursor.getColumnIndex(Database.ACTIVITIES_UUID);
            if(dbColumnIndex == uuidIndex) {
                ((TextView)view).setText(cursor.getString(uuidIndex));
                return true;
            }

            int createdAtIndex = cursor.getColumnIndex(Database.ROW_CREATED_AT);
            if(dbColumnIndex == createdAtIndex) {
                ((TextView)view).setText(cursor.getString(createdAtIndex)+"Z");
                return true;
            }

            return false;
        }

    }
}
