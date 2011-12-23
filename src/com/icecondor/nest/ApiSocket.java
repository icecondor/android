package com.icecondor.nest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import net.tootallnate.websocket.WebSocketClient;

public class ApiSocket extends WebSocketClient {
	Handler pigeon;
	String token;
	static String tag = "icecondor";
	boolean connected = false;

	public ApiSocket(String url, Handler h, String token) throws URISyntaxException {
		super(new URI(url));
		pigeon = h;
		this.token = token;
	}

	@Override
	public void onMessage(String message) {
		Log.i(tag,"thread: \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
		Log.i(tag,"received: \""+message+"\"");
		Bundle bundle = new Bundle();
		bundle.putString("type","message");
		bundle.putString("json", message);
		
		Message msg = new Message();
		msg.setData(bundle);
		pigeon.dispatchMessage(msg);
	}

	@Override
	public void onOpen() {
		Log.i(tag,"open \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
		connected = true;
		try {
			send("{\"type\":\"auth\"}");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onClose() {
		Log.i(tag,"close \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
		connected = false;
	}

	@Override
	public void onIOError(IOException ex) {
		Log.i(tag,""+ex);
	}

	@Override
	public void onPong() {
		Log.i(tag, "onPong");
	}

	public boolean isConnected() { return connected; }

	public boolean emit(String msg) {
		Log.i(tag, "emit: "+msg);
		if (isConnected()) {
			try {
				send(msg);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			Log.i(tag, "emit: blocked! notConnected");
		}
		return false;		
	}
}
