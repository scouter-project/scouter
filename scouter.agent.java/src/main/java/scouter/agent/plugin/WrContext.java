package scouter.agent.plugin;

import scouter.agent.netio.data.DataProxy;
import scouter.agent.trace.TraceContext;
import scouter.lang.enumeration.ParameterizedMessageLevel;
import scouter.lang.step.HashedMessageStep;
import scouter.lang.step.MessageStep;
import scouter.lang.step.ParameterizedMessageStep;
import scouter.util.HashUtil;
import scouter.util.SysJMX;

public class WrContext {

	private TraceContext ctx;

	public WrContext(TraceContext ctx) {
		this.ctx = ctx;
	}

	public String service() {
		return ctx.serviceName;
	}

	public void service(String name) {
		if (name == null)
			return;
		ctx.serviceHash = HashUtil.hash(name);
		ctx.serviceName = name;
	}

	public int serviceHash() {
		return ctx.serviceHash;
	}

	public void remoteIp(String ip) {
		ctx.remoteIp = ip;
	}

	public String remoteIp() {
		return ctx.remoteIp==null?"0.0.0.0":ctx.remoteIp;
	}


	public void error(String err) {
		if (ctx.error == 0) {
			ctx.error = DataProxy.sendError(err);
		}
	}

	public boolean isError() {
		return ctx.error != 0;
	}

	public void group(String group) {
		ctx.group = group;
	}

	public String group() {
		return ctx.group;
	}

	public void login(String id) {
		ctx.login = id;
	}

	public String login() {
		return ctx.login;
	}

	public void desc(String desc) {
		ctx.desc = desc;
	}

	public String desc() {
		return ctx.desc;
	}

	public void text1(String text) {
		ctx.text1 = text;
	}

	public String text1() {
		return ctx.text1;
	}

	public void text2(String text) {
		ctx.text2 = text;
	}

	public String text2() {
		return ctx.text2;
	}

	public void text3(String text) {
		ctx.text3 = text;
	}

	public String text3() {
		return ctx.text3;
	}

	public void text4(String text) {
		ctx.text4 = text;
	}

	public String text4() {
		return ctx.text4;
	}

	public void text5(String text) {
		ctx.text5 = text;
	}

	public String text5() {
		return ctx.text5;
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

	public void profile(String msg) {
		MessageStep p = new MessageStep();
		p.message = msg;
		p.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			p.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}
		ctx.profile.add(p);
	}

	/**
	 * add xlog profile
	 * profile display like --> msg #value elapsed
	 * @param msg message
	 * @param value any value to display on a profile.
	 * @param elapsed any value to display on a profile.
     */
	public void hashProfile(String msg, int value, int elapsed) {
		HashedMessageStep step = new HashedMessageStep();
		step.hash = DataProxy.sendHashedMessage(msg);
		step.value = value;
		step.time = elapsed;
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}
		ctx.profile.add(step);
	}

	/**
	 * add xlog profile
	 * profile display like --> #elasped(if the value is not -1) formatted message
	 * @param msg message format (ex- "Hello, my name is %s and my age is %s)"
	 * @param elapsed any value to display on a profile.
	 * @param params message format parameters.
	 */
	public void parameterizedProfile(int level, String msg, int elapsed, String... params) {
		ParameterizedMessageStep step = new ParameterizedMessageStep();
		step.setMessage(DataProxy.sendHashedMessage(msg), params);
		step.setElapsed(elapsed);
		step.setLevel(ParameterizedMessageLevel.of(level));
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);

		if (ctx.profile_thread_cputime) {
			step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}
		ctx.profile.add(step);
	}

	public void parameterizedProfile(String msg, String... params) {
		parameterizedProfile(0, msg, -1, params);
	}

	public void parameterizedProfile(int level, String msg, String... params) {
		parameterizedProfile(level, msg, -1, params);
	}

	public long txid() {
		return ctx.txid;
	}

	public long gxid() {
		return ctx.gxid;
	}
	
	public TraceContext inner(){
		return this.ctx;
	}
}
