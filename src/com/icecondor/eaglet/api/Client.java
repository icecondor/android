package com.icecondor.eaglet.api;

import java.net.URI;
import java.net.URISyntaxException;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpRequest;

public class Client {

    private final URI apiUrl;
    private final AsyncHttpClient socket;

    public Client(String serverURL) throws URISyntaxException {
        apiUrl = new URI(serverURL);
        //socket = new TooTallSocket(apiUrl, new Dispatch());
        socket = AsyncHttpClient.getDefaultInstance();
    }

    public void connect() {
        // AndroidSync quirk, uses http urls
        String httpQuirkUrl = apiUrl.toString().replace("ws://", "http://").replace("wss://", "https://");
        AsyncHttpRequest get = new AsyncHttpGet(httpQuirkUrl);
        get.setTimeout(2500);
        socket.websocket(get, "my-protocol", new KoushiSocket(new Dispatch()));
    }


}
