package com.icecondor.eaglet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.icecondor.eaglet.api.Client;
import com.icecondor.eaglet.api.Client.States;
import com.icecondor.eaglet.api.ClientActions;
import com.icecondor.eaglet.db.Connected;
import com.icecondor.eaglet.db.Connecting;
import com.icecondor.eaglet.db.Database;
import com.icecondor.eaglet.db.Disconnected;
import com.icecondor.eaglet.db.Start;
import com.icecondor.eaglet.service.AlarmReceiver;
import com.icecondor.eaglet.service.BatteryReceiver;
import com.icecondor.eaglet.service.GpsReceiver;
import com.icecondor.eaglet.ui.UiActions;

public class Condor extends Service {

    private Client api;
    private Database db;
    private SharedPreferences prefs;
    private PendingIntent wake_alarm_intent;
    private AlarmManager alarmManager;
    private BatteryReceiver batteryReceiver;
    private LocationManager locationManager;
    private GpsReceiver gpsReceiver;

    @Override
    public void onCreate() {
        Log.d(Constants.APP_TAG, "Condor onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String start_reason = "started";
        Log.d(Constants.APP_TAG, "Condor onStartCommand flags "+flags+" startId "+startId);
        if(intent == null) {
            Log.d(Constants.APP_TAG, "Condor null intent - restarted after kill");
            start_reason = "restarted";
        }
        if(db == null) { /* init only once */
            handleCommand(intent);
        } else {
            start_reason += " (no init)";
        }
        db.append(new Start(start_reason));
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    public void handleCommand(Intent intent) {
        Log.d(Constants.APP_TAG, "Condor handleCommand "+intent);
        Context ctx = getApplicationContext();

        /* Preferences */
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        /* Database */
        Log.d(Constants.APP_TAG, "Condor opening database. was "+db);
        db = new Database(ctx);
        db.open();

        /* Device ID */
        ensureDeviceID();

        /* Receive Alarms */
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        AlarmReceiver alarm_receiver = new AlarmReceiver();
        registerReceiver(alarm_receiver, new IntentFilter(Constants.ACTION_WAKE_ALARM));
        wake_alarm_intent = PendingIntent.getBroadcast(getApplicationContext(),
                                                            0,
                                                            new Intent(Constants.ACTION_WAKE_ALARM),
                                                            0);
        startAlarm();
        startApiThread();
        batteryReceiver = new BatteryReceiver();
        setupBatteryMonitor();
        gpsReceiver = new GpsReceiver();
        if(isRecording()) {
            Log.d(Constants.APP_TAG, "Condor isRecording is ON.");
            startGpsMonitor();
        }
    }

    private void ensureDeviceID() {
        String deviceId = getDeviceID();
        if(deviceId == null) {
            UUID did = UUID.randomUUID();
            prefs.edit().putString(Constants.SETTING_DEVICE_ID, did.toString()).commit();
        }
    }

    private String getDeviceID() {
        return prefs.getString(Constants.SETTING_DEVICE_ID, null);
    }

    protected void startApiThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                startApi();
                Looper.loop();
            }
        }).start();
    }

    protected void startApi() {
        /* API */
        try {
            String apiUrl = prefs.getString(Constants.PREFERENCE_API_URL,
                                            Constants.ICECONDOR_API_URL);
            api = new Client(apiUrl, new ApiActions());
            api.startPersistentConnect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    protected void startAlarm() {
        // clear any existing alarms
        alarmManager.cancel(wake_alarm_intent);
        long recording_frequency_minutes = getRecordingFrequencyInMilliseconds();
        Log.d(Constants.APP_TAG, "Condor startAlarm at "+recording_frequency_minutes+" minutes");
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis(),
                        getRecordingFrequencyInMilliseconds(),
                        wake_alarm_intent);
    }

    public long getRecordingFrequencyInMilliseconds() {
        long recording_frequency_minutes = Long.decode(prefs.getString(
                                               Constants.PREFERENCE_RECORDING_FREQUENCY,
                                               "3"));
        long recording_frequency_millisecs = recording_frequency_minutes*60*1000;
        return recording_frequency_millisecs;
    }

    protected void setupBatteryMonitor() {
        /* Battery */
        registerReceiver(batteryReceiver,
        new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(batteryReceiver,
        new IntentFilter(Intent.ACTION_POWER_CONNECTED));
        registerReceiver(batteryReceiver,
        new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
    }

    protected void startGpsMonitor() {
        Log.d(Constants.APP_TAG,"requesting GPS updates");
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                getRecordingFrequencyInMilliseconds(),
                0.0F, gpsReceiver);
    }

    /* public methods */
    public States getNetworkState() {
        return api.getState();
    }

    public boolean isConnected() {
        return api.getState() == Client.States.CONNECTED;
    }

    public boolean isRecording() {
        return prefs.getBoolean(Constants.SETTING_ON_OFF, false);
    }

    /* Callbacks from network client */
    public class ApiActions implements ClientActions {
        @Override
        public void onConnecting(URI url) {
            Log.d(Constants.APP_TAG, "Condor connecting to "+url);
            db.append(new Connecting(url.toString()));
            binder.onConnecting(url);
        }
        @Override
        public void onConnected() {
            db.append(new Connected());
            binder.onConnected();
        }
        @Override
        public void onDisconnected() {
            db.append(new Disconnected());
            binder.onDisconnected();
        }
        @Override
        public void onTimeout() {
            binder.onTimeout();
        }
    }

    /* API actions */
    public void doAccountCheck(String email) {
        api.accountCheck(email);
    }

    /* Emit signals to the bound Activity/UI */
    public class LocalBinder extends Binder implements UiActions {
        public Handler handler;
        public UiActions callback;
        public Condor getService() {
            return Condor.this;
        }
        /* handler setup */
        public void setHandler(Handler handler, UiActions callback) {
            Log.d(Constants.APP_TAG, "condor: localBinder: setHandler "+handler);
            this.handler = handler;
            this.callback = callback;
        }
        public void clearHandler() {
            Log.d(Constants.APP_TAG, "condor: localBinder: clearHandler ");
            this.handler = null;
            this.callback = null;
        }
        public boolean hasHandler() {
            if(handler != null) {
                return true;
            } else {
                return false;
            }
        }

        /* Relay messages to UI */
        @Override
        public void onConnecting(final URI uri) {
            if(hasHandler()) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onConnecting(uri);
                        callback.onNewActivity();
                    }
                });
            }
        }
        @Override
        public void onConnected() {
            if(hasHandler()) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onConnected();
                        callback.onNewActivity();
                    }
                });
            }
        }
        @Override
        public void onDisconnected() {
            if(hasHandler()) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onDisconnected();
                        callback.onNewActivity();
                    }
                });
            }
        }
        @Override
        public void onTimeout() {
            if(hasHandler()) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onTimeout();
                        callback.onNewActivity();
                    }
                });
            }
        }
        @Override
        public void onNewActivity() {
            if(hasHandler()) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onNewActivity();
                    }
                });
            }
        }
    }

    public final LocalBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(Constants.APP_TAG, "Condor onBind");
        return binder;
    }

}
