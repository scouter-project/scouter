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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceMain;

import java.util.List;


public class CapReturnASM implements IASM, Opcodes {
	private  List< HookingSet> target = HookingSet.getHookingMethodSet(Configure.getInstance().hook_return_patterns);

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance()._hook_cap_enabled == false) {
			return cv;
		}
		for (int i = 0; i < target.size(); i++) {
			HookingSet mset = target.get(i);
			if (mset.classMatch.include(className)) {
				return new CapReturnCV(cv, mset, className);
			}
		}
		return cv;
	}

}

// ///////////////////////////////////////////////////////////////////////////
class CapReturnCV extends ClassVisitor implements Opcodes {

	public String className;
	private HookingSet mset;

	public CapReturnCV(ClassVisitor cv, HookingSet mset, String className) {
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
		if(AsmUtil.isSpecial(name)){
			return mv;
		}		

		return new CapReturnMV(access, desc, mv, className, name, desc,(access & ACC_STATIC) != 0);
	}
}

// ///////////////////////////////////////////////////////////////////////////
class CapReturnMV extends LocalVariablesSorter implements Opcodes {
	private static final String CLASS = TraceMain.class.getName().replace('.', '/');
	private static final String METHOD = "capReturn";
	private static final String SIGNATURE = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V";

	private Type returnType;
	private String className;
	private String methodName;
	private String methodDesc;
	private boolean isStatic;
	
	public CapReturnMV(int access, String desc, MethodVisitor mv,
			String classname,
			String methodname,
			String methoddesc, boolean isStatic) {
		super(ASM9, access, desc, mv);
		this.returnType = Type.getReturnType(desc);
		this.className = classname;
		this.methodName = methodname;
		this.methodDesc = methoddesc;
		this.isStatic =  isStatic;

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
			pushCommon();
			mv.visitInsn(ACONST_NULL);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, CLASS, METHOD, SIGNATURE,false);
			return;
		}
		int i = newLocal(tp);
		switch (tp.getSort()) {
		case Type.BOOLEAN:
			mv.visitVarInsn(Opcodes.ISTORE, i);
			mv.visitVarInsn(Opcodes.ILOAD, i);

			pushCommon();
			mv.visitVarInsn(Opcodes.ILOAD, i);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;",false);
			break;
		case Type.BYTE:
			mv.visitVarInsn(Opcodes.ISTORE, i);
			mv.visitVarInsn(Opcodes.ILOAD, i);

			pushCommon();
			mv.visitVarInsn(Opcodes.ILOAD, i);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;",false);
			break;
		case Type.CHAR:
			mv.visitVarInsn(Opcodes.ISTORE, i);
			mv.visitVarInsn(Opcodes.ILOAD, i);

			pushCommon();
			mv.visitVarInsn(Opcodes.ILOAD, i);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;",false);
			break;
		case Type.SHORT:
			mv.visitVarInsn(Opcodes.ISTORE, i);
			mv.visitVarInsn(Opcodes.ILOAD, i);

			pushCommon();
			mv.visitVarInsn(Opcodes.ILOAD, i);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;",false);
			break;
		case Type.INT:
			mv.visitVarInsn(Opcodes.ISTORE, i);
			mv.visitVarInsn(Opcodes.ILOAD, i);

			pushCommon();
			mv.visitVarInsn(Opcodes.ILOAD, i);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;",false);
			break;
		case Type.LONG:
			mv.visitVarInsn(Opcodes.LSTORE, i);
			mv.visitVarInsn(Opcodes.LLOAD, i);

			pushCommon();
			mv.visitVarInsn(Opcodes.LLOAD, i);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;",false);
			break;
		case Type.FLOAT:
			mv.visitVarInsn(Opcodes.FSTORE, i);
			mv.visitVarInsn(Opcodes.FLOAD, i);

			pushCommon();
			mv.visitVarInsn(Opcodes.FLOAD, i);

			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;",false);
			break;
		case Type.DOUBLE:
			mv.visitVarInsn(Opcodes.DSTORE, i);
			mv.visitVarInsn(Opcodes.DLOAD, i);

			pushCommon();
			mv.visitVarInsn(Opcodes.DLOAD, i);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;",false);
			break;
		default:
			mv.visitVarInsn(Opcodes.ASTORE, i);
			mv.visitVarInsn(Opcodes.ALOAD, i);

			pushCommon();
			mv.visitVarInsn(Opcodes.ALOAD, i);
		}

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, CLASS, METHOD, SIGNATURE,false);
	}

	private void pushCommon() {
		AsmUtil.PUSH(mv, className);
		AsmUtil.PUSH(mv, methodName);
		AsmUtil.PUSH(mv, methodDesc);
		if (isStatic) {
			AsmUtil.PUSHNULL(mv);
		} else {
			mv.visitVarInsn(Opcodes.ALOAD, 0);
		}
	}
}
