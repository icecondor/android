package com.icecondor.eaglet.api;

import java.net.URI;

import org.json.JSONObject;

public interface ClientActions {
    public void onConnecting(URI uri);
    public void onConnected();
    public void onDisconnected();
    public void onTimeout();
    public void onMessage(JSONObject result);
}
