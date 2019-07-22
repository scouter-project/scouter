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
import scouter.agent.batch.asm.jdbc.RsInitMV;
import scouter.agent.batch.asm.jdbc.RsNextMV;
import scouter.agent.batch.trace.TraceSQL;

import java.util.HashSet;
public class JDBCResultSetASM implements IASM, Opcodes {
	public final HashSet<String> target = HookingSet.getHookingClassSet(Configure.getInstance().hook_jdbc_rs_classes);
	public JDBCResultSetASM() {
		//mariadb older
		target.add("org/mariadb/jdbc/MySQLResultSet");
		//mariadb 1.5.9
		target.add("org/mariadb/jdbc/internal/queryresults/resultset/MariaSelectResultSet");
		//mariadb 1.6.5
		target.add("org/mariadb/jdbc/internal/com/read/resultset/SelectResultSet");
		//mariadb 1.7.1
		target.add("org/mariadb/jdbc/internal/com/read/resultset/UpdatableResultSet");
		
		target.add("oracle/jdbc/driver/OracleResultSetImpl");
		target.add("com/mysql/jdbc/ResultSetImpl");

		//mysql 1.8x
		target.add("com/mysql/cj/jdbc/result/ResultSetImpl");

		target.add("org/postgresql/jdbc2/AbstractJdbc2ResultSet");
		//pg driver 42+
		target.add("org/postgresql/jdbc/PgResultSet");

		target.add("org/apache/derby/client/am/ResultSet");
		target.add("jdbc/FakeResultSet");
		target.add("net/sourceforge/jtds/jdbc/JtdsResultSet");
		target.add("com/microsoft/sqlserver/jdbc/SQLServerResultSet");
		target.add("oracle/jdbc/driver/InsensitiveScrollableResultSet");
		target.add("oracle/jdbc/driver/SensitiveScrollableResultSet");
		target.add("org/hsqldb/jdbc/JDBCResultSet");
		target.add("cubrid/jdbc/driver/CUBRIDResultSet");
		target.add("org/mariadb/jdbc/MariaDbResultSet");

		target.add("com/tmax/tibero/jdbc/TbResultSetBase"); //tibero5
		target.add("com/tmax/tibero/jdbc/driver/TbResultSetBase"); //tibero6

		target.add("Altibase/jdbc/driver/ABResultSet"); //tibero6

        target.add("org/h2/jdbc/JdbcResultSet"); //h2
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance().sql_enabled == false) {
			return cv;
		}
		if (target.contains(className) == false) {
			return cv;
		}
		Logger.println("A107", "jdbc rs found: " + className);
		return new ResultSetCV(cv);
	}
}
class ResultSetCV extends ClassVisitor implements Opcodes {
	private String owner;
	public ResultSetCV(ClassVisitor cv) {
		super(ASM5, cv);
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		super.visitField(ACC_PUBLIC, TraceSQL.CURRENT_TRACESQL_FIELD, Type.getDescriptor(TraceSQL.class), null, null)
		.visitEnd();
		this.owner = name;
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

		if ("<init>".equals(name)) {
			return new RsInitMV(access, owner, desc, mv);
		} else if ("next".equals(name) && "()Z".equals(desc)) {
			return new RsNextMV(owner, mv);
		}
		return mv;
	}
}
