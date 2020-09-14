package scouter.xtra.httpclient;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMessage;
import org.springframework.http.HttpRequest;
import scouter.agent.proxy.IHttpClient;

import java.util.List;

public class SpringRestTemplateHttpRequest implements IHttpClient {
	public String getHost(Object o) {
		if (o instanceof HttpRequest) {
			HttpRequest chr = (HttpRequest) o;
			return chr.getURI().getHost() + ":" + chr.getURI().getPort();
		}

		return o.toString();
	}

	public void addHeader(Object o, String key, String value) {
		if (o instanceof HttpRequest) {
			HttpRequest chr = (HttpRequest) o;
			HttpHeaders headers = chr.getHeaders();
			headers.set(key, value);
		}
	}

	public String getHeader(Object o, String key) {
		if (o instanceof HttpRequest) {
			HttpRequest chr = (HttpRequest) o;
			List<String> headerValues = chr.getHeaders().get(key);
			if(headerValues != null && headerValues.size() > 0) {
				return headerValues.get(0);
			}
		}
		return null;
	}

	public String getResponseHeader(Object o, String key) {
		if (o instanceof HttpMessage) {
			HttpMessage res = (HttpMessage) o;
			return res.getHeaders().getFirst(key);
		}
		return null;
	}

	public int getResponseStatusCode(Object o) {
		return 0;
	}

	public String getURI(Object o) {
		if (o instanceof HttpRequest) {
			HttpRequest chr = (HttpRequest) o;
			return chr.getURI().getPath();
		}
		return o.toString();
	}
}
