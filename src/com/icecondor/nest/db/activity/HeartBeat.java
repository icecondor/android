package com.icecondor.nest.db.activity;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;

import com.icecondor.nest.db.Activity;
import com.icecondor.nest.db.Database;

public class HeartBeat extends Activity  {
    private static final String VERB = "heartbeat";
    private final String description;
    private int batteryPercentage;
    private boolean power;

    public HeartBeat(String desc) {
        super(VERB);
        description = desc;
    }

    @Override
    public ContentValues getAttributes() {
        ContentValues cv = super.getAttributes();
        cv.put(Database.ACTIVITIES_VERB, VERB);
        String desc = description + " battery "+batteryPercentage+"%";
        if(power) { desc = desc + " charging"; }
        cv.put(Database.ACTIVITIES_DESCRIPTION, desc);
        cv.put(Database.ACTIVITIES_JSON, json.toString());
        return cv;
    }

    public void setBatteryPercentage(int battPercent) {
        batteryPercentage = battPercent;
        try {
            JSONObject battery = new JSONObject();
            battery.put("percentage", batteryPercentage);
            json.put("battery", battery);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setPower(boolean power) {
        this.power = power;
        try {
            json.put("power", power);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
