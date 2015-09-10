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
 */

package scouter.agent.asm.jdbc;

import scouter.agent.asm.util.AsmUtil;
import scouter.agent.trace.SqlParameter;
import scouter.agent.trace.TraceSQL;
import scouter.org.objectweb.asm.Label;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.Type;
import scouter.org.objectweb.asm.commons.LocalVariablesSorter;

public class P0InitMV extends LocalVariablesSorter implements Opcodes {

	private final static String TRACESQL = TraceSQL.class.getName().replace('.', '/');
	private final static String METHOD = "prepare";
	private final static String SIGNATURE = "(Ljava/lang/Object;Lscouter/agent/trace/SqlParameter;Ljava/lang/String;)V";

	public P0InitMV(int access, String desc, MethodVisitor mv, String owner) {
		super(ASM4,access, desc, mv);
		this.owner = owner;
		this.strArgIdx = AsmUtil.getStringIdx(access, desc);

	}

	private String owner;
	private int strArgIdx = -1;

	@Override
	public void visitInsn(int opcode) {
		if (strArgIdx >= 0 && (opcode >= IRETURN && opcode <= RETURN)) {

			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, owner, TraceSQL.PSTMT_PARAM_FIELD, "Lscouter/agent/trace/SqlParameter;");

			Label end = new Label();
			mv.visitJumpInsn(IFNONNULL, end);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitTypeInsn(NEW, Type.getInternalName(SqlParameter.class));
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(SqlParameter.class), "<init>", "()V",false);
			mv.visitFieldInsn(PUTFIELD, owner, TraceSQL.PSTMT_PARAM_FIELD, "Lscouter/agent/trace/SqlParameter;");

			mv.visitLabel(end);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, owner, TraceSQL.PSTMT_PARAM_FIELD, "Lscouter/agent/trace/SqlParameter;");
			mv.visitVarInsn(ALOAD, strArgIdx);

			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, METHOD, SIGNATURE,false);
		}
		mv.visitInsn(opcode);
	}

}