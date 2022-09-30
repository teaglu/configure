/****************************************************************************
 * Copyright (c) 2020, 2021 Teaglu, LLC                                     *
 * All Rights Reserved                                                      *
 ****************************************************************************/

package com.teaglu.configure.exception;

public class ApiStatusException extends Exception {
	private static final long serialVersionUID = 1L;

	public ApiStatusException(String message) {
		super(message);
	}
	
	public ApiStatusException(String message, Throwable cause) {
		super(message, cause);
	}
}
