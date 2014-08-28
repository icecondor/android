package com.icecondor.eaglet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.icecondor.eaglet.api.Client;
import com.icecondor.eaglet.api.Client.States;
import com.icecondor.eaglet.api.ClientActions;
import com.icecondor.eaglet.db.Database;
import com.icecondor.eaglet.db.Point;
import com.icecondor.eaglet.db.activity.Connected;
import com.icecondor.eaglet.db.activity.Connecting;
import com.icecondor.eaglet.db.activity.Disconnected;
import com.icecondor.eaglet.db.activity.GpsLocation;
import com.icecondor.eaglet.db.activity.Start;
import com.icecondor.eaglet.service.AlarmReceiver;
import com.icecondor.eaglet.service.BatteryReceiver;
import com.icecondor.eaglet.service.GpsReceiver;
import com.icecondor.eaglet.ui.UiActions;

public class Condor extends Service {

    private Client api;
    private boolean clientAuthenticated;
    private String authApiCall;
    private Database db;
    private Prefs prefs;
    private PendingIntent wake_alarm_intent;
    private AlarmManager alarmManager;
    private BatteryReceiver batteryReceiver;
    private LocationManager locationManager;
    private GpsReceiver gpsReceiver;
    private String token;

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
            start_reason += " (skipped init)";
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
        prefs = new Prefs(ctx);

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
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gpsReceiver = new GpsReceiver(this);
        if(isRecording()) {
            Log.d(Constants.APP_TAG, "Condor isRecording is ON.");
            startGpsMonitor();
        }
    }

    private void ensureDeviceID() {
        String deviceId = getDeviceID();
        if(deviceId == null) {
            deviceId = "device-"+UUID.randomUUID();
            prefs.setDeviceId(deviceId);
        }
    }

    private String getDeviceID() {
        return prefs.getDeviceId();
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
            api = new Client(prefs.getApiUrl(), new ApiActions());
            api.startPersistentConnect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    protected void startAlarm() {
        // clear any existing alarms
        alarmManager.cancel(wake_alarm_intent);
        int seconds = prefs.getRecordingFrequencyInSeconds();
        int minutes = seconds / 60;
        Log.d(Constants.APP_TAG, "Condor startAlarm at "+minutes+" minutes");
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis(),
                        seconds*1000,
                        wake_alarm_intent);
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
        int seconds = prefs.getRecordingFrequencyInSeconds();
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                seconds*1000,
                0.0F, gpsReceiver);
    }

    /* public methods */
    public States getNetworkState() {
        return api.getState();
    }

    public boolean isConnected() {
        return api.getState() == Client.States.CONNECTED;
    }

    public boolean isAuthenticated() {
        return clientAuthenticated;
    }

    public String testToken(String token) {
        return api.accountAuthSession(token, getDeviceID());
    }

    public boolean isRecording() {
        return prefs.getOnOff();
    }

    public void connectNow() {
        api.connect();
    }

    public void pushActivities() {
        if(clientAuthenticated) {
            Cursor unsynced = db.ActivitiesUnsynced();
            unsynced.moveToFirst();
            api.activityAdd(unsynced.getString(unsynced.getColumnIndex(Database.ACTIVITIES_JSON)));
        }
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
            if(prefs.isAuthenticatedUser()){
                authApiCall = api.accountAuthSession(prefs.getAuthenticationToken(), getDeviceID());
            }
            pushActivities();
        }
        @Override
        public void onDisconnected() {
            clientAuthenticated = false;
            db.append(new Disconnected());
            binder.onDisconnected();
        }
        @Override
        public void onConnectTimeout() {
            binder.onConnectTimeout();
        }
        @Override
        public void onMessage(JSONObject msg) {
            String id;
            try {
                if(msg.has("id")) {
                    id = msg.getString("id");
                    if(msg.has("result")){
                        JSONObject result = msg.getJSONObject("result");
                        binder.onApiResult(id, result);
                    }
                    if(msg.has("error")){
                        JSONObject result = msg.getJSONObject("error");
                        binder.onApiError(id, result);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onMessageTimeout(String id) {
            try {
                JSONObject err = new JSONObject();
                err.put("reason", "timeout");
                binder.onApiError(id, err);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /* API actions */
    public void doAccountAuth(String email) {
        api.accountAuthEmail(email, getDeviceID());
    }

    public String doUserDetail() {
        return api.userDetail();
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
        public void onConnectTimeout() {
            if(hasHandler()) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onConnectTimeout();
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
        @Override
        public void onApiResult(String _id, JSONObject _result) {
            final String id = _id;
            final JSONObject result = _result;
            if(_id.equals(authApiCall)){
                Log.d(Constants.APP_TAG, "condor: onApiResult caught authApiCall "+_result);
                clientAuthenticated = true;
            } else {
                if(hasHandler()) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onApiResult(id, result);
                        }
                    });
                }
            }
        }
        @Override
        public void onApiError(String _id, JSONObject _result) {
            final String id = _id;
            final JSONObject result = _result;
            if(hasHandler()) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onApiError(id, result);
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

    /* Location callbacks */
    public void onLocationChanged(Point point) {
        db.append(new GpsLocation(point));
        binder.onNewActivity();
    }

    public String updateUsername(String username) {
        return api.accountSetUsername(username);
    }

}
