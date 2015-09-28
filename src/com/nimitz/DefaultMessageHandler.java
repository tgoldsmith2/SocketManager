package com.nimitz;

import java.io.IOException;
import java.nio.charset.Charset;

public class DefaultMessageHandler extends MessageHandler {

	StateMonitor stateManager;
	
	public DefaultMessageHandler(StateMonitor stateManager) {
		this.stateManager = stateManager;
	}
	@Override
	public void handleMessage(String message) {
		
		System.out.println(Thread.currentThread().getName() + " " + message);
		stateManager.setMessageReceived(true);
	}

	@Override
	public void handleReadError(IOException e) {
		stateManager.setMessageReceived(false);
	}
	

}
