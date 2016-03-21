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
package scouter.agent.asm;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.jdbc.*;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.SqlParameter;
import scouter.agent.trace.TraceSQL;
import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.Type;

import java.util.HashSet;

/**
 * BCI for a JDBC PreparedStatement
 * @author @author Paul S.J. Kim(sjkim@whatap.io)
 * @author Gun Lee (gunlee01@gmail.com)
 */
public class JDBCPreparedStatementASM implements IASM, Opcodes {
	public final HashSet<String> target = HookingSet.getHookingClassSet(Configure.getInstance().hook_jdbc_pstmt_classes);
	public final HashSet<String> noField = new HashSet<String>();

	public JDBCPreparedStatementASM() {
		target.add("org/mariadb/jdbc/AbstractMariaDbPrepareStatement");
		target.add("org/mariadb/jdbc/MariaDbClientPreparedStatement");
		target.add("org/mariadb/jdbc/MariaDbServerPreparedStatement");
		target.add("org/mariadb/jdbc/MySQLPreparedStatement");
		target.add("oracle/jdbc/driver/OraclePreparedStatement");
		target.add("org/postgresql/jdbc2/AbstractJdbc2Statement");
		target.add("org/apache/derby/client/am/PreparedStatement");
		target.add("net/sourceforge/jtds/jdbc/JtdsPreparedStatement");
		target.add("jdbc/FakePreparedStatement");
		target.add("jdbc/FakePreparedStatement2");
		target.add("com/microsoft/sqlserver/jdbc/SQLServerPreparedStatement");
		target.add("com/tmax/tibero/jdbc/TbPreparedStatement");
		target.add("org/hsqldb/jdbc/JDBCPreparedStatement");
		target.add("com/mysql/jdbc/ServerPreparedStatement");
		target.add("com/mysql/jdbc/PreparedStatement");
        target.add("cubrid/jdbc/driver/CUBRIDPreparedStatement");

        // @skyworker - MySQL ServerPreparedStatement는 특별히 필드를 추가하지 않음
        noField.add("com/mysql/jdbc/ServerPreparedStatement");
		noField.add("jdbc/FakePreparedStatement2");
        noField.add("org/mariadb/jdbc/MariaDbClientPreparedStatement");
        noField.add("org/mariadb/jdbc/MariaDbServerPreparedStatement");
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance()._hook_dbsql_enabled == false) {
			return cv;
		}
		if (target.contains(className) == false) {
			return cv;
		}
		Logger.println("A106", "jdbc pstmt found: " + className);
		return new PreparedStatementCV(cv, noField);
	}
}
class PreparedStatementCV extends ClassVisitor implements Opcodes {

	HashSet<String> noField;
    private String owner;

    public PreparedStatementCV(ClassVisitor cv, HashSet<String> noField) {
		super(ASM4, cv);
		this.noField = noField;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.owner = name;
		if (noField.contains(name) == false) {
			// add trace fields
			super.visitField(ACC_PUBLIC, TraceSQL.PSTMT_PARAM_FIELD, Type.getDescriptor(SqlParameter.class), null, null)
					.visitEnd();
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if ("<init>".equals(name)) {
			return new PsInitMV(access, desc, mv, owner);

		} else {
			String targetDesc = PsSetMV.getSetSignature(name);
			if (targetDesc != null) {
				if (targetDesc.equals(desc)) {
					return new PsSetMV(access, name, desc, mv, owner);
				}
			} else if (PsExecuteMV.isTarget(name)) {
				if (desc.startsWith("()")) {
					return new PsExecuteMV(access, desc, mv, owner, name);
				} else if (desc.startsWith("(Ljava/lang/String;)")) {
					return new StExecuteMV(access, desc, mv, owner, name);
				}
			} else if ("clearParameters".equals(name) && "()V".equals(desc)) {
				return new PsClearParametersMV(access, desc, mv, owner);

			} else if ("getUpdateCount".equals(name) && "()I".equals(desc)) {
                return new PsUpdateCountMV(mv);
            }
		}
		return mv;
	}
}
