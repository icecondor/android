package com.icecondor.eaglet.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BatteryReceiver extends BroadcastReceiver {
    private int last_battery_level;
    private boolean ac_power;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            last_battery_level = intent.getIntExtra("level", 0);
        }
        if (action.equals("android.intent.action.ACTION_POWER_CONNECTED")) {
            ac_power = true;
        }
        if (action.equals("android.intent.action.ACTION_POWER_DISCONNECTED")) {
            ac_power = false;
        }
    }

    public int getLastBatteryLevel() {
        return last_battery_level;
    }

    public boolean isOnAcPower() {
        return ac_power;
    }
};