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
import java.util.HashSet;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.jdbc.P0ClearParametersMV;
import scouter.agent.asm.jdbc.P0ExecuteMV;
import scouter.agent.asm.jdbc.P0InitMV;
import scouter.agent.asm.jdbc.P0SetMV;
import scouter.agent.asm.jdbc.PsClearParametersMV;
import scouter.agent.asm.jdbc.PsExecuteMV;
import scouter.agent.asm.jdbc.PsInitMV;
import scouter.agent.asm.jdbc.PsSetMV;
import scouter.agent.asm.jdbc.StExecuteMV;
import scouter.agent.trace.SqlParameter;
import scouter.agent.trace.TraceSQL;
import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.Type;
public class JDBCPreparedStatementASM implements IASM, Opcodes {
	public  final HashSet<String> target = new HashSet<String>();
	public JDBCPreparedStatementASM() {
		target.add("org/mariadb/jdbc/MySQLPreparedStatement");
		target.add("oracle/jdbc/driver/OraclePreparedStatement");
		target.add("com/mysql/jdbc/PreparedStatement");
		target.add("org/postgresql/jdbc2/AbstractJdbc2Statement");
		target.add("org/apache/derby/client/am/PreparedStatement");
		target.add("jdbc/FakePreparedStatement");
		target.add("net/sourceforge/jtds/jdbc/JtdsPreparedStatement");
			
		target.add("com/microsoft/sqlserver/jdbc/SQLServerPreparedStatement");
		target.add("com/tmax/tibero/jdbc/TbPreparedStatement");
		target.add("org/hsqldb/jdbc/JDBCPreparedStatement");
	}
	public boolean isTarget(String className) {
		return target.contains(className) ;
	}
	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (target.contains(className) == false) {
			return cv;
		}
		if(Configure.getInstance().enable_asm_jdbc==false)
			return cv;
		Logger.println("A106", "jdbc pstmt found: " + className + "  redefinable="+Configure.JDBC_REDEFINED);
		return new PreparedStatementCV(cv);
	}
}
class PreparedStatementCV extends ClassVisitor implements Opcodes {
	public PreparedStatementCV(ClassVisitor cv) {
		super(ASM4, cv);
	}
	private String owner;
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		//add dummy field
		
		if(Configure.JDBC_REDEFINED==false){
			super.visitField(ACC_PUBLIC, TraceSQL.PSTMT_PARAM_FIELD, Type.getDescriptor(SqlParameter.class), null, null).visitEnd();
		}
		this.owner = name;
	}
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if(Configure.JDBC_REDEFINED){
			return ifRedefined(access, name, desc, mv);
		}else{
			return ifNotRedefined(access, name, desc, mv);
		}
	}
	private MethodVisitor ifRedefined(int access, String name, String desc, MethodVisitor mv) {
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
					return new PsExecuteMV(access, desc, mv, owner);
				} else if (desc.startsWith("(Ljava/lang/String;)")) {
					return new StExecuteMV(access, desc, mv, owner);
				}
			} else if ("clearParameters".equals(name) && "()V".equals(desc)) {
				return new PsClearParametersMV(access, desc, mv, owner);
			}
		}
		return mv;
	}
	private MethodVisitor ifNotRedefined(int access, String name, String desc, MethodVisitor mv) {
		if ("<init>".equals(name)) {
			return new P0InitMV(access, desc, mv, owner);
		} else {
			String targetDesc = P0SetMV.getSetSignature(name);
			if (targetDesc != null) {
				if (targetDesc.equals(desc)) {
					return new P0SetMV(access, name, desc, mv, owner);
				}
			} else if (P0ExecuteMV.isTarget(name)) {
				if (desc.startsWith("()")) {
					return new P0ExecuteMV(access, desc, mv, owner);
				} else if (desc.startsWith("(Ljava/lang/String;)")) {
					return new StExecuteMV(access, desc, mv, owner);
				}
			} else if ("clearParameters".equals(name) && "()V".equals(desc)) {
				return new P0ClearParametersMV(access, desc, mv, owner);
			}
		}
		return mv;
	}
}
