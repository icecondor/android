package com.icecondor.eaglet;

import java.net.URI;
import java.net.URISyntaxException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.icecondor.eaglet.api.Client;
import com.icecondor.eaglet.api.ClientActions;
import com.icecondor.eaglet.db.Connected;
import com.icecondor.eaglet.db.Connecting;
import com.icecondor.eaglet.db.Database;
import com.icecondor.eaglet.db.Disconnected;
import com.icecondor.eaglet.service.AlarmReceiver;
import com.icecondor.eaglet.ui.UiActions;

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
            api = new Client(apiUrl, new ApiActions());
            api.startPersistentConnect();
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

    /* Localbinder approach */
    public class LocalBinder extends Binder implements UiActions {
        public Handler handler;
        public UiActions callback;
        public Condor getService() {
            return Condor.this;
        }
        public void setHandler(Handler handler, UiActions callback) {
            Log.d(Constants.APP_TAG, "condor: localBinder: setHandler "+handler);
            this.handler = handler;
            this.callback = callback;
        }
        public boolean hasHandler() {
            if(handler != null) {
                return true;
            } else {
                Log.d(Constants.APP_TAG, "condor: localBinder: hasHandler warning, no handler");
                return false;
            }
        }
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
