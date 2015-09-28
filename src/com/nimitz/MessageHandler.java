package com.nimitz;

import java.io.IOException;
import java.nio.charset.Charset;

public abstract class MessageHandler {

	private Charset charset;
	private String terminator;
	
	
	public void init(Charset charset, String terminator) {
		this.charset = charset;
		this.terminator = terminator;
	}
	
	public abstract void handleMessage(String message);
	public abstract void handleReadError(IOException e);

	public Charset getCharset() {
		return charset;
	}

	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	public String getTerminator() {
		return terminator;
	}

	public void setTerminator(String terminator) {
		this.terminator = terminator;
	}

	public String dispatch(byte[] input, int length) {
		return new String(input, 0, length, charset);
	}
	
	public boolean endOfMessage(String msg) {
		return msg.contains(terminator);
	}
	
	
}
