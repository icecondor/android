package com.icecondor.eaglet.db.activity;

import android.content.ContentValues;

import com.icecondor.eaglet.db.Activity;
import com.icecondor.eaglet.db.Database;

public class Connecting extends Activity  {
    private static final String VERB = "connecting";
    private final String url;

    public Connecting(String url) {
        super(VERB);
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
