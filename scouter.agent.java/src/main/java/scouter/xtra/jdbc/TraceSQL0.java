/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package scouter.xtra.jdbc;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.proxy.ITraceSQL;
import scouter.agent.trace.LoadedContext;
import scouter.agent.trace.LocalContext;
import scouter.agent.trace.SqlParameter;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.lang.step.HashedMessageStep;
import scouter.lang.step.MessageStep;
import scouter.lang.step.MethodStep;
import scouter.lang.step.SqlStep3;
import scouter.lang.step.SqlXType;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static scouter.agent.trace.TraceSQL.escapeLiteral;

/**
 * Trace SQL
 * @author @author Paul S.J. Kim(sjkim@whatap.io)
 * @author Gun Lee (gunlee01@gmail.com)
 * @author Eunsu Kim
 */
public class TraceSQL0 implements ITraceSQL {
    private static Configure conf = Configure.getInstance();
    private static SQLException slowSqlException = new SLOW_SQL("Slow SQL", "SLOW_SQL");
    private static SQLException tooManyRecordException = new TOO_MANY_RECORDS("TOO_MANY_RECORDS", "TOO_MANY_RECORDS");
    private static SQLException connectionOpenFailException = new CONNECTION_OPEN_FAIL("CONNECTION_OPEN_FAIL", "CONNECTION_OPEN_FAIL");

	@Override
	public Exception getSlowSqlException() {
		return slowSqlException;
	}

	@Override
	public Exception getTooManyRecordException() {
		return tooManyRecordException;
	}

	@Override
	public Exception getConnectionOpenFailException() {
		return connectionOpenFailException;
	}

