package com.icecondor.nest.types;

import org.json.JSONException;
import org.json.JSONObject;

import com.icecondor.nest.Constants;
import com.icecondor.nest.Util;

import android.location.Location;
import android.util.Log;

public class Gps extends Base implements Constants {
	Location location;

	public Gps() {
	    super();
	}
	
	public static Gps fromJson(String json) {
	    try {
            JSONObject j = new JSONObject(json);
    	    return fromJson(j);
        } catch (Exception e) {
            Log.i(APP_TAG, "Gps.fromJson err: "+e);
            return null;
        }

	}
	
	public static Gps fromJson(JSONObject j) {
		/* GSON example:
		 * JSON result { "location": { "mResults": [ 0.0, 0.0 ], "mProvider": "gps",
		 * "mDistance": 0.0, "mTime": 1305208806000, "mAltitude": 0.0, "mLongitude":
		 * -122.084095, "mLon2": 0.0, "mLon1": 0.0, "mLatitude": 37.422005, "mLat1":
		 * 0.0, "mLat2": 0.0, "mInitialBearing": 0.0, "mHasSpeed": false,
		 * "mHasBearing": false, "mHasAltitude": true, "mHasAccuracy": false,
		 * "mAccuracy": 0.0, "mSpeed": 0.0, "mBearing": 0.0 }, "battery_level": 50 }
		 */
		try {
			JSONObject p;
			if (j.has("position")) {
				p = j.getJSONObject("position");
			} else {
				p = j.getJSONObject("location");
			}
			Location l = new Location(j.getString("provider"));
			l.setTime(Util.DateRfc822(j.getString("date")).getTime());
			l.setLatitude(p.getDouble("latitude"));
			l.setLongitude(p.getDouble("longitude"));
			if(p.has("altitude")) {
			  l.setAltitude(p.getDouble("altitude"));
			}
			if(p.has("accuracy"))
	            l.setAccuracy(new Float(p.getDouble("accuracy")));
			if(j.has("heading"))
			    l.setBearing(new Float(j.getDouble("heading")));
			if(j.has("velocity"))
			    l.setSpeed(new Float(j.getDouble("velocity")));
			
			Gps gps = new Gps();
			gps.id = j.getString("id");
			gps.setLocation(l);
			if(j.has("battery_level")) {
			    gps.setBattery(j.getInt("battery_level"));
			}
            if(j.has("ac_power")) {
                gps.setAC(j.getBoolean("ac_power"));
            }
			return gps;
		} catch (Exception e) {
			Log.i(APP_TAG, "Gps.fromJson err: "+e);
			return null;
		}

	}

	@Override
	public JSONObject toJson() {
		JSONObject jloc = new JSONObject();
		try {
		    jloc.put("id", id);
			JSONObject position = new JSONObject();
			position.put("latitude", location.getLatitude());
			position.put("longitude", location.getLongitude());
			position.put("altitude", location.getAltitude());
			position.put("accuracy", location.getAccuracy());
			
			// api hack
			jloc.put("type", "location");
			jloc.put("date", Util.DateTimeIso8601(location.getTime()));
			jloc.put("provider", location.getProvider());
			jloc.put("position", position);
			jloc.put("heading", location.getBearing());
			jloc.put("velocity", location.getSpeed());
			jloc.put("battery_level", battery_level);
			jloc.put("ac_power", ac_power);
		} catch (JSONException e) {
			Log.i(APP_TAG, "Gps.toJson err: "+e);
			try {
				jloc.put("error", "gps tojson err: "+e);
			} catch (JSONException e1) {}
		}
		return jloc;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public Location getLocation() {
		return location;
	}
}
