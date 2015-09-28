package com.nimitz;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Timer;

import com.nimitz.apex.ApexSocketCommand;

/************************************************************
 *  
 * 	1.  Need to pull out the keep-alive and state monitor
 *  	and allow them to be configured separately
 * 
 * 
 *  2.  It would be cool if the StateMonitor were more general and just
 *  	reported various information about the socket manager / listener.
 *  		ie, time between successful commands, last command run, current states, etc.
 *  
 *  3.  Then we could generalize then create a state handler which would trigger the
 *  	reconnect of the manager based on the status reported by the monitor
 * */
 
public class SocketManager implements CommandRunner {

	protected Socket socket;
	
	protected String host;
	protected int port;
	
	protected boolean connected;
	protected Object locker = new Object();
	protected boolean reconnecting = false;
	protected SocketListener listener;
	protected Thread listenerThread;
	
	protected SocketKeepAlive keepAlive;
	
	protected Timer stateMonitorTimer;
	protected StateMonitor stateMonitor;
	
	public SocketManager() {
		
	}
	
	protected void setReconnecting(boolean reconnecting) {
		synchronized(locker) {
			reconnecting = true;
		}
	}
	
	protected MessageHandler getMessageHandler() {
		MessageHandler handler = new  DefaultMessageHandler(stateMonitor);
		handler.init(Charset.forName("US-ASCII"), "\n");
		return handler;
	}
	
	protected void init(String host, int port ) {
		this.host = host;
		this.port = port;
	}

	
	public void start()  {
		
		try {
			establishSocket() ;	
		} catch(Exception e) { }
		stateMonitor = new StateMonitor(new ErrorHandler());
			
		
		listener = new SocketListener();
		listener.init(socket, getMessageHandler());
		
		listenerThread = new Thread(listener);
		
		SocketCommand apexCommand = new ApexSocketCommand();
		apexCommand.setCommand("06zs004D");
		keepAlive = new SocketKeepAlive(this, apexCommand,5000);
		keepAlive.init();
		try {
			listenerThread.start();
			keepAlive.run();
			
			stateMonitorTimer = new Timer();
			
			stateMonitorTimer.scheduleAtFixedRate(stateMonitor, 7500, 10000);
			
		} catch(Exception e) { 
			// log me
			e.printStackTrace();
		}
	}
	
	private void stop() {
		try {
			keepAlive.stop();
			listener.stop();
			listenerThread.join();
			
			stateMonitorTimer.cancel();
		} catch(InterruptedException e) {
			// log me
			e.printStackTrace();
		}
	}
	
	protected void establishSocket() throws UnknownHostException, IOException {
		socket = new Socket(host, port);
		connected = !socket.isClosed();
	}
	
	protected boolean blockReconnect() {
		
		synchronized(locker) {
			
			if(reconnecting) {
				try {
					System.out.println("reconnect blocked");
					locker.wait();	
				} catch(InterruptedException e) {
					// log me
				}
				return true;
			} 
			return false;
		}
	}
	
	public void reconnect() {
		
		if(blockReconnect()) {
			return;
		}
		
		synchronized(locker) {
			reconnecting = true;
			connected = false;
			
			stop();
			
			while(!connected || socket.isClosed()) {
				
				try {
					// we should schedule these re-attempts instead of just looping
					establishSocket();
				} catch(IOException e) {
					// log me
					e.printStackTrace();
				}
			}
			System.out.println("established");
			reconnecting = false;
			
			start();
			locker.notify();
		}
	}
	
	@Override
	public void runCommand(SocketCommand command) {
		if(reconnecting) {
			return;
		}
		synchronized(locker) {
			stateMonitor.setMessageReceived(false);
			try {
				DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
				stream.write(command.getCommandBytes());
				stream.flush();
			} catch(Exception e) {
				command.onFailure(e, this);
			}
		}
	}
	
	private class ErrorHandler implements StateErrorHandler {

		@Override
		public void handleError() {
			reconnect();
		}
		
	}
	
}
