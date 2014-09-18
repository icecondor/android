package com.icecondor.eaglet.db.activity;

import android.content.ContentValues;

import com.icecondor.eaglet.db.Activity;
import com.icecondor.eaglet.db.Database;

public class Disconnected  extends Activity  {
    private static final String VERB = "disconnected";

    public Disconnected() {
        super(VERB);
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
