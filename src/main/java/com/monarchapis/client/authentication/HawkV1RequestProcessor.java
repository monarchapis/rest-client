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

import java.net.URI;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import com.monarchapis.client.rest.BaseClient;
import com.monarchapis.client.rest.RequestProcessor;
import com.monarchapis.common.util.HmacUtils;

public class HawkV1RequestProcessor implements RequestProcessor {
	private String apiKey;
	private String sharedSecret;
	private String algorithm;
	private boolean requestPayloadVerification = true;
	private AccessTokenSource accessTokenSource;

	public HawkV1RequestProcessor(String apiKey, String sharedSecret, String algorithm) {
		this(apiKey, sharedSecret, algorithm, null);
	}

	public HawkV1RequestProcessor(String apiKey, String sharedSecret, String algorithm,
			AccessTokenSource accessTokenSource) {
		if (StringUtils.isBlank(apiKey)) {
			throw new IllegalArgumentException("apiKey must not be blank or null");
		}

		if (StringUtils.isBlank(sharedSecret)) {
			throw new IllegalArgumentException("sharedSecret must not be blank or null");
		}

		if (StringUtils.isBlank(algorithm)) {
			throw new IllegalArgumentException("algorithm must not be blank or null");
		}

		this.apiKey = apiKey;
		this.sharedSecret = sharedSecret;
		this.algorithm = algorithm;
		this.accessTokenSource = accessTokenSource;
	}

	@Override
	public void processRequest(BaseClient<?> client) {
		String accessToken = null;

		if (accessTokenSource != null) {
			accessToken = accessTokenSource.getAccessToken();
		}

		String payloadHash = requestPayloadVerification ? getHawkHash(client) : null;
		String header = getHawkHeader(client, accessToken, payloadHash, null);
		client.authorization(header);
	}

	private static String getHawkHash(BaseClient<?> client) {
		try {
			StringBuilder sb = new StringBuilder();
			String httpContent = client.getBody();
			String mimeType = "";
			String content = "";

			if (httpContent != null) {
				mimeType = StringUtils.trimToEmpty(StringUtils.substringBefore(client.getContentType(), ";"));
				content = httpContent;
			}

			sb.append("hawk.1.payload\n");
			sb.append(mimeType);
			sb.append("\n");
			sb.append(content);
			sb.append("\n");

			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(sb.toString().getBytes("UTF-8"));
			return Base64.encodeBase64String(hash);
		} catch (Exception e) {
			throw new RuntimeException("Could not create hawk hash", e);
		}
	}

	private String getHawkHeader(BaseClient<?> client, String accessToken, String payloadHash, String extData) {
		try {
			StringBuilder sb = new StringBuilder();

			long ts = System.currentTimeMillis() / 1000;
			String nonce = RandomStringUtils.randomAlphanumeric(6);

			URI uri = URI.create(client.getUrl());

			System.out.println("HawkV1RequestProcessor::getHawkHeader");

			sb.append("hawk.1.header\n");
			sb.append(ts);
			sb.append("\n");
			sb.append(nonce);
			sb.append("\n");
			sb.append(client.getMethod());
			sb.append("\n");
			sb.append(uri.getRawPath());

			if (uri.getRawQuery() != null) {
				sb.append("?");
				sb.append(uri.getRawQuery());
			}

			sb.append("\n");
			sb.append(uri.getHost());
			sb.append("\n");
			sb.append(uri.getPort());
			sb.append("\n");

			if (payloadHash != null) {
				sb.append(payloadHash);
			}

			sb.append("\n");

			if (extData != null) {
				sb.append(extData);
			}

			sb.append("\n");

			if (accessToken != null) {
				sb.append(apiKey);
				sb.append("\n");
			}

			String stringData = sb.toString();

			String algo = HmacUtils.getHMacAlgorithm(algorithm);
			byte[] key = sharedSecret.getBytes();
			SecretKeySpec signingKey = new SecretKeySpec(key, algo);

			Mac mac256 = Mac.getInstance(algo);
			mac256.init(signingKey);

			// compute the hmac on input data bytes
			byte[] hash = mac256.doFinal(stringData.getBytes("UTF-8"));
			String mac = Base64.encodeBase64String(hash);

			return "Hawk id=\"" + (accessToken != null ? accessToken : apiKey) + "\", ts=\"" + ts + "\", nonce=\""
					+ nonce + "\"" + (payloadHash != null ? ", hash=\"" + payloadHash + "\"" : "")
					+ (extData != null ? ", ext=\"" + extData + "\"," : "") + ", mac=\"" + mac + "\""
					+ (accessToken != null ? ", app=\"" + apiKey + "\"" : "");
		} catch (Exception e) {
			throw new RuntimeException("Could not create hawk header", e);
		}
	}
}
