package scouter.xtra.httpclient;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import scouter.agent.Logger;
import scouter.agent.proxy.IHttpClient;

import java.lang.reflect.Method;
import java.util.List;

public class WebClient implements IHttpClient {
	// Spring 6.x 호환성을 위한 reflection 관련 캐시
	private static volatile boolean useReflection = false;
	private static volatile Method getStatusCodeMethod = null;
	private static volatile Method valueMethod = null;

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

			if (useReflection) {
				return getStatusCodeByReflection(res);
			}

			try {
				return res.getRawStatusCode();
			} catch (NoSuchMethodError e) {
				useReflection = true;
				return getStatusCodeByReflection(res);
			}
		}
		return 0;
	}

	/**
	 * Spring 6.x 이상 버전용 - reflection으로 getStatusCode().value() 호출
	 * Method 객체를 캐싱하여 성능 향상
	 */
	private int getStatusCodeByReflection(ClientHttpResponse res) {
		try {
			if (getStatusCodeMethod == null) {
				synchronized (WebClient.class) {
					if (getStatusCodeMethod == null) {
						getStatusCodeMethod = res.getClass().getMethod("getStatusCode");
						getStatusCodeMethod.setAccessible(true);
					}
				}
			}

			Object statusCode = getStatusCodeMethod.invoke(res);
			if (statusCode != null) {
				if (valueMethod == null) {
					synchronized (WebClient.class) {
						if (valueMethod == null) {
							valueMethod = statusCode.getClass().getMethod("value");
							valueMethod.setAccessible(true);
						}
					}
				}

				Object value = valueMethod.invoke(statusCode);
				if (value instanceof Integer) {
					return (Integer) value;
				}
			}
		} catch (Exception ex) {
			Logger.println("X001", "fail to get status code by reflection", ex);
			return 0;
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
