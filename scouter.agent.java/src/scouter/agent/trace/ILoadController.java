package scouter.agent.trace;

import scouter.agent.proxy.IHttpTrace;
import scouter.agent.trace.TraceMain.Stat;

public interface ILoadController {
	public void start(TraceContext ctx, Object req, Object res);
	public void end(TraceContext ctx, Object req, Object res);
	public boolean reject(TraceContext ctx, Object req, Object res, IHttpTrace http);

}
