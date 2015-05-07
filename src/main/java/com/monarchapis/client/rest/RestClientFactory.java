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

import javax.annotation.PreDestroy;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;

public class RestClientFactory implements Closeable {
	/**
	 * Whether to trust all SSL certificates, which may be used for self-signed
	 * or invalidly-signed certs.
	 */
	private boolean trustAllCerts;

	/** Proxy host to use, if any. */
	private String proxyHost;

	/** Proxy port to use, if any. */
	private Integer proxyPort;

	private int connectionTimeout = 30000;

	private int soTimeout = 30000;

	private int threadCount = Runtime.getRuntime().availableProcessors();

	private int connectionMax = 100;

	// Create an HttpClient with the ThreadSafeClientConnManager.
	// This connection manager must be used if more than one thread will
	// be using the HttpClient.
	private PoolingHttpClientConnectionManager connectionManager;

	private PoolingNHttpClientConnectionManager asyncConnectionManager;

	private CloseableHttpClient client;

	private CloseableHttpAsyncClient asyncClient;

	public RestClientFactory() {
	}

	public RestClientFactory trustAllCerts(boolean trustAllCerts) {
		this.trustAllCerts = trustAllCerts;

		return this;
	}

	public RestClientFactory proxy(String proxyHost, Integer proxyPort) {
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;

		return this;
	}

	public RestClient create(String method, String url) {
		return new RestClient(method, url, createClient());
	}

	public RestAsyncClient createAsync(String method, String url) {
		return new RestAsyncClient(method, url, createAsyncClient());
	}

	@PreDestroy
	public void close() {
		if (client != null) {
			closeSilently(client);
			client = null;
		}

		if (asyncClient != null) {
			closeSilently(asyncClient);
			asyncClient = null;
		}
	}

	private CloseableHttpClient createClient() throws RestException {
		if (client == null) {
			try {
				connectionManager = new PoolingHttpClientConnectionManager();
				connectionManager.setMaxTotal(connectionMax);

				HttpClientBuilder builder = HttpClients.custom().setConnectionManager(connectionManager)
						.setRedirectStrategy(new NoRedirectStrategy());

				builder.setConnectionReuseStrategy(new DefaultConnectionReuseStrategy());
				builder.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());

				if (trustAllCerts) {
					SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
					sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
					// SSLConnectionSocketFactory sslsf = new
					// SSLConnectionSocketFactory(sslContextBuilder.build());

					builder.setSslcontext(sslContextBuilder.build());
					// builder.setSSLSocketFactory(sslsf);

				}

				if (this.proxyHost != null && this.proxyPort != null) {
					HttpHost proxy = new HttpHost(this.proxyHost, this.proxyPort);
					builder.setProxy(proxy);
				}

				client = builder.build();
			} catch (Exception e) {
				throw new RestException(e);
			}
		}

		return client;
	}

	private CloseableHttpAsyncClient createAsyncClient() throws RestException {
		if (asyncClient == null) {
			try {
				// Create I/O reactor configuration
				IOReactorConfig ioReactorConfig = IOReactorConfig.custom().setIoThreadCount(threadCount)
						.setConnectTimeout(connectionTimeout).setSoTimeout(soTimeout).build();

				// Create a custom I/O reactor
				ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);

				asyncConnectionManager = new PoolingNHttpClientConnectionManager(ioReactor);
				asyncConnectionManager.setMaxTotal(connectionMax);

				HttpAsyncClientBuilder builder = HttpAsyncClients.custom().setConnectionManager(asyncConnectionManager)
						.setRedirectStrategy(new NoRedirectStrategy());

				builder.setConnectionReuseStrategy(new DefaultConnectionReuseStrategy());
				builder.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());

				if (trustAllCerts) {
					SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
					sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());

					builder.setSSLContext(sslContextBuilder.build());
				}

				if (this.proxyHost != null && this.proxyPort != null) {
					HttpHost proxy = new HttpHost(this.proxyHost, this.proxyPort);
					builder.setProxy(proxy);
				}

				asyncClient = builder.build();
				asyncClient.start();
			} catch (Exception e) {
				throw new RestException(e);
			}
		}

		return asyncClient;
	}

	private static void closeSilently(Closeable closable) {
		try {
			closable.close();
		} catch (IOException ioe) {
			throw new RestException(ioe);
		}
	}

	private static class NoRedirectStrategy extends DefaultRedirectStrategy {
		@Override
		protected boolean isRedirectable(final String method) {
			return false;
		}
	}

	private static class DefaultConnectionReuseStrategy implements ConnectionReuseStrategy {
		@Override
		public boolean keepAlive(HttpResponse response, HttpContext context) {
			return true;
		}
	}

	private static class DefaultConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {
		@Override
		public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
			// Honor 'keep-alive' header
			HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
			while (it.hasNext()) {
				HeaderElement he = it.nextElement();
				String param = he.getName();
				String value = he.getValue();

				if (value != null && param.equalsIgnoreCase("timeout")) {
					try {
						return Long.parseLong(value) * 1000;
					} catch (NumberFormatException ignore) {
					}
				}
			}

			// HttpHost target = (HttpHost)
			// context.getAttribute(HttpClientContext.HTTP_TARGET_HOST);
			return 60 * 1000;
		}
	};

	public boolean isTrustAllCerts() {
		return trustAllCerts;
	}

	public void setTrustAllCerts(boolean trustAllCerts) {
		this.trustAllCerts = trustAllCerts;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public Integer getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(Integer proxyPort) {
		this.proxyPort = proxyPort;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getSoTimeout() {
		return soTimeout;
	}

	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}

	public int getThreadCount() {
		return threadCount;
	}

	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	public int getConnectionMax() {
		return connectionMax;
	}

	public void setConnectionMax(int connectionMax) {
		this.connectionMax = connectionMax;
	}
}
