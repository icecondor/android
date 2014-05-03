package com.icecondor.nest;

import java.net.URISyntaxException;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.icecondor.nest.api.Client;
import com.icecondor.nest.db.Database;

public class Condor extends Service {

    private Client api;
    private Database db;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        Log.d(Constants.APP_TAG, "Condor onCreate");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(Constants.APP_TAG, "Condor onStart");
        handleCommand(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.APP_TAG, "Condor onStartCommand");
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    public void handleCommand(Intent intent) {
        Log.d(Constants.APP_TAG, "Condor handleCommand");
        /* Preferences */
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        /* Database */
        Log.d(Constants.APP_TAG, "Condor: opening database");
        db = new Database(this);
        db.open();

        /* API */
        try {
            String apiUrl = prefs.getString("api_url", "");
            Log.d(Constants.APP_TAG, "Condor: connecting to "+apiUrl);
            api = new Client(apiUrl);
            api.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(Constants.APP_TAG, "Condor onBind");
        return null;
    }

}
