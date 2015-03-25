package com.icecondor.nest;

import java.net.URI;
import java.util.HashMap;
import java.util.UUID;

import org.joda.time.DateTime;
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
import android.util.Log;

import com.icecondor.nest.api.Client;
import com.icecondor.nest.api.Client.States;
import com.icecondor.nest.api.ClientActions;
import com.icecondor.nest.db.Database;
import com.icecondor.nest.db.Point;
import com.icecondor.nest.db.activity.Config;
import com.icecondor.nest.db.activity.Connected;
import com.icecondor.nest.db.activity.Connecting;
import com.icecondor.nest.db.activity.Disconnected;
import com.icecondor.nest.db.activity.GpsLocation;
import com.icecondor.nest.service.AlarmReceiver;
import com.icecondor.nest.service.BatteryReceiver;
import com.icecondor.nest.service.CellReceiver;
import com.icecondor.nest.service.GpsReceiver;
import com.icecondor.nest.ui.UiActions;

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
    private CellReceiver cellReceiver;
    private final HashMap<String, Integer> activityApiQueue = new HashMap<String, Integer>();
    protected Handler apiThreadHandler;
    private NotificationBar notificationBar;

    @Override
    public void onCreate() {
        Log.d(Constants.APP_TAG, "** Condor onCreate "+new DateTime()+" **");
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
        //db.append(new Start(start_reason));

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

        /* Battery Monitor */
        batteryReceiver = new BatteryReceiver();
        setupBatteryMonitor();

        /* Location Manager */
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gpsReceiver = new GpsReceiver(this);
        cellReceiver = new CellReceiver(this);

        /* Notification Bar */
        notificationBar = new NotificationBar(this);

        /* API */
        api = new Client(prefs.getApiUrl(), new ApiActions());
        if(prefs.isAuthenticatedUser() && isRecording()) {
            Log.d(Constants.APP_TAG, "Condor isRecording is ON.");
            startRecording();
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

    public void startApi() {
        notificationBar.updateText("Waiting for first location.");
        api.connect();
    }

    public void stopApi() {
        notificationBar.cancel();
        api.stop();
    }

    protected void startAlarm() {
        // clear any existing alarms
        stopAlarm();
        int seconds = prefs.getRecordingFrequencyInSeconds();
        int minutes = seconds / 60;
        Log.d(Constants.APP_TAG, "condor startAlarm at "+minutes+" minutes");
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis(),
                        seconds*1000,
                        wake_alarm_intent);
    }

    protected void stopAlarm() {
        alarmManager.cancel(wake_alarm_intent);
        Log.d(Constants.APP_TAG, "condor stopAlarm");
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
        int seconds = prefs.getRecordingFrequencyInSeconds();
        Log.d(Constants.APP_TAG,"condor requesting GPS updates every "+seconds/60+" minutes");
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                seconds*1000,
                0.0F, gpsReceiver);
    }

    protected void stopGpsMonitor() {
        Log.d(Constants.APP_TAG,"condor unrequesting GPS updates");
        locationManager.removeUpdates(gpsReceiver);
    }

    protected void startNetworkMonitor() {
        int seconds = prefs.getRecordingFrequencyInSeconds();
        Log.d(Constants.APP_TAG,"condor requesting NETWORK updates every "+seconds/60+" minutes");
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                seconds*1000,
                0.0F, cellReceiver);
    }

    protected void stopNetworkMonitor() {
        Log.d(Constants.APP_TAG,"condor unrequesting NETWORK updates");
        locationManager.removeUpdates(cellReceiver);
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

    public boolean isBatteryValid() {
        return batteryReceiver.isLastBatteryValid();
    }

    public int getBattPercent() {
        return batteryReceiver.getLastBatteryLevel();
    }

    public boolean getBattAc() {
        return batteryReceiver.isOnAcPower();
    }

    public boolean isRecording() {
        return prefs.isOnOff();
    }

    public void setRecording(boolean onOff) {
        Log.d(Constants.APP_TAG, "condor setRecording("+onOff+")");
        boolean oldOnOff = isRecording();
        prefs.setOnOff(onOff);
        if(!oldOnOff && onOff) {
            // transition to on
            configChange("recording", onOff);
            startRecording();
        }
        if(oldOnOff && !onOff) {
            // transition to off
            configChange("recording", onOff);
            stopRecording();
        }
    }

    public void configChange(String key, boolean value) {
        configChange(key, value ? "on" : "off");
    }

    public void configChange(String key, String value) {
        db.append(new Config(key, value));
    }

    public void startRecording() {
        startApi();
        startAlarm();
        if(prefs.isGpsOn()) {
            startGpsMonitor();
        }
        if(prefs.isCellOn() || prefs.isWifiOn()) {
            startNetworkMonitor();
        }
    }

    public void stopRecording() {
        stopApi();
        stopGpsMonitor();
        stopNetworkMonitor();
        stopAlarm();
    }

    public void connectNow() {
        api.connect();
    }

    public void disconnect() {
        api.stop();
    }

    public void clearHistory() {
        db.emptyTable(Database.TABLE_ACTIVITIES);
    }

    public void pushActivities() {
        Cursor unsynced = db.ActivitiesUnsynced();
        int count = unsynced.getCount();
        Log.d(Constants.APP_TAG, "condor pushActivities unsynced count "+count);
        if(count > 0) {
            unsynced.moveToFirst();
            String json = unsynced.getString(unsynced.getColumnIndex(Database.ACTIVITIES_JSON));
            JSONObject activity;
            try {
                activity = new JSONObject(json);
                int rowId = unsynced.getInt(unsynced.getColumnIndex(Database.ROW_ID));
                String apiId = api.activityAdd(activity);
                activityApiQueue.put(apiId, rowId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isUnsyncedPriorityWaiting() {
        return db.activitiesUnsyncedCount("location") > 0;
    }

    /* Callbacks from network client */
    public class ApiActions implements ClientActions {
        @Override
        public void onConnecting(URI url, int attempts) {
            Log.d(Constants.APP_TAG, "Condor connecting to "+url);
            String status = url.toString();
            if(attempts > 0) {
                status += " attempt #"+attempts;
            }
            db.append(new Connecting(status));
            binder.onConnecting(url);
        }
        @Override
        public void onConnected() {
            db.append(new Connected());
            binder.onConnected();
            if(prefs.isAuthenticatedUser()){
                authApiCall = api.accountAuthSession(prefs.getAuthenticationToken(), getDeviceID());
            }
        }
        @Override
        public void onDisconnected() {
            clientAuthenticated = false;
            db.append(new Disconnected("socket closed"));
            binder.onDisconnected();
            if(isRecording()) {
                if(prefs.isPersistentReconnect() || isUnsyncedPriorityWaiting()) {
                    Log.d(Constants.APP_TAG, "condor onDisconnected. reconnecting.");
                    api.reconnect();
                }
            }
        }
        @Override
        public void onConnectTimeout() {
            binder.onConnectTimeout();
            if(isRecording()) {
                api.reconnect();
            }
        }
        @Override
        public void onConnectException(Exception ex) {
            binder.onConnectException(ex);
        }
        @Override
        public void onMessage(JSONObject msg) {
            String apiId;
            try {
                if(msg.has("id")) {
                    apiId = msg.getString("id");
                    if(activityApiQueue.containsKey(apiId)){
                        int rowId = activityApiQueue.get(apiId);
                        activityApiQueue.remove(apiId);
                        db.markActivitySynced(rowId);
                        binder.onNewActivity();
                        JSONObject actJson = db.activityJson(rowId);
                        Log.d(Constants.APP_TAG,"condor marked as synced "+rowId+" "+actJson);
                        if(actJson.getString("type").equals("location")) {
                            notifyPosition(actJson);
                        }
                        pushActivities();
                    }
                    if(msg.has("result")){
                        JSONObject result = msg.getJSONObject("result");
                        binder.onApiResult(apiId, result);
                    }
                    if(msg.has("error")){
                        JSONObject result = msg.getJSONObject("error");
                        binder.onApiError(apiId, result);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void notifyPosition(JSONObject actJson) throws JSONException {
            GpsLocation loc = new GpsLocation(actJson);
            int count = Math.abs((int)(loc.getPoint().getAccuracy() * 3.28084 / 264));
            String unit = "block";
            if(count > 1) { unit = "blocks"; }
            String provider = loc.getPoint().getProvider().toUpperCase();
            if(provider.equals("NETWORK")) {
                if(loc.getPoint().getAccuracy() < 200) {
                    provider = "wifi";
                } else {
                    provider = "cell tower";
                }
            }
            String msg = "Located within "+count+" "+unit+" using "+provider+".";
            notificationBar.updateText(msg);
        }

        @Override
        public void onMessageTimeout(String id) {
            try {
                JSONObject err = new JSONObject();
                err.put("reason", "timeout");
                binder.onApiError(id, err);
                Log.d(Constants.APP_TAG, "condor: onMessageTimeout. disconnecting");
                db.append(new Disconnected("onMessageTimeout #"+id));
                api.disconnect();
                if(isRecording()) {
                    if(prefs.isPersistentReconnect() || isUnsyncedPriorityWaiting()) {
                        Log.d(Constants.APP_TAG, "condor onMessageTimeout. reconnecting.");
                        api.reconnect();
                    }
                }
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
            this.handler.removeCallbacksAndMessages(null);
            this.handler = null;
            this.callback = null;
        }
        public boolean hasHandler() {
            if(handler != null && callback != null) {
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
        public void onConnectException(Exception ex) {
            final Exception fex = ex;
            if(hasHandler()) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onConnectException(fex);
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
                pushActivities();
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
        pushActivities();
        binder.onNewActivity();
    }

    public String updateUsername(String username) {
        return api.accountSetUsername(username);
    }

    public void resetApiUrl(URI uri) {
        stopApi();
        api = new Client(uri, new ApiActions());
        if(isRecording()) {
            startApi();
        }
    }

    // let the Alarm thread write using the open db handle
    public Database getDb() {
        return db;
    }
}
