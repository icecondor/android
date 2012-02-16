package com.icecondor.nest.util;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.icecondor.nest.Constants;
import com.icecondor.nest.Util;

public class GrabAndSavePicture implements Runnable, Constants {
    DefaultHttpClient client;
    String username;
    String url;
    Context ctx;
    
    public GrabAndSavePicture(String url, String username, DefaultHttpClient client, Context ctx) {
        this.client = client;
        this.url = url;
        this.username = username;
        this.ctx = ctx;
    }
    
    @Override
    public void run() {
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response;
        try {            
            Log.i(APP_TAG, "downloading "+url+" "+Thread.currentThread());
            response = client.execute(httpGet);
            Log.i(APP_TAG, "saving for "+username);
            Util.profilePictureSave(username, response.getEntity().getContent(), ctx);
            Intent intent = new Intent(USER_PROFILE_UPDATE_ACTION);
            intent.putExtra("username", username);
            ctx.sendBroadcast(intent);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
