package scouter.xtra.java8;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.elasticsearch.client.Node;
import scouter.agent.AgentCommonConstant;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.proxy.IElasticSearchTracer;
import scouter.agent.trace.TraceContext;
import scouter.util.LinkedMap;
import scouter.util.StringUtil;

import java.lang.reflect.Field;

public class ElasticSearchTracer implements IElasticSearchTracer {
    private static final LinkedMap<Class<?>, Field> fieldMap = new LinkedMap<Class<?>, Field>().setMax(30);
    boolean err = false;

    private static final Configure conf = Configure.getInstance();

    @Override
    public String getRequestDescription(TraceContext ctx, Object httpRequestBase0) {
        return getRequestDescription0(httpRequestBase0, !conf.profile_elasticsearch_full_query_enabled);
    }

    @Override
    public String getNode(TraceContext ctx, Object hostOrNode) {
        if (hostOrNode == null) {
            return "Unknown-ElasticSearch";
        }
        if (hostOrNode instanceof HttpHost) {
            return ((HttpHost) hostOrNode).toHostString();

        } else if (hostOrNode instanceof Node) {
            return ((Node) hostOrNode).getHost().toHostString();
        } else {
            return "Unknown-ElasticSearch";
        }
    }

    @Override
    public Throwable getResponseError(Object httpRequestBase0, Object httpResponse0) {
        if (httpResponse0 instanceof HttpResponse) {
            HttpResponse resp = (HttpResponse) httpResponse0;
            if (resp.getStatusLine() == null) {
                return null;
            }
            if (resp.getStatusLine().getStatusCode() < 400) {
                return null;
            }
            return new RuntimeException(resp.getStatusLine().getStatusCode()
                    + ": " + resp.toString() + ", [REQUEST]" + getRequestDescription0(httpRequestBase0, false));

        } else {
            return null;
        }
    }

    private String getRequestDescription0(Object httpRequestBase0, boolean cut) {
        if (httpRequestBase0 == null) {
            return "No info";
        }
        if (httpRequestBase0 instanceof HttpEntityEnclosingRequestBase) {
            HttpEntityEnclosingRequestBase requestBase = (HttpEntityEnclosingRequestBase) httpRequestBase0;
            String url = requestBase.toString();
            if (cut) {
                return StringUtil.limiting(url, 45);
            }
            HttpEntity entity = requestBase.getEntity();
            if (entity == null) {
                return StringUtil.limiting(url, 45);
            }
            try {
                if (err) {
                    return StringUtil.limiting(url, 45);
                }
                Class<? extends HttpEntity> clazz = entity.getClass();
                Field field = fieldMap.get(clazz);
                if (field == null) {
                    field = clazz.getField(AgentCommonConstant.SCOUTER_ADDED_FIELD);
                    fieldMap.put(clazz, field);
                }
                Object entityDesc = field.get(entity);
                if (entityDesc == null) {
                    return StringUtil.limiting(url, 45);
                } else {
                    String append = entityDesc instanceof byte[] ? new String((byte[]) entityDesc)
                            : entityDesc.toString();
                    return url + ", entity desc: " + append;
                }
            } catch (Exception e) {
                err = true;
                Logger.println("G177p", "error, so skip it later.", e);
                return StringUtil.limiting(url, 45);
            }

        } else {
            String url = httpRequestBase0.toString();
            if (cut) {
                return StringUtil.limiting(url, 45);
            }
            return url;
        }
    }
}
