package com.icecondor.nest;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;

import net.tootallnate.websocket.Framedata;
import net.tootallnate.websocket.WebSocket;
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
	boolean connected = false;
	GeoRss rssdb;

	public ApiSocket(String url, Handler h, GeoRss rssdb) throws URISyntaxException {
		super(new URI(url));
		pigeon = h;
		this.rssdb = rssdb;
	}

	@Override
	public void onOpen() {
		Log.i(APP_TAG,"ApiSocket onOpen \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
		connected = true;
        emit("{\"type\":\"hello\", \"version\":\""+ICECONDOR_VERSION+"\"}");
        Bundle bundle = new Bundle();
        bundle.putString("type","open");
        
        Message msg = new Message();
        msg.setData(bundle);
        pigeon.dispatchMessage(msg);
	}

    @Override
    public void onMessage(String message) {
        Log.i(APP_TAG,"ApiSocket onMessage received: \""+message.trim()+"\" \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
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
        Bundle bundle = new Bundle();
        bundle.putString("type","close");
        
        Message msg = new Message();
        msg.setData(bundle);
        pigeon.dispatchMessage(msg);
	}

	@Override
	public void onError(Exception ex) {
		Log.i(APP_TAG,"ApiSocket error: "+ex);
		ex.printStackTrace();
	}

	public boolean isConnected() { return connected; }
	
	@Override
	public void onPing(WebSocket conn, Framedata f ) {
		super.onPing(conn, f);
		Log.i(APP_TAG,"ApiSocket onPing \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
        Bundle bundle = new Bundle();
        bundle.putString("type","ping");
        
        Message msg = new Message();
        msg.setData(bundle);
        pigeon.dispatchMessage(msg);
	}

	public boolean emit(String msg) {
		Log.i(APP_TAG, "ApiSocket emit: "+msg+" \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
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

    protected void auth(String token) {
        emit("{\"type\":\"auth\", \"oauth_token\":\""+token+"\"}");
    }
}
