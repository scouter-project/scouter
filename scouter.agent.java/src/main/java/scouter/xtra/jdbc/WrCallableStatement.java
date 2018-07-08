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

import java.sql.SQLException;
import java.util.Map;

public class WrCallableStatement extends WrPreparedStatement implements java.sql.CallableStatement {
	java.sql.CallableStatement inner;

	public WrCallableStatement(java.sql.CallableStatement inner, String sql) {
		super(inner, sql);
		this.inner = inner;
	}

	final public Object getObject(String a0) throws SQLException {
		return this.inner.getObject(a0);
	}

	final public Object getObject(int a0, Map<String, Class<?>> a1) throws SQLException {
		return this.inner.getObject(a0, a1);
	}

	final public Object getObject(int a0) throws SQLException {
		return this.inner.getObject(a0);
	}

	final public Object getObject(String a0, Map<String, Class<?>> a1) throws SQLException {
		return this.inner.getObject(a0, a1);
	}

	final public boolean getBoolean(String a0) throws SQLException {
		return this.inner.getBoolean(a0);
	}

	final public boolean getBoolean(int a0) throws SQLException {
		return this.inner.getBoolean(a0);
	}

	final public byte getByte(int a0) throws SQLException {
		return this.inner.getByte(a0);
	}

	final public byte getByte(String a0) throws SQLException {
		return this.inner.getByte(a0);
	}

	final public short getShort(String a0) throws SQLException {
		return this.inner.getShort(a0);
	}

	final public short getShort(int a0) throws SQLException {
		return this.inner.getShort(a0);
	}

	final public int getInt(int a0) throws SQLException {
		return this.inner.getInt(a0);
	}

	final public int getInt(String a0) throws SQLException {
		return this.inner.getInt(a0);
	}

	final public long getLong(String a0) throws SQLException {
		return this.inner.getLong(a0);
	}

	final public long getLong(int a0) throws SQLException {
		return this.inner.getLong(a0);
	}

	final public float getFloat(int a0) throws SQLException {
		return this.inner.getFloat(a0);
	}

	final public float getFloat(String a0) throws SQLException {
		return this.inner.getFloat(a0);
	}

	final public double getDouble(String a0) throws SQLException {
		return this.inner.getDouble(a0);
	}

	final public double getDouble(int a0) throws SQLException {
		return this.inner.getDouble(a0);
	}

	final public byte[] getBytes(int a0) throws SQLException {
		return this.inner.getBytes(a0);
	}

	final public byte[] getBytes(String a0) throws SQLException {
		return this.inner.getBytes(a0);
	}

	final public java.sql.Array getArray(String a0) throws SQLException {
		return this.inner.getArray(a0);
	}

	final public java.sql.Array getArray(int a0) throws SQLException {
		return this.inner.getArray(a0);
	}

	final public java.net.URL getURL(String a0) throws SQLException {
		return this.inner.getURL(a0);
	}

	final public java.net.URL getURL(int a0) throws SQLException {
		return this.inner.getURL(a0);
	}

	final public void setBoolean(String a0, boolean a1) throws SQLException {
		this.inner.setBoolean(a0, a1);
	}

	final public void setByte(String a0, byte a1) throws SQLException {
		this.inner.setByte(a0, a1);
	}

	final public void setShort(String a0, short a1) throws SQLException {
		this.inner.setShort(a0, a1);
	}

	final public void setInt(String a0, int a1) throws SQLException {
		this.inner.setInt(a0, a1);
	}

	final public void setLong(String a0, long a1) throws SQLException {
		this.inner.setLong(a0, a1);
	}

	final public void setFloat(String a0, float a1) throws SQLException {
		this.inner.setFloat(a0, a1);
	}

	final public void setDouble(String a0, double a1) throws SQLException {
		this.inner.setDouble(a0, a1);
	}

	final public void setTimestamp(String a0, java.sql.Timestamp a1) throws SQLException {
		this.inner.setTimestamp(a0, a1);
	}

	final public void setTimestamp(String a0, java.sql.Timestamp a1, java.util.Calendar a2)
			throws SQLException {
		this.inner.setTimestamp(a0, a1, a2);
	}

	final public java.sql.Ref getRef(String a0) throws SQLException {
		return this.inner.getRef(a0);
	}

	final public java.sql.Ref getRef(int a0) throws SQLException {
		return this.inner.getRef(a0);
	}

	final public String getString(String a0) throws SQLException {
		return this.inner.getString(a0);
	}

	final public String getString(int a0) throws SQLException {
		return this.inner.getString(a0);
	}

	final public void setURL(String a0, java.net.URL a1) throws SQLException {
		this.inner.setURL(a0, a1);
	}

	final public void setTime(String a0, java.sql.Time a1) throws SQLException {
		this.inner.setTime(a0, a1);
	}

