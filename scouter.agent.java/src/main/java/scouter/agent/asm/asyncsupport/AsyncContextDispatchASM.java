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
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.IASM;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceMain;

import java.util.ArrayList;
import java.util.List;

import static scouter.agent.asm.asyncsupport.AsyncContextDispatchASM.ON_COMPLETE_SELF_DISPATCH_METHOD;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 2. 24.
 */
public class AsyncContextDispatchASM implements IASM, Opcodes {
	private static List<String> preservedAsyncContextDispatchPatterns = new ArrayList<String>();
	public final static String ON_COMPLETE_SELF_DISPATCH_METHOD = "dispatch()V";
	static {
		preservedAsyncContextDispatchPatterns.add("org.apache.catalina.core.AsyncContextImpl.dispatch(Ljavax/servlet/ServletContext;Ljava/lang/String;)V");
	}

	private Configure conf = Configure.getInstance();
	private List<HookingSet> dispatchTarget;

	public AsyncContextDispatchASM() {
		dispatchTarget = HookingSet.getHookingMethodSet(HookingSet.buildPatterns(conf.hook_async_context_dispatch_patterns, preservedAsyncContextDispatchPatterns));
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (conf.hook_async_servlet_enabled == false) {
			return cv;
		}

		for (int i = 0; i < dispatchTarget.size(); i++) {
			HookingSet mset = dispatchTarget.get(i);
			if (mset.classMatch.include(className)) {
				return new AsyncContextCV(cv, mset, className);
			}
		}

		return cv;
	}
}

class AsyncContextCV extends ClassVisitor implements Opcodes {
	String className;
	HookingSet mset;

	public AsyncContextCV(ClassVisitor cv, HookingSet mset, String className) {
		super(ASM9, cv);
		this.mset = mset;
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (mv == null || mset.isA(name, desc) == false) {
			if(!ON_COMPLETE_SELF_DISPATCH_METHOD.equals(name + desc)) {
				return mv;
			}
		}
		if (AsmUtil.isSpecial(name)) {
			return mv;
		}

		return new DispatchMV(access, name, desc, mv);
	}
}


class DispatchMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
	private static final String START_METHOD = "dispatchAsyncServlet";
	private static final String START_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/String;)V";

	private static final String SELF_DISPATCH_METHOD = "selfDispatchAsyncServlet";
	private static final String SELF_DISPATCH_SIGNATURE = "(Ljava/lang/Object;)V";

	String name;
	String desc;

	public DispatchMV(int access, String name, String desc, MethodVisitor mv) {
		super(ASM9, access, desc, mv);
		this.name = name;
		this.desc = desc;
	}

	@Override
	public void visitCode() {
		if (ON_COMPLETE_SELF_DISPATCH_METHOD.equals(name+desc)) {
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, SELF_DISPATCH_METHOD, SELF_DISPATCH_SIGNATURE, false);
		} else {
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitVarInsn(Opcodes.ALOAD, 2);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_METHOD, START_SIGNATURE, false);
		}
		mv.visitCode();
	}
}
