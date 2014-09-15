package com.icecondor.eaglet.service;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.icecondor.eaglet.Condor;
import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.db.Database;
import com.icecondor.eaglet.db.activity.HeartBeat;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Database db = new Database(context);
        db.open();

        String action = intent.getAction();
        if (action.equals(Constants.ACTION_WAKE_ALARM)) {
            Log.i(Constants.APP_TAG, "AlarmReceiver onReceive "+
                     context.getClass().getSimpleName()+" now "+new Date());
            db.append(new HeartBeat(""+Thread.currentThread().getName()));
            ((Condor)context).binder.onNewActivity();
            ((Condor)context).pushActivities();
        }

        db.close();
    }


}
