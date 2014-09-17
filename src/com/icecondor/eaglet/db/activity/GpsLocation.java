package com.icecondor.eaglet.db.activity;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.location.Location;

import com.icecondor.eaglet.db.Activity;
import com.icecondor.eaglet.db.Database;
import com.icecondor.eaglet.db.Point;

public class GpsLocation extends Activity {
    private static final String VERB = "location";
    private final Point point;

    public GpsLocation(JSONObject json) throws JSONException {
        Location location = new Location(json.getString("provider"));
        location.setLatitude(json.getDouble("latitude"));
        location.setLongitude(json.getDouble("longitude"));
        location.setAccuracy((float)json.getDouble("longitude"));
        Point point = new Point(location);
        this.point = point;
    }

    public GpsLocation(Point point) {
        this.point = point;
        try {
            json.put("type", VERB);
            json.put("latitude", point.getLatitude());
            json.put("longitude", point.getLongitude());
            json.put("accuracy", point.getAccuracy());
            json.put("provider", point.getProvider());
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public Point getPoint() {
        return point;
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
