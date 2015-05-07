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

package com.monarchapis.client.resource;

import java.lang.reflect.Type;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.monarchapis.client.rest.AsyncFuture;
import com.monarchapis.client.rest.BaseClient;
import com.monarchapis.client.rest.Callback;
import com.monarchapis.client.rest.RequestProcessor;
import com.monarchapis.client.rest.RestAsyncClient;
import com.monarchapis.client.rest.RestClient;
import com.monarchapis.client.rest.RestClientFactory;
import com.monarchapis.client.rest.RestException;
import com.monarchapis.client.rest.RestResponse;

public abstract class AbstractResource {
	private static ObjectMapper MAPPER = getObjectMapper();

	private String baseUrl;
	private List<RequestProcessor> requestSigners;
	private RestClientFactory clientFactory;

	public AbstractResource(String baseUrl, RestClientFactory clientFactory) {
		this(baseUrl, clientFactory, null);
	}

	public AbstractResource(String baseUrl, RestClientFactory clientFactory, List<RequestProcessor> requestSigners) {
		baseUrl = StringUtils.removeEnd(baseUrl, "/");

		if (StringUtils.isBlank(baseUrl)) {
			throw new IllegalArgumentException("baseUrl must not be blank or null");
		}

		if (clientFactory == null) {
			throw new IllegalArgumentException("clientFactory must not be null");
		}

		this.baseUrl = baseUrl;
		this.clientFactory = clientFactory;
		this.requestSigners = requestSigners;
	}

	public RestClient newClient(String method, String path) {
		return clientFactory.create(method, getBaseUrl() + path);
	}

	public RestAsyncClient newAsyncClient(String method, String path) {
		return clientFactory.createAsync(method, getBaseUrl() + path);
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setRequestSigner(List<RequestProcessor> requestSigners) {
		this.requestSigners = requestSigners;
	}

	public List<RequestProcessor> getRequestSigners() {
		return requestSigners;
	}

	protected static void require(String argument, String message) {
		if (StringUtils.isBlank(argument)) {
			throw new IllegalArgumentException(message);
		}
	}

	protected static void require(Object argument, String message) {
		if (argument == null) {
			throw new IllegalArgumentException(message);
		}
	}

	protected String convert(String value) {
		return value;
	}

	protected String convert(DateTime dateTime) {
		return dateTime != null ? dateTime.toString() : null;
	}

	protected String convert(Object value) {
		return value != null ? String.valueOf(value) : null;
	}

	protected String toJson(Object value) {
		try {
			return MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new RestException(e);
		}
	}

	protected void signRequest(BaseClient<?> client) {
		if (requestSigners != null) {
			for (RequestProcessor requestSigner : requestSigners) {
				requestSigner.processRequest(client);
			}
		}
	}

	protected <T> T parseAs(RestResponse response, Class<T> clazz) {
		checkStatusCode(response);

		return parseAs(response.getResponseBody(), clazz);
	}

	protected <T> T parseAs(RestResponse response, TypeReference<T> reference) {
		checkStatusCode(response);

		return parseAs(response.getResponseBody(), reference);
	}

	protected static <T> T parseAs(String response, Class<T> clazz) {
		try {
			return MAPPER.readValue(response, clazz);
		} catch (Exception e) {
			throw new RestException(e);
		}
	}

	protected static <T> T parseAs(String response, final TypeReference<T> reference) {
		try {
			return MAPPER.readValue(response, new com.fasterxml.jackson.core.type.TypeReference<T>() {
				public Type getType() {
					return reference.getType();
				}
			});
		} catch (Exception e) {
			throw new RestException(e);
		}
	}

	protected void checkStatusCode(RestResponse response) {
		if (response.getStatusCode() >= 400) {
			throwErrorException(response);
		}
	}

	protected abstract void throwErrorException(RestResponse response);

	private static ObjectMapper getObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();

		mapper.registerModule(new JodaModule());
		mapper.registerModule(new GuavaModule());

		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		return mapper;
	}

	protected <T> CallbackAdapter<T> callbackAdapter(final AsyncFuture<T> callback, Class<T> clazz) {
		return new CallbackAdapter<T>(callback, clazz);
	}

	protected <T> CallbackAdapter<T> callbackAdapter(final AsyncFuture<T> callback, TypeReference<T> typeReference) {
		return new CallbackAdapter<T>(callback, typeReference);
	}

	protected static class CallbackAdapter<T> implements Callback<RestResponse> {
		final private AsyncFuture<T> callback;
		final private Class<T> clazz;
		final private TypeReference<T> typeReference;

		public CallbackAdapter(AsyncFuture<T> callback, Class<T> clazz) {
			this.callback = callback;
			this.clazz = clazz;
			this.typeReference = null;
		}

		public CallbackAdapter(AsyncFuture<T> callback, TypeReference<T> typeReference) {
			this.callback = callback;
			this.clazz = null;
			this.typeReference = typeReference;
		}

		@Override
		public void completed(RestResponse response) {
			try {
				T result;

				if (clazz != null) {
					result = parseAs(response.getResponseBody(), clazz);
				} else {
					result = parseAs(response.getResponseBody(), typeReference);
				}

				callback.completed(result);
			} catch (Exception ex) {
				callback.failed(ex);
			}
		}

		@Override
		public void failed(Exception ex) {
			callback.failed(ex);
		}

		@Override
		public void cancelled() {
			callback.cancel(true);
		}
	}
}
