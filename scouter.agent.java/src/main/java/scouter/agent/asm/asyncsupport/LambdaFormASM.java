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

import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.ILASM;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.trace.TraceMain;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 2. 24.
 */
public class LambdaFormASM implements ILASM, Opcodes {
	private Configure conf = Configure.getInstance();

	@Override
	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc, String lambdaMethodName, String lambdaMethodDesc, String factoryMethodName, String factoryMethodDesc) {
//		if (conf.hook_async_servlet_enabled == false) {
//			return cv;
//		}
		return new LambdaFormCV(cv, className, lambdaMethodName, lambdaMethodDesc, factoryMethodName, factoryMethodDesc);
	}
}

class LambdaFormCV extends ClassVisitor implements Opcodes {
	String className;
	String lambdaMethodName;
	String lambdaMethodDesc;
	String factoryMethodName;
	String factoryMethodDesc;

	public LambdaFormCV(ClassVisitor cv, String className, String lambdaMethodName, String lambdaMethodDesc, String factoryMethodName, String factoryMethodDesc) {
		super(ASM9, cv);
		this.className = className;
		this.lambdaMethodName = lambdaMethodName;
		this.lambdaMethodDesc = lambdaMethodDesc;
		this.factoryMethodName = factoryMethodName;
		this.factoryMethodDesc = factoryMethodDesc;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

		if (name.equals(lambdaMethodName) && desc.equals(lambdaMethodDesc)) {
			String fullName = AsmUtil.makeMethodFullName(className, name, desc);
			if((access & ACC_STATIC) != 0) return mv;

			return new LambdaMV(access, name, desc, mv,
					fullName, Type.getArgumentTypes(desc), className);

		} else if (name.equals(factoryMethodName) && desc.equals(factoryMethodDesc)) {
			return new FacotoryMV(access, name, desc, mv);
		} else {
			return mv;
		}
	}
}

class LambdaMV extends LocalVariablesSorter implements Opcodes {
	private static final String TARGET = TraceMain.class.getName().replace('.', '/');
	private static final String START_METHOD = "startAsyncPossibleService";
	private static final String START_METHOD_DESC = "(" +
			"Ljava/lang/Object;" +
			"Ljava/lang/String;" +
			"Ljava/lang/String;" +
			"Ljava/lang/String;" +
			"Ljava/lang/String;" +
			"Ljava/lang/Object;" +
			"[Ljava/lang/Object;" +
			")Ljava/lang/Object;";

	private static final String END_METHOD = "endAsyncPossibleService";
	private static final String END_METHOD_DESC = "(" +
			"Ljava/lang/Object;" +
			"Ljava/lang/Object;" +
			"Ljava/lang/Throwable;" +
			")V";

	private Label startFinally = new Label();

	private String name;
	private String desc;
	private Type[] paramTypes;
	private Type returnType;
	private String fullName;
	private int statIdx;
	private String className;


	public LambdaMV(int access, String name, String desc, MethodVisitor mv,
					String fullName, Type[] paramTypes, String className) {
		super(ASM9, access, desc, mv);
		this.name = name;
		this.desc = desc;

		this.fullName = fullName;
		this.paramTypes = paramTypes;
		this.returnType = Type.getReturnType(desc);
		this.className = className;
	}

	@Override
	public void visitCode() {
		int sidx = 1;
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		AsmUtil.PUSH(mv, fullName);
		AsmUtil.PUSH(mv, className);
		AsmUtil.PUSH(mv, name);
		AsmUtil.PUSH(mv, desc);
		mv.visitVarInsn(Opcodes.ALOAD, 0);

		int arrVarIdx = newLocal(Type.getType("[Ljava/lang/Object;"));
		AsmUtil.PUSH(mv, paramTypes.length);
		mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
		mv.visitVarInsn(Opcodes.ASTORE, arrVarIdx);

		for (int i = 0; i < paramTypes.length; i++) {
			Type tp = paramTypes[i];
			mv.visitVarInsn(Opcodes.ALOAD, arrVarIdx);
			AsmUtil.PUSH(mv, i);

			AsmUtil.loadForArrayElement(mv, tp, sidx);
			mv.visitInsn(Opcodes.AASTORE);
			sidx += tp.getSize();
		}
		mv.visitVarInsn(Opcodes.ALOAD, arrVarIdx);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, START_METHOD, START_METHOD_DESC,false);

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

class FacotoryMV extends LocalVariablesSorter implements Opcodes {
	private static final String TARGET = TraceMain.class.getName().replace('.', '/');
	private static final String END_METHOD = "asyncPossibleInstanceInitInvoked";
	private static final String END_METHOD_DESC = "(" +
			"Ljava/lang/Object;" +
			")V";

	String name;
	String desc;
	private Type returnType;

	public FacotoryMV(int access, String name, String desc, MethodVisitor mv) {
		super(ASM9, access, desc, mv);
		this.name = name;
		this.desc = desc;
		this.returnType = Type.getReturnType(desc);
	}

//	@Override
//	public void visitCode() {
//		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//		mv.visitLdcInsn("[factory method called!]" + name + desc);
//		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
//
//		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
//		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;", false);
//		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
//	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitInsn(DUP);

//			Type tp = returnType;
//			int i = newLocal(tp);
//			mv.visitVarInsn(Opcodes.ASTORE, i);
//			mv.visitVarInsn(Opcodes.ALOAD, i);
//			mv.visitVarInsn(Opcodes.ALOAD, i);

			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET, END_METHOD, END_METHOD_DESC, false);
		}
		mv.visitInsn(opcode);
	}
}


