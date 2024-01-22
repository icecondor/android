package com.icecondor.nest.service;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

import com.icecondor.nest.Constants;

public class BatteryReceiver extends BroadcastReceiver {
    private int last_battery_level;
    private Date last_battery_date;
    private boolean ac_power;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            last_battery_level = intent.getIntExtra("level", 0);
            last_battery_date = new Date();
            int ac_int = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            ac_power = ac_int != 0;
            Log.d(Constants.APP_TAG, "battery changed "+last_battery_level+" charging "+ac_power+" "+ac_int);
        }
        if (action.equals("android.intent.action.ACTION_POWER_CONNECTED")) {
            ac_power = true;
            Log.d(Constants.APP_TAG, "ac power"+ac_power);
        }
        if (action.equals("android.intent.action.ACTION_POWER_DISCONNECTED")) {
            ac_power = false;
            Log.d(Constants.APP_TAG, "ac power"+ac_power);
        }
    }

    public int getLastBatteryLevel() {
        return last_battery_level;
    }

    public boolean isLastBatteryValid() {
        return last_battery_date != null;
    }

    public int getLastBatteryAge() {
        return (new Date()).compareTo(last_battery_date);
    }

    public boolean isOnAcPower() {
        return ac_power;
    }
};