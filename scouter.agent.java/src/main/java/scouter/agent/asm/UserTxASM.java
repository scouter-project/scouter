/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scouter.agent.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.trace.TraceSQL;

public class UserTxASM implements IASM, Opcodes {

	public UserTxASM() {
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance()._hook_usertx_enabled == false)
			return cv;

		for (int i = 0; i < classDesc.interfaces.length; i++) {
			if ("javax/transaction/UserTransaction".equals(classDesc.interfaces[i])) {
				return new UserTxCV(cv);
			}
		}
		return cv;
	}
}

// ///////////////////////////////////////////////////////////////////////////

class UserTxCV extends ClassVisitor implements Opcodes {

	public UserTxCV(ClassVisitor cv) {
		super(ASM9, cv);
	}
	@Override
	public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
		if ("begin".equals(methodName) && "()V".equals(desc)) {
			return new UTXOpenMV(access, desc, mv);
		}
		if (("commit".equals(methodName) && "()V".equals(desc)) 
				|| ("rollback".equals(methodName)  && "()V".equals(desc))) {
			return new UTXCloseMV(access, desc, mv, methodName);
		}
		return mv;

	}
}

// ///////////////////////////////////////////////////////////////////////////
class UTXOpenMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACE_SQL = TraceSQL.class.getName().replace('.', '/');
	private static final String METHOD = "userTxOpen";
	private static final String SIGNATURE = "()V";

	public UTXOpenMV(int access, String desc, MethodVisitor mv) {
		super(ASM9, access, desc, mv);
	}

	@Override
	public void visitCode() {
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_SQL, METHOD, SIGNATURE, false);
		super.visitCode();
	}
}

// ///////////////////////////////////////////////////////////////////////////
class UTXCloseMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACE_SQL = TraceSQL.class.getName().replace('.', '/');
	private static final String METHOD = "userTxClose";
	private static final String SIGNATURE = "(Ljava/lang/String;)V";

	private String method;

	public UTXCloseMV(int access, String desc, MethodVisitor mv, String method) {
		super(ASM9, access, desc, mv);
		this.method = method;
	}

	@Override
	public void visitCode() {
		AsmUtil.PUSH(mv, method);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_SQL, METHOD, SIGNATURE, false);
		super.visitCode();
	}
}
