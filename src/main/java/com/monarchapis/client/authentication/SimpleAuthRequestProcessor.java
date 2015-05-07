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

package com.monarchapis.client.authentication;

import org.apache.commons.lang3.StringUtils;

import com.monarchapis.client.rest.BaseClient;
import com.monarchapis.client.rest.RequestProcessor;

public class SimpleAuthRequestProcessor implements RequestProcessor {
	private String apiKey;
	private AccessTokenSource accessTokenSource;

	public SimpleAuthRequestProcessor(String apiKey) {
		this(apiKey, null);
	}

	public SimpleAuthRequestProcessor(String apiKey, AccessTokenSource accessTokenSource) {
		if (StringUtils.isBlank(apiKey)) {
			throw new IllegalArgumentException("apiKey must not be blank or null");
		}

		this.apiKey = apiKey;
		this.accessTokenSource = accessTokenSource;
	}

	@Override
	public void processRequest(BaseClient<?> client) {
		client.addHeader("X-Api-Key", apiKey);

		if (accessTokenSource != null) {
			String accessToken = accessTokenSource.getAccessToken();

			if (StringUtils.isNotBlank(accessToken)) {
				client.addHeader("Authorization", "Bearer " + accessToken);
			}
		}
	}
}
