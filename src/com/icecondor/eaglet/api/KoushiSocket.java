package com.icecondor.eaglet.api;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.SSLSocketFactory;

import android.util.Log;

import com.icecondor.eaglet.Constants;
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

        Log.d(Constants.APP_TAG, "ws: onCompleted");
        webSocket.setStringCallback(new StringCallback() {
            @Override
            public void onStringAvailable(String s) {
                Log.d(Constants.APP_TAG, "ws: onStringAvailable: "+s);
                System.out.println("I got a string: " + s);
                dispatch.process(s);
            }
        });

        webSocket.setDataCallback(new DataCallback() {
            @Override
            public void onDataAvailable(DataEmitter emitter, ByteBufferList byteBufferList) {
                Log.d(Constants.APP_TAG, "ws: onStringAvailable ");
                byteBufferList.recycle();
            }
        });
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

final class NoTrustManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
