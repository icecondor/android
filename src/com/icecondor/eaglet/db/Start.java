package com.icecondor.eaglet.db;

import android.content.ContentValues;

public class Start extends DbActivity  {
    private static final String VERB = "start";
    private final String description;

    public Start(String desc) {
        description = desc;
    }

    @Override
    public ContentValues getAttributes() {
        ContentValues cv = super.getAttributes();
        cv.put(Database.ACTIVITIES_VERB, VERB);
        cv.put(Database.ACTIVITIES_DESCRIPTION, description);
        cv.put(Database.ACTIVITIES_JSON, json.toString());
        return cv;
    }

}
