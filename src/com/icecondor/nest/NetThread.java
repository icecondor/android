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
		Log.i("netthread", "connecting: \""+currentThread().getName()+"\""+" #"+currentThread().getId());
		InetSocketAddress addr;
		Log.i("netthread", "netSock connected:"+netSock.isConnected()+" closed:"+netSock.isClosed());
		while(!netSock.isConnected()) {
			try {
				addr = new InetSocketAddress("donpark.org",2020);
				netSock.connect(addr, 10000);
				Log.i("netthread", "NetThread: connected");
				try {
					Log.i("netthread", "netThreadListen: "+netThreadListen);
					netThreadListen = new Listener();
					netThreadListen.setHandler(handler);
					netThreadListen.setSocket(netSock);
					netThreadListen.start();
				} catch (Exception e) {
					Log.i("netthreadlistenlauncher", e.toString());
				}
			} catch (IOException e) {
				Log.i("netthread", "connect \""+currentThread().getName()+"\""+" #"+currentThread().getId()+" err: "+e);
				// hold your horses
				try {netSock.close(); netSock = new Socket();} catch (IOException e1) {}
				try {sleep(10000);} catch (InterruptedException e1) {}
			}
		}
		Log.i("netthread", "netSock finished connected:"+netSock.isConnected()+" closed:"+netSock.isClosed());
	}

	@Override
	public boolean handleMessage(Message msg) {
		Log.i("netthread", "handleMessage: \""+currentThread().getName()+"\""+" #"+currentThread().getId());
		String type = msg.getData().getString("type");
		if(type.equals("socket")) {
			Log.i("netthread", "handleMessage: socket closed");	
			netSock = new Socket();
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
			Log.i("netthread", "write err: "+e);
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
