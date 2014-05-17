package com.icecondor.eaglet.api;

import java.net.URI;
import java.net.URISyntaxException;

import com.icecondor.eaglet.Condor.ApiActions;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpRequest;

public class Client {

    private final URI apiUrl;
    private final AsyncHttpClient client;

    public Client(String serverURL, ApiActions apiActions) throws URISyntaxException {
        apiUrl = new URI(serverURL);
        //socket = new TooTallSocket(apiUrl, new Dispatch());
        client = AsyncHttpClient.getDefaultInstance();
        KoushiSocket.disableSSLCheck(client);
    }

    public void connect() {
        // AndroidSync quirk, uses http urls
        String httpQuirkUrl = apiUrl.toString().replace("ws://", "http://").replace("wss://", "https://");
        AsyncHttpRequest get = new AsyncHttpGet(httpQuirkUrl);
        get.setTimeout(2500);
        client.websocket(get, null, new KoushiSocket(new Dispatch()));
        //com.koushikdutta.async.http.ConnectionClosedException) {
    }

}
