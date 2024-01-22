package com.icecondor.nest.db.activity;

import android.content.ContentValues;

import com.icecondor.nest.db.Activity;
import com.icecondor.nest.db.Database;

public class Start extends Activity  {
    private static final String VERB = "start";
    private final String description;

    public Start(String desc) {
        super(VERB);
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
