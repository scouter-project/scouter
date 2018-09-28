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
import scouter.agent.error.CONNECTION_NOT_CLOSE;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.util.ICloseManager;
import scouter.agent.util.ILeakableObject;
import scouter.agent.util.LeakableObject;
import scouter.agent.util.LeakableObject2;
import scouter.lang.step.MethodStep;
import scouter.util.SysJMX;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Executor;

public class DetectConnection implements java.sql.Connection {

    private static CONNECTION_NOT_CLOSE error = new CONNECTION_NOT_CLOSE("connection leak detected");
    private final ILeakableObject object;
    java.sql.Connection inner;
    private static Configure conf = Configure.getInstance();

    public DetectConnection(java.sql.Connection inner) {
        this.inner = inner;
        int serviceHash = 0;
        long txid = 0;

        //if(conf._log_trace_enabled) Logger.trace("[DetectConn-InnerObject]" + System.identityHashCode(inner));

        TraceContext ctx = TraceContextManager.getContext();
        if (ctx != null) {
            serviceHash = ctx.serviceHash;
            txid = ctx.txid;
        }

        if (conf._summary_connection_leak_fullstack_enabled) {
            if(conf.__control_connection_leak_autoclose_enabled && conf.__experimental) {
                this.object = new LeakableObject2(new CONNECTION_NOT_CLOSE(), inner, ConnectionCloseManager.getInstance(), serviceHash, txid, true, 2);
            } else {
                this.object = new LeakableObject(new CONNECTION_NOT_CLOSE(), inner.getClass().getName(), serviceHash, txid, true, 2);
            }
        } else {
            if(conf.__control_connection_leak_autoclose_enabled && conf.__experimental) {
                this.object = new LeakableObject2(error, inner, ConnectionCloseManager.getInstance(), serviceHash, txid, false, 0);
            } else {
                this.object = new LeakableObject(error, inner.getClass().getName(), serviceHash, txid, false, 0);
            }
        }
    }

    private static class ConnectionCloseManager implements ICloseManager {
        private static ConnectionCloseManager connectionCloseManager = new ConnectionCloseManager();

        public static ConnectionCloseManager getInstance() {
            return connectionCloseManager;
        }

        @Override
        public boolean close(Object o) {
            try {
                if(o instanceof java.sql.Connection) {
                    ((java.sql.Connection) o).close();
                    return true;
                } else {
                    Logger.println("G001", "Connection auto close - not a connection instance");
                    return false;
                }
            } catch (SQLException e) {
                Logger.println("G002", "Connection auto close exception", e);
                return false;
            }
        }
    }

    final public void setReadOnly(boolean a0) throws SQLException {
        this.inner.setReadOnly(a0);
    }

    private static String MSG_CLOSE = "CLOSE";
    private static int HASH_CLOSE;
    private static String MSG_COMMIT = "COMMIT";
    private static int HASH_COMMIT;
    private static String MSG_AUTO_COMMIT_TRUE = "setAutoCommit(true)";
    private static int HASH_AUTO_COMMIT_TRUE;
    private static String MSG_AUTO_COMMIT_FALSE = "setAutoCommit(false)";
    private static int HASH_AUTO_COMMIT_FALSE;

    static {
        try {
            HASH_CLOSE = DataProxy.sendMethodName(MSG_CLOSE);
            HASH_COMMIT = DataProxy.sendMethodName(MSG_COMMIT);
            HASH_AUTO_COMMIT_TRUE = DataProxy.sendMethodName(MSG_AUTO_COMMIT_TRUE);
            HASH_AUTO_COMMIT_FALSE = DataProxy.sendMethodName(MSG_AUTO_COMMIT_FALSE);
        } catch (Exception e) {
        }
    }

    final public void close() throws SQLException {

        long stime = System.currentTimeMillis();
        object.close();
        this.inner.close();
        long etime = System.currentTimeMillis();

        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null)
            return;

