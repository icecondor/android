package com.icecondor.eaglet.db;

import android.content.ContentValues;

public class HeartBeat extends DbActivity  {
    private static final String VERB = "heartbeat";

    @Override
    public ContentValues getAttributes() {
        ContentValues cv = super.getAttributes();
        cv.put(Database.ACTIVITIES_JSON, json.toString());
        cv.put(Database.ACTIVITIES_VERB, VERB);
        return cv;
    }
}
