package com.icecondor.nest;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class Api extends WebSocketClient {
    public Api(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void connect() {

    }

    @Override
    public void onClose(int arg0, String arg1, boolean arg2) {

    }

    @Override
    public void onError(Exception arg0) {

    }

    @Override
    public void onMessage(String arg0) {

    }

    @Override
    public void onOpen(ServerHandshake arg0) {

    }
}