	@Override
	public Object start(Object o, String sql, byte methodType) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx == null) {
			if (conf._log_background_sql) {
				Logger.println("background: " + sql);
			}
			return null;
		}
		// to debug
		if (conf._profile_fullstack_sql_connection_enabled && ctx.debug_sql_call == false) {
			ctx.debug_sql_call = true;
			StringBuffer sb = new StringBuffer();
			if (o instanceof Statement) {
				try {
					Connection c = ((Statement) o).getConnection();
					sb.append("Connection = ").append(c.getClass().getName()).append("\n");
					sb.append("          ").append(c).append("\n");
					sb.append("          AutoCommit =").append(c.getAutoCommit()).append("\n");
				} catch (Exception e) {
					sb.append(e).append("\n");
				}
			}
			sb.append(ThreadUtil.getThreadStack());
			ctx.profile.add(new MessageStep((int) (System.currentTimeMillis() - ctx.startTime), sb.toString()));
		}
		// Looking for the position of calling SQL COMMIT
		if (conf.profile_fullstack_sql_commit_enabled) {
			if ("commit".equalsIgnoreCase(sql)) {
				ctx.profile.add(new MessageStep((int) (System.currentTimeMillis() - ctx.startTime),
						ThreadUtil.getThreadStack()));
			}
		}
		SqlStep3 step = new SqlStep3();
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}
		if (sql == null) {
			sql = "unknown";
		} else {
			sql = escapeLiteral(sql, step);
		}
		step.hash = DataProxy.sendSqlText(sql);
		step.xtype =(byte)(SqlXType.STMT | methodType);
		ctx.profile.push(step);
		ctx.sqltext = sql;
		return new LocalContext(ctx, step);
	}

	@Override
	public Object start(Object o, SqlParameter args, byte methodType) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx == null) {
			if (conf._log_background_sql && args != null) {
				Logger.println("background: " + args.getSql());
			}
			return null;
		}

		//debug sql call
		if (conf._profile_fullstack_sql_execute_debug_enabled) {
			StringBuffer sb = new StringBuffer();
			if (o instanceof Statement) {
				try {
					Connection c = ((Statement) o).getConnection();
					sb.append(c).append("\n");
					sb.append("Connection = ").append(c.getClass().getName()).append("\n");
					sb.append("AutoCommit = ").append(c.getAutoCommit()).append("\n");
				} catch (Exception e) {
					sb.append(e).append("\n");
				}
			}
			sb.append(ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace(), 2));
			ctx.profile.add(new MessageStep((int) (System.currentTimeMillis() - ctx.startTime), sb.toString()));

		} else if (conf._profile_fullstack_sql_connection_enabled && ctx.debug_sql_call == false) {
			ctx.debug_sql_call = true;
			StringBuffer sb = new StringBuffer();
			if (o instanceof Statement) {
				try {
					Connection c = ((Statement) o).getConnection();
					sb.append(c).append("\n");
					sb.append("Connection = ").append(c.getClass().getName()).append("\n");
					sb.append("AutoCommit = ").append(c.getAutoCommit()).append("\n");
				} catch (Exception e) {
					sb.append(e).append("\n");
				}
			}
			sb.append(ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace(), 2));
			ctx.profile.add(new MessageStep((int) (System.currentTimeMillis() - ctx.startTime), sb.toString()));
		}

		// Looking for the position of calling SQL COMMIT
		if (conf.profile_fullstack_sql_commit_enabled) {
			if ("commit".equalsIgnoreCase(args.getSql())) {
				ctx.profile.add(new MessageStep((int) (System.currentTimeMillis() - ctx.startTime),
						ThreadUtil.getThreadStack()));
			}
		}
		SqlStep3 step = new SqlStep3();
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}
		ctx.sqlActiveArgs = args;
		String sql = "unknown";
		if (args != null) {
			sql = args.getSql();
			sql = escapeLiteral(sql, step);
			step.param = args.toString(step.param);
		}
		if (sql != null) {
			step.hash = DataProxy.sendSqlText(sql);
		}
		step.xtype =(byte)(SqlXType.PREPARED | methodType);
		ctx.profile.push(step);
		ctx.sqltext = sql;
		ctx.currentSqlStep = step;
		return new LocalContext(ctx, step);
	}

	@Override
	public Object driverConnect(Object conn, String url) {
		if (conn == null)
			return conn;
		if (conf.trace_db2_enabled == false)
			return conn;
		if (conn instanceof WrConnection)
			return conn;
		return new WrConnection((Connection) conn);
	}

	@Override
	public Object getConnection(Object conn) {
		if (conn == null)
			return conn;
		if (conn instanceof WrConnection) 
			return conn;
		return new WrConnection((Connection) conn);
	}

	@Override
	public Object dbcOpenEnd(Object conn, Object stat) {
		if (stat == null)
			return conn;
		LocalContext lctx = (LocalContext) stat;
		MethodStep step = (MethodStep) lctx.stepSingle;
		if (step == null)
			return conn;
		TraceContext tctx = lctx.context;
		if (tctx == null)
			return conn;

		Connection conn0 = (Connection) conn;
		step.elapsed = (int) (System.currentTimeMillis() - tctx.startTime) - step.start_time;
		if (tctx.profile_thread_cputime) {
			step.cputime = (int) (SysJMX.getCurrentThreadCPU() - tctx.startCpu) - step.start_cpu;
		}
		tctx.profile.pop(step);
		if (conf.profile_connection_autocommit_status_enabled) {
			HashedMessageStep ms = new HashedMessageStep();
			try {
				ms.hash = DataProxy.sendHashedMessage("AutoCommit : " + conn0.getAutoCommit());
			} catch (Exception e) {
				ms.hash = DataProxy.sendHashedMessage("AutoCommit : " + e);
			}
			ms.start_time = (int) (System.currentTimeMillis() - tctx.startTime);
			if (tctx.profile_thread_cputime) {
				ms.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - tctx.startCpu);
	        }
			tctx.profile.add(ms);
		}
		if (conn instanceof DetectConnection)
			return conn;
		else
			return new DetectConnection(conn0);
	}

	@Override
	public void ctxLookup(Object this1, Object ctx) {
		if (TraceContextManager.isForceDiscarded()) {
			return;
		}

		if (ctx instanceof DataSource) {
			LoadedContext.put(ctx);
		}
	}
}
