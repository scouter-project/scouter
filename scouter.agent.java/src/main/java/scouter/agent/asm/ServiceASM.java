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

import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.ClassDesc;
import scouter.agent.Configure;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.asm.util.HookingSet;
import scouter.agent.trace.TraceMain;
import scouter.lang.pack.XLogTypes;
import scouter.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceASM implements IASM, Opcodes {
	private List<HookingSet> target = HookingSet.getHookingMethodSet(Configure.getInstance().hook_service_patterns);
	private Map<String, HookingSet> reserved = new HashMap<String, HookingSet>();

	public static class ServiceTargetRegister {
        public static final List<Pair<String,String>> klassMethod = new ArrayList<Pair<String,String>>();
		public static void regist(String klass, String method) {
			klassMethod.add(new Pair<String, String>(klass, method));
		}
	}

	public ServiceASM() {
		for(int i = ServiceTargetRegister.klassMethod.size() - 1; i >= 0; i--) {
			AsmUtil.add(reserved, ServiceTargetRegister.klassMethod.get(i).getLeft(), ServiceTargetRegister.klassMethod.get(i).getRight());
		}
	}

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (Configure.getInstance()._hook_serivce_enabled == false) {
			return cv;
		}
        HookingSet mset = reserved.get(className);
        if (mset != null)
            return new ServiceCV(cv, mset, className, mset.xType == 0 ? XLogTypes.APP_SERVICE : mset.xType);

		for (int i = 0; i < target.size(); i++) {
			mset = target.get(i);
			if (mset.classMatch.include(className)) {
				return new ServiceCV(cv, mset, className, mset.xType == 0 ? XLogTypes.APP_SERVICE : mset.xType);
			}
		}
		return cv;
	}
}

class ServiceCV extends ClassVisitor implements Opcodes {

	public String className;
	private HookingSet mset;
	private byte xType;

	public ServiceCV(ClassVisitor cv, HookingSet mset, String className,byte xType) {
		super(ASM9, cv);
		this.mset = mset;
		this.className = className;
		this.xType=xType;
	}

    /**
     * @param access     the method's access flags (see {@link Opcodes}). This
     *                   parameter also indicates if the method is synthetic and/or
     *                   deprecated.
     * @param name       the method's name.
     * @param desc       the method's descriptor (see {@link Type Type}). (ex) (Ljava/lang/String;)Lorg/mybatis/jpetstore/domain/Category;
     * @param signature  the method's signature. May be <tt>null</tt> if the method
     *                   parameters, return type and exceptions do not use generic
     *                   types.
     * @param exceptions the internal names of the method's exception classes (see
     *                   {@link Type#getInternalName() getInternalName}). May be
     *                   <tt>null</tt>.
     * @return
     */
    @Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        //System.out.println("access = [" + access + "], name = [" + name + "], desc = [" + desc + "], signature = [" + signature + "], exceptions = [" + exceptions + "]");

		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (mv == null || mset.isA(name, desc) == false) {
			return mv;
		}
		if (AsmUtil.isSpecial(name)) {
			return mv;
		}

		String fullname = AsmUtil.makeMethodFullName(className, name, desc);
		return new ServiceMV(access, desc, mv, fullname,Type.getArgumentTypes(desc),(access & ACC_STATIC) != 0,xType,className,
				name, desc);
	}
}

