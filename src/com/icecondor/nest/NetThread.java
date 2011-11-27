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
        handler = new Handler();
        if(netSock.isConnected() == false) { connect(); }
		Looper.loop();        
    }
	
	Handler getHandler() {return handler;}
	
	public void connect() {
		try {
			InetAddress addr = InetAddress.getByName("donpark.org");
			try {
				Log.i("netthread", "NetThread: connecting");
				netSock = new Socket(addr, 2020);
				Log.i("netthread", "NetThread: connected");
				if (netThreadListen != null && netThreadListen.isAlive()) { netThreadListen.destroy(); }
				netThreadListen = new Listener();
				netThreadListen.setHandler(handler);
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
		String str = msg.getData().getString("json");
		Log.i("netthread", "NetThread: writing "+str);
		try {
			netSock.getOutputStream().write(str.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
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
				String line_in = reader.readLine();
				Log.i("netthread", "NetThreadListener: read "+line_in);				
				Bundle bundle = new Bundle();
				bundle.putString("json", line_in);
				Message msg = new Message();
				msg.setData(bundle);
				parentHandler.dispatchMessage(msg);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
