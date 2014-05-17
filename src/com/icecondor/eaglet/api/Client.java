package com.icecondor.eaglet.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.SSLSocketFactory;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpRequest;

public class Client {

    private final URI apiUrl;
    private final AsyncHttpClient client;

    public Client(String serverURL) throws URISyntaxException {
        apiUrl = new URI(serverURL);
        //socket = new TooTallSocket(apiUrl, new Dispatch());
        client = AsyncHttpClient.getDefaultInstance();
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

    public void connect() {
        // AndroidSync quirk, uses http urls
        String httpQuirkUrl = apiUrl.toString().replace("ws://", "http://").replace("wss://", "https://");
        AsyncHttpRequest get = new AsyncHttpGet(httpQuirkUrl);
        get.setTimeout(2500);
        client.websocket(get, null, new KoushiSocket(new Dispatch()));
        //com.koushikdutta.async.http.ConnectionClosedException) {
    }

    private class NoTrustManager implements X509TrustManager {
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

}
