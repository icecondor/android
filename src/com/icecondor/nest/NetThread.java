package com.icecondor.nest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class NetThread extends Thread implements Handler.Callback {
	Handler handler;
	Socket netSock;
	Listener netThreadListen;

	@Override
	public void run() {
		Log.i("netthread", "Starting NetThread");
		Looper.prepare();
        handler = new Handler(this);
        if(netSock == null || netSock.isConnected() == false) { connect(); }
		Looper.loop();        
    }
	
	Handler getHandler() {return handler;}
	
	public void connect() {
		Log.i("netthread", "NetThread: connecting");
		try {
			InetAddress addr = InetAddress.getByName("donpark.org");
			try {
				netSock = new Socket(addr, 2020);
				Log.i("netthread", "NetThread: connected");
				if (netThreadListen != null && netThreadListen.isAlive()) { netThreadListen.destroy(); }
				netThreadListen = new Listener();
				netThreadListen.setHandler(handler);
				netThreadListen.setSocket(netSock);
				netThreadListen.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}		
	}

	@Override
	public boolean handleMessage(Message msg) {
		Log.i("netthread", "NetThread: handleMessage");
		String str = msg.getData().getString("json");
		Log.i("netthread", "NetThread: writing "+str);
		try {
			netSock.getOutputStream().write(str.getBytes());
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	class Listener extends Thread {
		Handler parentHandler;
		Socket clientSocket;
		public void setHandler(Handler handler) { parentHandler = handler;}
		public void setSocket(Socket socket) { clientSocket = socket; }
		@Override
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				while(true) {
					Log.i("netthread", "NetThreadListener: listening");
					String line_in = reader.readLine();
					Log.i("netthread", "NetThreadListener: read "+line_in);				
					Bundle bundle = new Bundle();
					bundle.putString("json", line_in);
					Log.i("netthread", "NetThreadListener: bundle built");								
					
					Message msg = new Message();
					msg.setData(bundle);
					Log.i("netthread", "NetThreadListener: msg built");								
					//parentHandler.dispatchMessage(msg);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
