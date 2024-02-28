package com.icecondor.hawk.api;

import java.net.URI;

import org.json.JSONObject;

public interface ClientActions {
    public void onConnecting(URI uri, int reconnects);
    public void onConnected();
    public void onDisconnected();
    public void onConnectTimeout();
    public void onMessageTimeout(String id);
    public void onMessage(JSONObject result);
    public void onConnectException(Exception ex);
}
