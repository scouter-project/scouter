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
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.meter.MeterSQL;
import scouter.agent.error.CONNECTION_OPEN_FAIL;
import scouter.agent.error.SLOW_SQL;
import scouter.agent.error.TOO_MANY_RECORDS;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.plugin.PluginJdbcPoolTrace;
import scouter.agent.summary.ServiceSummary;
import scouter.jdbc.DetectConnection;
import scouter.jdbc.WrConnection;
import scouter.lang.AlertLevel;
import scouter.lang.step.*;
import scouter.util.*;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Trace SQL
 * @author @author Paul S.J. Kim(sjkim@whatap.io)
 * @author Gun Lee (gunlee01@gmail.com)
 * @author Eunsu Kim
 */
public class TraceSQL {
    private static Configure conf = Configure.getInstance();

    public final static int MAX_STRING = 20;

    // JDBC_REDEFINED==false
    public final static String PSTMT_PARAM_FIELD = "_param_";
    private static int RESULT_SET_FETCH = 0;

    private static IntLinkedSet noLiteralSql = new IntLinkedSet().setMax(10000);
    private static IntKeyLinkedMap<ParsedSql> checkedSql = new IntKeyLinkedMap<ParsedSql>().setMax(1001);

    private static SQLException slowSqlException = new SLOW_SQL("Slow SQL", "SLOW_SQL");
    private static SQLException tooManyRecordException = new TOO_MANY_RECORDS("TOO_MANY_RECORDS", "TOO_MANY_RECORDS");
    private static SQLException connectionOpenFailException = new CONNECTION_OPEN_FAIL("CONNECTION_OPEN_FAIL", "CONNECTION_OPEN_FAIL");

    static IntKeyLinkedMap<DBURL> urlTable = new IntKeyLinkedMap<DBURL>().setMax(500);
    static DBURL unknown = new DBURL(0, null);

