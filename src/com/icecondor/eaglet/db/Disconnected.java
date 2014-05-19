package com.icecondor.eaglet.db;

import android.content.ContentValues;

public class Disconnected  extends DbActivity  {
    private static final String VERB = "disconnected";

    public Disconnected() {
    }

    @Override
    public ContentValues getAttributes() {
        ContentValues cv = super.getAttributes();
        cv.put(Database.ACTIVITIES_VERB, VERB);
        cv.put(Database.ACTIVITIES_DESCRIPTION, "");
        cv.put(Database.ACTIVITIES_JSON, json.toString());
        return cv;
    }

}
