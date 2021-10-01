/*
 *  Copyright 2015 Scouter Project.
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

package scouter.agent.asm.jdbc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.trace.TraceSQL;

import java.util.HashMap;
import java.util.Map;

public class PsSetMV extends LocalVariablesSorter implements Opcodes {

	private String owner;
	private String name;
	private Type[] args;
	private static Map<String, String> target = new HashMap<String, String>();

	static {
		target.put("setNull", "(II)V");

		target.put("setByte", "(IB)V");
		target.put("setBoolean", "(IZ)V");
		target.put("setShort", "(IS)V");
		target.put("setInt", "(II)V");
		target.put("setFloat", "(IF)V");
		target.put("setLong", "(IJ)V");
		target.put("setDouble", "(ID)V");

		target.put("setBigDecimal", "(ILjava/math/BigDecimal;)V");
		target.put("setBlob", "(ILjava/sql/Blob;)V");
		target.put("setClob", "(ILjava/sql/Clob;)V");
		target.put("setObject", "(ILjava/lang/Object;)V");
		target.put("setString", "(ILjava/lang/String;)V");
		target.put("setDate", "(ILjava/sql/Date;)V");
		target.put("setTime", "(ILjava/sql/Time;)V");
		target.put("setTimestamp", "(ILjava/sql/Timestamp;)V");
		target.put("setURL", "(ILjava/net/URL;)V"); //
	}

	public static String getSetSignature(String name) {
        return target.get(name);
	}

	private final static String TRACESQL = TraceSQL.class.getName().replace('.', '/');

	public PsSetMV(int access, String name, String desc, MethodVisitor mv, String owner) {
		super(ASM9,access, desc, mv);

		this.owner = owner;
		this.args = Type.getArgumentTypes(desc);
		this.name = name;
	}

	public void visitCode() {
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, owner, TraceSQL.PSTMT_PARAM_FIELD, "Lscouter/agent/trace/SqlParameter;");
		mv.visitVarInsn(Opcodes.ILOAD, 1);

		if (name.equals("setNull")) {
			AsmUtil.PUSH(mv, (String) null);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, "set", "(Lscouter/agent/trace/SqlParameter;ILjava/lang/String;)V",false);
		} else {
			Type tp = args[1];
			switch (tp.getSort()) {
			case Type.BOOLEAN:
				mv.visitVarInsn(Opcodes.ILOAD, 2);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, "set", "(Lscouter/agent/trace/SqlParameter;IZ)V",false);
				break;
			case Type.CHAR:
			case Type.BYTE:
			case Type.SHORT:
			case Type.INT:
				mv.visitVarInsn(Opcodes.ILOAD, 2);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, "set", "(Lscouter/agent/trace/SqlParameter;II)V",false);
				break;
			case Type.LONG:
				mv.visitVarInsn(Opcodes.LLOAD, 2);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, "set", "(Lscouter/agent/trace/SqlParameter;IJ)V",false);
				break;
			case Type.FLOAT:
				mv.visitVarInsn(Opcodes.FLOAD, 2);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, "set", "(Lscouter/agent/trace/SqlParameter;IF)V",false);
				break;
			case Type.DOUBLE:
				mv.visitVarInsn(Opcodes.DLOAD, 2);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, "set", "(Lscouter/agent/trace/SqlParameter;ID)V",false);
				break;
			case Type.ARRAY:
			case Type.OBJECT:
				mv.visitVarInsn(Opcodes.ALOAD, 2);
				if (tp.equals(AsmUtil.stringType)) {
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, "set", "(Lscouter/agent/trace/SqlParameter;ILjava/lang/String;)V",false);
				} else {
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, "set", "(Lscouter/agent/trace/SqlParameter;ILjava/lang/Object;)V",false);
				}
				break;
			default:
				mv.visitVarInsn(Opcodes.ALOAD, 2);
				AsmUtil.PUSH(mv, "unknown " + tp);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, "set", "(Lscouter/agent/trace/SqlParameter;ILjava/lang/String;)V",false);
				break;
			}
		}

		super.visitCode();
	}

}
