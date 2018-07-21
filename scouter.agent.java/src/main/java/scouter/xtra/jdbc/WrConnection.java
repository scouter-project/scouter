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
import java.util.concurrent.Executor;

public class WrConnection implements java.sql.Connection {
	java.sql.Connection inner;

	public WrConnection(java.sql.Connection inner) {
		this.inner = inner;
	}

	final public void setReadOnly(boolean a0) throws SQLException {
		this.inner.setReadOnly(a0);
	}

	final public void close() throws SQLException {
		this.inner.close();
	}

	final public boolean isReadOnly() throws SQLException {
		return this.inner.isReadOnly();
	}

	final public java.sql.Statement createStatement() throws SQLException {
		return new WrStatement(this.inner.createStatement());
	}

	final public java.sql.Statement createStatement(int a0, int a1) throws SQLException {
		return new WrStatement(this.inner.createStatement(a0, a1));
	}

	final public java.sql.Statement createStatement(int a0, int a1, int a2) throws SQLException {
		return new WrStatement(this.inner.createStatement(a0, a1, a2));
	}

	final public java.sql.PreparedStatement prepareStatement(String a0, int a1) throws SQLException {
		return new WrPreparedStatement(this.inner.prepareStatement(a0, a1), a0);
	}

	final public java.sql.PreparedStatement prepareStatement(String a0, int a1, int a2)
			throws SQLException {
		return new WrPreparedStatement(this.inner.prepareStatement(a0, a1, a2), a0);
	}

	final public java.sql.PreparedStatement prepareStatement(String a0, int a1, int a2, int a3)
			throws SQLException {
		return new WrPreparedStatement(this.inner.prepareStatement(a0, a1, a2, a3), a0);
	}

	final public java.sql.PreparedStatement prepareStatement(String a0, int[] a1)
			throws SQLException {
		return new WrPreparedStatement(this.inner.prepareStatement(a0, a1), a0);
	}

	final public java.sql.PreparedStatement prepareStatement(String a0, String[] a1)
			throws SQLException {
		return new WrPreparedStatement(this.inner.prepareStatement(a0, a1), a0);
	}

	final public java.sql.PreparedStatement prepareStatement(String a0) throws SQLException {
		return new WrPreparedStatement(inner.prepareStatement(a0), a0);
	}

	final public java.sql.CallableStatement prepareCall(String a0) throws SQLException {
		return new WrCallableStatement(this.inner.prepareCall(a0), a0);
	}

	final public java.sql.CallableStatement prepareCall(String a0, int a1, int a2)
			throws SQLException {
		return new WrCallableStatement(this.inner.prepareCall(a0, a1, a2), a0);
	}

	final public java.sql.CallableStatement prepareCall(String a0, int a1, int a2, int a3)
			throws SQLException {
		return new WrCallableStatement(this.inner.prepareCall(a0, a1, a2, a3), a0);
	}

	final public String nativeSQL(String a0) throws SQLException {
		return this.inner.nativeSQL(a0);
	}

	final public void setAutoCommit(boolean a0) throws SQLException {
		this.inner.setAutoCommit(a0);
	}

	final public boolean getAutoCommit() throws SQLException {
		return this.inner.getAutoCommit();
	}

	final public void commit() throws SQLException {
		this.inner.commit();
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