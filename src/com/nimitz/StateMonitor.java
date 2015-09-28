package com.nimitz;

import java.util.TimerTask;

public class StateMonitor extends TimerTask {

	private boolean messageReceived = true;
	
	private StateErrorHandler handler;
	
	public StateMonitor(StateErrorHandler handler) {
		this.handler = handler;
	}
	
	@Override
	public void run() {
		if(!getMessageReceived()) {
			handler.handleError();
		} else {
			messageReceived = false;
		}
	}
	
	public synchronized boolean getMessageReceived() {
		return messageReceived;
	}
	
	public synchronized void setMessageReceived(boolean value) {
		messageReceived = value;
	}
	
}
