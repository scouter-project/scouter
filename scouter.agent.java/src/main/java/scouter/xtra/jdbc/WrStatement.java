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

import scouter.agent.trace.TraceSQL;
import scouter.lang.step.SqlXType;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WrStatement implements java.sql.Statement {
    java.sql.Statement inner;

    public WrStatement(java.sql.Statement inner) {
        this.inner = inner;
    }

    final public void close() throws SQLException {
        this.inner.close();
    }

    final public java.sql.Connection getConnection() throws SQLException {
        return this.inner.getConnection();
    }

    final public boolean execute(String a0, int[] a1) throws SQLException {
        Object stat = TraceSQL.start(this, a0, SqlXType.METHOD_EXECUTE);
        try {
            boolean b = this.inner.execute(a0, a1);
            TraceSQL.end(stat, null, TraceSQL.toInt(b));
            return b;
        } catch (SQLException ex) {
            TraceSQL.end(stat, ex, -3);
            throw ex;
        } catch (Throwable t) {
            TraceSQL.end(stat, t, -3);
            throw new SQLException(t);
        }
    }

    final public boolean execute(String a0, String[] a1) throws SQLException {
        Object stat = TraceSQL.start(this, a0, SqlXType.METHOD_EXECUTE);
        try {
            boolean b = this.inner.execute(a0, a1);
            TraceSQL.end(stat, null,TraceSQL.toInt(b));
            return b;
        } catch (SQLException ex) {
            TraceSQL.end(stat, ex, -3);
            throw ex;
        } catch (Throwable t) {
            TraceSQL.end(stat, t, -3);
            throw new SQLException(t);
        }
    }

    final public boolean execute(String a0, int a1) throws SQLException {
        Object stat = TraceSQL.start(this, a0, SqlXType.METHOD_EXECUTE);
        try {
            boolean b = this.inner.execute(a0, a1);
            TraceSQL.end(stat, null, TraceSQL.toInt(b));
            return b;
        } catch (SQLException ex) {
            TraceSQL.end(stat, ex, -3);
            throw ex;
        } catch (Throwable t) {
            TraceSQL.end(stat, t, -3);
            throw new SQLException(t);
        }
    }

    final public boolean execute(String a0) throws SQLException {

        Object stat = TraceSQL.start(this, a0, SqlXType.METHOD_EXECUTE);
        try {
            boolean b = this.inner.execute(a0);
            TraceSQL.end(stat, null, TraceSQL.toInt(b));
            return b;
        } catch (SQLException ex) {
            TraceSQL.end(stat, ex, 0);
            throw ex;
        } catch (Throwable t) {
            TraceSQL.end(stat, t, 0);
            throw new SQLException(t);
        }
    }

    final public ResultSet executeQuery(String a0) throws SQLException {
        Object stat = TraceSQL.start(this, a0, SqlXType.METHOD_QUERY);
        try {
            ResultSet rs = this.inner.executeQuery(a0);
            TraceSQL.end(stat, null, -1);
            return new WrResultSet(rs);
        } catch (SQLException ex) {
            TraceSQL.end(stat, ex, -3);
            throw ex;
        } catch (Throwable t) {
            TraceSQL.end(stat, t, -3);
            throw new SQLException(t);
        }
    }

    final public int executeUpdate(String a0, String[] a1) throws SQLException {
        Object stat = TraceSQL.start(this, a0, SqlXType.METHOD_UPDATE);
        try {
            int n = this.inner.executeUpdate(a0, a1);
            TraceSQL.end(stat, null, n);
            return n;
        } catch (SQLException ex) {
            TraceSQL.end(stat, ex, -3);
            throw ex;
        } catch (Throwable t) {
            TraceSQL.end(stat, t, -3);
            throw new SQLException(t);
        }
    }

    final public int executeUpdate(String a0, int a1) throws SQLException {
        Object stat = TraceSQL.start(this, a0, SqlXType.METHOD_UPDATE);
        try {
            int n = this.inner.executeUpdate(a0, a1);
            TraceSQL.end(stat, null, n);
            return n;
        } catch (SQLException ex) {
            TraceSQL.end(stat, ex, -3);
            throw ex;
        } catch (Throwable t) {
            TraceSQL.end(stat, t, -3);
            throw new SQLException(t);
        }
    }

    final public int executeUpdate(String a0) throws SQLException {
        Object stat = TraceSQL.start(this, a0, SqlXType.METHOD_UPDATE);
        try {
            int b = this.inner.executeUpdate(a0);
            TraceSQL.end(stat, null, b);
            return b;
        } catch (SQLException ex) {
            TraceSQL.end(stat, ex, -3);
            throw ex;
        } catch (Throwable t) {
            TraceSQL.end(stat, t, -3);
            throw new SQLException(t);
        }
    }

    final public int executeUpdate(String a0, int[] a1) throws SQLException {
        Object stat = TraceSQL.start(this, a0, SqlXType.METHOD_UPDATE);
        try {
            int b = this.inner.executeUpdate(a0, a1);
            TraceSQL.end(stat, null, b);
            return b;
        } catch (SQLException ex) {
            TraceSQL.end(stat, ex, -3);
            throw ex;
        } catch (Throwable t) {
            TraceSQL.end(stat, t, -3);
            throw new SQLException(t);
        }
    }

    final public int getMaxFieldSize() throws SQLException {
        return this.inner.getMaxFieldSize();
    }

    final public void setMaxFieldSize(int a0) throws SQLException {
        this.inner.setMaxFieldSize(a0);
    }

    final public int getMaxRows() throws SQLException {
        return this.inner.getMaxRows();
    }

    final public void setMaxRows(int a0) throws SQLException {
        this.inner.setMaxRows(a0);
    }

    final public void setEscapeProcessing(boolean a0) throws SQLException {
        this.inner.setEscapeProcessing(a0);
    }

    final public int getQueryTimeout() throws SQLException {
        return this.inner.getQueryTimeout();
    }

    final public void setQueryTimeout(int a0) throws SQLException {
        this.inner.setQueryTimeout(a0);
    }

    final public void cancel() throws SQLException {
        this.inner.cancel();
    }

    final public java.sql.SQLWarning getWarnings() throws SQLException {
        return this.inner.getWarnings();
    }

    final public void clearWarnings() throws SQLException {
        this.inner.clearWarnings();
    }

    final public void setCursorName(String a0) throws SQLException {
        this.inner.setCursorName(a0);
    }

    final public ResultSet getResultSet() throws SQLException {
        ResultSet rs = this.inner.getResultSet();
        return new WrResultSet(rs);
    }

    final public int getUpdateCount() throws SQLException {
        return TraceSQL.incUpdateCount(this.inner.getUpdateCount());
        //return this.inner.getUpdateCount();
    }

    final public boolean getMoreResults(int a0) throws SQLException {
        return this.inner.getMoreResults(a0);
    }

    final public boolean getMoreResults() throws SQLException {
        return this.inner.getMoreResults();
    }

    final public void setFetchDirection(int a0) throws SQLException {
        this.inner.setFetchDirection(a0);
    }

    final public int getFetchDirection() throws SQLException {
        return this.inner.getFetchDirection();
    }

    final public void setFetchSize(int a0) throws SQLException {
        this.inner.setFetchSize(a0);
    }

    final public int getFetchSize() throws SQLException {
        return this.inner.getFetchSize();
    }

    final public int getResultSetConcurrency() throws SQLException {
        return this.inner.getResultSetConcurrency();
    }

    final public int getResultSetType() throws SQLException {
        return this.inner.getResultSetType();
    }

    final public void addBatch(String a0) throws SQLException {
        this.inner.addBatch(a0);
    }

    final public void clearBatch() throws SQLException {
        this.inner.clearBatch();
    }

    final public int[] executeBatch() throws SQLException {
        return this.inner.executeBatch();
    }

    final public ResultSet getGeneratedKeys() throws SQLException {
        return this.inner.getGeneratedKeys();
    }

    final public int getResultSetHoldability() throws SQLException {
        return this.inner.getResultSetHoldability();
    }

    final public boolean isClosed() throws SQLException {
        return this.inner.isClosed();
    }

    final public void setPoolable(boolean a0) throws SQLException {
        this.inner.setPoolable(a0);
    }

    final public boolean isPoolable() throws SQLException {
        return this.inner.isPoolable();
    }


    public void closeOnCompletion() throws SQLException {
        // TODO Auto-generated method stub

    }

    public boolean isCloseOnCompletion() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }
}