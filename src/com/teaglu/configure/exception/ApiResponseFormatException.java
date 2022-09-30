/****************************************************************************
 * Copyright (c) 2020, 2021 Teaglu, LLC                                     *
 * All Rights Reserved                                                      *
 ****************************************************************************/

package com.teaglu.configure.exception;

public class ApiResponseFormatException extends Exception {
	private static final long serialVersionUID = 1L;

	public ApiResponseFormatException(String message) {
		super(message);
	}
	public ApiResponseFormatException(String message, Throwable cause) {
		super(message, cause);
	}
}
