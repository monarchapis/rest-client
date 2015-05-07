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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

public abstract class BaseClient<T extends BaseClient<T>> {
	private static final Function<Object, String> TO_STRINGS = new Function<Object, String>() {
		@Override
		public String apply(Object value) {
			return String.valueOf(value);
		}
	};

	protected static final String CHARSET = "UTF-8";

	/** The HTTP method. */
	private final String method;

	/** URL that request will be sent to. */
	private final String url;

	/** Http headers to send. */
	private final Map<String, String> paths;

	/** Http headers to send. */
	private final Map<String, List<String>> headers;

	/** Http general parameters to send. */
	private final Map<String, List<String>> parameters;

	/** Http query parameters to send. */
	private final Map<String, List<String>> query;

	/** Http form parameters to send. */
	private final Map<String, List<String>> form;

	protected HttpEntity body;
	private String bodyString;

	/**
	 * Internal method used to build an APIResponse using the specified
	 * HttpResponse object.
	 * 
	 * @param response
	 *            response wrapped inside an APIResponse object
	 * @return api response
	 */
	RestResponse buildResponse(HttpResponse response) throws RestException {
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			String rb = "";

			if (response.getEntity() != null) {
				rb = EntityUtils.toString(response.getEntity());
			}

			HttpHeader[] headers = buildHeaders(response);

			return new RestResponse(statusCode, rb, headers);
		} catch (IOException ioe) {
			throw new RestException(ioe);
		}
	}

	/**
	 * Given an HttpResponse object, this method generates an array of HTTP
	 * headers.
	 * 
	 * @param httpResponse
	 *            used for building HTTP headers
	 * @return array of http headers
	 */
	private static HttpHeader[] buildHeaders(final HttpResponse httpResponse) {
		final Header[] headers = httpResponse.getAllHeaders();

		HttpHeader[] httpHeaders = new HttpHeader[headers.length];
		for (int i = 0; i < headers.length; ++i) {
			final Header header = headers[i];
			final String name = header.getName();
			final String value = header.getValue();
			final HttpHeader httpHeader = new HttpHeader(name, value);
			httpHeaders[i] = httpHeader;
		}

		return httpHeaders;
	}

	/**
	 * Sets headers to the http message.
	 * 
	 * @param request
	 *            http message to set headers for
	 */
	protected void addInternalHeaders(HttpRequestBase request) {
		if (headers.isEmpty()) {
			return;
		}

		final Set<String> keySet = headers.keySet();

		for (final String key : keySet) {
			final List<String> values = headers.get(key);

			for (final String value : values) {
				request.addHeader(key, value);
			}
		}
	}

	public String getMethod() {
		return method;
	}

	public String getPath() {
		String url = this.url;

		for (Entry<String, String> entry : this.paths.entrySet()) {
			url = StringUtils.replace(url, "{" + entry.getKey() + "}", entry.getValue());
		}

		return url;
	}

	/**
	 * Builds the query part of a URL using the UTF-8 encoding.
	 * 
	 * @return query
	 */
	public String getQuery() {
		StringBuilder sb = new StringBuilder();

		try {
			if ("POST".equals(method) || "PUT".equals(method)) {
				appendParameters(sb, this.parameters);
			}

			appendParameters(sb, this.query);
		} catch (UnsupportedEncodingException e) {
			// UTF-8 is a Java supported encoding.
			// This should not occur unless the Java VM is not functioning
			// properly.
			throw new IllegalStateException();
		}

		return sb.toString();
	}

	public String getUrl() {
		String query = "";
		String builtQuery = getQuery();

		if (StringUtils.isNotEmpty(builtQuery)) {
			query = "?" + builtQuery;
		}

		return getPath() + query;
	}

	/**
	 * Builds the form part of a URL using the UTF-8 encoding.
	 * 
	 * @return query
	 */
	protected String buildForm() {
		StringBuilder sb = new StringBuilder();

		try {
			appendParameters(sb, this.parameters);
			appendParameters(sb, this.form);
		} catch (UnsupportedEncodingException e) {
			// UTF-8 is a Java supported encoding.
			// This should not occur unless the Java VM is not functioning
			// properly.
			throw new IllegalStateException();
		}

		return sb.toString();
	}

	private static void appendParameters(StringBuilder sb, Map<String, List<String>> parameters)
			throws UnsupportedEncodingException {
		for (Entry<String, List<String>> entry : parameters.entrySet()) {
			if (sb.length() > 0) {
				sb.append("&");
			}

			final String name = entry.getKey();
			final List<String> values = entry.getValue();

			for (final String value : values) {
				sb.append(URLEncoder.encode(name, CHARSET));
				sb.append("=");
				sb.append(URLEncoder.encode(value, CHARSET));
			}
		}
	}

	/**
	 * Creates a BaseClient with the BaseClient object.
	 * 
	 * @param url
	 *            The URL to send request to
	 */
	public BaseClient(String method, String url) {
		this.paths = new HashMap<String, String>();
		this.headers = new HashMap<String, List<String>>();
		this.parameters = new HashMap<String, List<String>>();
		this.query = new HashMap<String, List<String>>();
		this.form = new HashMap<String, List<String>>();

		this.method = method;
		this.url = url;
	}

	public T setPath(String variable, String value) {
		this.paths.put(variable, value);

		return me();
	}

	public T setPath(String name, DateTime dateTime) {
		return setPath(name, (String) (dateTime != null ? dateTime.toString() : null));
	}

	public T setPath(String name, Object value) {
		return setPath(name, (String) (value != null ? String.valueOf(value) : null));
	}

	/**
	 * Adds general parameter to be sent during http request.
	 * 
	 * <p>
	 * Does not remove any parameters with the same name, thus allowing
	 * duplicates.
	 * </p>
	 * 
	 * @param name
	 *            name of parameter
	 * @param value
	 *            value of parameter
	 * @return a reference to 'this', which can be used for method chaining
	 */
	public T addParameter(String name, String value) {
		if (value == null) {
			return me();
		}

		if (!parameters.containsKey(name)) {
			parameters.put(name, new ArrayList<String>());
		}

		List<String> values = parameters.get(name);
		values.add(value);

		return me();
	}

	public T addParameter(String name, DateTime dateTime) {
		return addParameter(name, (String) (dateTime != null ? dateTime.toString() : null));
	}

	public T addParameter(String name, Object value) {
		return addParameter(name, (String) (value != null ? String.valueOf(value) : null));
	}

	/**
	 * Sets general parameter to be sent during http request.
	 * 
	 * <p>
	 * Removes any parameters with the same name, thus disallowing duplicates.
	 * </p>
	 * 
	 * @param name
	 *            name of parameter
	 * @param value
	 *            value of parameter
	 * @return a reference to 'this', which can be used for method chaining
	 */
	public T setParameter(String name, String value) {
		if (parameters.containsKey(name)) {
			parameters.get(name).clear();
		}

		return addParameter(name, value);
	}

	public T setParameter(String name, DateTime dateTime) {
		return setParameter(name, (String) (dateTime != null ? dateTime.toString() : null));
	}

	public T setParameter(String name, Object value) {
		return setParameter(name, (String) (value != null ? String.valueOf(value) : null));
	}

	/**
	 * Adds query parameter to be sent during http request.
	 * 
	 * <p>
	 * Does not remove any parameters with the same name, thus allowing
	 * duplicates.
	 * </p>
	 * 
	 * @param name
	 *            name of parameter
	 * @param value
	 *            value of parameter
	 * @return a reference to 'this', which can be used for method chaining
	 */
	public T addQuery(String name, String value) {
		if (value == null) {
			return me();
		}

		if (!query.containsKey(name)) {
			query.put(name, new ArrayList<String>());
		}

		List<String> values = query.get(name);
		values.add(value);

		return me();
	}

	public T addQuery(String name, DateTime dateTime) {
		return addQuery(name, (String) (dateTime != null ? dateTime.toString() : null));
	}

	public T addQuery(String name, Object value) {
		return addQuery(name, (String) (value != null ? String.valueOf(value) : null));
	}

	/**
	 * Sets query parameter to be sent during http request.
	 * 
	 * <p>
	 * Removes any parameters with the same name, thus disallowing duplicates.
	 * </p>
	 * 
	 * @param name
	 *            name of parameter
	 * @param value
	 *            value of parameter
	 * @return a reference to 'this', which can be used for method chaining
	 */
	public T setQuery(String name, String value) {
		if (query.containsKey(name)) {
			query.get(name).clear();
		}

		return addQuery(name, value);
	}

	public T setQuery(String name, DateTime dateTime) {
		return setQuery(name, (String) (dateTime != null ? dateTime.toString() : null));
	}

	public T setQuery(String name, Object value) {
		return setQuery(name, (String) (value != null ? String.valueOf(value) : null));
	}

	public T addQueryCollection(String name, Collection<?> values, CollectionFormat format) {
		if (values != null && !values.isEmpty()) {
			char delimiter = format.getDelimiter();

			if (delimiter == (char) 0) {
				for (Object value : values) {
					addQuery(name, value);
				}
			} else {
				String value = StringUtils.join(Collections2.transform(values, TO_STRINGS), delimiter);
				addQuery(name, value);
			}
		}

		return me();
	}

	/**
	 * Adds form parameter to be sent during http request.
	 * 
	 * <p>
	 * Does not remove any parameters with the same name, thus allowing
	 * duplicates.
	 * </p>
	 * 
	 * @param name
	 *            name of parameter
	 * @param value
	 *            value of parameter
	 * @return a reference to 'this', which can be used for method chaining
	 */
	public T addForm(String name, String value) {
		if (value == null) {
			return me();
		}

		if (!form.containsKey(name)) {
			form.put(name, new ArrayList<String>());
		}

		List<String> values = form.get(name);
		values.add(value);

		return me();
	}

	public T addForm(String name, DateTime dateTime) {
		return addForm(name, (String) (dateTime != null ? dateTime.toString() : null));
	}

	public T addForm(String name, Object value) {
		return addForm(name, (String) (value != null ? String.valueOf(value) : null));
	}

	public T addForm(String name, InputStream value) {
		throw new UnsupportedOperationException("adding files is not supported");
	}

	/**
	 * Sets query parameter to be sent during http request.
	 * 
	 * <p>
	 * Removes any parameters with the same name, thus disallowing duplicates.
	 * </p>
	 * 
	 * @param name
	 *            name of parameter
	 * @param value
	 *            value of parameter
	 * @return a reference to 'this', which can be used for method chaining
	 */
	public T setForm(String name, String value) {
		if (form.containsKey(name)) {
			form.get(name).clear();
		}

		return addForm(name, value);
	}

	public T setForm(String name, DateTime dateTime) {
		return setForm(name, (String) (dateTime != null ? dateTime.toString() : null));
	}

	public T setForm(String name, Object value) {
		return setForm(name, (String) (value != null ? String.valueOf(value) : null));
	}

	public T setForm(String name, InputStream value) {
		throw new UnsupportedOperationException("setting files is not supported");
	}

	/**
	 * Adds http header to be sent during http request.
	 * 
	 * <p>
	 * Does not remove any headers with the same name, thus allowing duplicates.
	 * </p>
	 * 
	 * @param name
	 *            name of header
	 * @param value
	 *            value of header
	 * @return a reference to 'this', which can be used for method chaining
	 */
	public T addHeader(String name, String value) {
		if (value == null) {
			return me();
		}

		if (!headers.containsKey(name)) {
			headers.put(name, new ArrayList<String>());
		}

		List<String> values = headers.get(name);
		values.add(value);

		return me();
	}

	public T addHeader(String name, DateTime dateTime) {
		return addHeader(name, (String) (dateTime != null ? dateTime.toString() : null));
	}

	public T addHeader(String name, Object value) {
		return addHeader(name, (String) (value != null ? String.valueOf(value) : null));
	}

	/**
	 * Sets http header to be sent during http request.
	 * 
	 * <p>
	 * Does not remove any headers with the same name, thus allowing duplicates.
	 * </p>
	 * 
	 * @param name
	 *            name of header
	 * @param value
	 *            value of header
	 * @return a reference to 'this', which can be used for method chaining
	 */
	public T setHeader(String name, String value) {
		if (headers.containsKey(name)) {
			headers.get(name).clear();
		}

		return addHeader(name, value);
	}

	public T setHeader(String name, DateTime dateTime) {
		return addHeader(name, (String) (dateTime != null ? dateTime.toString() : null));
	}

	public T setHeader(String name, Object value) {
		return addHeader(name, (String) (value != null ? String.valueOf(value) : null));
	}

	public String getHeader(String name) {
		List<String> list = headers.get(name);

		return list != null ? list.get(0) : null;
	}

	public T setBody(String body) {
		try {
			if (StringUtils.isNotEmpty(body)) {
				this.body = new StringEntity(body, CHARSET);
				bodyString = body;
			} else {
				this.body = null;
				bodyString = null;
			}
		} catch (Exception uee) {
			throw new IllegalStateException();
		}

		return me();
	}

	public String getBody() throws IOException {
		if (bodyString != null) {
			return bodyString;
		} else {
			return buildForm();
		}
	}

	@SuppressWarnings("deprecation")
	public T setBody(File file, String contentType) throws RestException {
		this.body = new FileEntity(file, contentType);

		return me();
	}

	public T accepts(String mimeType) {
		setHeader("Accept", mimeType);

		return me();
	}

	public T contentType(String mimeType) {
		setHeader("Content-Type", mimeType);

		return me();
	}

	public String getContentType() {
		String contentType = getHeader("Content-Type");

		if (contentType == null) {
			contentType = ContentType.APPLICATION_FORM_URLENCODED.getMimeType();
		}

		return contentType;
	}

	public T authorization(String authorization) {
		setHeader("Authorization", authorization);

		return me();
	}

	@SuppressWarnings("unchecked")
	private T me() {
		return (T) this;
	}

	protected HttpRequestBase prepareRequest() {
		String method = getMethod();
		String url = getUrl();

		HttpRequestBase request = null;

		if ("GET".equals(method)) {
			request = new HttpGet(url);
		} else if ("POST".equals(method)) {
			HttpPost post = new HttpPost(url);
			setEntity(post);
			request = post;
		} else if ("PUT".equals(method)) {
			HttpPut put = new HttpPut(url);
			setEntity(put);
			request = put;
		} else if ("DELETE".equals(method)) {
			request = new HttpDelete(url);
		}

		addInternalHeaders(request);

		return request;
	}

	protected void setEntity(HttpEntityEnclosingRequestBase request) {
		if (body != null) {
			request.setEntity(body);
		} else {
			String form = buildForm();
			bodyString = form;

			if (StringUtils.isNotBlank(form)) {
				request.setEntity(new StringEntity(form, ContentType.APPLICATION_FORM_URLENCODED));
			} else {
				throw new RestException("No body was specified.");
			}
		}
	}

	public enum CollectionFormat {
		CSV(','), SSV(' '), TSV('\t'), PIPES('|'), MULTI((char) 0);

		private char delimiter;

		CollectionFormat(char delimiter) {
			this.delimiter = delimiter;
		}

		char getDelimiter() {
			return delimiter;
		}
	}
}
