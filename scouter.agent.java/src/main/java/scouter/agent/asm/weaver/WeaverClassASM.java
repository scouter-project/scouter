package scouter.agent.asm.weaver;
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
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.IASM;
import scouter.agent.asm.util.AsmUtil;
import scouter.weaver.TraceSupportWeave;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 2021/10/21
 */
public class WeaverClassASM implements IASM, Opcodes {

	public static Set<String> weaveMethodNames = new HashSet<String>();

	@Override
	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance().hook_scouter_weaver_enabled == false) {
			return cv;
		}

		if ("scouterx/weaver/Scouter$Weaving".equalsIgnoreCase(className)) {
			TraceSupportWeave.touch();
			Method[] weaveMethods = TraceSupportWeave.class.getDeclaredMethods();
			for (int i = 0; i < weaveMethods.length; i++) {
				weaveMethodNames.add(weaveMethods[i].getName());
			}
			return new WeaverClassCV(cv, className);
		}
		return cv;
	}
}

class WeaverClassCV extends ClassVisitor implements Opcodes {

	String className;

	public WeaverClassCV(ClassVisitor cv, String className) {
		super(ASM9, cv);
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
		if (mv == null) {
			return mv;
		}
		if (AsmUtil.isSpecial(methodName)) {
			return mv;
		}

		return new WeaverClassMV(access, desc, mv, Type.getArgumentTypes(desc), className, methodName, desc);
	}
}

class WeaverClassMV extends LocalVariablesSorter implements Opcodes {

	private static final String CLASS = TraceSupportWeave.class.getName().replace('.', '/');
	private static final String SIGNATURE = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";

	private int statIdx;
	private Type returnType;

	private Type[] paramTypes;
	private String className;
	private String methodName;
	private String methodDesc;
	private String callMethod;

	public WeaverClassMV(int access, String desc, MethodVisitor mv, Type[] paramTypes, String classname, String methodname, String methoddesc) {
		super(ASM9, access, desc, mv);
		this.paramTypes = paramTypes;
		this.className = classname;
		this.methodName = methodname;
		this.methodDesc = methodDesc;
		this.callMethod = methodname;
		if (!WeaverClassASM.weaveMethodNames.contains(methodname)) {
			this.callMethod = "nothing";
		}
		this.returnType = Type.getReturnType(desc);
	}

	@Override
	public void visitCode() {
		int sidx = 0;

		int arrVarIdx = newLocal(Type.getType("[Ljava/lang/Object;"));
		AsmUtil.PUSH(mv, paramTypes.length);
		mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
		mv.visitVarInsn(Opcodes.ASTORE, arrVarIdx);

		for (int i = 0; i < paramTypes.length; i++) {
			Type type = paramTypes[i];
			mv.visitVarInsn(Opcodes.ALOAD, arrVarIdx);
			AsmUtil.PUSH(mv, i);

			switch (type.getSort()) {
				case Type.BOOLEAN:
					mv.visitVarInsn(Opcodes.ILOAD, sidx);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;",
							false);
					break;
				case Type.BYTE:
					mv.visitVarInsn(Opcodes.ILOAD, sidx);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
					break;
				case Type.CHAR:
					mv.visitVarInsn(Opcodes.ILOAD, sidx);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;",
							false);
					break;
				case Type.SHORT:
					mv.visitVarInsn(Opcodes.ILOAD, sidx);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
					break;
				case Type.INT:
					mv.visitVarInsn(Opcodes.ILOAD, sidx);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;",
							false);
					break;
				case Type.LONG:
					mv.visitVarInsn(Opcodes.LLOAD, sidx);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
					break;
				case Type.FLOAT:
					mv.visitVarInsn(Opcodes.FLOAD, sidx);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
					break;
				case Type.DOUBLE:
					mv.visitVarInsn(Opcodes.DLOAD, sidx);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
					break;
				default:
					mv.visitVarInsn(Opcodes.ALOAD, sidx);
			}
			mv.visitInsn(Opcodes.AASTORE);
			sidx += type.getSize();
		}
		AsmUtil.PUSH(mv, className);
		AsmUtil.PUSH(mv, methodName);
		AsmUtil.PUSH(mv, methodDesc);
		AsmUtil.PUSHNULL(mv); //all method is static
		mv.visitVarInsn(Opcodes.ALOAD, arrVarIdx);

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, CLASS, callMethod, SIGNATURE, false);
		statIdx = newLocal(Type.getType(Object.class));
		mv.visitVarInsn(Opcodes.ASTORE, statIdx);

		mv.visitCode();
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			if (returnType.getSort() != Type.VOID) {
				mv.visitVarInsn(Opcodes.ALOAD, statIdx);
			}
		}
		mv.visitInsn(opcode);
	}
}