class ServiceMV extends LocalVariablesSorter implements Opcodes {
	private static final String TRACEMAIN = TraceMain.class.getName().replace('.', '/');
	private final static String START_METHOD = "startService";
	private static final String START_SIGNATURE = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;B)Ljava/lang/Object;";
	private final static String END_METHOD = "endService";
	private static final String END_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Throwable;)V";

	private Label startFinally = new Label();
	private byte xType;

	public ServiceMV(int access, String desc, MethodVisitor mv, String fullname,Type[] paramTypes,
			boolean isStatic,byte xType,String classname, String methodname, String methoddesc) {
		super(ASM9, access, desc, mv);
		this.fullname = fullname;
		this.paramTypes = paramTypes;
		this.strArgIdx = AsmUtil.getStringIdx(access, desc);
		this.xType=xType;
		this.isStatic = isStatic;
		this.returnType = Type.getReturnType(desc);
		this.className = classname;
		this.methodName = methodname;
		this.methodDesc = methoddesc;
	}

	private Type[] paramTypes;
	private Type returnType;
	private String fullname;
	private int statIdx;
	private int strArgIdx;
	private boolean isStatic;
	private String className;
	private String methodName;
	private String methodDesc;
	@Override
	public void visitCode() {
		int sidx = isStatic ? 0 : 1;
		if (strArgIdx >= 0) {
			if (Configure.getInstance().hook_service_name_use_1st_string_enabled) {
				mv.visitVarInsn(Opcodes.ALOAD, strArgIdx);
			} else {
				AsmUtil.PUSH(mv, fullname);
			}
		} else {
			AsmUtil.PUSH(mv, fullname);
		}
		
		int arrVarIdx = newLocal(Type.getType("[Ljava/lang/Object;"));
		AsmUtil.PUSH(mv, paramTypes.length);
		mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
		mv.visitVarInsn(Opcodes.ASTORE, arrVarIdx);

		for (int i = 0; i < paramTypes.length; i++) {
			Type tp = paramTypes[i];
			mv.visitVarInsn(Opcodes.ALOAD, arrVarIdx);
			AsmUtil.PUSH(mv, i);

			switch (tp.getSort()) {
			case Type.BOOLEAN:
				mv.visitVarInsn(Opcodes.ILOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;",false);
				break;
			case Type.BYTE:
				mv.visitVarInsn(Opcodes.ILOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;",false);
				break;
			case Type.CHAR:
				mv.visitVarInsn(Opcodes.ILOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;",false);
				break;
			case Type.SHORT:
				mv.visitVarInsn(Opcodes.ILOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;",false);
				break;
			case Type.INT:
				mv.visitVarInsn(Opcodes.ILOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;",false);
				break;
			case Type.LONG:
				mv.visitVarInsn(Opcodes.LLOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;",false);
				break;
			case Type.FLOAT:
				mv.visitVarInsn(Opcodes.FLOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;",false);
				break;
			case Type.DOUBLE:
				mv.visitVarInsn(Opcodes.DLOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;",false);
				break;
			default:
				mv.visitVarInsn(Opcodes.ALOAD, sidx);
			}
			mv.visitInsn(Opcodes.AASTORE);
			sidx += tp.getSize();
		}
		
		AsmUtil.PUSH(mv, className);
		AsmUtil.PUSH(mv, methodName);
		AsmUtil.PUSH(mv, methodDesc);
		
		if (isStatic) {
			AsmUtil.PUSHNULL(mv);
		} else {
			mv.visitVarInsn(Opcodes.ALOAD, 0);
		}
		mv.visitVarInsn(Opcodes.ALOAD, arrVarIdx);
		
		AsmUtil.PUSH(mv, xType);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, START_METHOD, START_SIGNATURE,false);

		statIdx = newLocal(Type.getType(Object.class));
		mv.visitVarInsn(Opcodes.ASTORE, statIdx);
		mv.visitLabel(startFinally);
		mv.visitCode();
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
//			mv.visitVarInsn(Opcodes.ALOAD, statIdx);
//			mv.visitInsn(Opcodes.ACONST_NULL);
//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, END_METHOD, END_SIGNATURE);
			capReturn();
		}
		mv.visitInsn(opcode);
	}
	private void capReturn() {
		Type tp = returnType;

		if (tp == null || tp.equals(Type.VOID_TYPE)) {

			mv.visitVarInsn(Opcodes.ALOAD, statIdx);
			mv.visitInsn(Opcodes.ACONST_NULL);
			mv.visitInsn(Opcodes.ACONST_NULL);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, END_METHOD, END_SIGNATURE, false);
			return;
		}
		int i = newLocal(tp);
		switch (tp.getSort()) {
		case Type.BOOLEAN:
			mv.visitVarInsn(Opcodes.ISTORE, i);
			mv.visitVarInsn(Opcodes.ILOAD, i);

			mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
			mv.visitVarInsn(Opcodes.ILOAD, i);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
			mv.visitInsn(Opcodes.ACONST_NULL);// throwable
			break;
		case Type.BYTE:
			mv.visitVarInsn(Opcodes.ISTORE, i);
			mv.visitVarInsn(Opcodes.ILOAD, i);

			mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
			mv.visitVarInsn(Opcodes.ILOAD, i);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
			mv.visitInsn(Opcodes.ACONST_NULL);// throwable
			break;
		case Type.CHAR:
			mv.visitVarInsn(Opcodes.ISTORE, i);
			mv.visitVarInsn(Opcodes.ILOAD, i);

			mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
			mv.visitVarInsn(Opcodes.ILOAD, i);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;",
					false);
			mv.visitInsn(Opcodes.ACONST_NULL);// throwable
			break;
		case Type.SHORT:
			mv.visitVarInsn(Opcodes.ISTORE, i);
			mv.visitVarInsn(Opcodes.ILOAD, i);

			mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
			mv.visitVarInsn(Opcodes.ILOAD, i);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
			mv.visitInsn(Opcodes.ACONST_NULL);// throwable
			break;
		case Type.INT:
			mv.visitVarInsn(Opcodes.ISTORE, i);
			mv.visitVarInsn(Opcodes.ILOAD, i);

			mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
			mv.visitVarInsn(Opcodes.ILOAD, i);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
			mv.visitInsn(Opcodes.ACONST_NULL);// throwable
			break;
		case Type.LONG:
			mv.visitVarInsn(Opcodes.LSTORE, i);
			mv.visitVarInsn(Opcodes.LLOAD, i);

			mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
			mv.visitVarInsn(Opcodes.LLOAD, i);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
			mv.visitInsn(Opcodes.ACONST_NULL);// throwable
			break;
		case Type.FLOAT:
			mv.visitVarInsn(Opcodes.FSTORE, i);
			mv.visitVarInsn(Opcodes.FLOAD, i);

			mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
			mv.visitVarInsn(Opcodes.FLOAD, i);

			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
			mv.visitInsn(Opcodes.ACONST_NULL);// throwable
			break;
		case Type.DOUBLE:
			mv.visitVarInsn(Opcodes.DSTORE, i);
			mv.visitVarInsn(Opcodes.DLOAD, i);

			mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
			mv.visitVarInsn(Opcodes.DLOAD, i);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
			mv.visitInsn(Opcodes.ACONST_NULL);// throwable
			break;
		default:
			mv.visitVarInsn(Opcodes.ASTORE, i);
			mv.visitVarInsn(Opcodes.ALOAD, i);

			mv.visitVarInsn(Opcodes.ALOAD, statIdx);// stat
			mv.visitVarInsn(Opcodes.ALOAD, i);// return
			mv.visitInsn(Opcodes.ACONST_NULL);// throwable
		}

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, END_METHOD, END_SIGNATURE, false);
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
		mv.visitInsn(Opcodes.ACONST_NULL);// return
		mv.visitVarInsn(Opcodes.ALOAD, errIdx);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, END_METHOD, END_SIGNATURE,false);
		mv.visitInsn(ATHROW);
		mv.visitMaxs(maxStack + 8, maxLocals + 2);
	}
}
