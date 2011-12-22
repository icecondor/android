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

	public ApiSocket(String url, Handler h) throws URISyntaxException {
		super(new URI(url));
		pigeon = h;
	}

	@Override
	public void onMessage(String message) {
		Log.i("websocket","thread:"+Thread.currentThread());
		Log.i("websocket","received: \""+message+"\"");
		Bundle bundle = new Bundle();
		bundle.putString("type","message");
		bundle.putString("json", message);
		
		Message msg = new Message();
		msg.setData(bundle);
		pigeon.dispatchMessage(msg);
	}

	@Override
	public void onOpen() {
		Log.i("websocket","open");
	}

	@Override
	public void onClose() {
		Log.i("websocket","close");
	}

	@Override
	public void onIOError(IOException ex) {
		Log.i("websocket",""+ex);
	}

}
