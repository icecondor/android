package com.icecondor.nest.api;

import java.net.URI;

import android.util.Log;

import com.icecondor.nest.Constants;

public class Client {

    private final TooTallSocket socket;

    public Client(URI serverURI) {
        Log.d(Constants.APP_TAG, "Api constructor");
        socket = new TooTallSocket(serverURI);
        //asyncSocket = AsyncHttpClient.getDefaultInstance();
    }

    public void connect() {
        Log.d(Constants.APP_TAG, "Api connecting");

        //asyncSocket.websocket("wss://server", "my-protocol", new KoushiSocket());
        socket.connect();

    }


}
