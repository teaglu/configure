package com.teaglu.configure.exception;

/**
 * ConfigException
 * 
 * Exception thrown when configuration information is incorrect
 */
public class ConfigException extends Exception {
	private static final long serialVersionUID = 1L;

	public ConfigException(String message) {
		super(message);
	}
	public ConfigException(String message, Throwable cause) {
		super(message, cause);
	}
}
