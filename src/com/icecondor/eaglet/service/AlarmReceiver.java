package com.icecondor.eaglet.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.db.AlarmTrigger;
import com.icecondor.eaglet.db.Database;

public class AlarmReceiver  extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Database db = new Database(context);
        db.open();

        String action = intent.getAction();
        if (action.equals("com.icecondor.nest.WAKE_ALARM")) {
            Log.i(Constants.APP_TAG, "AlarmReceiver onReceive");
            db.append(new AlarmTrigger());
        }

        db.close();
    }


}
