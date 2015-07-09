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
import scouter.agent.trace.TraceSQL;
import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.commons.LocalVariablesSorter;

public class JDBCLeakDetectorASM implements IASM, Opcodes {
	private List<MethodSet> target = MethodSet.getHookingMethodSet(Configure.getInstance().hook_dbc_open_detect);
	private Map<String, MethodSet> reserved = new HashMap<String, MethodSet>();

	public JDBCLeakDetectorASM() {
		// Tomcat7
		// AsmUtil.add(reserved, "org/apache/tomcat/dbcp/dbcp/BasicDataSource",
		// "getConnection");
		// AsmUtil.add(reserved, "org/apache/tomcat/jdbc/pool/ConnectionPool",
		// "getConnection");
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
		if (Configure.getInstance().enable_asm_jdbc == false)
			return cv;

		MethodSet mset = reserved.get(className);
		if (mset != null)
			return new JDBCLeakDetectorCV(cv, mset, className);

		for (int i = 0; i < target.size(); i++) {
			mset = target.get(i);
			if (mset.classMatch.include(className)) {
				return new JDBCLeakDetectorCV(cv, mset, className);
			}
		}
		return cv;
	}
}

class JDBCLeakDetectorCV extends ClassVisitor implements Opcodes {

	public String className;
	private MethodSet mset;

	public JDBCLeakDetectorCV(ClassVisitor cv, MethodSet mset, String className) {
		super(ASM4, cv);
		this.mset = mset;
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (mv == null || mset.isA(name, desc) == false) {
			return mv;
		}
		if (AsmUtil.isSpecial(name) || desc.endsWith("Ljava/sql/Connection;") == false) {
			return mv;
		}
		return new JDBCLeakDetectorMV(access, desc, mv);
	}
}

// ///////////////////////////////////////////////////////////////////////////
class JDBCLeakDetectorMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACE_SQL = TraceSQL.class.getName().replace('.', '/');
	private final static String METHOD = "detectConnection";
	private static final String SIGNATURE = "(Ljava/sql/Connection;)Ljava/sql/Connection;";

	public JDBCLeakDetectorMV(int access, String desc, MethodVisitor mv) {
		super(Opcodes.ASM4, access, desc, mv);
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_SQL, METHOD, SIGNATURE, false);
		}
		mv.visitInsn(opcode);
	}
}