package com.icecondor.eaglet.db.activity;

import org.json.JSONException;

import android.content.ContentValues;

import com.icecondor.eaglet.db.Activity;
import com.icecondor.eaglet.db.Database;
import com.icecondor.eaglet.db.Point;

public class GpsLocation extends Activity {
    private static final String VERB = "gps_point";
    private final Point point;

    public GpsLocation(Point point) {
        this.point = point;
        try {
            json.put("type", VERB);
            json.put("latitude", point.getLatitude());
            json.put("longitude", point.getLongitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }

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
