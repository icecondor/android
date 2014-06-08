package com.icecondor.eaglet.db;

import android.content.ContentValues;

public class GpsLocation extends DbActivity {
    private static final String VERB = "gps_point";
    private final Point point;

    public GpsLocation(Point point) {
        this.point = point;
    }

    @Override
    public ContentValues getAttributes() {
        ContentValues cv = super.getAttributes();
        cv.put(Database.ACTIVITIES_VERB, VERB);
        cv.put(Database.ACTIVITIES_DESCRIPTION, "accuracy "+(int)point.getAccuracy());
        cv.put(Database.ACTIVITIES_JSON, json.toString());
        return cv;
    }

}
