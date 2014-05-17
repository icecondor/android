package com.icecondor.eaglet;

import java.net.URISyntaxException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.icecondor.eaglet.api.Client;
import com.icecondor.eaglet.db.Connecting;
import com.icecondor.eaglet.db.Database;
import com.icecondor.eaglet.service.AlarmReceiver;

public class Condor extends Service {

    private Client api;
    private Database db;
    private SharedPreferences prefs;
    private PendingIntent wake_alarm_intent;
    private AlarmManager alarmManager;

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
        Context ctx = getApplicationContext();

        /* Preferences */
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        /* Database */
        Log.d(Constants.APP_TAG, "Condor opening database");
        db = new Database(ctx);
        db.open();

        /* Alarm */
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        AlarmReceiver alarm_receiver = new AlarmReceiver();
        registerReceiver(alarm_receiver, new IntentFilter("com.icecondor.nest.WAKE_ALARM"));
        wake_alarm_intent = PendingIntent.getBroadcast(getApplicationContext(),
                                                            0,
                                                            new Intent("com.icecondor.nest.WAKE_ALARM"),
                                                            0);
        startAlarm();

        /* API */
        try {
            String apiUrl = prefs.getString("api_url", "");
            Log.d(Constants.APP_TAG, "Condor connecting to "+apiUrl);
            db.append(new Connecting(apiUrl));
            api = new Client(apiUrl, new ApiActions());
            api.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void startAlarm() {
        long record_frequency = 180000; //Long.decode(prefs.getString(SETTING_TRANSMISSION_FREQUENCY, "300000"));
        Log.d(Constants.APP_TAG, "startAlarm at "+record_frequency/1000/60+" minutes");
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis(),
                        record_frequency,
                        wake_alarm_intent);
    }

    public class ApiActions {

    }

    /* Localbinder approach */
    public class LocalBinder extends Binder {
        Condor getService() {
            return Condor.this;
        }
    }
    private final IBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(Constants.APP_TAG, "Condor onBind");
        return binder;
    }

}
