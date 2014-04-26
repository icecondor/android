package com.icecondor.nest.api;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import android.util.Log;

import com.icecondor.nest.Constants;


public class TooTallSocket extends WebSocketClient {
    private final Dispatch dispatch;

    public TooTallSocket(URI serverURI, Dispatch callback) {
        super(serverURI, new Draft_17(), null, 500);
        Log.d(Constants.APP_TAG, "Socket constructor");
        dispatch = callback;
    }

    @Override
    public void onClose(int arg0, String arg1, boolean arg2) {

    }

    @Override
    public void onError(Exception arg0) {
        Log.d(Constants.APP_TAG, "Socket onError "+arg0);
    }

    @Override
    public void onMessage(String arg0) {
        dispatch.process(arg0);
    }

    @Override
    public void onOpen(ServerHandshake arg0) {
        Log.d(Constants.APP_TAG, "Socket onOpen ");
    }

}
