package com.nimitz;

import java.io.IOException;
import java.nio.charset.Charset;

public abstract class SocketCommand {

	private String command;
	
	protected abstract boolean onFailure(Exception e, CommandRunner runner);
	protected abstract Charset getCharset();
	
	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}
	
	public byte[] getCommandBytes() {
		return command.getBytes(getCharset());
	}
}
