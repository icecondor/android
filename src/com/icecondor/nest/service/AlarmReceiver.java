package com.icecondor.nest.service;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.icecondor.nest.Condor;
import com.icecondor.nest.Constants;
import com.icecondor.nest.db.Database;
import com.icecondor.nest.db.activity.HeartBeat;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Database db = new Database(context);
        db.open();

        // can we always assume context is Condor?
        Condor condor = (Condor)context;

        String action = intent.getAction();
        if (action.equals(Constants.ACTION_WAKE_ALARM)) {
            Log.i(Constants.APP_TAG, "AlarmReceiver onReceive "+
                     context.getClass().getSimpleName()+" now "+new Date());
            HeartBeat heartBeat = new HeartBeat(" thread "+Thread.currentThread().getName());
            if(condor.isBatteryValid()){
                heartBeat.setBatteryPercentage(condor.getBattPercent());
                heartBeat.setPower(condor.getBattAc());
            } else {
                Log.i(Constants.APP_TAG, "AlarmReceiver onReceive Warning Batt not valid");
            }
            db.append(heartBeat);
            ((Condor)context).binder.onNewActivity();
            ((Condor)context).pushActivities();
        }

        db.close();
    }


}
