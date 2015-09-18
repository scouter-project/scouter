/*
 *  Copyright 2015 LG CNS.
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

import java.lang.reflect.Field;
import java.sql.Connection;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.netio.data.DataProxy;
import scouter.jdbc.DetectConnection;
import scouter.jdbc.JdbcTrace;
import scouter.lang.AlertLevel;
import scouter.lang.step.MessageStep;
import scouter.lang.step.MethodStep;
import scouter.lang.step.SqlStep2;
import scouter.lang.step.SqlXType;
import scouter.util.EscapeLiteralSQL;
import scouter.util.HashUtil;
import scouter.util.IntKeyLinkedMap;
import scouter.util.IntLinkedSet;
import scouter.util.StringUtil;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;

public class TraceSQL {
	public final static int MAX_STRING = 20;

	public static void set(int idx, boolean p) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx != null) {
			ctx.sql.put(idx, Boolean.toString(p));
		}
	}

	public static void set(int idx, int p) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx != null) {
			ctx.sql.put(idx, Integer.toString(p));
		}
	}

	public static void set(int idx, float p) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx != null) {
			ctx.sql.put(idx, Float.toString(p));
		}
	}

	public static void set(int idx, long p) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx != null) {
			ctx.sql.put(idx, Long.toString(p));
		}
	}

	public static void set(int idx, double p) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx != null) {
			ctx.sql.put(idx, Double.toString(p));
		}
	}

	public static void set(int idx, String p) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx != null) {
			if (p == null) {
				ctx.sql.put(idx, "null");
			} else {
				p = StringUtil.truncate(p, MAX_STRING);
				ctx.sql.put(idx, "'" + p + "'");
			}
		}
	}

	public static void set(int idx, Object p) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx != null) {
			if (p == null) {
				ctx.sql.put(idx, "null");
			} else {
				String s = StringUtil.truncate(p.toString(), MAX_STRING);
				ctx.sql.put(idx, s);
			}
		}
	}

	public static void clear(Object o) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx != null) {
			ctx.sql.clear();
		}
	}

	public static Object start(Object o) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx == null) {
			return null;
		}
		SqlStep2 step = new SqlStep2();
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}
		step.xtype = SqlXType.DYNA;

		ctx.sqlActiveArgs = ctx.sql;

		String sql = "unknown";
		sql = ctx.sql.getSql();
		sql = escapeLiteral(sql, step);
		step.param = ctx.sql.toString(step.param);

		if (sql != null) {
			step.hash = DataProxy.sendSqlText(sql);
		}

		ctx.profile.push(step);
		ctx.sqltext = sql;

		return new LocalContext(ctx, step);
	}

	public static Object start(Object o, String sql) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx == null) {
			if (conf.debug_background_sql) {
				Logger.info("BGSQL:" + sql);
			}
			return null;
		}
		SqlStep2 step = new SqlStep2();
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
		step.xtype = SqlXType.STMT;

		ctx.profile.push(step);
		ctx.sqltext = sql;

		return new LocalContext(ctx, step);
	}

	private static class ParsedSql {
		public ParsedSql(String sql, String param) {
			this.sql = sql;
			this.param = param;
		}

		String sql;
		String param;
	}

	private static IntLinkedSet noLiteralSql = new IntLinkedSet().setMax(10000);
	private static IntKeyLinkedMap<ParsedSql> checkedSql = new IntKeyLinkedMap<ParsedSql>().setMax(1001);

	private static String escapeLiteral(String sql, SqlStep2 step) {
		if (conf.profile_sql_escape == false)
			return sql;
		int sqlHash = sql.hashCode();
		if (noLiteralSql.contains(sqlHash)) {
			return sql;
		}
		ParsedSql psql = checkedSql.get(sqlHash);
		if (psql != null) {
			step.param = psql.param;
			return psql.sql;
		}
		EscapeLiteralSQL els = new EscapeLiteralSQL(sql);
		els.process();
		String parsed = els.getParsedSql();
		if (parsed.hashCode() == sqlHash) {
			noLiteralSql.put(sqlHash);
		} else {
			psql = new ParsedSql(parsed, els.getParameter());
			checkedSql.put(sqlHash, psql);
			step.param = psql.param;
		}
		return parsed;
	}

	public static void end(Object stat, Throwable thr) {
		if (stat == null) {
			if (conf.debug_background_sql && thr != null) {
				Logger.info("BG-SQL:" + thr);
			}
			return;
		}
		LocalContext lctx = (LocalContext) stat;
		TraceContext tctx = lctx.context;
		SqlStep2 ps = (SqlStep2) lctx.stepSingle;

		ps.elapsed = (int) (System.currentTimeMillis() - tctx.startTime) - ps.start_time;
		if (tctx.profile_thread_cputime) {
			ps.cputime = (int) (SysJMX.getCurrentThreadCPU() - tctx.startCpu) - ps.start_cpu;
		}
		if (thr != null) {
			String msg = thr.toString();
			int hash = DataProxy.sendError(msg);
			if (tctx.error == 0) {
				tctx.error = hash;
			}
			ps.error = hash;

			AlertProxy.sendAlert(AlertLevel.ERROR, "SQL_EXCEPTION", msg);

		} else if (ps.elapsed > conf.alert_sql_time) {
			String msg = "warning slow sql, over " + conf.alert_sql_time + " ms";
			int hash = DataProxy.sendError(msg);
			if (tctx.error == 0) {
				tctx.error = hash;
			}
			AlertProxy.sendAlertSlowSql(AlertLevel.WARN, "SLOW_SQL", msg, tctx.sqltext, ps.elapsed, tctx.txid);
		}

		tctx.sqltext = null;
		tctx.sqlActiveArgs = null;

		tctx.sqlCount++;
		tctx.sqlTime += ps.elapsed;

		tctx.profile.pop(ps);
	}

	public static void prepare(Object o, String sql) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx != null) {
			ctx.sql.clear();
			ctx.sql.setSql(sql);
		}
	}

	public static boolean rsnext(boolean b) {
		TraceContext c = TraceContextManager.getLocalContext();
		if (c != null) {
			if (b) {
				if (c.rs_start == 0) {
					c.rs_start = System.currentTimeMillis();
				}
				c.rs_count++;
			}
		}
		return b;
	}

	private static Configure conf = Configure.getInstance();

	private static void fetch(TraceContext c) {
		MessageStep p = new MessageStep();

		long time = System.currentTimeMillis() - c.rs_start;

		p.start_time = (int) (System.currentTimeMillis() - c.startTime);
		if (c.profile_thread_cputime) {
			p.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - c.startCpu);
		}

		p.message = new StringBuffer(50).append("RESULT ").append(c.rs_count).append(" ").append(time).append(" ms")
				.toString();
		c.profile.add(p);

		if (c.rs_count > conf.alert_fetch_count) {

			String msg = "warning too many resultset, over #" + conf.alert_fetch_count;
			int hash = DataProxy.sendError(msg);
			if (c.error == 0) {
				c.error = hash;
			}
			AlertProxy
					.sendAlertTooManyFetch(AlertLevel.WARN, "TOO_MANY_RESULT", msg, c.serviceName, c.rs_count, c.txid);
		}
	}

	public static void rsclose() {
		TraceContext c = TraceContextManager.getLocalContext();
		if (c != null) {
			if (c.rs_start != 0) {
				fetch(c);
			}
			c.rs_start = 0;
			c.rs_count = 0;
		}
	}

	// JDBC_REDEFINED==false
	public final static String PSTMT_PARAM_FIELD = "_param_";

	public static void set(SqlParameter args, int idx, boolean p) {
		if (args != null) {
			args.put(idx, Boolean.toString(p));
		}
	}

	public static void set(SqlParameter args, int idx, int p) {
		if (args != null) {
			args.put(idx, Integer.toString(p));
		}
	}

	public static void set(SqlParameter args, int idx, float p) {
		if (args != null) {
			args.put(idx, Float.toString(p));
		}
	}

	public static void set(SqlParameter args, int idx, long p) {
		if (args != null) {
			args.put(idx, Long.toString(p));
		}
	}

	public static void set(SqlParameter args, int idx, double p) {
		if (args != null) {
			args.put(idx, Double.toString(p));
		}
	}

	public static void set(SqlParameter args, int idx, String p) {
		if (args != null) {
			if (p == null) {
				args.put(idx, "null");
			} else {
				if (p.length() > MAX_STRING) {
					p = p.substring(0, MAX_STRING);
				}
				args.put(idx, "'" + p + "'");
			}
		}
	}

	public static void set(SqlParameter args, int idx, Object p) {
		if (args != null) {
			if (p == null) {
				args.put(idx, "null");
			} else {
				String s = p.toString();
				if (s.length() > MAX_STRING) {
					s = s.substring(0, MAX_STRING);
				}
				args.put(idx, s);
			}
		}
	}

	public static void clear(Object o, SqlParameter args) {
		if (args != null) {
			args.clear();
		}
	}

	public static Object start(Object o, SqlParameter args) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx == null) {
			if (conf.debug_background_sql && args != null) {
				Logger.info("BG=>" + args.getSql());
			}
			return null;
		}
		SqlStep2 step = new SqlStep2();
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
		step.xtype = SqlXType.PREPARED;

		ctx.profile.push(step);
		ctx.sqltext = sql;
		return new LocalContext(ctx, step);
	}

	public static void prepare(Object o, SqlParameter args, String sql) {
		if (args != null) {
			args.setSql(sql);
		}
		
		// @skyworker : debug code 2015.09.18
		// TraceContext ctx = TraceContextManager.getLocalContext();
		// if (ctx != null) {
		// MessageStep m = new MessageStep();
		// m.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		// m.message =
		// ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace());
		// ctx.profile.add(m);
		// }

	}

	/**
	 * JDBC Wrapper is only available for the DB2 Driver
	 * 
	 * @param url
	 *            : JDBC Connection URL
	 * @return - trace object
	 */
	public static Object startCreateDBC(String url) {
		String name = "CREATE-DBC " + url;
		int hash = DataProxy.sendMethodName(name);
		return TraceSQL.dbcOpenStart(hash, name, null);
	}

	public static Connection endCreateDBC(Connection conn, Object stat) {
		if (conn == null) {
			conn = TraceSQL.dbcOpenEnd(conn, stat);
			return conn;
		}
		conn = TraceSQL.dbcOpenEnd(conn, stat);
		return JdbcTrace.connect(conn);
	}

	public static void endCreateDBC(Object stat, Throwable thr) {
		TraceSQL.dbcOpenEnd(stat, thr);
	}

	public static Object dbcOpenStart(int hash, String msg, Object pool) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx == null)
			return null;

		if (conf.enable_dbopen == false)
			return null;

		MethodStep p = new MethodStep();

		p.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			p.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}

		DBURL dbUrl = getUrl(pool);
		if (dbUrl != unknown) {
			hash = DataProxy.sendMethodName(dbUrl.url);
		}

		p.hash = hash;
		ctx.profile.push(p);

		if (conf.debug_dbopen_fullstack) {
			String stack = ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace(), 2);
			MessageStep ms = new MessageStep(stack);
			ms.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
			ctx.profile.add(ms);
		}

		LocalContext lctx = new LocalContext(ctx, p);
		return lctx;
	}

	static class DBURL {
		int hash;
		String url;

		public DBURL(int hash, String url) {
			this.hash = hash;
			this.url = url;
		}
	}

	static IntKeyLinkedMap<DBURL> urlTable = new IntKeyLinkedMap<DBURL>().setMax(500);
	static DBURL unknown = new DBURL(0, null);

	private static DBURL getUrl(Object pool) {
		if (pool == null)
			return unknown;

		int key = System.identityHashCode(pool);
		DBURL dbUrl = urlTable.get(key);
		if (dbUrl != null) {
			return dbUrl;
		}
		try {
			Field field = pool.getClass().getDeclaredField("url");
			if (field != null) {
				field.setAccessible(true);
				String u = "OPEN-DBC " + field.get(pool);
				dbUrl = new DBURL(HashUtil.hash(u), u);
			}
		} catch (Throwable e) {
		}
		if (dbUrl == null) {
			dbUrl = unknown;
		}
		urlTable.put(key, dbUrl);
		return dbUrl;
	}

	public static java.sql.Connection dbcOpenEnd(java.sql.Connection conn, Object stat) {
		if (stat == null)
			return conn;

		LocalContext lctx = (LocalContext) stat;

		MethodStep step = (MethodStep) lctx.stepSingle;
		if (step == null)
			return conn;
		TraceContext tctx = lctx.context;
		if (tctx == null)
			return conn;

		step.elapsed = (int) (System.currentTimeMillis() - tctx.startTime) - step.start_time;
		if (tctx.profile_thread_cputime) {
			step.cputime = (int) (SysJMX.getCurrentThreadCPU() - tctx.startCpu) - step.start_cpu;
		}
		tctx.profile.pop(step);

		if (conf.debug_dbopen_autocommit) {
			MessageStep ms = null;
			try {
				ms = new MessageStep("AutoCommit : " + conn.getAutoCommit());
			} catch (Exception e) {
				ms = new MessageStep("AutoCommit : " + e);
			}
			ms.start_time = (int) (System.currentTimeMillis() - tctx.startTime);
			tctx.profile.add(ms);
		}

		if (conn instanceof DetectConnection)
			return conn;
		else
			return new DetectConnection(conn);
	}

	public static void dbcOpenEnd(Object stat, Throwable thr) {
		if (stat == null)
			return;

		LocalContext lctx = (LocalContext) stat;

		MethodStep step = (MethodStep) lctx.stepSingle;
		if (step == null)
			return;
		TraceContext tctx = lctx.context;
		if (tctx == null)
			return;

		step.elapsed = (int) (System.currentTimeMillis() - tctx.startTime) - step.start_time;
		if (tctx.profile_thread_cputime) {
			step.cputime = (int) (SysJMX.getCurrentThreadCPU() - tctx.startCpu) - step.start_cpu;
		}

		if (thr != null) {
			String msg = thr.toString();
			int hash = DataProxy.sendError(msg);
			if (tctx.error == 0) {
				tctx.error = hash;
			}

			AlertProxy.sendAlert(AlertLevel.ERROR, "OPEN-DBC", msg);
		}
		tctx.profile.pop(step);
	}

	public static void sqlMap(String methodName, String sqlname) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx == null)
			return;
		MessageStep p = new MessageStep();
		p.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		p.message = new StringBuffer(40).append("SQLMAP ").append(methodName).append(" { ").append(sqlname)
				.append(" }").toString();
		ctx.profile.add(p);

	}

}