package com.icecondor.eaglet.api;

import org.json.JSONObject;

public interface ConnectCallbacks {
    public void onTimeout();
    public void onConnected();
    public void onDisconnected();
    public void onMessage(JSONObject msg);
}
