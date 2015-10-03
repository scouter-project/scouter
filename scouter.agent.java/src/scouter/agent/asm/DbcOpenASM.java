/*
 *  Copyright 2015 the original author or authors.
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
import scouter.org.objectweb.asm.Label;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.Type;
import scouter.org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.util.HashUtil;
import scouter.util.StringUtil;

public class DbcOpenASM implements IASM, Opcodes {
	private List<MethodSet> target = MethodSet.getHookingMethodSet(Configure.getInstance().hook_dbopen);
	private Map<String, MethodSet> reserved = new HashMap<String, MethodSet>();

	public DbcOpenASM() {
		// Tomcat7
		AsmUtil.add(reserved, "org/apache/tomcat/dbcp/dbcp/BasicDataSource", "getConnection");

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
			return new DbcOpenCV(cv, mset, className);

		for (int i = 0; i < target.size(); i++) {
			mset = target.get(i);
			if (mset.classMatch.include(className)) {
				return new DbcOpenCV(cv, mset, className);
			}
		}
		return cv;
	}
}

class DbcOpenCV extends ClassVisitor implements Opcodes {

	public String className;
	private MethodSet mset;

	public DbcOpenCV(ClassVisitor cv, MethodSet mset, String className) {
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
		if (AsmUtil.isSpecial(name)) {
			return mv;
		}

		String fullname = "OPEN " + StringUtil.cutLastString(className, '/') + "." + name;
		int fullname_hash = DataProxy.sendMethodName(fullname);

		return new DbcOpenMV(access, desc, mv, fullname, fullname_hash);
	}
}

// ///////////////////////////////////////////////////////////////////////////
class DbcOpenMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACE_SQL = TraceSQL.class.getName().replace('.', '/');
	private final static String START_METHOD = "dbcOpenStart";
	private static final String START_SIGNATURE = "(ILjava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;";
	private final static String END_METHOD = "dbcOpenEnd";
	private static final String END_SIGNATURE = "(Ljava/sql/Connection;Ljava/lang/Object;)Ljava/sql/Connection;";
	private static final String END_SIGNATURE2 = "(Ljava/lang/Object;Ljava/lang/Throwable;)V";

	private Label startFinally = new Label();

	public DbcOpenMV(int access, String desc, MethodVisitor mv, String fullname, int fullname_hash) {
		super(Opcodes.ASM4, access, desc, mv);
		this.fullname = fullname;
		this.fullname_hash = fullname_hash;
		this.isStatic = (access & ACC_STATIC) != 0;
	}

	private int fullname_hash;
	private String fullname;
	private int statIdx;
	private boolean isStatic = false;

	@Override
	public void visitCode() {
		AsmUtil.PUSH(mv, fullname_hash);
		mv.visitLdcInsn(fullname);
		if (isStatic) {
			AsmUtil.PUSHNULL(mv);
		} else {
			mv.visitVarInsn(Opcodes.ALOAD, 0);
		}

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_SQL, START_METHOD, START_SIGNATURE, false);

		statIdx = newLocal(Type.getType(Object.class));

		mv.visitVarInsn(Opcodes.ASTORE, statIdx);
		mv.visitLabel(startFinally);
		mv.visitCode();
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitVarInsn(Opcodes.ALOAD, statIdx);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_SQL, END_METHOD, END_SIGNATURE, false);
		}
		mv.visitInsn(opcode);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		Label endFinally = new Label();
		mv.visitTryCatchBlock(startFinally, endFinally, endFinally, null);
		mv.visitLabel(endFinally);
		mv.visitInsn(DUP);
		int errIdx = newLocal(Type.getType(Throwable.class));
		mv.visitVarInsn(Opcodes.ASTORE, errIdx);

		mv.visitVarInsn(Opcodes.ALOAD, statIdx);
		mv.visitVarInsn(Opcodes.ALOAD, errIdx);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_SQL, END_METHOD, END_SIGNATURE2, false);
		mv.visitInsn(ATHROW);
		mv.visitMaxs(maxStack + 8, maxLocals + 2);
	}
}