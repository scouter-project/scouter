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

import scouter.agent.trace.SqlParameter;
import scouter.agent.trace.TraceSQL;
import scouter.lang.step.SqlXType;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WrPreparedStatement extends WrStatement implements java.sql.PreparedStatement {
    java.sql.PreparedStatement inner;
    SqlParameter sql = new SqlParameter();

    public WrPreparedStatement(java.sql.PreparedStatement inner, String sql) {
        super(inner);
        this.inner = inner;
        this.sql.setSql(sql);
    }

    final public void setBoolean(int a0, boolean a1) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setBoolean(a0, a1);
    }

    final public void setByte(int a0, byte a1) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setByte(a0, a1);
    }

    final public void setShort(int a0, short a1) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setShort(a0, a1);
    }

    final public void setInt(int a0, int a1) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setInt(a0, a1);
    }

    final public void setLong(int a0, long a1) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setLong(a0, a1);
    }

    final public void setFloat(int a0, float a1) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setFloat(a0, a1);
    }

    final public void setDouble(int a0, double a1) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setDouble(a0, a1);
    }

    final public void setTimestamp(int a0, java.sql.Timestamp a1) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setTimestamp(a0, a1);
    }

    final public void setTimestamp(int a0, java.sql.Timestamp a1, java.util.Calendar a2) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setTimestamp(a0, a1, a2);
    }

    final public void setURL(int a0, java.net.URL a1) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setURL(a0, a1);
    }

    final public void setTime(int a0, java.sql.Time a1) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setTime(a0, a1);
    }

    final public void setTime(int a0, java.sql.Time a1, java.util.Calendar a2) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setTime(a0, a1, a2);
    }

    final public boolean execute() throws SQLException {
        Object stat = TraceSQL.start(this, sql, SqlXType.METHOD_EXECUTE);
        try {
            boolean b = this.inner.execute();
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

    final public ResultSet executeQuery() throws SQLException {
        Object stat = TraceSQL.start(this, sql, SqlXType.METHOD_QUERY);
        try {
            ResultSet rs = this.inner.executeQuery();
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

    final public int executeUpdate() throws SQLException {
        Object stat = TraceSQL.start(this, sql, SqlXType.METHOD_UPDATE);
        try {
            int n = this.inner.executeUpdate();
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

    final public void setNull(int a0, int a1, String a2) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setNull(a0, a1, a2);
    }

    final public void setNull(int a0, int a1) throws SQLException {
        TraceSQL.set(sql, a0, null);
        this.inner.setNull(a0, a1);
    }

    final public void setBigDecimal(int a0, java.math.BigDecimal a1) throws SQLException {
        this.inner.setBigDecimal(a0, a1);
    }

    final public void setString(int a0, String a1) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setString(a0, a1);
    }

    final public void setBytes(int a0, byte[] a1) throws SQLException {
        TraceSQL.set(sql, a0, "[bytes]");
        this.inner.setBytes(a0, a1);
    }

    final public void setDate(int a0, java.sql.Date a1, java.util.Calendar a2) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setDate(a0, a1, a2);
    }

    final public void setDate(int a0, java.sql.Date a1) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setDate(a0, a1);
    }

    final public void setAsciiStream(int a0, java.io.InputStream a1, int a2) throws SQLException {
        TraceSQL.set(sql, a0, "[asiistream]");
        this.inner.setAsciiStream(a0, a1, a2);
    }

    final public void setAsciiStream(int a0, java.io.InputStream a1) throws SQLException {
        TraceSQL.set(sql, a0, "[asciistream]");
        this.inner.setAsciiStream(a0, a1);
    }

    final public void setAsciiStream(int a0, java.io.InputStream a1, long a2) throws SQLException {
        TraceSQL.set(sql, a0, "[asciistream]");
        this.inner.setAsciiStream(a0, a1, a2);
    }

    final public void setUnicodeStream(int a0, java.io.InputStream a1, int a2) throws SQLException {
        this.inner.setUnicodeStream(a0, a1, a2);
    }

    final public void setBinaryStream(int a0, java.io.InputStream a1, int a2) throws SQLException {
        TraceSQL.set(sql, a0, "[binstream]");
        this.inner.setBinaryStream(a0, a1, a2);
    }

    final public void setBinaryStream(int a0, java.io.InputStream a1, long a2) throws SQLException {
        TraceSQL.set(sql, a0, "[binstream]");
        this.inner.setBinaryStream(a0, a1, a2);
    }

    final public void setBinaryStream(int a0, java.io.InputStream a1) throws SQLException {
        TraceSQL.set(sql, a0, "[binstream]");
        this.inner.setBinaryStream(a0, a1);
    }

    final public void clearParameters() throws SQLException {
        this.sql.clear();
        this.inner.clearParameters();
    }

    final public void setObject(int a0, Object a1, int a2, int a3) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setObject(a0, a1, a2, a3);
    }

    final public void setObject(int a0, Object a1, int a2) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setObject(a0, a1, a2);
    }

    final public void setObject(int a0, Object a1) throws SQLException {
        TraceSQL.set(sql, a0, a1);
        this.inner.setObject(a0, a1);
    }

    final public void addBatch() throws SQLException {
        this.inner.addBatch();
    }

    final public void setCharacterStream(int a0, java.io.Reader a1, long a2) throws SQLException {
        this.inner.setCharacterStream(a0, a1, a2);
    }

    final public void setCharacterStream(int a0, java.io.Reader a1, int a2) throws SQLException {
        this.inner.setCharacterStream(a0, a1, a2);
    }

    final public void setCharacterStream(int a0, java.io.Reader a1) throws SQLException {
        this.inner.setCharacterStream(a0, a1);
    }

    final public void setRef(int a0, java.sql.Ref a1) throws SQLException {
        this.inner.setRef(a0, a1);
    }

    final public void setBlob(int a0, java.io.InputStream a1, long a2) throws SQLException {
        TraceSQL.set(sql, a0, "[blob]");
        this.inner.setBlob(a0, a1, a2);
    }

    final public void setBlob(int a0, java.io.InputStream a1) throws SQLException {
        TraceSQL.set(sql, a0, "[blob]");
        this.inner.setBlob(a0, a1);
    }

    final public void setBlob(int a0, java.sql.Blob a1) throws SQLException {
        TraceSQL.set(sql, a0, "[blob]");
        this.inner.setBlob(a0, a1);
    }

    final public void setClob(int a0, java.io.Reader a1) throws SQLException {
        this.inner.setClob(a0, a1);
    }

    final public void setClob(int a0, java.sql.Clob a1) throws SQLException {
        TraceSQL.set(sql, a0, "[clob]");

        this.inner.setClob(a0, a1);
    }

    final public void setClob(int a0, java.io.Reader a1, long a2) throws SQLException {
        this.inner.setClob(a0, a1, a2);
    }

    final public void setArray(int a0, java.sql.Array a1) throws SQLException {
        TraceSQL.set(sql, a0, "[array]");
        this.inner.setArray(a0, a1);
    }

    final public java.sql.ResultSetMetaData getMetaData() throws SQLException {
        return this.inner.getMetaData();
    }

    final public java.sql.ParameterMetaData getParameterMetaData() throws SQLException {
        return this.inner.getParameterMetaData();
    }

    final public void setRowId(int a0, java.sql.RowId a1) throws SQLException {
        this.inner.setRowId(a0, a1);
    }

    final public void setNString(int a0, String a1) throws SQLException {
        this.inner.setNString(a0, a1);
    }

    final public void setNCharacterStream(int a0, java.io.Reader a1) throws SQLException {
        this.inner.setNCharacterStream(a0, a1);
    }

    final public void setNCharacterStream(int a0, java.io.Reader a1, long a2) throws SQLException {
        this.inner.setNCharacterStream(a0, a1, a2);
    }

    final public void setNClob(int a0, java.io.Reader a1) throws SQLException {
        this.inner.setNClob(a0, a1);
    }

    final public void setNClob(int a0, java.io.Reader a1, long a2) throws SQLException {
        this.inner.setNClob(a0, a1, a2);
    }

    final public void setNClob(int a0, java.sql.NClob a1) throws SQLException {
        this.inner.setNClob(a0, a1);
    }

    final public void setSQLXML(int a0, java.sql.SQLXML a1) throws SQLException {
        this.inner.setSQLXML(a0, a1);
    }

}