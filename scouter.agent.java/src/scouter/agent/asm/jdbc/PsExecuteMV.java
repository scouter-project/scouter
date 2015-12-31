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

import java.util.HashSet;
import java.util.Set;

import scouter.agent.asm.util.AsmUtil;
import scouter.agent.trace.TraceSQL;
import scouter.org.objectweb.asm.Label;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.Type;
import scouter.org.objectweb.asm.commons.LocalVariablesSorter;


public class PsExecuteMV extends LocalVariablesSorter implements Opcodes {
	private static Set<String> target = new HashSet<String>();
	static {
		target.add("execute");
		target.add("executeQuery");
		target.add("executeUpdate");
		target.add("executeBatch");
	}

	public static boolean isTarget(String name) {
		return target.contains(name);
	}

	private final static String TRACESQL = TraceSQL.class.getName().replace('.', '/');
	private final static String START_METHOD = "start";
	private static final String START_SIGNATURE = "(Ljava/lang/Object;Lscouter/agent/trace/SqlParameter;)Ljava/lang/Object;";
	private final static String END_METHOD = "end";
	private static final String END_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Throwable;I)V";

	public PsExecuteMV(int access, String desc, MethodVisitor mv, String owner) {
		super(ASM4,access, desc, mv);
		this.owner = owner;
		this.returnType = Type.getReturnType(desc);
	}
	private Label startFinally = new Label();

	private String owner;
	private int statIdx;
	private final Type returnType;

	@Override
	public void visitCode() {
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 0);
	    mv.visitFieldInsn(GETFIELD, owner, TraceSQL.PSTMT_PARAM_FIELD, "Lscouter/agent/trace/SqlParameter;");
		
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, START_METHOD, START_SIGNATURE,false);

		statIdx = newLocal(scouter.org.objectweb.asm.Type.getType(Object.class));
		mv.visitVarInsn(Opcodes.ASTORE, statIdx);
		mv.visitLabel(startFinally);
		mv.visitCode();
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {

			switch (returnType.getSort()) {
				case Type.BOOLEAN:
				case Type.INT:
					int i = newLocal(returnType);
					mv.visitVarInsn(Opcodes.ISTORE, i);
					mv.visitVarInsn(Opcodes.ILOAD, i);

					mv.visitVarInsn(Opcodes.ALOAD, statIdx);
					mv.visitInsn(Opcodes.ACONST_NULL);

					mv.visitVarInsn(Opcodes.ILOAD, i);

					if(returnType.getSort()== Type.BOOLEAN){
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, "toInt", "(Z)I",false);
					}
					break;
				default:
					mv.visitVarInsn(Opcodes.ALOAD, statIdx);
					mv.visitInsn(Opcodes.ACONST_NULL);
					AsmUtil.PUSH(mv,0);
			}


			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, END_METHOD, END_SIGNATURE,false);
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
		AsmUtil.PUSH(mv,0);

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, END_METHOD, END_SIGNATURE,false);
		mv.visitInsn(ATHROW);
		mv.visitMaxs(maxStack + 8, maxLocals + 2);
	}


}