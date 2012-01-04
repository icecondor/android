package com.icecondor.nest.types;

import java.util.UUID;

import org.json.JSONObject;

abstract public class Base {

    abstract public JSONObject toJson();
    
    String id;
    
    public Base() {
        id = UUID.randomUUID().toString();
    }
    
    public String getId() {
        return id;
    }

	// "integral" fields
	int battery_level;
	boolean ac_power;

	public void setBattery(int last_battery_level) {
		battery_level = last_battery_level;
	}

	public int getBattery() {
		return battery_level;
	}

	public void setAC(boolean last_ac_power) {
		ac_power = last_ac_power;
	}

	public boolean getAC() {
		return ac_power;
	}
}
