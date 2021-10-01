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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.jdbc.PsUpdateCountMV;
import scouter.agent.asm.jdbc.PsCloseMV;
import scouter.agent.asm.jdbc.StExecuteMV;
import scouter.agent.asm.jdbc.StInitMV;
import scouter.agent.asm.util.HookingSet;

import java.util.HashSet;

/**
 * BCI for a JDBC Statement
 * @author @author Paul S.J. Kim(sjkim@whatap.io)
 * @author Gun Lee (gunlee01@gmail.com)
 * @author Eunsu Kim
 */
public class JDBCStatementASM implements IASM, Opcodes {
	public final HashSet<String> target =  HookingSet.getHookingClassSet(Configure.getInstance().hook_jdbc_stmt_classes);
	public JDBCStatementASM() {
		target.add("org/mariadb/jdbc/MariaDbStatement");
		target.add("org/mariadb/jdbc/MySQLStatement");
		target.add("oracle/jdbc/driver/OracleStatement");

		//pg driver 42+
		target.add("org/postgresql/jdbc/PgStatement");

		target.add("com/mysql/jdbc/StatementImpl");
		target.add("org/apache/derby/client/am/Statement");
		target.add("jdbc/FakeStatement");
		target.add("net/sourceforge/jtds/jdbc/JtdsStatement");
		target.add("com/microsoft/sqlserver/jdbc/SQLServerStatement");

		target.add("com/tmax/tibero/jdbc/TbStatement"); //tibero5
		target.add("com/tmax/tibero/jdbc/driver/TbStatement"); //tibero6

		target.add("org/hsqldb/jdbc/JDBCStatement");
		target.add("cubrid/jdbc/driver/CUBRIDStatement");
		target.add("Altibase/jdbc/driver/AltibaseStatement");
		target.add("Altibase/jdbc/driver/ABStatement");
        target.add("org/h2/jdbc/JdbcStatement"); //h2
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance()._hook_dbsql_enabled == false) {
			return cv;
		}
		if (target.contains(className) == false) {
			return cv;
		}
		Logger.println("A108", "jdbc stmt found: " + className);
		return new StatementCV(cv);
	}
}
class StatementCV extends ClassVisitor implements Opcodes {
	private String owner;
	public StatementCV(ClassVisitor cv) {
		super(ASM9, cv);
	}
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.owner = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if ("<init>".equals(name)) {
			return new StInitMV(access, desc, mv);
		}

		if (StExecuteMV.isTarget(name)) {
			if (desc.startsWith("(Ljava/lang/String;)")) {
				return new StExecuteMV(access, desc, mv, owner, name);
			}
		} else if ("getUpdateCount".equals(name) && "()I".equals(desc)) {
			return new PsUpdateCountMV(mv);
		} else if ("close".equals(name) && "()V".equals(desc)) {
			return new PsCloseMV(mv);
		}
		return mv;
	}
}
