package com.icecondor.nest.types;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

public class Gps extends Base {
	Location location;

	public static Gps fromJson(String json) {
		/* GSON example:
		 * JSON result { "location": { "mResults": [ 0.0, 0.0 ], "mProvider": "gps",
		 * "mDistance": 0.0, "mTime": 1305208806000, "mAltitude": 0.0, "mLongitude":
		 * -122.084095, "mLon2": 0.0, "mLon1": 0.0, "mLatitude": 37.422005, "mLat1":
		 * 0.0, "mLat2": 0.0, "mInitialBearing": 0.0, "mHasSpeed": false,
		 * "mHasBearing": false, "mHasAltitude": true, "mHasAccuracy": false,
		 * "mAccuracy": 0.0, "mSpeed": 0.0, "mBearing": 0.0 }, "battery_level": 50 }
		 */

		try {
			JSONObject j = new JSONObject(json);
			JSONObject p = j.getJSONObject("location");
			Location l = new Location("");
			l.setProvider(p.getString("latitude"));
			l.setLatitude(p.getDouble("latitude"));
			l.setLongitude(p.getDouble("longitude"));
			l.setTime(p.getLong("time"));
			l.setAltitude(p.getDouble("altitude"));
			l.setAccuracy(new Float(p.getDouble("accuracy")));
			l.setBearing(new Float(p.getDouble("bearing")));
			l.setSpeed(new Float(p.getDouble("speed")));
			
			Gps gps = new Gps();
			gps.setLocation(l);
			return gps;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}

	}

	public String toJson() {
		try {
			JSONObject position = new JSONObject();
			position.put("provider", location.getProvider());
			position.put("latitude", location.getLatitude());
			position.put("longitude", location.getLongitude());
			position.put("time", location.getTime());
			position.put("altitude", location.getAltitude());
			position.put("accuracy", location.getAccuracy());
			position.put("bearing", location.getBearing());
			position.put("speed", location.getSpeed());
			
			JSONObject jloc = new JSONObject();
			jloc.put("location", position);
			return jloc.toString();
		} catch (JSONException e) {
			return "{\"ERROR\":\"" + e.toString() + "\"}";
		}

	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public Location getLocation() {
		return location;
	}
}
