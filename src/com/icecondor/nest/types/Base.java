package com.icecondor.nest.types;

abstract public class Base {

	abstract public String toJson();

	// "integral" fields
	int battery_level;

	public void setBattery(int last_battery_level) {
		battery_level = last_battery_level;
	}

	public int getBattery() {
		return battery_level;
	}

}
