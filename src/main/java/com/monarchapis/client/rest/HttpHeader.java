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

import org.apache.commons.lang3.StringUtils;

public final class HttpHeader {
	/** Http header name. */
	private final String name;

	/** Http header value. */
	private final String value;

	/**
	 * Creates a key-value pair used to represent an http header.
	 * 
	 * @param name
	 *            http name
	 * @param value
	 *            http value
	 */
	public HttpHeader(final String name, final String value) {
		if (StringUtils.isBlank(name)) {
			throw new IllegalArgumentException("Name must not be blank.");
		}

		this.name = name;
		this.value = value;
	}

	/**
	 * Gets http header name.
	 * 
	 * @return http header name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets http header value.
	 * 
	 * @return http header value
	 */
	public String getValue() {
		return this.value;
	}
}
