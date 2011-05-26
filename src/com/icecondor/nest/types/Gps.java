package com.icecondor.nest.types;

import android.location.Location;

public class Gps extends Base {
	Location location;
	
	public static Gps fromJson(String jobj) {
		return gson.fromJson(jobj, Gps.class);
	}

	public void setLocation(Location location) {
		this.location = location;
	}
	
	public Location getLocation() {
		return location;
	}
	/*
	 * JSON result
	{ "location": {
        "mResults": [
            0.0,
            0.0
        ],
        "mProvider": "gps",
        "mDistance": 0.0,
        "mTime": 1305208806000,
        "mAltitude": 0.0,
        "mLongitude": -122.084095,
        "mLon2": 0.0,
        "mLon1": 0.0,
        "mLatitude": 37.422005,
        "mLat1": 0.0,
        "mLat2": 0.0,
        "mInitialBearing": 0.0,
        "mHasSpeed": false,
        "mHasBearing": false,
        "mHasAltitude": true,
        "mHasAccuracy": false,
        "mAccuracy": 0.0,
        "mSpeed": 0.0,
        "mBearing": 0.0
      },
      "battery_level": 50
    }
   */

}