    public static void set(int idx, boolean p) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null) {
			ctx.sql.put(idx, Boolean.toString(p));
		}
	}

	public static void set(int idx, int p) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null) {
			ctx.sql.put(idx, Integer.toString(p));
		}
	}

	public static void set(int idx, float p) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null) {
			ctx.sql.put(idx, Float.toString(p));
		}
	}

	public static void set(int idx, long p) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null) {
			ctx.sql.put(idx, Long.toString(p));
		}
	}

	public static void set(int idx, double p) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null) {
			ctx.sql.put(idx, Double.toString(p));
		}
	}

	public static void set(int idx, String p) {
		TraceContext ctx = TraceContextManager.getContext();
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
		TraceContext ctx = TraceContextManager.getContext();
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
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null) {
			ctx.sql.clear();
		}
	}

	public static Object start(Object o) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx == null) {
			return null;
		}
		SqlStep3 step = new SqlStep3();
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

	public static Object start(Object o, String sql, byte methodType) {
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
	private static class ParsedSql {
		public ParsedSql(String sql, String param) {
			this.sql = sql;
			this.param = param;
		}
		String sql;
		String param;
	}

    private static String escapeLiteral(String sql, SqlStep3 step) {
		if (conf.profile_sql_escape_enabled == false)
			return sql;
	    try {
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
	    } catch (Throwable t) {
		    Logger.println("B102", "fail to escape literal", t);
	    } finally {
			return sql;
	    }
    }

    public static void end(Object stat, Throwable thr, int updatedCount) {
		if (stat == null) {
			if (conf._log_background_sql && thr != null) {
				Logger.println("BG-SQL:" + thr);
			}
			return;
		}

        LocalContext lCtx = (LocalContext) stat;
        TraceContext tCtx = lCtx.context;

        //Logger.trace("affected row = " + updatedCount);

        SqlStep3 step = (SqlStep3) lCtx.stepSingle;
        tCtx.lastSqlStep = step;

		step.elapsed = (int) (System.currentTimeMillis() - tCtx.startTime) - step.start_time;
		step.updated = updatedCount;
		if (tCtx.profile_thread_cputime) {
			step.cputime = (int) (SysJMX.getCurrentThreadCPU() - tCtx.startCpu) - step.start_cpu;
		}
		if (thr != null) {
			String msg = thr.toString();
			if (conf.profile_fullstack_sql_error_enabled) {
				StringBuffer sb = new StringBuffer();
				sb.append(msg).append("\n");
				ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_max_lines);
				thr = thr.getCause();
				while (thr != null) {
					sb.append("\nCause...\n");
					ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_max_lines);
					thr = thr.getCause();
				}
				msg = sb.toString();
			}
			int hash = DataProxy.sendError(msg);
			if (tCtx.error == 0) {
				tCtx.error = hash;
			}
			step.error = hash;
			ServiceSummary.getInstance().process(thr, hash, tCtx.serviceHash, tCtx.txid, step.hash, 0);
		} else if (step.elapsed > conf.xlog_error_sql_time_max_ms) {
			String msg = "warning slow sql, over " + conf.xlog_error_sql_time_max_ms + " ms";
			int hash = DataProxy.sendError(msg);
			if (tCtx.error == 0) {
				tCtx.error = hash;
			}
			ServiceSummary.getInstance().process(slowSqlException, hash, tCtx.serviceHash, tCtx.txid, step.hash, 0);
		}
		tCtx.sqltext = null;
		tCtx.sqlActiveArgs = null;
		tCtx.sqlCount++;
		tCtx.sqlTime += step.elapsed;
		ServiceSummary.getInstance().process(step);
		MeterSQL.getInstance().add(step.elapsed, step.error != 0);
		tCtx.profile.pop(step);
	}
	public static void prepare(Object o, String sql) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null) {
			ctx.sql.clear();
			ctx.sql.setSql(sql);
		}
	}
	public static boolean rsnext(boolean b) {
		TraceContext c = TraceContextManager.getContext();
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

	private static void fetch(TraceContext c) {
		HashedMessageStep p = new HashedMessageStep();
		long time = System.currentTimeMillis() - c.rs_start;
		p.start_time = (int) (System.currentTimeMillis() - c.startTime);
		if (c.profile_thread_cputime) {
			p.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - c.startCpu);
		}
		if (RESULT_SET_FETCH == 0) {
			RESULT_SET_FETCH = DataProxy.sendHashedMessage("RESULT-SET-FETCH");
		}
		p.hash = RESULT_SET_FETCH;
		p.value = c.rs_count;
		p.time = (int) time;
		c.profile.add(p);
		if (c.rs_count > conf.xlog_error_jdbc_fetch_max) {
			String msg = "warning too many resultset, over #" + conf.xlog_error_jdbc_fetch_max;
			int hash = DataProxy.sendError(msg);
			if (c.error == 0) {
				c.error = hash;
			}
			ServiceSummary.getInstance().process(tooManyRecordException, hash, c.serviceHash, c.txid, 0, 0);
		}
	}

    public static void stmtInit(Object stmt) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx != null) {
            if(conf.trace_stmt_leak_enabled) {
                if(conf.profile_fullstack_stmt_leak_enabled) {
                    ctx.unclosedStmtMap.put(System.identityHashCode(stmt), ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace(), 2));
                } else {
                    ctx.unclosedStmtMap.put(System.identityHashCode(stmt), "");
                }
            }
        }
    }

    public static void stmtClose(Object stmt) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx != null) {
            if(conf.trace_stmt_leak_enabled) {
                ctx.unclosedStmtMap.remove(System.identityHashCode(stmt));
            }
        }
    }

	public static void rsInit(Object rs) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null) {
            if(conf.trace_rs_leak_enabled) {
                if(conf.profile_fullstack_rs_leak_enabled) {
                    ctx.unclosedRsMap.put(System.identityHashCode(rs), ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace(), 2));
                } else {
                    ctx.unclosedRsMap.put(System.identityHashCode(rs), "");
                }
            }
		}
	}

	public static void rsclose(Object rs) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null) {
			if (ctx.rs_start != 0) {
                if(conf.trace_rs_leak_enabled) {
                    ctx.unclosedRsMap.remove(System.identityHashCode(rs));
                }
				fetch(ctx);
			}
			ctx.rs_start = 0;
			ctx.rs_count = 0;
		}
	}

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

	public static Object start(Object o, SqlParameter args, byte methodType) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx == null) {
			if (conf._log_background_sql && args != null) {
				Logger.println("background: " + args.getSql());
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
					sb.append(c).append("\n");
					sb.append("Connection = ").append(c.getClass().getName()).append("\n");
					sb.append("AutoCommit = ").append(c.getAutoCommit()).append("\n");
				} catch (Exception e) {
					sb.append(e).append("\n");
				}
			}
			sb.append(ThreadUtil.getThreadStack());
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

	public static Connection driverConnect(Connection conn, String url) {
		if (conn == null)
			return conn;
		if (conf.trace_db2_enabled == false)
			return conn;
		if (conn instanceof WrConnection)
			return conn;
		return new WrConnection(conn);
	}

	public static void driverConnect(String url, Throwable thr) {
		AlertProxy.sendAlert(AlertLevel.ERROR, "CONNECT", url + " " + thr);
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null) {
			ServiceSummary.getInstance().process(connectionOpenFailException, 0, ctx.serviceHash, ctx.txid, 0, 0);
		}
	}

	public static void userTxOpen() {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx == null)
			return;
		ctx.userTransaction++;
		MessageStep ms = new MessageStep("utx-begin");
		ms.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		ctx.profile.add(ms);
	}

	public static void userTxClose(String method) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx == null)
			return;
		if (ctx.userTransaction > 0) {
			ctx.userTransaction--;
		}
		MessageStep ms = new MessageStep("utx-" + method);
		ms.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		ctx.profile.add(ms);
	}

	public static Object dbcOpenStart(int hash, String msg, Object pool) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx == null)
			return null;
		if (conf.profile_connection_open_enabled == false)
			return null;
		MethodStep p = new MethodStep();
		p.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			p.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}
		DBURL dbUrl = getUrl(ctx, msg, pool);
		if (dbUrl != unknown) {
			hash = DataProxy.sendMethodName(dbUrl.url);
		}
		p.hash = hash;
		ctx.profile.push(p);
		if (conf.profile_connection_open_fullstack_enabled) {
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

	public static void clearUrlMap() {
        urlTable.clear();
	}

    private static DBURL getUrl(TraceContext ctx, String msg, Object pool) {
		if (pool == null)
			return unknown;
		int key = System.identityHashCode(pool);
		DBURL dbUrl = urlTable.get(key);
		if (dbUrl != null) {
			return dbUrl;
		}
		try {
			Method m = pool.getClass().getMethod("getUrl", new Class[0]);
			if (m != null) {
				String u = "OPEN-DBC " + m.invoke(pool, new Object[0]);
				dbUrl = new DBURL(HashUtil.hash(u), u);
			}
		} catch (Exception e) {
			try {
				String u = PluginJdbcPoolTrace.url(ctx, msg, pool);
				if (u != null) {
					u = "OPEN-DBC " + u;
					dbUrl = new DBURL(HashUtil.hash(u), u);
				}
			} catch (Throwable ignore) {
			}
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
		if (conf.profile_connection_autocommit_status_enabled) {
			HashedMessageStep ms = new HashedMessageStep();
			try {
				ms.hash = DataProxy.sendHashedMessage("AutoCommit : " + conn.getAutoCommit());
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
			ServiceSummary.getInstance().process(connectionOpenFailException, hash, tctx.serviceHash, tctx.txid, 0, 0);
		}
		tctx.profile.pop(step);
	}

	/**
	 * profile sqlMap
	 * 
	 * @param methodName
	 *            sqlMap method name
	 * @param sqlname
	 *            sqlMap name
	 */
	public static void sqlMap(String methodName, String sqlname) {
		if (Configure.getInstance().profile_sqlmap_name_enabled == false)
			return;
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx == null)
			return;
		HashedMessageStep p = new HashedMessageStep();
		p.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
            p.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
        }
		p.hash = DataProxy.sendHashedMessage(new StringBuilder(40).append("SQLMAP ").append(methodName).append(" { ")
				.append(sqlname).append(" }").toString());
		ctx.profile.add(p);
	}

	/**
	 * used for tracing the return of xxx.execute()
	 * @param b true for resultSet, false for no result or update count
	 * @return resultSet case -1, update count case -2
     */
	public static int toInt(boolean b) {
		//return b?1:0;
        return b ? -1 : -2;
	}

	/**
	 * sum of int array
	 * @param arr
	 * @return
	 */
	public static int getIntArraySum(int[] arr) {
		int sum = 0;
		for(int i=arr.length-1; i>=0; i--) {
			sum += arr[i];
		}
        Logger.trace("executeBatch-count=" + sum);
        return sum;
	}

    /**
     * apply update count
     * @param cnt
     * @return
     */
    public static int incUpdateCount(int cnt) {
        Logger.trace("stmt.getUpdateCount()=" + cnt);
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null) {
            return cnt;
        }
        SqlStep3 lastSqlStep = (SqlStep3)ctx.lastSqlStep;
        if(lastSqlStep == null) {
            return cnt;
        }
        int lastCnt = lastSqlStep.updated;
        if(lastCnt == -2 && cnt > 0) { // -2 : execute & the return is the case of update-count
            lastCnt = cnt;
            lastSqlStep.updated = lastCnt;
        } else if(lastCnt >= 0 && cnt > 0) {
            lastCnt += cnt;
            lastSqlStep.updated = lastCnt;
        }
        return cnt;
    }
}
