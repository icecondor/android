package com.icecondor.nest.api;

import java.net.URI;
import java.net.URISyntaxException;

import android.util.Log;

import com.icecondor.nest.Constants;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpRequest;

public class Client {

    private final URI apiUrl;
    private final AsyncHttpClient socket;

    public Client(String serverURL) throws URISyntaxException {
        Log.d(Constants.APP_TAG, "Api constructor");
        apiUrl = new URI(serverURL);
        //socket = new TooTallSocket(apiUrl, new Dispatch());
        socket = AsyncHttpClient.getDefaultInstance();
    }

    public void connect() {
        Log.d(Constants.APP_TAG, "Api connecting");
        // AndroidSync quirk, uses http urls
        String fauxApiUrl = apiUrl.toString().replace("ws://", "http://").replace("wss://", "https://");
        AsyncHttpRequest get = new AsyncHttpGet(fauxApiUrl);
        get.setTimeout(2500);
        socket.websocket(get, "my-protocol", new KoushiSocket(new Dispatch()));
    }


}
