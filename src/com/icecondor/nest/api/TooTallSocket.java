package com.icecondor.nest.api;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import android.util.Log;

import com.icecondor.nest.Constants;


public class TooTallSocket extends WebSocketClient {
    public TooTallSocket(URI serverURI) {
        super(serverURI);
        Log.d(Constants.APP_TAG, "Socket constructor");
    }

    @Override
    public void onClose(int arg0, String arg1, boolean arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onError(Exception arg0) {
        Log.d(Constants.APP_TAG, "Socket onError "+arg0);
    }

    @Override
    public void onMessage(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOpen(ServerHandshake arg0) {
        Log.d(Constants.APP_TAG, "Socket onOpen ");

    }

}
