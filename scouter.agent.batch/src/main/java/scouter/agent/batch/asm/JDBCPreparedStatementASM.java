/*
 *  Copyright 2016 the original author or authors. 
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
package scouter.agent.batch.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import scouter.agent.batch.ClassDesc;
import scouter.agent.batch.asm.IASM;
import scouter.agent.batch.asm.util.HookingSet;
import scouter.agent.batch.Configure;
import scouter.agent.batch.Logger;
import scouter.agent.batch.asm.jdbc.PsExecuteMV;
import scouter.agent.batch.asm.jdbc.PsInitMV;
import scouter.agent.batch.asm.jdbc.StExecuteMV;
import scouter.agent.batch.trace.TraceSQL;

import java.util.HashSet;

/**
 * BCI for a JDBC PreparedStatement
 * @author @author Paul S.J. Kim(sjkim@whatap.io)
 * @author Gun Lee (gunlee01@gmail.com)
 * @author Munsoo Kwon (mskweon82@daum.net)
 */
public class JDBCPreparedStatementASM implements IASM, Opcodes {
	public final HashSet<String> target = HookingSet.getHookingClassSet(Configure.getInstance().hook_jdbc_pstmt_classes);
	public final HashSet<String> noField = new HashSet<String>();

	public JDBCPreparedStatementASM() {
		target.add("oracle/jdbc/driver/OraclePreparedStatement");
		
		//mariadb older
		target.add("org/mariadb/jdbc/MySQLPreparedStatement");
		//mariadb 1.5.9
		target.add("org/mariadb/jdbc/AbstractPrepareStatement");
		target.add("org/mariadb/jdbc/AbstractMariaDbPrepareStatement");
		target.add("org/mariadb/jdbc/MariaDbClientPreparedStatement");
		target.add("org/mariadb/jdbc/MariaDbServerPreparedStatement");
		//mariadb 1.6.4, 1.7.1
		target.add("org/mariadb/jdbc/MariaDbPreparedStatementClient");
		target.add("org/mariadb/jdbc/MariaDbPreparedStatementServer");

		// mariadb 1.8.0 and 2.0.x
		target.add("org/mariadb/jdbc/ClientSidePreparedStatement");
		target.add("org/mariadb/jdbc/ServerSidePreparedStatement");
		
		target.add("org/postgresql/jdbc2/AbstractJdbc2Statement");
		//pg driver 42+
		target.add("org/postgresql/jdbc/PgPreparedStatement");

		target.add("org/apache/derby/client/am/PreparedStatement");
		target.add("net/sourceforge/jtds/jdbc/JtdsPreparedStatement");
		target.add("jdbc/FakePreparedStatement");
		target.add("jdbc/FakePreparedStatement2");
		target.add("com/microsoft/sqlserver/jdbc/SQLServerPreparedStatement");

		target.add("com/tmax/tibero/jdbc/TbPreparedStatement"); //tibero5
		target.add("com/tmax/tibero/jdbc/driver/TbPreparedStatement"); //tibero6

		target.add("org/hsqldb/jdbc/JDBCPreparedStatement");
		target.add("com/mysql/jdbc/ServerPreparedStatement");
		target.add("com/mysql/jdbc/PreparedStatement");
		target.add("com/mysql/cj/jdbc/ServerPreparedStatement");
        target.add("cubrid/jdbc/driver/CUBRIDPreparedStatement");
		target.add("Altibase/jdbc/driver/AltibasePreparedStatement");
		target.add("Altibase/jdbc/driver/ABPreparedStatement");

		// MySql Connector/j 6.X
        target.add("com/mysql/cj/jdbc/PreparedStatement");
		// MySql Connector/j 8.X
		target.add("com/mysql/cj/jdbc/ServerPreparedStatement");
		target.add("com/mysql/cj/jdbc/ClientPreparedStatement");

        target.add("org/h2/jdbc/JdbcPreparedStatement"); // h2

        // @skyworker - MySQL ServerPreparedStatement는 특별히 필드를 추가하지 않음
        noField.add("com/mysql/jdbc/ServerPreparedStatement");
		noField.add("jdbc/FakePreparedStatement2");
        noField.add("org/mariadb/jdbc/MariaDbClientPreparedStatement");
        noField.add("org/mariadb/jdbc/MariaDbServerPreparedStatement");
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance().sql_enabled == false) {
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
		super(ASM5, cv);
		this.noField = noField;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.owner = name;
		if (noField.contains(name) == false) {
			// add trace fields
			super.visitField(ACC_PUBLIC, TraceSQL.CURRENT_TRACESQL_FIELD, Type.getDescriptor(TraceSQL.class), null, null)
					.visitEnd();
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if ("<init>".equals(name)) {
			return new PsInitMV(access, desc, mv, owner);
		} else {
			if (PsExecuteMV.isTarget(name)) {
				if (desc.startsWith("()")) {
					return new PsExecuteMV(access, desc, mv, owner, name);
				} else if (desc.startsWith("(Ljava/lang/String;)")) {
					return new StExecuteMV(access, desc, mv, owner, name);
				}
            } 
		}
		return mv;
	}
}
