package com.icecondor.eaglet.api;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.conn.ssl.SSLSocketFactory;

import android.util.Log;

import com.icecondor.eaglet.Constants;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.WebSocket.StringCallback;

public class KoushiSocket implements AsyncHttpClient.WebSocketConnectCallback {

    private final ConnectCallbacks connectCallbacks;

    public KoushiSocket(ConnectCallbacks connBack) {
        connectCallbacks = connBack;
    }

    @Override
    public void onCompleted(Exception ex, WebSocket webSocket) {
        if (ex != null) {
            if(ex.getClass().isAssignableFrom(java.util.concurrent.TimeoutException.class)) {
                Log.d(Constants.APP_TAG, "ws: timeout!");
                connectCallbacks.onTimeout();
            } else {
                Log.d(Constants.APP_TAG, "ws: stacktrace!!");
                ex.printStackTrace();
            }
            return; // no webSocket
        }

        webSocket.setClosedCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception arg0) {
                Log.d(Constants.APP_TAG, "ws: closedCallback onCompleted: "+arg0);
                connectCallbacks.onDisconnected();
            }
        });

        webSocket.setEndCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception arg0) {
                Log.d(Constants.APP_TAG, "ws: endCallback onCompleted: "+arg0);

            }
        });

        webSocket.setStringCallback(new StringCallback() {
            @Override
            public void onStringAvailable(String s) {
                    connectCallbacks.onMessage(s);
            }
        });

        webSocket.setDataCallback(new DataCallback() {
            @Override
            public void onDataAvailable(DataEmitter emitter, ByteBufferList byteBufferList) {
                byteBufferList.recycle();
            }
        });

        connectCallbacks.onConnected();
    }

    public static void disableSSLCheck(AsyncHttpClient client) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { new NoTrustManager() }, null);
            client.getSSLSocketMiddleware().setSSLContext(sslContext);
            client.getSSLSocketMiddleware().setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            client.getSSLSocketMiddleware().setTrustManagers(new TrustManager[]{new NoTrustManager()});
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
