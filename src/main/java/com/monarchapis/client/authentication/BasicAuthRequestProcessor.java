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

import org.apache.commons.codec.binary.Base64;

import com.monarchapis.client.rest.BaseClient;
import com.monarchapis.client.rest.RequestProcessor;

public class BasicAuthRequestProcessor implements RequestProcessor {
	private String username;
	private String password;

	public BasicAuthRequestProcessor(String username, String password) {
		this.username = username;
		this.password = password;
	}

	@Override
	public void processRequest(BaseClient<?> client) {
		client.addHeader("Authorization", "Basic " + Base64.encodeBase64String((username + ":" + password).getBytes()));
	}
}
