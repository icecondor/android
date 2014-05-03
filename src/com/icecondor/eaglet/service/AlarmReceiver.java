package com.icecondor.eaglet.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.icecondor.eaglet.Constants;

public class AlarmReceiver  extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals("com.icecondor.nest.WAKE_ALARM")) {
            Log.i(Constants.APP_TAG, "AlarmReceiver onReceive");
        }
    }


}
