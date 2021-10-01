package scouter.agent.asm.asyncsupport;
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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.IASM;
import scouter.agent.trace.TraceReactive;

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 30/07/2020
 */
public class ThreadASM implements IASM, Opcodes {

	private Configure conf = Configure.getInstance();

	@Override
	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (conf._hook_thread_name_enabled == false) {
			return cv;
		}

		if ("java/lang/Thread".equals(className)){
			return new ThreadCV(cv, className);
		}

		return cv;
	}
}

class ThreadCV extends ClassVisitor implements Opcodes {

	private String className;
	public ThreadCV(ClassVisitor cv, String className) {
		super(ASM9, cv);
		this.className = className;
		Logger.println("G001", "Thread.class - " + className);
	}
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (mv == null) {
			return mv;
		}
		if ("setName".equals(name)) {
			return new ThreadNameMV(access, desc, mv, className);
		}
		return mv;
	}
}

class ThreadNameMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACE = TraceReactive.class.getName().replace('.', '/');

	private String className;

	public ThreadNameMV(int access, String desc, MethodVisitor mv, String className) {
		super(ASM9, access, desc, mv);
		this.className = className;
	}

	@Override
	public void visitCode() {
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, "threadSetName", "(Ljava/lang/Thread;Ljava/lang/String;)V", false);

		mv.visitCode();
	}
}
