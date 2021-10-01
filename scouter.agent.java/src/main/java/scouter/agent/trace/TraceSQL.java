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
import scouter.agent.counter.meter.MeterInteraction;
import scouter.agent.counter.meter.MeterInteractionManager;
import scouter.agent.counter.meter.MeterSQL;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.plugin.PluginJdbcPoolTrace;
import scouter.agent.proxy.ITraceSQL;
import scouter.agent.proxy.TraceSQLFactory;
import scouter.agent.summary.ServiceSummary;
import scouter.lang.AlertLevel;
import scouter.lang.step.HashedMessageStep;
import scouter.lang.step.MessageStep;
import scouter.lang.step.MethodStep;
import scouter.lang.step.SqlStep3;
import scouter.lang.step.SqlXType;
import scouter.util.EscapeLiteralSQL;
import scouter.util.IntKeyLinkedMap;
import scouter.util.IntLinkedSet;
import scouter.util.StringUtil;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Trace SQL
 * @author @author Paul S.J. Kim(sjkim@whatap.io)
 * @author Gun Lee (gunlee01@gmail.com)
 * @author Eunsu Kim
 */
public class TraceSQL {
    private static Configure conf = Configure.getInstance();
	private static ClassLoader jdbcClassLoader;
	private static ITraceSQL traceSQL0;
	private static Exception slowSqlException;
	private static Exception tooManyRecordException;
	private static Exception connectionOpenFailException;

	static {
		try
		{
			Method m = ClassLoader.class.getDeclaredMethod("getPlatformClassLoader", new Class[0]);
			jdbcClassLoader = (ClassLoader) m.invoke(null, new Object[0]);
		} catch (Exception ignored) {}

		traceSQL0 = TraceSQLFactory.create(jdbcClassLoader);
		slowSqlException = traceSQL0.getSlowSqlException();
		tooManyRecordException = traceSQL0.getTooManyRecordException();
		connectionOpenFailException = traceSQL0.getConnectionOpenFailException();
	}

	private final static int MAX_STRING = conf.trace_sql_parameter_max_length;

    // JDBC_REDEFINED==false
    public final static String PSTMT_PARAM_FIELD = "_param_";
    private static int RESULT_SET_FETCH = 0;

    private static IntLinkedSet noLiteralSql = new IntLinkedSet().setMax(10000);
    private static IntKeyLinkedMap<ParsedSql> checkedSql = new IntKeyLinkedMap<ParsedSql>().setMax(1001);

    static IntKeyLinkedMap<DBURL> urlTable = new IntKeyLinkedMap<DBURL>().setMax(500);
    static DBURL unknown = new DBURL(null, null);

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
		return traceSQL0.start(o, sql, methodType);
	}
	private static class ParsedSql {
		public ParsedSql(String sql, String param) {
			this.sql = sql;
			this.param = param;
		}
		String sql;
		String param;
	}

    public static String escapeLiteral(String sql, SqlStep3 step) {
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
				Throwable cause = thr.getCause();
				while (cause != null) {
					sb.append("\nCause...\n");
					ThreadUtil.getStackTrace(sb, cause, conf.profile_fullstack_max_lines);
					cause = cause.getCause();
				}
				msg = sb.toString();
			}
			int hash = DataProxy.sendError(msg);
			if (tCtx.error == 0 && conf.xlog_error_on_sqlexception_enabled) {
				tCtx.error = hash;
			}
			step.error = hash;
			tCtx.offerErrorEntity(ErrorEntity.of(thr, hash, step.hash, 0));
		} else if (step.elapsed > conf.xlog_error_sql_time_max_ms) {
			String msg = "warning slow sql, over " + conf.xlog_error_sql_time_max_ms + " ms";
			int hash = DataProxy.sendError(msg);
			if (tCtx.error == 0) {
				tCtx.error = hash;
			}
			tCtx.offerErrorEntity(ErrorEntity.of(slowSqlException, hash, step.hash, 0));
		}
		tCtx.sqltext = null;
		tCtx.sqlActiveArgs = null;
		tCtx.sqlCount++;
		tCtx.sqlTime += step.elapsed;
		ServiceSummary.getInstance().process(step);
		MeterSQL.getInstance().add(step.elapsed, step.error != 0);
		if (conf.counter_interaction_enabled) {
			MeterInteraction meter = MeterInteractionManager.getInstance().getDbCallMeter(conf.getObjHash(), tCtx.lastDbUrl);
			if (meter != null) {
				meter.add(step.elapsed, step.error != 0);
			}
		}
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
			c.offerErrorEntity(ErrorEntity.of(tooManyRecordException, hash, 0,0));
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
			if (p == null || p.toString() == null) {
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
		return traceSQL0.start(o, args, methodType);
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

	public static Object driverConnect(Object conn, String url) {
		return traceSQL0.driverConnect(conn, url);
	}

	public static void driverConnect(String url, Throwable thr) {
		AlertProxy.sendAlert(AlertLevel.ERROR, "CONNECT", url + " " + thr);
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null) {
		    ctx.offerErrorEntity(ErrorEntity.of(connectionOpenFailException, 0, 0, 0));
		}
	}

	public static Object getConnection(Object conn) {
		return traceSQL0.getConnection(conn);
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
		if (dbUrl != unknown && dbUrl.url != null) {
			hash = DataProxy.sendMethodName(dbUrl.description);
			int urlHash = DataProxy.sendObjName(dbUrl.url);
			ctx.lastDbUrl = urlHash;
		} else {
			ctx.lastDbUrl = 0;
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
		String description;
		String url;


		public DBURL(String url, String description) {
			this.url = url;
			this.description = description;
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
				String url = (String) m.invoke(pool, new Object[0]);
				String description = "OPEN-DBC " + url + " (" + msg + ")";
				dbUrl = new DBURL(url, description);
			}
		} catch (Exception e) {
			try {
				Method m = pool.getClass().getMethod("getJdbcUrl", new Class[0]);
				if (m != null) {
					String url = (String) m.invoke(pool, new Object[0]);
					if(url == null) {
						Method m2 = pool.getClass().getMethod("getDataSourceProperties", new Class[0]);
						if (m2 != null) {
							Properties prop =(Properties) m2.invoke(pool, new Object[0]);
							url = prop.getProperty("url");
							if(url == null || "".equals(url)){
								url = prop.getProperty("serverName") + ":" + prop.getProperty("port") + "/" + prop.getProperty("databaseName");
							}
						}
					}
					String description = "OPEN-DBC " + url + " (" + msg + ")";
					dbUrl = new DBURL(url, description);
				}
			} catch (Exception e1) {
				try {
					String url = PluginJdbcPoolTrace.url(ctx, msg, pool);
					if (url != null) {
						String description = "OPEN-DBC " + url + " (" + msg + ")";
						dbUrl = new DBURL(url, description);
					}
				} catch (Throwable ignore) {
				}
			}
		}
		if (dbUrl == null) {
			dbUrl = unknown;
		}
		urlTable.put(key, dbUrl);
		return dbUrl;
	}

	public static Object dbcOpenEnd(Object conn, Object stat) {
		return traceSQL0.dbcOpenEnd(conn, stat);
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
			tctx.offerErrorEntity(ErrorEntity.of(connectionOpenFailException, hash, 0, 0));
		}
		tctx.profile.pop(step);
	}

	public static void ctxLookup(Object this1, Object ctx) {
		traceSQL0.ctxLookup(this1, ctx);
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
		p.time = -1;
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
