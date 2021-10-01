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

public class MonoKtASM implements IASM, Opcodes {

	private Configure conf = Configure.getInstance();

	public MonoKtASM() {
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (conf._hook_coroutine_enabled == false) {
			return cv;
		}
		if (conf._hook_reactive_enabled == false) {
			return cv;
		}

		if ("kotlinx/coroutines/reactor/MonoKt".equals(className)) {
			return new MonoKtCV(cv, className);
		}
		return cv;
	}
}

class MonoKtCV extends ClassVisitor implements Opcodes {

	public String className;

	public MonoKtCV(ClassVisitor cv, String className) {
		super(ASM9, cv);
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (mv == null) {
			return mv;
		}

		if ("mono".equals(name) && "(Lkotlin/coroutines/CoroutineContext;Lkotlin/jvm/functions/Function2;)Lreactor/core/publisher/Mono;".equals(desc)) {
			return new MonoKtMV(access, desc, mv, className);

		}
		return mv;
	}
}

class MonoKtMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACE = TraceReactive.class.getName().replace('.', '/');

	private Label startFinally = new Label();
	private String className;

	public MonoKtMV(int access, String desc, MethodVisitor mv, String className) {
		super(ASM9, access, desc, mv);
		this.className = className;
	}

	@Override
	public void visitCode() {
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, "startMonoKtMono", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
		mv.visitVarInsn(ASTORE, 0);
		mv.visitLabel(startFinally);
		mv.visitCode();
	}
}
