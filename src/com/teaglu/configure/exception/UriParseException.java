package com.teaglu.configure.exception;

public class UriParseException extends ConfigException {
	private static final long serialVersionUID = 1L;

	public UriParseException(String message) {
		super(message);
	}
	
	public UriParseException(String message, Throwable cause) {
		super(message, cause);
	}
}
