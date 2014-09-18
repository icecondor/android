package com.icecondor.eaglet.db.activity;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;

import com.icecondor.eaglet.db.Activity;
import com.icecondor.eaglet.db.Database;

public class HeartBeat extends Activity  {
    private static final String VERB = "heartbeat";
    private final String description;
    private int batteryPercentage;

    public HeartBeat(String desc) {
        super(VERB);
        description = desc;
        try {
            JSONObject battery = new JSONObject();
            battery.put("percentage", batteryPercentage);
            json.put("battery", battery);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ContentValues getAttributes() {
        ContentValues cv = super.getAttributes();
        cv.put(Database.ACTIVITIES_VERB, VERB);
        String desc = description + " batt "+batteryPercentage+"%";
        cv.put(Database.ACTIVITIES_DESCRIPTION, desc);
        cv.put(Database.ACTIVITIES_JSON, json.toString());
        return cv;
    }

    public void setBatteryPercentage(int battPercent) {
        batteryPercentage = battPercent;
    }
}
