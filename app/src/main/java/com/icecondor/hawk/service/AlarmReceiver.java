package com.icecondor.nest.service;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import com.icecondor.nest.Condor;
import com.icecondor.nest.Constants;
import com.icecondor.nest.Prefs;
import com.icecondor.nest.db.activity.GpsLocation;
import com.icecondor.nest.db.activity.HeartBeat;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // can we always assume context is Condor?
        Condor condor = (Condor)context;

        String action = intent.getAction();
        if (action.equals(Constants.ACTION_WAKE_ALARM)) {
            Log.i(Constants.APP_TAG, "AlarmReceiver onReceive "+
                     context.getClass().getSimpleName()+" now "+new Date());
            HeartBeat heartBeat = new HeartBeat("");
            heartBeat.setCellData(condor.isDataActive(ConnectivityManager.TYPE_MOBILE));
            heartBeat.setWifiData(condor.isDataActive(ConnectivityManager.TYPE_WIFI));
            if(condor.isBatteryValid()){
                heartBeat.setBatteryPercentage(condor.getBattPercent());
                heartBeat.setPower(condor.getBattAc());
            } else {
                Log.i(Constants.APP_TAG, "AlarmReceiver onReceive Warning Batt not valid");
            }
            condor.getDb().append(heartBeat);
            condor.binder.onNewActivity();
            GpsLocation lastLocation = condor.getLastLocation();
            if(lastLocation != null) {
                Prefs prefs = new Prefs(context);
            	long elapsed = System.currentTimeMillis() - lastLocation.getPoint().getTime();
                int seconds = prefs.getRecordingFrequencyInSeconds();
            	if(elapsed/1000 > seconds) {
            		if(prefs.isGpsOn()) {
            			condor.gpsOneShot();
            		}
            	}
            }
        }
    }


}
