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

import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceFutureTask;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.Type;
import scouter.org.objectweb.asm.commons.LocalVariablesSorter;

public class FutureTaskInitMV extends LocalVariablesSorter implements Opcodes {

	private final static String TRACEFUTURE = TraceFutureTask.class.getName().replace('.', '/');
	private final static String METHOD = "getContext";
	private final static String SIGNATURE = "()Lscouter/agent/trace/TraceContext;";

	public FutureTaskInitMV(int access, String desc, MethodVisitor mv, String owner) {
		super(ASM4,access, desc, mv);
		this.owner = owner;
	}

	private String owner;

	@Override
	public void visitInsn(int opcode) {
		if ( opcode >= IRETURN && opcode <= RETURN) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEFUTURE, METHOD, SIGNATURE,false);			
			mv.visitFieldInsn(PUTFIELD, owner, TraceFutureTask.CTX_FIELD, Type.getDescriptor(TraceContext.class));
		}
		mv.visitInsn(opcode);
	}

}