	final public void setTime(String a0, java.sql.Time a1, java.util.Calendar a2)
			throws SQLException {
		this.inner.setTime(a0, a1, a2);
	}

	final public java.sql.Time getTime(int a0) throws SQLException {
		return this.inner.getTime(a0);
	}

	final public java.sql.Time getTime(String a0) throws SQLException {
		return this.inner.getTime(a0);
	}

	final public java.sql.Time getTime(String a0, java.util.Calendar a1) throws SQLException {
		return this.inner.getTime(a0, a1);
	}

	final public java.sql.Time getTime(int a0, java.util.Calendar a1) throws SQLException {
		return this.inner.getTime(a0, a1);
	}

	final public java.sql.Date getDate(int a0, java.util.Calendar a1) throws SQLException {
		return this.inner.getDate(a0, a1);
	}

	final public java.sql.Date getDate(String a0) throws SQLException {
		return this.inner.getDate(a0);
	}

	final public java.sql.Date getDate(String a0, java.util.Calendar a1) throws SQLException {
		return this.inner.getDate(a0, a1);
	}

	final public java.sql.Date getDate(int a0) throws SQLException {
		return this.inner.getDate(a0);
	}

	final public void registerOutParameter(int a0, int a1, int a2) throws SQLException {
		this.inner.registerOutParameter(a0, a1, a2);
	}

	final public void registerOutParameter(String a0, int a1) throws SQLException {
		this.inner.registerOutParameter(a0, a1);
	}

	final public void registerOutParameter(String a0, int a1, int a2) throws SQLException {
		this.inner.registerOutParameter(a0, a1, a2);
	}

	final public void registerOutParameter(int a0, int a1, String a2) throws SQLException {
		this.inner.registerOutParameter(a0, a1, a2);
	}

	final public void registerOutParameter(int a0, int a1) throws SQLException {
		this.inner.registerOutParameter(a0, a1);
	}

	final public void registerOutParameter(String a0, int a1, String a2)
			throws SQLException {
		this.inner.registerOutParameter(a0, a1, a2);
	}

	final public boolean wasNull() throws SQLException {
		return this.inner.wasNull();
	}

	final public java.math.BigDecimal getBigDecimal(int a0) throws SQLException {
		return this.inner.getBigDecimal(a0);
	}

	final public java.math.BigDecimal getBigDecimal(int a0, int a1) throws SQLException {
		return this.inner.getBigDecimal(a0, a1);
	}

	final public java.math.BigDecimal getBigDecimal(String a0) throws SQLException {
		return this.inner.getBigDecimal(a0);
	}

	final public java.sql.Timestamp getTimestamp(String a0) throws SQLException {
		return this.inner.getTimestamp(a0);
	}

	final public java.sql.Timestamp getTimestamp(int a0) throws SQLException {
		return this.inner.getTimestamp(a0);
	}

	final public java.sql.Timestamp getTimestamp(int a0, java.util.Calendar a1) throws SQLException {
		return this.inner.getTimestamp(a0, a1);
	}

	final public java.sql.Timestamp getTimestamp(String a0, java.util.Calendar a1)
			throws SQLException {
		return this.inner.getTimestamp(a0, a1);
	}

	final public java.sql.Blob getBlob(String a0) throws SQLException {
		return this.inner.getBlob(a0);
	}

	final public java.sql.Blob getBlob(int a0) throws SQLException {
		return this.inner.getBlob(a0);
	}

	final public java.sql.Clob getClob(String a0) throws SQLException {
		return this.inner.getClob(a0);
	}

	final public java.sql.Clob getClob(int a0) throws SQLException {
		return this.inner.getClob(a0);
	}

	final public void setNull(String a0, int a1) throws SQLException {
		this.inner.setNull(a0, a1);
	}

	final public void setNull(String a0, int a1, String a2) throws SQLException {
		this.inner.setNull(a0, a1, a2);
	}

	final public void setBigDecimal(String a0, java.math.BigDecimal a1) throws SQLException {
		this.inner.setBigDecimal(a0, a1);
	}

	final public void setString(String a0, String a1) throws SQLException {
		this.inner.setString(a0, a1);
	}

	final public void setBytes(String a0, byte[] a1) throws SQLException {
		this.inner.setBytes(a0, a1);
	}

	final public void setDate(String a0, java.sql.Date a1) throws SQLException {
		this.inner.setDate(a0, a1);
	}

	final public void setDate(String a0, java.sql.Date a1, java.util.Calendar a2)
			throws SQLException {
		this.inner.setDate(a0, a1, a2);
	}

	final public void setAsciiStream(String a0, java.io.InputStream a1, long a2) throws SQLException {
		this.inner.setAsciiStream(a0, a1, a2);
	}

	final public void setAsciiStream(String a0, java.io.InputStream a1) throws SQLException {
		this.inner.setAsciiStream(a0, a1);
	}

