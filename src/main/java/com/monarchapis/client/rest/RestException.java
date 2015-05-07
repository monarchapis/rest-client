/*
 * Copyright (C) 2015 CapTech Ventures, Inc.
 * (http://www.captechconsulting.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.monarchapis.client.rest;

public class RestException extends RuntimeException {
	private static final long serialVersionUID = -6874042744590915838L;

	/** Http status code. */
	private final int statusCode;

	/** Error message. */
	private final String errorMessage;

	/**
	 * Creates a RestException with the specified error message.
	 * 
	 * @param errorMessage
	 *            error message
	 */
	public RestException(final String errorMessage) {
		this(-1, errorMessage);
	}

	/**
	 * Creates a RESTException that Wraps a <code>Throwable</code> object.
	 * 
	 * @param cause
	 *            <code>Throwable</code> object to wrap
	 */
	public RestException(final Throwable cause) {
		super(cause);
		this.statusCode = -1;
		this.errorMessage = cause.getMessage();
	}

	/**
	 * Creates a RestException object with the specified status code and error
	 * message.
	 * 
	 * @param statusCode
	 *            http status code
	 * @param errorMessage
	 *            error message
	 */
	public RestException(final int statusCode, final String errorMessage) {
		super(errorMessage);
		this.statusCode = statusCode;
		this.errorMessage = errorMessage;
	}

	/**
	 * Gets http status code or -1 if there wasn't any.
	 * 
	 * @return status code
	 */
	public int getStatusCode() {
		return this.statusCode;
	}

	/**
	 * Gets error message.
	 * 
	 * @return error message
	 */
	public String getErrorMessage() {
		return this.errorMessage;
	}
}
