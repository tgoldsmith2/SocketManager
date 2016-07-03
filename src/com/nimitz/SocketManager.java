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
 *  	subscribers of the statemonitor could use that information to trigger other requests like reconnect or shutdown
 *  
 * */
 
public class SocketManager implements CommandRunner {

	protected Socket socket;
	
	protected String host;
	protected int port;
	
	protected boolean connected;
	protected Object locker = new Object();
	protected volatile boolean reconnecting = false;
	protected SocketListener listener;
	protected Thread listenerThread;
	
	protected SocketKeepAlive keepAlive;
	
	protected Timer stateMonitorTimer;
	protected StateMonitor stateMonitor;
	
	// these should be made configurable
	protected static int KEEP_ALIVE_PULSE_PERIOD_IN_MS = 5000;
	protected static int MONITOR_CHECKER_SCHEDULE_IN_MS = 7500;
	
	// this class maintains references to a keep alive pulse and state monitor which 
	// is attached to that pulse. 
	// it would be better if these three things were all more detached from the socketmanager.
	
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
		} catch(Exception e) { 
			// log me
			e.printStackTrace();
		}
		stateMonitor = new StateMonitor(new ErrorHandler());
			

		listener = new SocketListener();
		listener.init(socket, getMessageHandler());
		
		listenerThread = new Thread(listener);
		
		SocketCommand apexKeepAliveCommand = new ApexSocketCommand();
		// get rid of magic strings and make an enum with all various commands
		apexKeepAliveCommand.setCommand("06zs004D");
		keepAlive = new SocketKeepAlive(this, apexKeepAliveCommand,KEEP_ALIVE_PULSE_PERIOD_IN_MS);
		keepAlive.init();
		try {
			listenerThread.start();
			keepAlive.run();
			
			stateMonitorTimer = new Timer();
			
			stateMonitorTimer.scheduleAtFixedRate(stateMonitor, MONITOR_CHECKER_SCHEDULE_IN_MS, MONITOR_CHECKER_SCHEDULE_IN_MS);
			
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
	
	protected boolean isConnecting() {
		if(reconnecting) {
			System.out.println("reconnect blocked");
			return true;
		} 
		return false;
	}
	
	public void reconnect() {
		
		if(isConnecting()) {
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
					// do something about this. if reconnect fails / continues to fail there is an issue
					e.printStackTrace();
				}
			}
			System.out.println("established");
			reconnecting = false;
			
			start();
		}
	}
	
	@Override
	public void runCommand(SocketCommand command) {
		if(reconnecting) {
			// it's possible to build some kind of queue of missed commands. 
			// at the very least is seams like we should log them
			// you could queue the commands and replay them after reconnected...
			// but that could be bad.
			// Simpler behavior is just to log and drop that command completely
			// perhaps this method could notify the caller of the commands status instead of returning void
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
