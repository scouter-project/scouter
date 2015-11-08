package scouter.agent.plugin;

import scouter.agent.netio.data.DataProxy;
import scouter.agent.trace.TraceContext;
import scouter.util.HashUtil;
import scouter.util.IPUtil;

public class ContextWrapper {

	private TraceContext ctx;

	public ContextWrapper(TraceContext ctx) {
		this.ctx = ctx;
	}

	public String service() {
		return ctx.serviceName;
	}

	public void service(String name) {
		if(name==null)
			return;
		ctx.serviceHash = HashUtil.hash(name);
		ctx.serviceName = name;
	}

	public int serviceHash() {
		return ctx.serviceHash;
	}

	public void ip(String ip) {
		ctx.remoteAddr = IPUtil.toBytes(ip);
	}

	public void error(String err) {
		if (ctx.error == 0) {
			ctx.error = DataProxy.sendError(err);
		}
	}

	public void login(String id) {
		ctx.login = id;
	}

	public void desc(String desc) {
		ctx.desc = desc;
	}

	public String httpMethod() {
		return ctx.http_method;
	}

	public String httpQuery() {
		return ctx.http_query;
	}

	public String httpContentType() {
		return ctx.http_content_type;
	}

	public String userAgent() {
		return ctx.userAgentString;
	}
}
