package com.icecondor.nest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import net.tootallnate.websocket.WebSocketClient;

public class ApiSocket extends WebSocketClient implements Constants {
	Handler pigeon;
	String token;
	boolean connected = false;

	public ApiSocket(String url, Handler h, String token) throws URISyntaxException {
		super(new URI(url));
		pigeon = h;
		this.token = token;
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
		Log.i(APP_TAG,"ApiSocket open \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
		connected = true;
		try {
			send("{\"type\":\"auth\"}");
		} catch (IOException e) {
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
