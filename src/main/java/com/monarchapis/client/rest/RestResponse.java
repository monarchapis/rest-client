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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Immutable class that holds API response information.
 */
public class RestResponse {

	/** HTTP status code. */
	private final int statusCode;

	/** HTTP response body. */
	private final String responseBody;

	/** Array of HTTP headers. */
	private final List<HttpHeader> headers;

	/**
	 * Creates an API response with the specified status code, response body,
	 * and http headers.
	 * 
	 * @param statusCode
	 *            status code
	 * @param responseBody
	 *            response body
	 * @param headers
	 *            http headers
	 */
	public RestResponse(int statusCode, String responseBody, HttpHeader[] headers) {
		this.statusCode = statusCode;
		this.responseBody = responseBody;
		this.headers = Collections.unmodifiableList(Arrays.asList(headers));
	}

	/**
	 * Gets HTTP status code.
	 * 
	 * @return http status code
	 */
	public int getStatusCode() {
		return this.statusCode;
	}

	/**
	 * Gets HTTP response body.
	 * 
	 * @return http response body
	 */
	public String getResponseBody() {
		return this.responseBody;
	}

	/**
	 * Gets an array of all http headers returned.
	 * 
	 * <p>
	 * <strong>NOTE</strong>: Use <code>getHeader()</code> to obtain a specific
	 * header instead of this function, for performance reasons.
	 * </p>
	 * 
	 * @return array of http headers
	 * @see #getHeader(String)
	 */
	public List<HttpHeader> getAllHeaders() {
		return headers;
	}

	/**
	 * Gets the the value of the specified http header name or <tt>null</tt> if
	 * none is found.
	 * 
	 * @param name
	 *            header name
	 * @return http header value
	 */
	public String getHeader(String name) {
		// Linear search is used for finding value.
		// Although asympotically this is O(n), where n is the number of
		// headers, linear search is in practice faster for a sufficiently
		// small array than an algorithm that would require building a data
		// structure.
		for (HttpHeader header : headers) {
			if (header.getName().equals(name)) {
				return header.getValue();
			}
		}

		return null;
	}
}