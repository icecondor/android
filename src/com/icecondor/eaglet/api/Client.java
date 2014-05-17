package com.icecondor.eaglet.api;

import java.net.URI;
import java.net.URISyntaxException;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.icecondor.eaglet.Constants;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpRequest;

public class Client implements ConnectCallbacks, Handler.Callback {

    private final URI apiUrl;
    private final AsyncHttpClient client;
    private final ClientActions actions;
    private boolean reconnect = true;
    private static final int RECONNECT_WAIT_MS = 5000;
    private int reconnects = 0;
    private final Handler handler;

    public Client(String serverURL, ClientActions actions) throws URISyntaxException {
        this.apiUrl = new URI(serverURL);
        this.actions = actions;
        handler = new Handler(this);
        //socket = new TooTallSocket(apiUrl, new Dispatch());
        this.client = AsyncHttpClient.getDefaultInstance();
        KoushiSocket.disableSSLCheck(client);
    }

    public void connect() {
        actions.onConnecting(apiUrl);
        // AndroidSync quirk, uses http urls
        String httpQuirkUrl = apiUrl.toString().replace("ws://", "http://").replace("wss://", "https://");
        AsyncHttpRequest get = new AsyncHttpGet(httpQuirkUrl);
        get.setTimeout(2500);
        client.websocket(get, null, new KoushiSocket(new Dispatch(), this));
    }

    public void startPersistentConnect() {
        reconnect = true;
        doConnect();
    }

    @Override
    public void onTimeout() {
        if(reconnect) {
            reconnects += 1;
            long waitMillis = exponentialBackoffTime(reconnects);
            Log.d(Constants.APP_TAG, "api: onTimeout. reconnects = "+reconnects+". next try "+(waitMillis/1000)+"s.");
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

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

}
