package com.icecondor.eaglet.api;

import java.net.URI;

public interface ClientActions {
    public void onConnecting(URI uri);
    public void onConnected();
    public void onDisconnected();
    public void onTimeout();
}
