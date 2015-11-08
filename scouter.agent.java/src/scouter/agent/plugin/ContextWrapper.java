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

	public void service(String name) {
		ctx.serviceHash = HashUtil.hash(name);
		ctx.serviceName = name;
	}

	public void ip(String ip) {
		ctx.remoteAddr = IPUtil.toBytes(ip);
	}

	public void error(String err) {
		if (ctx.error == 0) {
			ctx.error = DataProxy.sendError(err);
		}
	}
}
