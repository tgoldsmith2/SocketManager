package com.nimitz;

public class Application {

	public static void main(String[] args) throws InterruptedException {
		SocketManager manager = new SocketManager();
		manager.init("127.0.0.1", 2101);
		manager.start();
		

	}

}
