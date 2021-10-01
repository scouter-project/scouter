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
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceSQL;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.Set;

public class InitialContextASM implements IASM, Opcodes {
	private Set<String> target = HookingSet.getClassSet(Configure.getInstance().hook_context_classes);

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance()._hook_dbconn_enabled == false) {
			return cv;
		}
		if (target.contains(className)) {
			return new InitialContextCV(cv, className);
		}
		return cv;
	}

}

// ///////////////////////////////////////////////////////////////////////////
class InitialContextCV extends ClassVisitor implements Opcodes {

	public String className;
	public InitialContextCV(ClassVisitor cv, String className) {
		super(ASM9, cv);
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (mv == null) {
			return mv;
		}
		if ("lookup".equals(name) && "(Ljava/lang/String;)Ljava/lang/Object;".equals(desc)) {
			return new InitialContextMV(access, desc, mv, className, name, desc);
		}
		return mv;

	}
}

// ///////////////////////////////////////////////////////////////////////////
class InitialContextMV extends LocalVariablesSorter implements Opcodes {
	private static final String CLASS = TraceSQL.class.getName().replace('.', '/');
	private static final String METHOD = "ctxLookup";
	private static final String SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;)V";

	private Type returnType;

	public InitialContextMV(int access, String desc, MethodVisitor mv, String classname, String methodname,
			String methoddesc) {
		super(ASM9, access, desc, mv);
		this.returnType = Type.getReturnType(desc);
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			code();
		}
		mv.visitInsn(opcode);
	}

	private void code() {
		int i = newLocal(returnType);

		mv.visitVarInsn(Opcodes.ASTORE, i);
		mv.visitVarInsn(Opcodes.ALOAD, i);

		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, i);

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, CLASS, METHOD, SIGNATURE, false);
	}

}
