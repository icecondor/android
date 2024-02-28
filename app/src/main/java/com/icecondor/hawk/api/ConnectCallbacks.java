package com.icecondor.hawk.api;


public interface ConnectCallbacks {
    public void onTimeout();
    public void onConnected();
    public void onDisconnected();
    public void onMessage(String msg);
    public void onConnectionException(Exception ex);
}
