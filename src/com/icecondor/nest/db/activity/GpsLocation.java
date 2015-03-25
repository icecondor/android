package com.icecondor.nest.db.activity;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.location.Location;

import com.icecondor.nest.db.Activity;
import com.icecondor.nest.db.Database;
import com.icecondor.nest.db.Point;

public class GpsLocation extends Activity {
    private static final String VERB = "location";
    private final Point point;

    public GpsLocation(JSONObject json) throws JSONException {
        super(VERB);
        Location location = new Location(json.getString("provider"));
        location.setLatitude(json.getDouble("latitude"));
        location.setLongitude(json.getDouble("longitude"));
        location.setAccuracy((float)json.getDouble("longitude"));
        Point point = new Point(location);
        this.point = point;
    }

    public GpsLocation(Point point) {
        super(VERB);
        this.point = point;
        try {
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

    public String providerNameNetworkSpecial(Point point) {
        String provider = point.getProvider();
        float accuracy = point.getAccuracy();
        if(provider.equals("network")) {
            if(accuracy < 200) {
                return provider+"/wifi";
            } else {
                return provider+"/tower";
            }
        } else {
            return provider;
        }
    }

    @Override
    public ContentValues getAttributes() {
        ContentValues cv = super.getAttributes();
        cv.put(Database.ACTIVITIES_VERB, VERB);
        cv.put(Database.ACTIVITIES_DESCRIPTION, providerNameNetworkSpecial(point)+
                                                " accuracy "+(int)point.getAccuracy()+
                                                " meters");
        cv.put(Database.ACTIVITIES_JSON, json.toString());
        return cv;
    }

}