        ctx.lastDbUrl = 0;
        MethodStep p = new MethodStep();
        p.hash = HASH_CLOSE;
        p.start_time = (int) (stime - ctx.startTime);
        if (ctx.profile_thread_cputime) {
            p.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
        }
        p.elapsed = (int) (etime - stime);
        ctx.profile.add(p);
    }

    final public boolean isReadOnly() throws SQLException {
        return this.inner.isReadOnly();
    }

    final public java.sql.Statement createStatement() throws SQLException {
        return this.inner.createStatement();
    }

    final public java.sql.Statement createStatement(int a0, int a1) throws SQLException {
        return this.inner.createStatement(a0, a1);
    }

    final public java.sql.Statement createStatement(int a0, int a1, int a2) throws SQLException {
        return this.inner.createStatement(a0, a1, a2);
    }

    final public java.sql.PreparedStatement prepareStatement(String a0, int a1) throws SQLException {
        return this.inner.prepareStatement(a0, a1);
    }

    final public java.sql.PreparedStatement prepareStatement(String a0, int a1, int a2)
            throws SQLException {
        return this.inner.prepareStatement(a0, a1, a2);
    }

    final public java.sql.PreparedStatement prepareStatement(String a0, int a1, int a2, int a3)
            throws SQLException {
        return this.inner.prepareStatement(a0, a1, a2, a3);
    }

    final public java.sql.PreparedStatement prepareStatement(String a0, int[] a1)
            throws SQLException {
        return this.inner.prepareStatement(a0, a1);
    }

    final public java.sql.PreparedStatement prepareStatement(String a0, String[] a1)
            throws SQLException {
        return this.inner.prepareStatement(a0, a1);
    }

    final public java.sql.PreparedStatement prepareStatement(String a0) throws SQLException {
        return inner.prepareStatement(a0);
    }

    final public java.sql.CallableStatement prepareCall(String a0) throws SQLException {
        return this.inner.prepareCall(a0);
    }

    final public java.sql.CallableStatement prepareCall(String a0, int a1, int a2)
            throws SQLException {
        return this.inner.prepareCall(a0, a1, a2);
    }

    final public java.sql.CallableStatement prepareCall(String a0, int a1, int a2, int a3)
            throws SQLException {
        return this.inner.prepareCall(a0, a1, a2, a3);
    }

    final public String nativeSQL(String a0) throws SQLException {
        return this.inner.nativeSQL(a0);
    }

    final public void setAutoCommit(boolean a0) throws SQLException {
        this.inner.setAutoCommit(a0);

        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null)
            return;

        MethodStep p = new MethodStep();
        p.hash = a0 ? HASH_AUTO_COMMIT_TRUE : HASH_AUTO_COMMIT_FALSE;
        p.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
        if (ctx.profile_thread_cputime) {
            p.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
        }

        ctx.profile.add(p);
    }

    final public boolean getAutoCommit() throws SQLException {
        return this.inner.getAutoCommit();
    }

    final public void commit() throws SQLException {
        long stime = System.currentTimeMillis();
        this.inner.commit();
        long etime = System.currentTimeMillis();

        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null)
            return;

        MethodStep p = new MethodStep();
        p.hash = HASH_COMMIT;
        p.start_time = (int) (stime - ctx.startTime);
        if (ctx.profile_thread_cputime) {
            p.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
        }
        p.elapsed = (int) (etime - stime);
        ctx.profile.add(p);
    }

    final public void rollback(java.sql.Savepoint a0) throws SQLException {
        this.inner.rollback(a0);
    }

    final public void rollback() throws SQLException {
        this.inner.rollback();
    }

    final public boolean isClosed() throws SQLException {
        return this.inner.isClosed();
    }

    final public java.sql.DatabaseMetaData getMetaData() throws SQLException {
        return this.inner.getMetaData();
    }

    final public void setCatalog(String a0) throws SQLException {
        this.inner.setCatalog(a0);
    }

    final public String getCatalog() throws SQLException {
        return this.inner.getCatalog();
    }

    final public void setTransactionIsolation(int a0) throws SQLException {
        this.inner.setTransactionIsolation(a0);
    }

    final public int getTransactionIsolation() throws SQLException {
        return this.inner.getTransactionIsolation();
    }

    final public java.sql.SQLWarning getWarnings() throws SQLException {
        return this.inner.getWarnings();
    }

    final public void clearWarnings() throws SQLException {
        this.inner.clearWarnings();
    }

    final public Map getTypeMap() throws SQLException {
        return this.inner.getTypeMap();
    }

    public <T> T unwrap(Class<T> a0) throws SQLException {
        return this.inner.unwrap(a0);
    }

    public boolean isWrapperFor(Class<?> a0) throws SQLException {
        return this.inner.isWrapperFor(a0);
    }

    public void setTypeMap(Map<String, Class<?>> a0) throws SQLException {
        this.inner.setTypeMap(a0);
    }

    final public void setHoldability(int a0) throws SQLException {
        this.inner.setHoldability(a0);
    }

    final public int getHoldability() throws SQLException {
        return this.inner.getHoldability();
    }

    final public java.sql.Savepoint setSavepoint() throws SQLException {
        return this.inner.setSavepoint();
    }

    final public java.sql.Savepoint setSavepoint(String a0) throws SQLException {
        return this.inner.setSavepoint(a0);
    }

    final public void releaseSavepoint(java.sql.Savepoint a0) throws SQLException {
        this.inner.releaseSavepoint(a0);
    }

    final public java.sql.Clob createClob() throws SQLException {
        return this.inner.createClob();
    }

    final public java.sql.Blob createBlob() throws SQLException {
        return this.inner.createBlob();
    }

    final public java.sql.NClob createNClob() throws SQLException {
        return this.inner.createNClob();
    }

    final public java.sql.SQLXML createSQLXML() throws SQLException {
        return this.inner.createSQLXML();
    }

    final public boolean isValid(int a0) throws SQLException {
        return this.inner.isValid(a0);
    }

    final public void setClientInfo(java.util.Properties a0) throws java.sql.SQLClientInfoException {
        this.inner.setClientInfo(a0);
    }

    final public void setClientInfo(String a0, String a1) throws java.sql.SQLClientInfoException {
        this.inner.setClientInfo(a0, a1);
    }

    final public java.util.Properties getClientInfo() throws SQLException {
        return this.inner.getClientInfo();
    }

    final public String getClientInfo(String a0) throws SQLException {
        return this.inner.getClientInfo(a0);
    }

    final public java.sql.Array createArrayOf(String a0, Object[] a1) throws SQLException {
        return this.inner.createArrayOf(a0, a1);
    }

    final public java.sql.Struct createStruct(String a0, Object[] a1) throws SQLException {
        return this.inner.createStruct(a0, a1);
    }

    public void setSchema(String schema) throws SQLException {
        // TODO Auto-generated method stub

    }

    public String getSchema() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public void abort(Executor executor) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        // TODO Auto-generated method stub

    }

    public int getNetworkTimeout() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }
}