/*
 *  Copyright 2016 Scouter Project.
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

package scouter.agent.batch.asm.jdbc;


import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import scouter.agent.batch.trace.TraceSQL;

public class RsNextMV extends MethodVisitor implements Opcodes {
	private static final String TRACESQL = TraceSQL.class.getName().replace('.', '/');
	private static final String METHOD = "addRow";
	private static final String SIGNATURE = "()V";

	public RsNextMV(String owner, MethodVisitor mv) {
		super(ASM5, mv);
		this.owner = owner;
	}
	private String owner;
	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, owner, TraceSQL.CURRENT_TRACESQL_FIELD, "Lscouter/agent/batch/trace/TraceSQL;");
			Label dump = new Label();
			mv.visitJumpInsn(IFNULL, dump);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, owner, TraceSQL.CURRENT_TRACESQL_FIELD, "Lscouter/agent/batch/trace/TraceSQL;");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, TRACESQL, METHOD, SIGNATURE,false);
			mv.visitLabel(dump);
		}
		mv.visitInsn(opcode);
	}
	
	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		mv.visitMaxs(maxStack + 4, maxLocals + 2);
	}	
}