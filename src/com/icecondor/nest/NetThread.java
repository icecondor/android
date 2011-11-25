package com.icecondor.nest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class NetThread extends Thread implements Handler.Callback {
	Handler handler;
	Socket netSock;

	@Override
	public void run() {
		Log.i("netthread", "Starting NetThread");
		Looper.prepare();
        handler = new Handler();
		try {
			InetAddress addr = InetAddress.getByName("donpark.org");
			try {
				netSock = new Socket(addr, 2020);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		Looper.loop();        
    }
	
	Handler getHandler() {return handler;}

	@Override
	public boolean handleMessage(Message msg) {
		String str = msg.getData().getString("json");
		try {
			netSock.getOutputStream().write(str.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
