package com.icecondor.nest.api;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.icecondor.nest.Constants;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.WebSocket;

public class Client implements ConnectCallbacks {

    private final URI apiUrl;
    private final AsyncHttpClient client;
    private final ClientActions actions;
    private static final int RECONNECT_WAIT_BASE = 2;
    private int reconnects = 0;
    private final Handler handler;
    private Future<WebSocket> websocketFuture;
    private WebSocket websocket;
    private final Timer apiTimer;
    private final HashSet<String> apiQueue;

    public enum States { IDLE, WAITING, CONNECTING, CONNECTED };
    private States state;
    private Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(Constants.APP_TAG, "api.Client postDelayed firing.");
            state = States.IDLE;
            connect();
        }
    };

    public Client(URI serverURI, ClientActions actions) {
        this.apiUrl = serverURI;
        this.actions = actions;
        handler = new Handler();
        this.client = AsyncHttpClient.getDefaultInstance();
        KoushiSocket.disableSSLCheck(client);
        state = States.IDLE;
        apiTimer = new Timer("apiTimer");
        apiQueue = new HashSet<String>();
    }

    public void connect() {
        Log.d(Constants.APP_TAG, "client: connect(). state = "+state);
        if(state == States.IDLE) {
            state = States.CONNECTING;
            actions.onConnecting(apiUrl, reconnects);
            // AndroidAsync quirk, uses http urls
            String httpQuirkUrl = apiUrl.toString().replace("ws://", "http://").replace("wss://", "https://");
            AsyncHttpRequest get = new AsyncHttpGet(httpQuirkUrl);
            get.setTimeout(0);
            if(websocketFuture != null) {
                websocketFuture.cancel();
            }
            websocketFuture = client.websocket(get, null, new KoushiSocket(this));
        } else {
            Log.d(Constants.APP_TAG, "client: ignoring connect(). State "+state);
        }
    }

    public boolean isConnected() {
        return state == States.CONNECTED;
    }

    public void stop() {
        disconnect();
    }

    public void disconnect() {
        reconnects = 0;
        apiQueue.clear();
        handler.removeCallbacks(reconnectRunnable);
        if(websocket != null) {
            websocket.close();
        }
        state = States.IDLE;
    }

    public States getState() {
        return state;
    }

    @Override
    public void onTimeout() {
        state = States.IDLE;
        actions.onConnectTimeout();
    }

    public void reconnect() {
        reconnects += 1;
        long waitMillis = exponentialBackoffTimeMs(reconnects);
        Log.d(Constants.APP_TAG, "api.Client connect: onTimeout. "+
                                 "reconnects = "+reconnects+". "+
                                 "next try "+(waitMillis/1000)+"s.");
        handler.postDelayed(reconnectRunnable , waitMillis);
        state = States.WAITING;
    }

    private long exponentialBackoffTimeMs(int reconnects) {
        double secs = Math.min(Math.pow(RECONNECT_WAIT_BASE,reconnects), 60*60);
        return (long)(secs*1000);
    }

    /* ConnectCallbacks */
    @Override
    public void onMessage(String msg) {
        Log.d(Constants.APP_TAG, "api.Client onMessage: "+msg);
        try {
            JSONObject payload = new JSONObject(msg);
            String id = payload.getString("id");
            apiQueue.remove(id);
            actions.onMessage(payload);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected() {
        Log.d(Constants.APP_TAG, "api.Client onConnected.");
        try {
            websocket = websocketFuture.get();
            state = States.CONNECTED;
            reconnects = 0;
            actions.onConnected();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
			e.printStackTrace();
		}
    }

    @Override
    public void onDisconnected() {
        Log.d(Constants.APP_TAG, "api.Client onDisconnected.");
        state = States.IDLE;
        reconnects = 0;
        actions.onDisconnected();
        apiQueue.clear();
    }

    @Override
    public void onConnectionException(Exception ex) {
        actions.onConnectException(ex);
    }

    /* Track the call and its response */
    protected String apiCall(String method, JSONObject params) {
        String id = UUID.randomUUID().toString().substring(0, 7);
        JSONObject payload = new JSONObject();
        try {
            payload.put("id", id);
            payload.put("method", method);
            payload.put("params", params);
            Log.d(Constants.APP_TAG, "api.Client apiCall "+payload);
            apiQueue(payload);
            return id;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void apiQueue(JSONObject payload) {
        try {
            final String id = payload.getString("id");
            apiQueue.add(id);
            if(state == States.CONNECTED) {
                websocket.send(payload.toString());
            }
            // allow timer to fail, triggering reconnect
            apiTimer.schedule(new TimerTask(){
                @Override
                public void run() {
                    if(apiQueue.contains(id)) {
                        // timeout
                        Log.d(Constants.APP_TAG,"api.Client apiCall "+id+" timed out!");
                        actions.onMessageTimeout(id);
                    }
                }}, 30000);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /* API Calls */
    public String accountAuthEmail(String email, String deviceId) {
        JSONObject params = new JSONObject();
        try {
            params.put("email", email);
            params.put("device_id", deviceId);
            return apiCall("auth.email", params);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String accountAuthSession(String token, String deviceId) {
        JSONObject params = new JSONObject();
        try {
            String deviceKey = sha256base64(deviceId+token);
            params.put("device_key", deviceKey);
            return apiCall("auth.session", params);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String sha256base64(String text) throws NoSuchAlgorithmException{
        MessageDigest hasher = MessageDigest.getInstance("SHA-256");
        hasher.update(text.getBytes());
        String hash = Base64.encodeToString(hasher.digest(), Base64.NO_WRAP);
        return hash;
    }

    public String userDetail() {
        JSONObject params = new JSONObject();
        return apiCall("user.detail", params);
    }

    public String activityAdd(JSONObject activity) {
        return apiCall("activity.add", activity);
    }

    public String accountSetUsername(String username) {
        JSONObject params = new JSONObject();
        try {
            params.put("username", username);
            return apiCall("user.update", params);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}
