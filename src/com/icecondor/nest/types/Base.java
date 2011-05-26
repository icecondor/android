package com.icecondor.nest.types;

import com.google.gson.Gson;

public class Base {
	protected static Gson gson = new Gson();
	
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
