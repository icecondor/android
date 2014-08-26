package com.icecondor.eaglet.ui;

import java.net.URI;

import org.json.JSONObject;

public interface UiActions {
    public void onConnecting(URI uri);
    public void onConnected();
    public void onDisconnected();
    public void onConnectTimeout();
    public void onNewActivity();
    public void onApiResult(String id, JSONObject result);
    public void onApiError(String id, JSONObject result);
}
