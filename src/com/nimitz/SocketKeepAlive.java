package com.nimitz;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SocketKeepAlive {

	private SocketCommand command;
	private CommandRunner commandRunner;
	private long frequencyInMilliseconds;
	
	
	private Object locker = new Object();
	private boolean running = false;
	
	private ScheduledFuture<?> keepAliveHandler; 
	private KeepAlive keepAlive;
	
	private ScheduledExecutorService scheduler; 
	
	public SocketKeepAlive(CommandRunner commandRunner, SocketCommand command, long frequencyInMilliseconds) {
		this.commandRunner = commandRunner;
		this.command = command;
		this.frequencyInMilliseconds = frequencyInMilliseconds;
	}
	
	public void init() {
		scheduler = Executors.newScheduledThreadPool(1);
	}
	
	private boolean isRunning() {
		synchronized(locker) {
			return running;
		}
	}
	
	public void stop() {
		synchronized(locker) {
			running = false;
			keepAliveHandler.cancel(true);
			scheduler.shutdown();
		}
	}
	
	public void run() throws ExecutionException {
		
		keepAlive = new KeepAlive();
		running = true;

		scheduleAndWaitKeepAlive(keepAlive);
		
	}
	
	protected void scheduleAndWaitKeepAlive(Callable<Void> keepAlive) throws ExecutionException {
		keepAliveHandler =
			       schedule(keepAlive, frequencyInMilliseconds);

	}
	
	
	private ScheduledFuture<?> schedule(Callable<Void> keepAlive,long delay) {
		return scheduler.schedule(keepAlive, delay, TimeUnit.MILLISECONDS);
	}
		
	
	private class KeepAlive implements Callable<Void> {

		@Override
		public Void call() throws IOException, ExecutionException {
			commandRunner.runCommand(command);
			
			if(isRunning()) {
				scheduleAndWaitKeepAlive(this);
			}
			return null;
		}
	}
}
