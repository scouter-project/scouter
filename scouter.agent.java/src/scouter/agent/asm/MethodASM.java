/*
 *  Copyright 2015 LG CNS.
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

import java.util.List;

import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.MethodSet;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.trace.TraceMain;
import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.Label;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.Type;
import scouter.org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.util.HashUtil;
import scouter.util.StringSet;

public class MethodASM implements IASM, Opcodes {

	private List<MethodSet> target = MethodSet.getHookingMethodSet(Configure.getInstance().hook_method);

	public boolean isTarget(String className) {
		if (target.size() == 0)
			return false;

		for (int i = 0; i < target.size(); i++) {
			MethodSet mset = target.get(i);
			if (mset.classMatch.include(className)) {
				return true;
			}
		}
		return false;
	}

	Configure conf = Configure.getInstance();

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (target.size() == 0)
			return cv;

		if (conf.isIgnoreMethodClass(className))
			return cv;

		for (int i = 0; i < target.size(); i++) {
			MethodSet mset = target.get(i);
			if (mset.classMatch.include(className)) {
				return new MethodCV(cv, mset, className);
			}
		}
		return cv;
	}
}

class MethodCV extends ClassVisitor implements Opcodes {

	public String className;
	private MethodSet mset;

	public MethodCV(ClassVisitor cv, MethodSet mset, String className) {
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

		Configure conf = Configure.getInstance();
		boolean isPublic = conf.hook_method_access_public;
		boolean isProtected = conf.hook_method_access_protected;
		boolean isPrivate = conf.hook_method_access_private;
		boolean isNone = conf.hook_method_access_none;
		switch (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE)) {
		case Opcodes.ACC_PUBLIC:
			if (isPublic == false)
				return mv;
			break;
		case Opcodes.ACC_PROTECTED:
			if (isProtected == false)
				return mv;
			break;
		case Opcodes.ACC_PRIVATE:
			if (isPrivate == false)
				return mv;
			break;
		default:
			if (isNone == false)
				return mv;
			break;
		}
		// check prefix, to ignore simple method such as getter,setter
		if (conf.isIgnoreMethodPrefix(name))
			return mv;

		String fullname = AsmUtil.add(className, name, desc);
		int fullname_hash = DataProxy.sendMethodName(fullname);

		return new MethodMV(access, desc, mv, fullname, fullname_hash);
	}
}

class MethodMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
	private final static String START_METHOD = "startMethod";
	private static final String START_SIGNATURE = "(ILjava/lang/String;)Ljava/lang/Object;";
	private final static String END_METHOD = "endMethod";
	private static final String END_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Throwable;)V";

	private Label startFinally = new Label();

	public MethodMV(int access, String desc, MethodVisitor mv, String fullname, int fullname_hash) {
		super(ASM4, access, desc, mv);
		this.fullname = fullname;
		this.fullname_hash = fullname_hash;
	}

	private int fullname_hash;
	private String fullname;
	private int statIdx;

	@Override
	public void visitCode() {
		AsmUtil.PUSH(mv, fullname_hash);
		mv.visitLdcInsn(fullname);

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_METHOD, START_SIGNATURE, false);

		statIdx = newLocal(Type.getType(Object.class));

		mv.visitVarInsn(Opcodes.ASTORE, statIdx);
		mv.visitLabel(startFinally);
		mv.visitCode();
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitVarInsn(Opcodes.ALOAD, statIdx);
			mv.visitInsn(Opcodes.ACONST_NULL);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, END_METHOD, END_SIGNATURE, false);
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
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, END_METHOD, END_SIGNATURE, false);
		mv.visitInsn(ATHROW);
		mv.visitMaxs(maxStack + 8, maxLocals + 2);
	}
}