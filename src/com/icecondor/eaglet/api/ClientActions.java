package com.icecondor.eaglet.api;

import java.net.URI;

import org.json.JSONObject;

public interface ClientActions {
    public void onConnecting(URI uri);
    public void onConnected();
    public void onDisconnected();
    public void onConnectTimeout();
    public void onMessageTimeout(String id);
    public void onMessage(JSONObject result);
}
