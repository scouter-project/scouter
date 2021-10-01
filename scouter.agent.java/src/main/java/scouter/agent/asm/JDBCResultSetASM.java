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
import scouter.agent.asm.jdbc.RsCloseMV;
import scouter.agent.asm.jdbc.RsInitMV;
import scouter.agent.asm.jdbc.RsNextMV;
import scouter.agent.asm.util.HookingSet;

import java.util.HashSet;
public class JDBCResultSetASM implements IASM, Opcodes {
	public final HashSet<String> target = HookingSet.getHookingClassSet(Configure.getInstance().hook_jdbc_rs_classes);
	public final HashSet<String> onlyCloseTarget = new HashSet<String>();
	public final HashSet<String> onlyNextTarget = new HashSet<String>();

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

		//only close
		String alti1 = "Altibase/jdbc/driver/AltibaseResultSet";
		target.add(alti1); onlyCloseTarget.add(alti1);

		//only next
		String alti2 = "Altibase/jdbc/driver/AltibaseEmptyResultSet";
		String alti3 = "Altibase/jdbc/driver/AltibaseForwardOnlyResultSet";
		String alti4 = "Altibase/jdbc/driver/AltibaseScrollInsensitiveResultSet";
		String alti5 = "Altibase/jdbc/driver/AltibaseLightWeightResultSet";
		String alti6 = "Altibase/jdbc/driver/AltibaseTempResultSet";
		String alti7 = "Altibase/jdbc/driver/AltibaseUpdatableResultSet";

		target.add(alti2); onlyNextTarget.add(alti2);
		target.add(alti3); onlyNextTarget.add(alti3);
		target.add(alti4); onlyNextTarget.add(alti4);
		target.add(alti5); onlyNextTarget.add(alti5);
		target.add(alti6); onlyNextTarget.add(alti6);
		target.add(alti7); onlyNextTarget.add(alti7);
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance()._hook_dbsql_enabled == false) {
			return cv;
		}
		if (target.contains(className) == false) {
			return cv;
		}
		Logger.println("A107", "jdbc rs found: " + className);

		Scope scope = Scope.ALL;
		if (onlyCloseTarget.contains(className)) {
			scope = Scope.ONLYINIT;
		} else if (onlyNextTarget.contains(className)) {
			scope = Scope.ONLYNEXT;
		}

		return new ResultSetCV(cv, scope);
	}

	protected static enum Scope {
		ALL, ONLYINIT, ONLYNEXT,;
	}
}

class ResultSetCV extends ClassVisitor implements Opcodes {
	JDBCResultSetASM.Scope scope;

	public ResultSetCV(ClassVisitor cv, JDBCResultSetASM.Scope scope) {
		super(ASM9, cv);
		this.scope = scope;
	}
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

		if ("<init>".equals(name) && scope != JDBCResultSetASM.Scope.ONLYNEXT) {
			return new RsInitMV(access, desc, mv);
		} else if ("next".equals(name) && "()Z".equals(desc) && scope != JDBCResultSetASM.Scope.ONLYINIT) {
			return new RsNextMV(mv);
		} else if ("close".equals(name) && "()V".equals(desc) && scope != JDBCResultSetASM.Scope.ONLYNEXT) {
			return new RsCloseMV(mv);
		}
		return mv;
	}
}
