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
import java.sql.Statement;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.netio.data.DataProxy;
import scouter.jdbc.DetectConnection;
import scouter.jdbc.JdbcTrace;
import scouter.lang.AlertLevel;
import scouter.lang.ref.INT;
import scouter.lang.step.MessageStep;
import scouter.lang.step.MethodStep;
import scouter.lang.step.SqlStep;
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
		TraceContext c = TraceContextManager.getLocalContext();
		if (c == null) {
			return null;
		}
		SqlStep step = new SqlStep();
		step.start_time = (int) (System.currentTimeMillis() - c.startTime);
		if (c.profile_thread_cputime) {
			step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - c.startCpu);
		}

		c.sqlActiveArgs = c.sql;

		String sql = "unknown";
		sql = c.sql.getSql();
		sql = escapeLiteral(sql, step);
		step.param = c.sql.toString(step.param);

		if (sql != null) {
			step.hash = StringHashCache.getSqlHash(sql);
			DataProxy.sendSqlText(step.hash, sql);
		}
		
		if (conf.debug_jdbc_autocommit) {
			if (o instanceof Statement) {
				MessageStep p = new MessageStep();
				p.start_time = (int) (System.currentTimeMillis() - c.startTime);
				try {
					Connection con = ((Statement) o).getConnection();
					p.message = con.getAutoCommit() ? "AUTOCOMMIT=true" : "AUTOCOMMIT=false";
				} catch (Exception e) {
					p.message = e.toString();
				}
				c.profile.add(p);
			}
		}
		
		c.profile.push(step);
		c.sqltext = sql;
		
		return new LocalContext(c, step);
	}

	public static Object start(Object o, String sql) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx == null) {
			if (conf.debug_background_sql) {
				Logger.info("BGSQL:" + sql);
			}
			return null;
		}
		SqlStep step = new SqlStep();
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}

		if (sql == null) {
			sql = "unknown";
		} else {
			sql = escapeLiteral(sql, step);
		}
		step.hash = StringHashCache.getSqlHash(sql);
		DataProxy.sendSqlText(step.hash, sql);

		
		if (conf.debug_jdbc_autocommit) {
			if (o instanceof Statement) {
				MessageStep p = new MessageStep();
				p.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
				try {
					Connection con = ((Statement) o).getConnection();
					p.message = con.getAutoCommit() ? "AUTOCOMMIT=true" : "AUTOCOMMIT=false";
				} catch (Exception e) {
					p.message = e.toString();
				}
				ctx.profile.add(p);
			}
		}
		
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

	private static String escapeLiteral(String sql, SqlStep step) {
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
			checkedSql.put(sqlHash, new ParsedSql(parsed, els.getParameter()));
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
		SqlStep ps = (SqlStep) lctx.stepSingle;

		ps.elapsed = (int) (System.currentTimeMillis() - tctx.startTime) - ps.start_time;
		if (tctx.profile_thread_cputime) {
			ps.cputime = (int) (SysJMX.getCurrentThreadCPU() - tctx.startCpu) - ps.start_cpu;
		}
		if (thr != null) {
			String msg = thr.toString();
			int hash = StringHashCache.getErrHash(msg);
			if (tctx.error == 0) {
				tctx.error = hash;
			}

			ps.error = hash;
			DataProxy.sendError(hash, msg);
			AlertProxy.sendAlert(AlertLevel.ERROR, "SQL_EXCEPTION", msg);

		} else if (ps.elapsed > conf.alert_sql_time) {
			String msg = "warning slow sql, over " + conf.alert_sql_time + " ms";
			int hash = StringHashCache.getErrHash(msg);

			if (tctx.error == 0) {
				tctx.error = hash;
			}
			DataProxy.sendError(hash, msg);
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
			int hash = StringHashCache.getErrHash(msg);
			if (c.error == 0) {
				c.error = hash;
			}
			DataProxy.sendError(hash, msg);
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
		SqlStep step = new SqlStep();
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
			step.hash = StringHashCache.getSqlHash(sql);
			DataProxy.sendSqlText(step.hash, sql);
		}
		
		if (conf.debug_jdbc_autocommit) {
			if (o instanceof Statement) {
				MessageStep p = new MessageStep();
				p.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
				try {
					Connection con = ((Statement) o).getConnection();
					p.message = con.getAutoCommit() ? "AUTOCOMMIT=true" : "AUTOCOMMIT=false";
				} catch (Exception e) {
					p.message = e.toString();
				}
				ctx.profile.add(p);
			}
		}
		
		ctx.profile.push(step);
		ctx.sqltext = sql;
		return new LocalContext(ctx, step);
	}

	public static void prepare(Object o, SqlParameter args, String sql) {
		if (args != null) {
			args.setSql(sql);
		}
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
		int hash = StringHashCache.getErrHash(name);
		DataProxy.sendMethodName(hash, name);
		return TraceSQL.dbcOpenStart(hash, name, null);
	}

	public static Connection endCreateDBC(Connection conn, Object stat) {
		if (conn == null) {
			TraceSQL.dbcOpenEnd(stat, null);
			return conn;
		}
		TraceSQL.dbcOpenEnd(stat, null);
		return JdbcTrace.connect(conn);
	}

	public static void endCreateDBC(Object stat, Throwable thr) {
		TraceSQL.dbcOpenEnd(stat, thr);
	}

	public static Object dbcOpenStart(int hash, String msg, Object pool) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx == null)
			return null;

		MethodStep p = new MethodStep();

		p.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			p.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}

		DBURL dbUrl = getUrl(pool);
		if (dbUrl != unknown) {
			hash = dbUrl.hash;
			DataProxy.sendMethodName(hash, dbUrl.url);
		}

		p.hash = hash;
		ctx.profile.push(p);

		if (conf.debug_connection_stack) {
			String stack = ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace());
			MessageStep ms = new MessageStep(stack);
			ms.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
			ctx.profile.add(ms);
		}

		LocalContext lctx = new LocalContext(ctx, p);
		lctx.option = new INT(ctx.opencon);
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
			int hash = StringHashCache.getSqlHash(msg);
			if (tctx.error == 0) {
				tctx.error = hash;
			}
			DataProxy.sendError(hash, msg);
			AlertProxy.sendAlert(AlertLevel.ERROR, "OPEN-DBC", msg);
		} else {
			if (((INT) lctx.option).value == tctx.opencon) {
				tctx.opencon++;
			}
		}
		tctx.profile.pop(step);

	}

	public static void dbcClose(int hash, String msg) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx == null)
			return;

		ctx.opencon--;

		MethodStep p = new MethodStep();
		p.hash = hash;
		p.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			p.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}

		ctx.profile.add(p);
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

	public static Connection detectConnection(Connection inner) {
		if (Configure.getInstance().enable_dbc_open_detect == false)
			return inner;
		else
			return new DetectConnection(inner);
	}
}