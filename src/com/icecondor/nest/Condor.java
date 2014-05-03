package com.icecondor.nest;

import java.net.URISyntaxException;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.icecondor.nest.api.Client;

public class Condor extends Service {

    private Client api;

    @Override
    public void onCreate() {
        Log.d(Constants.APP_TAG, "Bird service created");
        try {
            api = new Client("wss://api.icecondor.com");
            api.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(Constants.APP_TAG, "Bird service started");
        return null;
    }

}