	final public void setAsciiStream(String a0, java.io.InputStream a1, int a2) throws SQLException {
		this.inner.setAsciiStream(a0, a1, a2);
	}

	final public void setBinaryStream(String a0, java.io.InputStream a1, int a2) throws SQLException {
		this.inner.setBinaryStream(a0, a1, a2);
	}

	final public void setBinaryStream(String a0, java.io.InputStream a1) throws SQLException {
		this.inner.setBinaryStream(a0, a1);
	}

	final public void setBinaryStream(String a0, java.io.InputStream a1, long a2)
			throws SQLException {
		this.inner.setBinaryStream(a0, a1, a2);
	}

	final public void setObject(String a0, Object a1, int a2, int a3) throws SQLException {
		this.inner.setObject(a0, a1, a2, a3);
	}

	final public void setObject(String a0, Object a1) throws SQLException {
		this.inner.setObject(a0, a1);
	}

	final public void setObject(String a0, Object a1, int a2) throws SQLException {
		this.inner.setObject(a0, a1, a2);
	}

	final public void setCharacterStream(String a0, java.io.Reader a1, long a2) throws SQLException {
		this.inner.setCharacterStream(a0, a1, a2);
	}

	final public void setCharacterStream(String a0, java.io.Reader a1) throws SQLException {
		this.inner.setCharacterStream(a0, a1);
	}

	final public void setCharacterStream(String a0, java.io.Reader a1, int a2) throws SQLException {
		this.inner.setCharacterStream(a0, a1, a2);
	}

	final public java.sql.RowId getRowId(int a0) throws SQLException {
		return this.inner.getRowId(a0);
	}

	final public java.sql.RowId getRowId(String a0) throws SQLException {
		return this.inner.getRowId(a0);
	}

	final public void setRowId(String a0, java.sql.RowId a1) throws SQLException {
		this.inner.setRowId(a0, a1);
	}

	final public void setNString(String a0, String a1) throws SQLException {
		this.inner.setNString(a0, a1);
	}

	final public void setNCharacterStream(String a0, java.io.Reader a1, long a2) throws SQLException {
		this.inner.setNCharacterStream(a0, a1, a2);
	}

	final public void setNCharacterStream(String a0, java.io.Reader a1) throws SQLException {
		this.inner.setNCharacterStream(a0, a1);
	}

	final public void setNClob(String a0, java.io.Reader a1) throws SQLException {
		this.inner.setNClob(a0, a1);
	}

	final public void setNClob(String a0, java.io.Reader a1, long a2) throws SQLException {
		this.inner.setNClob(a0, a1, a2);
	}

	final public void setNClob(String a0, java.sql.NClob a1) throws SQLException {
		this.inner.setNClob(a0, a1);
	}

	final public void setClob(String a0, java.io.Reader a1) throws SQLException {
		this.inner.setClob(a0, a1);
	}

	final public void setClob(String a0, java.sql.Clob a1) throws SQLException {
		this.inner.setClob(a0, a1);
	}

	final public void setClob(String a0, java.io.Reader a1, long a2) throws SQLException {
		this.inner.setClob(a0, a1, a2);
	}

	final public void setBlob(String a0, java.sql.Blob a1) throws SQLException {
		this.inner.setBlob(a0, a1);
	}

	final public void setBlob(String a0, java.io.InputStream a1) throws SQLException {
		this.inner.setBlob(a0, a1);
	}

	final public void setBlob(String a0, java.io.InputStream a1, long a2) throws SQLException {
		this.inner.setBlob(a0, a1, a2);
	}

	final public java.sql.NClob getNClob(String a0) throws SQLException {
		return this.inner.getNClob(a0);
	}

	final public java.sql.NClob getNClob(int a0) throws SQLException {
		return this.inner.getNClob(a0);
	}

	final public void setSQLXML(String a0, java.sql.SQLXML a1) throws SQLException {
		this.inner.setSQLXML(a0, a1);
	}

	final public java.sql.SQLXML getSQLXML(int a0) throws SQLException {
		return this.inner.getSQLXML(a0);
	}

	final public java.sql.SQLXML getSQLXML(String a0) throws SQLException {
		return this.inner.getSQLXML(a0);
	}

	final public String getNString(String a0) throws SQLException {
		return this.inner.getNString(a0);
	}

	final public String getNString(int a0) throws SQLException {
		return this.inner.getNString(a0);
	}

	final public java.io.Reader getNCharacterStream(int a0) throws SQLException {
		return this.inner.getNCharacterStream(a0);
	}

	final public java.io.Reader getNCharacterStream(String a0) throws SQLException {
		return this.inner.getNCharacterStream(a0);
	}

	final public java.io.Reader getCharacterStream(int a0) throws SQLException {
		return this.inner.getCharacterStream(a0);
	}

	final public java.io.Reader getCharacterStream(String a0) throws SQLException {
		return this.inner.getCharacterStream(a0);
	}

	public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
}