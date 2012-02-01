package com.icecondor.nest;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;

import net.tootallnate.websocket.WebSocketClient;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.icecondor.nest.db.GeoRss;

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
	public void onOpen() {
		Log.i(APP_TAG,"ApiSocket onOpen \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
		connected = true;
		try {
            send("{\"type\":\"hello\", \"version\":\""+ICECONDOR_VERSION+"\"}");
            send("{\"type\":\"auth\", \"oauth_token\":\""+token+"\"}");
		} catch (NotYetConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
	public void onClose() {
		Log.i(APP_TAG,"ApiSocket close \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
		connected = false;
	}

	@Override
	public void onError(Exception ex) {
		Log.i(APP_TAG,"ApiSocket error: "+ex);
	}

	public boolean isConnected() { return connected; }

	public boolean emit(String msg) {
		Log.i(APP_TAG, "ApiSocket emit: "+msg);
		if (isConnected()) {
			try {
				send(msg);
				return true;
			} catch (NotYetConnectedException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
		} else {
			Log.i(APP_TAG, "emit: blocked! notConnected");
		}
		return false;		
	}
	
    protected void followFriend(String username) {
        try {
            rssdb.log("following "+username);
            JSONObject j = new JSONObject();
            j.put("type", "follow");
            j.put("username", username);
            emit(j.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void unfollowFriend(String username) {
        try {
            rssdb.log("unfollowing "+username);
            JSONObject j = new JSONObject();
            j.put("type", "unfollow");
            j.put("username", username);
            emit(j.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void addFriend(String username) {
        try {
            rssdb.log("friending "+username);
            JSONObject j = new JSONObject();
            j.put("type", "friend");
            j.put("username", username);
            emit(j.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void unFriend(String username) {
        try {
            rssdb.log("unfriending "+username);
            JSONObject j = new JSONObject();
            j.put("type", "friend");
            j.put("username", username);
            j.put("action", "remove");
            emit(j.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
