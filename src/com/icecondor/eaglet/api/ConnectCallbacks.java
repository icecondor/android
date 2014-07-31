package com.icecondor.eaglet.api;


public interface ConnectCallbacks {
    public void onTimeout();
    public void onConnected();
    public void onDisconnected();
    public void onMessage(String msg);
}
