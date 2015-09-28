package com.nimitz.apex;

import java.nio.charset.Charset;

import com.nimitz.CommandRunner;
import com.nimitz.SocketCommand;

public class ApexSocketCommand extends SocketCommand {

	static Charset charset = Charset.forName("US-ASCII");
	
	@Override
	protected boolean onFailure(Exception e, CommandRunner runner) {
		System.out.println("command failure");
		return false;
	}

	@Override
	protected Charset getCharset() {
		return charset;
	}

}
