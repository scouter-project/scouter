package scouter.xtra.httpclient;

import scouter.agent.Logger;
import scouter.agent.proxy.IHttpClient;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class JavaNetHttpClient implements IHttpClient {

    @Override
    public String getHost(Object o) {
        if (o instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) o;
            if (httpRequest.uri().getPort() > 0) {
                return httpRequest.uri().getHost() + ":" + httpRequest.uri().getPort();
            } else {
                return httpRequest.uri().getHost();
            }
        }
        return null;
    }

    @Override
    public void addHeader(Object o, String key, String value) {
        try {
            if (o instanceof HttpRequest.Builder) {
                HttpRequest.Builder builder = (HttpRequest.Builder) o;
                builder.header(key, value);
            }
        } catch (Throwable th) {
            Logger.println("JC-101", th.getMessage(), th);
        }
    }

    @Override
    public String getURI(Object o) {
        if (o instanceof java.net.http.HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) o;
            return httpRequest.uri().getPath();
        }
        return null;
    }

    @Override
    public String getHeader(Object o, String key) {
        if (o instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) o;
            List<String> values = httpRequest.headers().allValues(key);
            if (values != null && values.size() > 0) {
                return values.get(0);
            }
        }
        return null;
    }

    @Override
    public String getResponseHeader(Object o, String key) {
        if (o instanceof HttpResponse) {
            HttpResponse httpResponse = (HttpResponse) o;
            List<String> values = httpResponse.headers().allValues(key);
            if (values != null && values.size() > 0) {
                return values.get(0);
            }
        }
        return null;
    }

    @Override
    public int getResponseStatusCode(Object o) {
        return 0;
    }
}
