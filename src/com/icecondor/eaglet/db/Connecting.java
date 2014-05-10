package com.icecondor.eaglet.db;

import android.content.ContentValues;

public class Connecting  extends DbActivity  {
    private static final String VERB = "connecting";
    private final String url;

    public Connecting(String url) {
        this.url = url;
    }

    @Override
    public ContentValues getAttributes() {
        ContentValues cv = super.getAttributes();
        cv.put(Database.ACTIVITIES_VERB, VERB);
        cv.put(Database.ACTIVITIES_DESCRIPTION, ""+url);
        cv.put(Database.ACTIVITIES_JSON, json.toString());
        return cv;
    }

}
