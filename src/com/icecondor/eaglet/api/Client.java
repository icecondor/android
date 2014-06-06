package com.icecondor.eaglet.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.util.Log;

import com.icecondor.eaglet.Constants;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.WebSocket;

public class Client implements ConnectCallbacks {

    private final URI apiUrl;
    private final AsyncHttpClient client;
    private final ClientActions actions;
    private boolean reconnect = true;
    private static final int RECONNECT_WAIT_MS = 5000;
    private int reconnects = 0;
    private final Handler handler;
    private Future<WebSocket> websocketFuture;
    private WebSocket websocket;

    public enum States { WAITING, CONNECTING, CONNECTED};
    private States state;

    public Client(String serverURL, ClientActions actions) throws URISyntaxException {
        this.apiUrl = new URI(serverURL);
        this.actions = actions;
        handler = new Handler();
        this.client = AsyncHttpClient.getDefaultInstance();
        KoushiSocket.disableSSLCheck(client);
        state = States.WAITING;
    }

    public void connect() {
        Log.d(Constants.APP_TAG, "client: connect(). state = "+state);
        if(state != States.CONNECTED) {
            state = States.CONNECTING;
            actions.onConnecting(apiUrl);
            // AndroidSync quirk, uses http urls
            String httpQuirkUrl = apiUrl.toString().replace("ws://", "http://").replace("wss://", "https://");
            AsyncHttpRequest get = new AsyncHttpGet(httpQuirkUrl);
            get.setTimeout(2500);
            websocketFuture = client.websocket(get, null, new KoushiSocket(this));
        } else {
            Log.d(Constants.APP_TAG, "client: ignoring connect(). connecting in progress.");
        }
    }

    public void startPersistentConnect() {
        reconnect = true;
        doConnect();
    }

    public States getState() {
        return state;
    }

    @Override
    public void onTimeout() {
        state = States.WAITING;
        actions.onTimeout();
        if(reconnect) {
            reconnects += 1;
            long waitMillis = exponentialBackoffTime(reconnects);
            Log.d(Constants.APP_TAG, "api.Client connect: onTimeout. reconnects = "+reconnects+". next try "+(waitMillis/1000)+"s.");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    doConnect();
                }
            }, waitMillis);
        }
    }

    private long exponentialBackoffTime(int reconnects) {
        return (long)Math.pow(RECONNECT_WAIT_MS/1000,reconnects);
    }

    private void doConnect() {
        connect();
    }

    /* ConnectCallbacks */
    @Override
    public void onMessage(JSONObject msg) {
        Log.d(Constants.APP_TAG, "Client onMessage: "+msg);
    }

    @Override
    public void onConnected() {
        Log.d(Constants.APP_TAG, "Client onConnected.");
        try {
            websocket = websocketFuture.get();
            state = States.CONNECTED;
            reconnects = 0;
            actions.onConnected();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnected() {
        Log.d(Constants.APP_TAG, "Client onDisconnected.");
        state = States.WAITING;
        reconnects = 0;
        actions.onDisconnected();
        doConnect();
    }

    /* Track the call and its response */
    protected void apiCall(JSONObject payload) {
        websocket.send(payload.toString());
    }

    /* API Calls */
    public void accountCheck(String email) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("email", email);
            apiCall(payload);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
