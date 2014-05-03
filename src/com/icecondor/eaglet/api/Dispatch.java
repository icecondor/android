package com.icecondor.eaglet.api;

import android.util.Log;

import com.icecondor.eaglet.Constants;

public class Dispatch {

    public void process(String msg) {
        Log.d(Constants.APP_TAG, "ws: "+msg);
    }
}
