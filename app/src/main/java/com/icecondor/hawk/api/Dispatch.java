package com.icecondor.hawk.api;

import android.util.Log;

import com.icecondor.nest.Constants;

public class Dispatch {

    public void process(String msg) {
        Log.d(Constants.APP_TAG, "ws: "+msg);
    }
}
