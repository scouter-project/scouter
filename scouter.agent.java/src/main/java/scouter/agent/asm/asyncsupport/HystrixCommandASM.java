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

import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.IASM;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceMain;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 2. 21.
 */
public class HystrixCommandASM implements IASM, Opcodes {
	private Configure conf = Configure.getInstance();
	private List<HookingSet> prepareTarget = HookingSet.getHookingMethodSet("");
	private List<HookingSet> receiveTarget = HookingSet.getHookingMethodSet("");
	private Map<String, HookingSet> prepareReserved = new HashMap<String, HookingSet>();
	private Map<String, HookingSet> receiveReserved = new HashMap<String, HookingSet>();
	private Map<String, HookingSet> receiveReservedSuperType = new HashMap<String, HookingSet>();

	public HystrixCommandASM() {
		if (conf.hook_hystrix_enabled) {
			AsmUtil.add(prepareReserved, "com/netflix/hystrix/HystrixCommand", "execute()Ljava/lang/Object;");
			AsmUtil.add(receiveReserved, "com/netflix/hystrix/contrib/javanica/command/GenericCommand", "run()Ljava/lang/Object;");
			AsmUtil.add(receiveReservedSuperType, "com/netflix/hystrix/HystrixCommand", "run()Ljava/lang/Object;");
		}
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		HookingSet mset = prepareReserved.get(className);
		if (mset != null){
			return new HystrixCommandPrepareCV(cv, mset, className);
		}
		mset = receiveReserved.get(className);
		if (mset != null){
			return new HystrixCommandReceiveCV(cv, mset, className);
		}
		mset = receiveReservedSuperType.get(classDesc.superName);
		if (mset != null){
			return new HystrixCommandReceiveCV(cv, mset, className);
		}
		return cv;
	}
}

class HystrixCommandPrepareCV extends ClassVisitor implements Opcodes {
	String className;
	private HookingSet mset;

	public HystrixCommandPrepareCV(ClassVisitor cv, HookingSet mset, String className) {
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
		return new HystrixCommandPrepareMV(access, name, desc, mv);
	}
}

class HystrixCommandReceiveCV extends ClassVisitor implements Opcodes {
	String className;
	private HookingSet mset;

	public HystrixCommandReceiveCV(ClassVisitor cv, HookingSet mset, String className) {
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
		return new HystrixCommandReceiveMV(access, name, desc, mv);
	}
}

class HystrixCommandReceiveMV extends LocalVariablesSorter implements Opcodes {
	private static final String TARGET = TraceMain.class.getName().replace('.', '/');
	private static final String START_METHOD = "callRunnableCallInvoked";
	private static final String START_SIGNATURE = "(Ljava/lang/Object;)Ljava/lang/Object;";

	private static final String END_METHOD = "callRunnableCallEnd";
	private static final String END_METHOD_DESC = "(" +
			"Ljava/lang/Object;" +
			"Ljava/lang/Object;" +
			"Ljava/lang/Throwable;" +
			")V";

	private Label startFinally = new Label();
	private Type returnType;
	String name;
	String desc;
	private int statIdx;

	public HystrixCommandReceiveMV(int access, String name, String desc, MethodVisitor mv) {
		super(ASM9, access, desc, mv);
		this.name = name;
		this.desc = desc;
		this.returnType = Type.getReturnType(desc);
	}

	@Override
	public void visitCode() {
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, START_METHOD, START_SIGNATURE, false);

		statIdx = newLocal(Type.getType(Object.class));
		mv.visitVarInsn(Opcodes.ASTORE, statIdx);
		mv.visitLabel(startFinally);
		mv.visitCode();
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			capReturn();
		}
		mv.visitInsn(opcode);
	}

	private void capReturn() {
		Type tp = returnType;

		if (tp == null || tp.equals(Type.VOID_TYPE)) {
			mv.visitInsn(Opcodes.ACONST_NULL);
			mv.visitVarInsn(Opcodes.ALOAD, statIdx);
			mv.visitInsn(Opcodes.ACONST_NULL);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, END_METHOD, END_METHOD_DESC, false);
			return;
		}

		switch (tp.getSort()) {
			case Type.DOUBLE:
			case Type.LONG:
				mv.visitInsn(Opcodes.DUP2);
				break;
			default:
				mv.visitInsn(Opcodes.DUP);
		}
		// TODO method return test dup and store
//		int rtnIdx = newLocal(tp);
//		mv.visitVarInsn(Opcodes.ASTORE, rtnIdx);
//		mv.visitVarInsn(Opcodes.ALOAD, rtnIdx);

		mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
		mv.visitInsn(Opcodes.ACONST_NULL);// throwable
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, END_METHOD, END_METHOD_DESC, false);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		Label endFinally = new Label();
		mv.visitTryCatchBlock(startFinally, endFinally, endFinally, null);
		mv.visitLabel(endFinally);
		mv.visitInsn(DUP);
		int errIdx = newLocal(Type.getType(Throwable.class));
		mv.visitVarInsn(Opcodes.ASTORE, errIdx);

		mv.visitInsn(Opcodes.ACONST_NULL);// return
		mv.visitVarInsn(Opcodes.ALOAD, statIdx);
		mv.visitVarInsn(Opcodes.ALOAD, errIdx);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, END_METHOD, END_METHOD_DESC,false);
		mv.visitInsn(ATHROW);
		mv.visitMaxs(maxStack + 8, maxLocals + 2);
	}
}

class HystrixCommandPrepareMV extends LocalVariablesSorter implements Opcodes {
	private static final String TARGET = TraceMain.class.getName().replace('.', '/');
	private static final String START_METHOD = "hystrixPrepareInvoked";
	private static final String START_SIGNATURE = "(Ljava/lang/Object;)V";

	String name;
	String desc;

	public HystrixCommandPrepareMV(int access, String name, String desc, MethodVisitor mv) {
		super(ASM9, access, desc, mv);
		this.name = name;
		this.desc = desc;
	}

	@Override
	public void visitCode() {
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, START_METHOD, START_SIGNATURE, false);
		mv.visitCode();
	}
}
