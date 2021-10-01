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
import scouter.agent.trace.TraceSQL;

public class PsCloseMV extends MethodVisitor implements Opcodes {
	private static final String TRACESQL = TraceSQL.class.getName().replace('.', '/');
	private static final String METHOD = "stmtClose";
	private static final String SIGNATURE = "(Ljava/lang/Object;)V";

	public PsCloseMV(MethodVisitor mv) {
		super(ASM9, mv);
	}

	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, METHOD, SIGNATURE,false);
		}
		mv.visitInsn(opcode);
	}
}
