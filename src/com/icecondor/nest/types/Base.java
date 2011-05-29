package com.icecondor.nest.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Base {
	protected static Gson gson = new GsonBuilder().registerTypeAdapter(Gps.class, new LocationSerializer()).create();
	
	public String toJson() {
		return gson.toJson(this);
	}
	
	// "integral" fields
	int battery_level;
	public void setBattery(int last_battery_level) {
		battery_level = last_battery_level;		
	}
	public int getBattery() {
		return battery_level;		
	}

}
