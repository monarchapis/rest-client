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

import java.io.Closeable;
import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;

public class RestClient extends BaseClient<RestClient> {
	private CloseableHttpClient client;

	/**
	 * Creates a RESTClient with the RESTConfig object.
	 * 
	 * @param url
	 *            The URL to send request to
	 * @param client
	 *            The HTTP client instance
	 */
	public RestClient(String method, String url, CloseableHttpClient client) {
		super(method, url);
		this.client = client;
	}

	/**
	 * Sends an HTTP request using the parameters and headers previously set.
	 * 
	 * @return API response
	 * @throws RestException
	 *             if request was unsuccessful
	 */
	public RestResponse send() throws RestException {
		CloseableHttpResponse response = null;

		try {
			HttpRequestBase request = prepareRequest();

			response = client.execute(request);

			RestResponse apiResponse = buildResponse(response);
			return apiResponse;
		} catch (IOException ioe) {
			throw new RestException(ioe);
		} finally {
			closeSilently(response);
		}
	}

	/**
	 * Used to release any resources used by the connection.
	 * 
	 * @param closeable
	 *            Closeable object held by the connection
	 * @throws RestException
	 *             if unable to release connection
	 */
	private static void closeSilently(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException ioe) {
				throw new RestException(ioe);
			}
		}
	}
}
