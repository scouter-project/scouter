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

package scouter.agent.asm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.MethodSet;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.trace.TraceSQL;
import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.util.HashUtil;
import scouter.util.StringUtil;

public class DbcCloseASM implements IASM, Opcodes {
	private List<MethodSet> target = MethodSet.getHookingMethodSet(Configure.getInstance().hook_dbc_close);
	private Map<String, MethodSet> reserved = new HashMap<String, MethodSet>();

	public DbcCloseASM() {
		//JBOSS6
	    //AsmUtil.add(reserved, "org/jboss/jca/core/connectionmanager/pool/AbstractPool", "returnConnection");
	    AsmUtil.add(reserved,"org/jboss/jca/adapters/jdbc/WrappedConnection","close");
		//Tomcat7
	    AsmUtil.add(reserved, "org/apache/tomcat/dbcp/dbcp/PoolableConnection", "close");
	    AsmUtil.add(reserved, "org/apache/tomcat/jdbc/pool/ConnectionPool", "returnConnection");
		//Tomcat6
		//AsmUtil.add(reserved, "org/apache/tomcat/dbcp/pool/impl/GenericObjectPool", "returnObject");
		AsmUtil.add(reserved, "org/springframework/orm/hibernate3/LocalDataSourceConnectionProvider", "closeConnection");

		AsmUtil.add(reserved, "org.springframework.jdbc.datasource.DataSourceUtils", "releaseConnection(Ljava/sql/Connection;Ljavax/sql/DataSource;)V");	
		
		// commons-dbcp-1.4
		//AsmUtil.add(reserved, "org/apache/commons/dbcp/PoolableConnection","close");
		}

	public boolean isTarget(String className) {
		MethodSet mset = reserved.get(className);
		if (mset != null)
			return true;

		for (int i = 0; i < target.size(); i++) {
			mset = target.get(i);
			if (mset.classMatch.include(className)) {
				return true;
			}
		}
		return false;
	}
	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if(Configure.getInstance().enable_asm_jdbc==false)
			return cv;
		
		MethodSet mset = reserved.get(className);
		if (mset != null)
			return new DbcCloseCV(cv, mset, className);

		for (int i = 0; i < target.size(); i++) {
			mset = target.get(i);
			if (mset.classMatch.include(className)) {
				return new DbcCloseCV(cv, mset, className);
			}
		}
		return cv;
	}

}

// ///////////////////////////////////////////////////////////////////////////
class DbcCloseCV extends ClassVisitor implements Opcodes {

	public String className;
	private MethodSet mset;

	public DbcCloseCV(ClassVisitor cv, MethodSet mset, String className) {
		super(ASM4, cv);
		this.mset = mset;
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
		if (mv == null || mset.isA(methodName, desc) == false) {
			return mv;
		}
		if (AsmUtil.isSpecial(methodName)) {
			return mv;
		}
		String fullname = "CLOSE-DBC "+StringUtil.cutLastString(className,'/')+"."+methodName;
		int fullname_hash = HashUtil.hash(fullname);
		DataProxy.sendMethodName(fullname_hash, fullname);
		
		return new DbcCloseMV(access, desc, mv, fullname, fullname_hash);
	}
}

// ///////////////////////////////////////////////////////////////////////////
class DbcCloseMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACE_SQL = TraceSQL.class.getName().replace('.', '/');
	private static final String METHOD = "dbcClose";
	private static final String SIGNATURE = "(ILjava/lang/String;)V";

	private String fullname;
	private int fullname_hash;

	public DbcCloseMV(int access, String desc, MethodVisitor mv, String fullname, int fullname_hash) {
		super(ASM4, access, desc, mv);
		this.fullname = fullname;
		this.fullname_hash = fullname_hash;

	}

	@Override
	public void visitCode() {
		AsmUtil.PUSH(mv, fullname_hash);
		mv.visitLdcInsn(fullname);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_SQL, METHOD, SIGNATURE);
		super.visitCode();
	}
}