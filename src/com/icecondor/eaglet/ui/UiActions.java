package com.icecondor.eaglet.ui;

import java.net.URI;

import org.json.JSONObject;

public interface UiActions {
    public void onConnecting(URI uri);
    public void onConnected();
    public void onDisconnected();
    public void onTimeout();
    public void onNewActivity();
    public void onApiResult(String id, JSONObject result);
}
