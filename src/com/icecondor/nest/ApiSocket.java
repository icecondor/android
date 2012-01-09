package com.icecondor.nest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import com.icecondor.nest.db.GeoRss;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import net.tootallnate.websocket.WebSocketClient;

public class ApiSocket extends WebSocketClient implements Constants {
	Handler pigeon;
	String token;
	boolean connected = false;
	GeoRss rssdb;

	public ApiSocket(String url, Handler h, String token, GeoRss rssdb) throws URISyntaxException {
		super(new URI(url));
		pigeon = h;
		this.token = token;
		this.rssdb = rssdb;
	}

	@Override
	public void onMessage(String message) {
		Log.i(APP_TAG,"ApiSocket thread: \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
		Log.i(APP_TAG,"ApiSocket received: \""+message+"\"");
		Bundle bundle = new Bundle();
		bundle.putString("type","message");
		bundle.putString("json", message);
		
		Message msg = new Message();
		msg.setData(bundle);
		pigeon.dispatchMessage(msg);
	}

	@Override
	public void onOpen() {
		Log.i(APP_TAG,"ApiSocket onOpen \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
		connected = true;
		try {
			send("{\"type\":\"auth\", \"oauth_token\":\""+token+"\"}");
			/* follow our friends */
			Cursor c = rssdb.findFeedsByService("IceCondor");
			rssdb.log("apiSocket onOpen IceCondor friends "+c.getCount());
			while(c.moveToNext()) {
			    String username = c.getString(c.getColumnIndex(GeoRss.FEEDS_EXTRA));
	            rssdb.log("apiSocket onOpen IceCondor following "+username);
			    JSONObject j = new JSONObject();
			    j.put("type", "follow");
			    j.put("username", username);
			    send(j.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
            e.printStackTrace();
        }
	}

	@Override
	public void onClose() {
		Log.i(APP_TAG,"ApiSocket close \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
		connected = false;
	}

	@Override
	public void onIOError(IOException ex) {
		Log.i(APP_TAG,"ApiSocket "+ex);
	}

	@Override
	public void onPong() {
		Log.i(APP_TAG, "ApiSocket onPong");
	}

	public boolean isConnected() { return connected; }

	public boolean emit(String msg) {
		Log.i(APP_TAG, "ApiSocket emit: "+msg);
		if (isConnected()) {
			try {
				send(msg);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			Log.i(APP_TAG, "emit: blocked! notConnected");
		}
		return false;		
	}
}
