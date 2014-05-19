package com.icecondor.eaglet.ui;

import java.net.URI;

public interface UiActions {
    public void onConnecting(URI uri);
    public void onConnected();
    public void onDisconnected();
    public void onTimeout();
    public void onNewActivity();
}
