/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */
package scouter.xtra.httpclient;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import scouter.agent.proxy.IHttpClient;

public class HttpClient5 implements IHttpClient {
	public String getHost(Object o) {
		try {
			HttpHost host = (HttpHost) o;
			return host.toHostString();
		} catch (Exception e) {
			return null;
		}
	}

	public void addHeader(Object o, String key, String value) {
		if (o instanceof HttpRequest) {
			HttpRequest req = (HttpRequest) o;
			req.addHeader(key, value);
		}
	}

	public String getHeader(Object o, String key) {
		if (o instanceof HttpRequest) {
			HttpRequest req = (HttpRequest) o;
			Header h = req.getFirstHeader(key);
			if (h != null) {
				return h.getValue();
			}
		}
		return null;
	}

	public String getResponseHeader(Object o, String key) {
		if (o instanceof HttpResponse) {
			HttpResponse res = (HttpResponse) o;
			Header h = res.getFirstHeader(key);
			if (h != null) {
				return h.getValue();
			}
		}
		return null;
	}

	public int getResponseStatusCode(Object o) {
		if (o instanceof HttpResponse) {
			HttpResponse res = (HttpResponse) o;
			return res.getCode();
		}
		return 0;
	}

	public String getURI(Object o) {
		try {
			HttpUriRequest request;
			if (o instanceof HttpUriRequest) {
				HttpUriRequest req = (HttpUriRequest) o;
				return req.getUri().getPath();
			} else if (o instanceof HttpGet) {
				HttpGet req = (HttpGet) o;
				return req.getUri().getPath();
			} else if (o instanceof HttpPut) {
				HttpPut req = (HttpPut) o;
				return req.getUri().getPath();
			} else if (o instanceof HttpRequest) {
				HttpRequest req = (HttpRequest) o;
				return req.getUri().toString();
			}
		} catch (Exception e) {
			return o.toString();
		}
		return o.toString();
	}
}
