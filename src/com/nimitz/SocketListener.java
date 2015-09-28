package com.nimitz;


import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class SocketListener implements Runnable {
	
	protected Socket listener;
	protected MessageHandler handler;
	private boolean running = false;
	protected Object locker = new Object();
	
	public void init(Socket listener, MessageHandler handler) {
		this.listener = listener;
		this.handler = handler;
		
	}

	public void stop() {
		synchronized(locker) {
			running = false;
			try {
				listener.close();
			} catch(IOException e) { }
		}
		
	}
	
	protected boolean isRunning() {
		synchronized(locker) {
			return running;
		}
	}
	
	protected void setRunning(boolean value) {
		synchronized(locker) {
			running = value;
		}
	}
	
	@Override
	public void run()  {
		
		setRunning(true);
		byte[] buffer = new byte[1024];
		try {
			DataInputStream stream = new DataInputStream(listener.getInputStream());
			int bytesRead = -1;
			
			String data = "";
			while(isRunning() && (bytesRead = stream.read(buffer)) != -1) {
				
				data +=  handler.dispatch(buffer, bytesRead);
				if(handler.endOfMessage(data)) {
					
					handler.handleMessage(data);
					data = "";
				}
			}
				
			
		} catch (IOException e) {
			if(isRunning()) {
				handler.handleReadError(e);	
			}
		}
	}
	
	
	
}
