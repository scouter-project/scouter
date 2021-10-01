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

package scouter.agent.asm.asyncsupport;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.IASM;
import scouter.agent.trace.TraceReactive;

public class CoroutineThreadNameASM implements IASM, Opcodes {

	private Configure conf = Configure.getInstance();

	public CoroutineThreadNameASM() {
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (conf._hook_coroutine_debugger_hook_enabled == false) {
			return cv;
		}

		if ("kotlinx/coroutines/CoroutineId".equals(className)) {
			return new CoroutineIdCV(cv, className);
		}
		return cv;
	}
}

class CoroutineIdCV extends ClassVisitor implements Opcodes {

	public String className;

	public CoroutineIdCV(ClassVisitor cv, String className) {
		super(ASM9, cv);
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (mv == null) {
			return mv;
		}

		if ("updateThreadContext".equals(name) && "(Lkotlin/coroutines/CoroutineContext;)Ljava/lang/String;".equals(desc)) {
			return new CoroutineIdUpdateThreadContextMV(access, desc, mv, className);

		} else if ("restoreThreadContext".equals(name) && "(Lkotlin/coroutines/CoroutineContext;Ljava/lang/String;)V".equals(desc)) {
			return new CoroutineIdRestoreThreadContextMV(access, desc, mv, className);
		}
		return mv;
	}
}

class CoroutineIdUpdateThreadContextMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACE = TraceReactive.class.getName().replace('.', '/');

	private Label startFinally = new Label();
	private String className;

	public CoroutineIdUpdateThreadContextMV(int access, String desc, MethodVisitor mv, String className) {
		super(ASM9, access, desc, mv);
		this.className = className;
	}

	@Override
	public void visitCode() {
		mv.visitVarInsn(ALOAD, 0);

		mv.visitFieldInsn(GETFIELD, className, "id", "J");
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, "startCoroutineIdUpdateThreadContext", "(J)V", false);
		mv.visitLabel(startFinally);
		mv.visitCode();
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, "endCoroutineIdUpdateThreadContext", "()V", false);
		}
		mv.visitInsn(opcode);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		Label endFinally = new Label();
		mv.visitTryCatchBlock(startFinally, endFinally, endFinally, null);
		mv.visitLabel(endFinally);

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, "endCoroutineIdUpdateThreadContext", "()V", false);
		mv.visitInsn(ATHROW);
		mv.visitMaxs(maxStack + 8, maxLocals + 2);
	}
}


class CoroutineIdRestoreThreadContextMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACE = TraceReactive.class.getName().replace('.', '/');

	private String className;

	public CoroutineIdRestoreThreadContextMV(int access, String desc, MethodVisitor mv, String className) {
		super(ASM9, access, desc, mv);
		this.className = className;
	}

	@Override
	public void visitCode() {
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, "startCoroutineIdRestoreThreadContext", "(Ljava/lang/Object;)V", false);
		mv.visitCode();
	}
}
