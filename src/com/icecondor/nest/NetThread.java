package com.icecondor.nest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class NetThread extends Thread implements Handler.Callback {
	Handler handler;
	Socket netSock = new Socket();
	Listener netThreadListen;

	NetThread() {
		super("NetThread");		
	}
	
	@Override
	public void run() {
		Log.i("netthread", "run: "+currentThread());
		Looper.prepare();
        handler = new Handler(this);
        connect();
		Log.i("netthread", "Looping");
		Looper.loop();        
    }
	
	Handler getHandler() {return handler;}
	
	public void connect() {
		InetSocketAddress addr = new InetSocketAddress("donpark.org",2020);
		if (netSock.isConnected()) { try {netSock.close();} catch (IOException e) {}}
		if (netSock.isClosed()) { netSock = new Socket(); }
		while(!netSock.isConnected()) {
			try {
				Log.i("netthread", "connecting: \""+currentThread().getName()+"\""+" #"+currentThread().getId());
				netSock.connect(addr);
				Log.i("netthread", "NetThread: connected");
				if (netThreadListen != null && netThreadListen.isAlive()) { netThreadListen.destroy(); }
				netThreadListen = new Listener();
				netThreadListen.setHandler(handler);
				netThreadListen.setSocket(netSock);
				netThreadListen.start();
			} catch (IOException e) {
				Log.i("netthread", e.toString());
				// hold your horses
				try {sleep(10000);} catch (InterruptedException e1) {}
			}
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		Log.i("netthread", "handleMessage: \""+currentThread().getName()+"\""+" #"+currentThread().getId());
		String type = msg.getData().getString("type");
		if(type.equals("socket")) {
			Log.i("netthread", "handleMessage: socket closed");			
			connect();
		} else if (type.equals("message")) {
			String str = msg.getData().getString("json");
			Log.i("netthread", "handleMessage: dispatch: "+str);
		}
		return true;
	}
	
	public boolean write(String str) {
		Log.i("netthread", "write: \""+currentThread().getName()+"\""+" #"+currentThread().getId());
		try {
			Log.i("netthread", "write: "+str);
			netSock.getOutputStream().write(str.getBytes());
			return true;
		} catch (IOException e) {
			Log.i("netthread", e.toString());
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
				Log.i("netthread", "NetThreadListener: clientSocket "+clientSocket);		
				String line_in;
				while((line_in = reader.readLine()) != null) {
					Log.i("netthread", "NetThreadListener: read "+line_in);				
					Bundle bundle = new Bundle();
					bundle.putString("type","message");
					bundle.putString("json", line_in);
					
					Message msg = new Message();
					msg.setData(bundle);
					parentHandler.dispatchMessage(msg);
				}
				Log.i("netthread", "NetThreadListener: Shutting down");
				Bundle obundle = new Bundle();
				obundle.putString("type","socket");
				obundle.putString("socket","closed");
				Message omsg = new Message();
				omsg.setData(obundle);
				parentHandler.dispatchMessage(omsg);
			} catch (Exception e) {
				Log.i("netthreadlistener", e.toString());
			}
		}
	}
}
