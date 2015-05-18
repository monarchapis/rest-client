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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

public class RestAsyncClient extends BaseClient<RestAsyncClient> {
	private CloseableHttpAsyncClient client;

	/**
	 * Creates a RESTClient with the RESTConfig object.
	 * 
	 * @param url
	 *            The URL to send request to
	 * @param client
	 *            The HTTP client instance
	 * @see RestConfig
	 */
	public RestAsyncClient(String method, String url, CloseableHttpAsyncClient client) {
		super(method, url);
		this.client = client;
	}

	/**
	 * Sends an HTTP request using the parameters and headers previously set.
	 * 
	 * @throws RestException
	 *             if request was unsuccessful
	 */
	public void send(final Callback<RestResponse> callback) throws RestException {
		HttpRequestBase request = prepareRequest();

		client.execute(request, new FutureCallback<HttpResponse>() {
			@Override
			public void failed(Exception ex) {
				callback.failed(ex);
			}

			@Override
			public void completed(HttpResponse response) {
				RestResponse apiResponse = buildResponse(response);
				callback.completed(apiResponse);
			}

			@Override
			public void cancelled() {
				callback.cancelled();
			}
		});
	}

	public <T> AsyncFuture<T> future(Callback<T> callback) {
		return new AsyncFuture<T>(callback);
	}
}
