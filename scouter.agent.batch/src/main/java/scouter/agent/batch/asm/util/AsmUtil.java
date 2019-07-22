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

package scouter.agent.batch.asm.util;


import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Map;

/**
 * BCI for a constructor of PreparedStatement
 * @author @author Paul S.J. Kim(sjkim@whatap.io)
 * @author Gun Lee (gunlee01@gmail.com)
 */
public class AsmUtil implements Opcodes {
	public static boolean isStatic(int access) {
		return (access & ACC_STATIC) != 0;
	}
	public static boolean isPublic(int access) {
		return (access & ACC_PUBLIC) != 0;
	}
	public static Type stringType = Type.getType(String.class);

	public static String makeMethodFullName(String className, String methodName, String methodSignature) {
		return new StringBuffer().append(className.replace('/','.')).append(".").append(methodName).append(methodSignature).toString();
	}

	public static void add(Map<String, HookingSet> classSet, String klass, String method) {
		klass=klass.replace('.', '/');
		HookingSet mset = classSet.get(klass);
		if (mset == null) {
			mset = new HookingSet();
			classSet.put(klass, mset);
		}
		mset.add(method);
	}

	public static void PUSH(MethodVisitor mv, int value) {
		if ((value >= -1) && (value <= 5)) // Use ICONST_n
			mv.visitInsn(ICONST_0 + value);
		else if ((value >= -128) && (value <= 127)) // Use BIPUSH
			mv.visitIntInsn(BIPUSH, value);
		else if ((value >= -32768) && (value <= 32767)) // Use SIPUSH
			mv.visitIntInsn(SIPUSH, value);
		else
			mv.visitLdcInsn(new Integer(value));
	}

	public static void PUSH(MethodVisitor mv, boolean value) {
		mv.visitInsn(ICONST_0 + (value ? 1 : 0));
	}

	public static void PUSH(MethodVisitor mv, float value) {
		if (value == 0.0)
			mv.visitInsn(FCONST_0);
		else if (value == 1.0)
			mv.visitInsn(FCONST_1);
		else if (value == 2.0)
			mv.visitInsn(FCONST_2);
		else
			mv.visitLdcInsn(new Float(value));
	}

	public static void PUSH(MethodVisitor mv, long value) {
		if (value == 0)
			mv.visitInsn(LCONST_0);
		else if (value == 1)
			mv.visitInsn(LCONST_1);
		else
			mv.visitLdcInsn(new Long(value));
		// LDC2_W
	}

	/**
	 * @param mv
	 * @param value
	 */
	public static void PUSH(MethodVisitor mv, double value) {
		if (value == 0)
			mv.visitInsn(DCONST_0);
		else if (value == 1)
			mv.visitInsn(DCONST_1);
		else
			mv.visitLdcInsn(new Double(value));
	}

	public static void PUSHNULL(MethodVisitor mv) {
		mv.visitInsn(ACONST_NULL);
	}

	public static void PUSH(MethodVisitor mv, String value) {
		if (value == null)
			mv.visitInsn(ACONST_NULL);
		else
			mv.visitLdcInsn(value);
	}

	public static void PUSH(MethodVisitor mv, Number value) {
		if ((value instanceof Integer) || (value instanceof Short) || (value instanceof Byte))
			PUSH(mv, value.intValue());
		else if (value instanceof Double)
			PUSH(mv, value.doubleValue());
		else if (value instanceof Float)
			PUSH(mv, value.floatValue());
		else if (value instanceof Long)
			PUSH(mv, value.longValue());
		else
			throw new RuntimeException("What's this: " + value);
	}

	public static void PUSH(MethodVisitor mv, Character value) {
		PUSH(mv, (int) value.charValue());
	}

	public static void PUSH(MethodVisitor mv, Boolean value) {
		PUSH(mv, value.booleanValue());
	}

	public static int getStringIdx(int access, String desc) {
        return getIdxByType(access, desc, AsmUtil.stringType);

		/*Type[] t = Type.getArgumentTypes(desc);
		int sidx = (AsmUtil.isStatic(access) ? 0 : 1);
		for (int i = 0; t != null && i < t.length; i++) {
			if (AsmUtil.stringType.equals(t[i])) {
				return sidx;
			}
			sidx += t[i].getSize();
		}
		return -1;*/
	}

    public static int getIdxByType(int access, String desc, Type type) {
        Type[] t = Type.getArgumentTypes(desc);
        int sidx = (AsmUtil.isStatic(access) ? 0 : 1);
        for (int i = 0; t != null && i < t.length; i++) {
            if (type.equals(t[i])) {
                return sidx;
            }
            sidx += t[i].getSize();
        }
        return -1;
    }

	public static boolean isSpecial(String name) {
		//return (name.indexOf("$") >= 0 && name.indexOf("lambda$") != 0) || name.startsWith("<");
		//FIXME for testing
		return name.startsWith("<");
	}

	public static boolean isInterface(int access) {
		return (access & ACC_INTERFACE) != 0;
	}

	public static void loadForArrayElement(MethodVisitor mv, Type tp, int sidx) {
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
	}
}
