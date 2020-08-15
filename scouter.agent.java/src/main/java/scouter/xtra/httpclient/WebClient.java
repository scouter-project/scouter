package scouter.xtra.httpclient;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import scouter.agent.proxy.IHttpClient;

import java.util.List;

public class WebClient implements IHttpClient {
	public String getHost(Object o) {
		if (o instanceof ClientHttpRequest) {
			ClientHttpRequest chr = (ClientHttpRequest) o;
			return chr.getURI().getHost() + ":" + chr.getURI().getPort();
		}

		return o.toString();
	}

	public void addHeader(Object o, String key, String value) {
		if (o instanceof ClientHttpRequest) {
			ClientHttpRequest chr = (ClientHttpRequest) o;
			HttpHeaders headers = chr.getHeaders();
			headers.set(key, value);
		}
	}

	public String getHeader(Object o, String key) {
		if (o instanceof ClientHttpRequest) {
			ClientHttpRequest chr = (ClientHttpRequest) o;
			List<String> headerValues = chr.getHeaders().get(key);
			if(headerValues != null && headerValues.size() > 0) {
				return headerValues.get(0);
			}
		}
		return null;
	}

	public String getResponseHeader(Object o, String key) {
		if (o instanceof ClientHttpResponse) {
			ClientHttpResponse res = (ClientHttpResponse) o;
			return res.getHeaders().getFirst(key);
		}
		return null;
	}

	public int getResponseStatusCode(Object o) {
		if (o instanceof ClientHttpResponse) {
			ClientHttpResponse res = (ClientHttpResponse) o;
			return res.getRawStatusCode();
		}
		return 0;
	}

	public String getURI(Object o) {
		if (o instanceof ClientHttpRequest) {
			ClientHttpRequest chr = (ClientHttpRequest) o;
			return chr.getURI().getPath();
		}
		return o.toString();
	}
}
