package com.icecondor.eaglet.db;

import android.content.ContentValues;

public class Connected extends DbActivity  {
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
