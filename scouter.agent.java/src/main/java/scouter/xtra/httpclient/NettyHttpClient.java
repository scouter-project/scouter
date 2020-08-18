package scouter.xtra.httpclient;

import com.netflix.loadbalancer.Server;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import scouter.agent.proxy.IHttpClient;

public class NettyHttpClient implements IHttpClient {
	public String getHost(Object o) {
		if (o instanceof Server) {
			com.netflix.loadbalancer.Server server = (Server) o;
			return server.getHostPort();
		}
		return null;
	}

	public void addHeader(Object o, String key, String value) {
		if (o instanceof HttpClientRequest) {
			HttpClientRequest req = (HttpClientRequest) o;
			req.getHeaders().addHeader(key, value);
		}
	}

	public java.lang.String getHeader(Object o, java.lang.String key) {
		try {
			if (o instanceof HttpClientRequest) {
				HttpClientRequest req = (HttpClientRequest) o;
				return req.getHeaders().get(key);
			}
		} catch (Exception e) {
		}
		return null;
	}

	public String getResponseHeader(Object o, String key) {
		if (o instanceof HttpClientResponse) {
			HttpClientResponse res = (HttpClientResponse) o;
			String headerValue = res.getHeaders().get(key);
			return headerValue;
		}
		return null;
	}

	public int getResponseStatusCode(Object o) {
		if (o instanceof HttpClientResponse) {
			HttpClientResponse res = (HttpClientResponse) o;
			return res.getStatus().code();
		}
		return 0;
	}


	public String getURI(Object o) {
		if (o instanceof HttpClientRequest) {
			HttpClientRequest req = (HttpClientRequest) o;
			return req.getUri();
		}
		return o.toString();
	}
}
