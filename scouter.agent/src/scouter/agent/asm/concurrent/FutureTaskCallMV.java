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
 *
 */
package scouter.agent.asm.concurrent;

import scouter.agent.Logger;
import scouter.agent.trace.TraceFutureTask;
import scouter.org.objectweb.asm.Label;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.Type;
import scouter.org.objectweb.asm.commons.LocalVariablesSorter;


public class FutureTaskCallMV extends LocalVariablesSorter implements Opcodes {
	

	private final static String TRACEFUTURE = TraceFutureTask.class.getName().replace('.', '/');
	private final static String START_METHOD = "start";
	private final static String END_METHOD = "end";
	private static final String END_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Throwable;)V";
	private static final String START_SIGNATURE = "(Ljava/lang/Object;Lscouter/agent/trace/TraceContext;)Ljava/lang/Object;";

	public FutureTaskCallMV(int access, String desc, MethodVisitor mv, String owner) {
		super(ASM4,access, desc, mv);
		this.owner = owner;
		Logger.println("future: " +owner + ".call"+desc);
	}
	private Label startFinally = new Label();

	private String owner;
	private int statIdx;

	@Override
	public void visitCode() {
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 0);
	   mv.visitFieldInsn(GETFIELD, owner, TraceFutureTask.CTX_FIELD, "Lscouter/agent/trace/TraceContext;");
		
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEFUTURE, START_METHOD, START_SIGNATURE);

		statIdx = newLocal(scouter.org.objectweb.asm.Type.getType(Object.class));
		mv.visitVarInsn(Opcodes.ASTORE, statIdx);
		mv.visitLabel(startFinally);
		mv.visitCode();
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitVarInsn(Opcodes.ALOAD, statIdx);
			mv.visitInsn(Opcodes.ACONST_NULL);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEFUTURE, END_METHOD, END_SIGNATURE);
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
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEFUTURE, END_METHOD, END_SIGNATURE);
		mv.visitInsn(ATHROW);
		mv.visitMaxs(maxStack + 8, maxLocals + 2);
	}
}