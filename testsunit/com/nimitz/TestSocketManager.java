package com.nimitz;

import org.junit.Test;

public class TestSocketManager {

	@Test
	public void test() throws Exception {
		SocketManager manager = new SocketManager();
		manager.init("127.0.0.1", 2101);
		manager.start();
		
		while(true) {
			Thread.sleep(500);
		}
	}

}
