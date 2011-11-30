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

	NetThread() {
		super("NetThread");		
	}
	
	@Override
	public void run() {
		Log.i("netthread", "run: "+currentThread());
		Looper.prepare();
        handler = new Handler(this);
        if(netSock == null || netSock.isConnected() == false) { connect(); }
		Log.i("netthread", "Looping");
		Looper.loop();        
    }
	
	Handler getHandler() {return handler;}
	
	public void connect() {
		Log.i("netthread", "connect: connecting");
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
				Log.i("netthread", e.toString());
			}
		} catch (UnknownHostException e) {
			Log.i("netthread", e.toString());
		}		
	}

	@Override
	public boolean handleMessage(Message msg) {
		Log.i("netthread", "handleMessage: \""+currentThread().getName()+"\""+" #"+currentThread().getId());
		String type = msg.getData().getString("type");
		if(type.equals("socket")) {
			connect();
		} else if (type.equals("message")) {
			String str = msg.getData().getString("json");
			Log.i("netthread", "handleMessage: socket connected is "+netSock.isConnected());			
			return write(str);
		}
		return true;
	}
	
	public boolean write(String str) {
		Log.i("netthread", "write: \""+currentThread().getName()+"\""+" #"+currentThread().getId());
		try {
			if (netSock != null && netSock.isConnected()) {
				Log.i("netthread", "write: "+str);
				netSock.getOutputStream().write(str.getBytes());
				return true;
			} else {
				Log.i("netthread", "write: Socket is not connected");
				Bundle obundle = new Bundle();
				obundle.putString("type","socket");
				obundle.putString("socket","closed");
				Message omsg = new Message();
				omsg.setData(obundle);
				handler.dispatchMessage(omsg);
			}
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
					//parentHandler.dispatchMessage(msg);
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
