package com.icecondor.eaglet.db;

import android.content.ContentValues;

public class AlarmTrigger extends Activity  {

    @Override
    public ContentValues getAttributes() {
        ContentValues cv = super.getAttributes();
        cv.put(Database.ACTIVITIES_JSON, json.toString());
        return cv;
    }
}
