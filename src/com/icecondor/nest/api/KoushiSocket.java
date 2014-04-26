package com.icecondor.nest.api;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.WebSocket.StringCallback;

public class KoushiSocket implements AsyncHttpClient.WebSocketConnectCallback {

    private final Dispatch dispatch;

    public KoushiSocket(Dispatch callback) {
        dispatch = callback;
    }

    @Override
    public void onCompleted(Exception ex, WebSocket webSocket) {
        if (ex != null) {
            ex.printStackTrace();
            return;
        }

        webSocket.setStringCallback(new StringCallback() {
            @Override
            public void onStringAvailable(String s) {
                System.out.println("I got a string: " + s);
                dispatch.process(s);
            }
        });

        webSocket.setDataCallback(new DataCallback() {
            @Override
            public void onDataAvailable(DataEmitter emitter, ByteBufferList byteBufferList) {
                System.out.println("I got some bytes!");
                byteBufferList.recycle();
            }
        });
    }

}
