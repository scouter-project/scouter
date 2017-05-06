/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

package scouter.agent.trace;

import scouter.lang.step.ApiCallStep;
import scouter.lang.step.DumpStep;
import scouter.lang.step.SqlStep;
import scouter.util.IntKeyMap;
import scouter.util.SysJMX;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class TraceContext {
	private boolean isSummary;
	public boolean isStaticContents;

	protected TraceContext() {
	}

	public TraceContext(boolean profile_summary) {
		this.isSummary = profile_summary;
		if (profile_summary) {
			this.profile = new ProfileSummary(this);
		} else {
			this.profile = new ProfileCollector(this);
		}
	}

	public TraceContext parent;
	public long txid;
	public Thread thread;
	public long threadId;
	public long gxid;

	// profile
	public IProfileCollector profile;
	public long startTime;
	public long startCpu;
	public long latestCpu;

	public long bytes;
	public long latestBytes;
	public int status;

	// service
	public byte xType;

	public int serviceHash;
	public String serviceName;
	public String remoteIp;
	public String threadName;
	
	public int error;
	//public boolean done_http_service;
	public String http_method;
	public String http_query;
	public String http_content_type;

	// sql
	public int sqlCount;
	public int sqlTime;
	public String sqltext;

	// apicall
	public String apicall_name;
	public int apicall_count;
	public int apicall_time;
	public String apicall_target;

	//thread dispatch
	public String lastThreadCallName;

	// rs
	public long rs_start;
	public int rs_count;
	final public SqlParameter sql = new SqlParameter();
	public SqlParameter sqlActiveArgs;

	public long userid;
	public int userAgent;
	public String userAgentString;
	public int referer;

	public boolean profile_thread_cputime;
	public boolean is_child_tx;
	public long caller;
	public long callee;

	public String login;
	public String desc;

	public String text1;
	public String text2;

	public String web_name;
	public int web_time;

	public int userTransaction;
	public IntKeyMap<String> unclosedRsMap = new IntKeyMap<String>();
	public IntKeyMap<String> unclosedStmtMap = new IntKeyMap<String>();;

	public boolean debug_sql_call;
	public String group;

	public SqlStep lastSqlStep;
	public ApiCallStep lastApiCallStep;

    public Queue<DumpStep> temporaryDumpSteps = new LinkedBlockingQueue<DumpStep>(5);
	public boolean hasDumpStack;

	public boolean asyncServletStarted = false;
	public boolean endHttpProcessingStarted = false;
	public Throwable asyncThrowable;

	public ArrayList<String> plcGroupList = new ArrayList<String>();
	public TraceContext createChild() {
		TraceContext child = new TraceContext(this.isSummary);
		child.parent = this;
		child.txid = this.txid;
		child.thread = this.thread;
		child.threadId = this.threadId;
		child.gxid = this.gxid;

		// child.profile = this.profile;

		child.startTime = this.startTime;

		if (this.profile_thread_cputime) {
			child.startCpu = SysJMX.getCurrentThreadCPU();
		}
		child.bytes = this.bytes;
		child.status = this.status;

		child.xType = this.xType;
		child.serviceHash = this.serviceHash;
		child.serviceName = this.serviceName;
		child.remoteIp = this.remoteIp;
		
		child.http_method = this.http_method;
		child.http_query = this.http_query;
		child.http_content_type = this.http_content_type;

		// child.sqlCount = this.sqlCount;
		// child.sqlTime = this.sqlTime;
		// child.sqltext = this.sqltext;
		// child.apicall_name = this.apicall_name;
		// child.apicall_count = this.apicall_count;
		// child.apicall_time = this.apicall_time;

		// child.rs_start = this.rs_start;
		// child.rs_count = this.rs_count;
		// child.sql = this.sql;

		child.userid = this.userid;
		child.userAgent = this.userAgent;
		child.referer = this.referer;
		// child.opencon = this.opencon;
		child.profile_thread_cputime = this.profile_thread_cputime;
		child.is_child_tx = this.is_child_tx;
		child.caller = this.caller;
		child.callee = this.callee;

		return child;
	}

	public void closeChild(TraceContext ctx) {
		if (this.error == 0) {
			this.error = ctx.error;
		}
	}

	public static void main(String[] args) {
		java.lang.reflect.Field[] f = TraceContext.class.getFields();
		for (int i = 0; i < f.length; i++) {
			System.out.println("child." + f[i].getName() + " = this." + f[i].getName() + ";");
		}
	}

}
