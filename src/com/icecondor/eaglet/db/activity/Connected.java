package com.icecondor.eaglet.db.activity;

import com.icecondor.eaglet.db.Database;

import android.content.ContentValues;

public class Connected extends Base  {
    private static final String VERB = "connected";

    public Connected() {
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
