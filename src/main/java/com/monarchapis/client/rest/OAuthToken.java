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

/**
 * An immutable OAuthToken object that encapsulates an OAuth 2.0 token, which
 * can be used for accessing protected resources.
 * 
 * <p>
 * This class also offers convenience methods for checking whether the token is
 * expired, and saving/loading token from file in an asynchronous-safe manner.
 * </p>
 * 
 * @see <a href="https://tools.ietf.org/html/rfc6749">OAuth 2.0 Framework</a>
 */
public class OAuthToken {
	/** Access token. */
	private final String accessToken;

	/** Unix timestamp, in seconds, to denote access token expiry. */
	private final long accessTokenExpiry;

	/** Refresh token. */
	private final String refreshToken;

	/** Used to indicate access token does not expire. */
	public static final long NO_EXPIRATION = -1;

	/**
	 * Gets the current time as a Unix timestamp.
	 * 
	 * @return seconds since Unix epoch
	 */
	private static long xtimestamp() {
		return System.currentTimeMillis() / 1000;
	}

	/**
	 * Creates an OAuthToken object with the specified parameters.
	 * 
	 * <p>
	 * <strong>NOTE:</strong> To make an access token never expire, set the
	 * <code>expiresIn</code> parameter to <code>OAuthToken.NO_EXPIRATION</code>
	 * </p>
	 * 
	 * @param accessToken
	 *            access token
	 * @param expiresIn
	 *            time in seconds token expires since <code>creationTime</code>
	 * @param refreshToken
	 *            refresh token
	 * @param creationTime
	 *            access token creation time as a Unix timestamp
	 */
	public OAuthToken(String accessToken, long expiresIn, String refreshToken, long creationTime) {

		if (expiresIn == OAuthToken.NO_EXPIRATION) {
			this.accessTokenExpiry = OAuthToken.NO_EXPIRATION;
		} else {
			this.accessTokenExpiry = expiresIn + creationTime;
		}

		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}

	/**
	 * Creates an OAuthToken object with the <code>creationTime</code> set to
	 * the current time.
	 * 
	 * @param accessToken
	 *            access token
	 * @param expiresIn
	 *            time in seconds token expires from current time
	 * @param refreshToken
	 *            refresh token
	 * @see #OAuthToken(String, long, String, long)
	 */
	public OAuthToken(String accessToken, long expiresIn, String refreshToken) {
		this(accessToken, expiresIn, refreshToken, xtimestamp());
	}

	/**
	 * Gets whether the access token is expired.
	 * 
	 * @return <tt>true</tt> if access token is expired, <tt>false</tt>
	 *         otherwise
	 */
	public boolean isAccessTokenExpired() {
		return accessTokenExpiry != NO_EXPIRATION && xtimestamp() >= accessTokenExpiry;
	}

	/**
	 * Gets access token.
	 * 
	 * @return access token
	 */
	public String getAccessToken() {
		return accessToken;
	}

	/**
	 * Gets refresh token.
	 * 
	 * @return refresh token
	 */
	public String getRefreshToken() {
		return refreshToken;
	}
}
