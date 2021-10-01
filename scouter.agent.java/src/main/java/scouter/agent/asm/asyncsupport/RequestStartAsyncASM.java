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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.IASM;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceMain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 2. 21.
 */
public class RequestStartAsyncASM implements IASM, Opcodes {
	private static List<String> preservedAsyncServletStartPatterns = new ArrayList<String>();
	static {
		preservedAsyncServletStartPatterns.add("org.apache.catalina.connector.Request.startAsync(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)Ljavax/servlet/AsyncContext;");
	}

	private Configure conf = Configure.getInstance();
	private List<HookingSet> startTarget;

	public RequestStartAsyncASM() {
		startTarget = HookingSet.getHookingMethodSet(HookingSet.buildPatterns(conf.hook_async_servlet_start_patterns, preservedAsyncServletStartPatterns));
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (conf.hook_async_servlet_enabled == false) {
			return cv;
		}

		for (int i = 0; i < startTarget.size(); i++) {
			HookingSet mset = startTarget.get(i);
			if (mset.classMatch.include(className)) {
				return new RequestCV(cv, mset, className);
			}
		}

		return cv;
	}
}

class RequestCV extends ClassVisitor implements Opcodes {
	String className;
	HookingSet mset;

	public RequestCV(ClassVisitor cv, HookingSet mset, String className) {
		super(ASM9, cv);
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

		return new StartAsyncMV(access, desc, mv);
	}
}


class StartAsyncMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
	private static final String END_METHOD = "endRequestAsyncStart";
	private static final String END_SIGNATURE = "(Ljava/lang/Object;)V";

	private Type returnType;

	public StartAsyncMV(int access, String desc, MethodVisitor mv) {
		super(ASM9, access, desc, mv);
		returnType = Type.getReturnType(desc);
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			Type tp = returnType;
			if(!"javax/servlet/AsyncContext".equals(tp.getInternalName())) {
				return;
			}
			int i = newLocal(tp);
			mv.visitVarInsn(Opcodes.ASTORE, i);
			mv.visitVarInsn(Opcodes.ALOAD, i);
			mv.visitVarInsn(Opcodes.ALOAD, i);

			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, END_METHOD, END_SIGNATURE, false);
		}
		mv.visitInsn(opcode);
	}
}

