package scouter.xtra.httpclient;

import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import scouter.agent.proxy.IHttpClient;

import com.netflix.loadbalancer.Server;

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

	public String getURI(Object o) {
		if (o instanceof HttpClientRequest) {
			HttpClientRequest req = (HttpClientRequest) o;
			return req.getUri();
		}
		return o.toString();
	}
}
