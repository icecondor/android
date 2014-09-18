package com.icecondor.eaglet.service;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.icecondor.eaglet.Condor;
import com.icecondor.eaglet.Constants;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // ReceiverRestrictedContext

        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.i(Constants.APP_TAG, "BootReceiver onReceive "+
                     context.getClass().getSimpleName()+" now "+new Date());
            context.startService(new Intent(context, Condor.class));
        }
    }


}
