package com.icecondor.nest.types;

abstract public class Base {

	abstract public String toJson();

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
