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

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.Logger;
import scouter.agent.asm.util.AsmUtil;
import scouter.agent.trace.SqlParameter;
import scouter.agent.trace.TraceSQL;

/**
 * BCI for a constructor of PreparedStatement
 * @author @author Paul S.J. Kim(sjkim@whatap.io)
 * @author Gun Lee (gunlee01@gmail.com)
 */
public class PsInitMV extends LocalVariablesSorter implements Opcodes {

	private final static String TRACESQL = TraceSQL.class.getName().replace('.', '/');
	private final static String METHOD = "prepare";
	private final static String SIGNATURE = "(Ljava/lang/Object;Lscouter/agent/trace/SqlParameter;Ljava/lang/String;)V";

	private final static String METHOD_INIT = "stmtInit";
	private final static String SIGNATURE_INIT = "(Ljava/lang/Object;)V";

	private String owner;
	private int sqlIdx = -1;
    private boolean isUstatement = false;

	public PsInitMV(int access, String desc, MethodVisitor mv, String owner) {
		super(ASM9,access, desc, mv);
		this.owner = owner;
		this.sqlIdx = AsmUtil.getStringIdx(access, desc);

        if(this.sqlIdx < 0) {
            //CUBRID Case
            this.sqlIdx = AsmUtil.getIdxByType(access, desc, Type.getType("Lcubrid/jdbc/jci/UStatement;"));
            Logger.trace("CUBRID PSTMT LOAD - " + this.sqlIdx);
            this.isUstatement = true;
		}
	}

	@Override
	public void visitInsn(int opcode) {
		if (sqlIdx >= 0 && (opcode >= IRETURN && opcode <= RETURN)) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, METHOD_INIT, SIGNATURE_INIT, false);

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
			mv.visitVarInsn(ALOAD, sqlIdx);

            if(isUstatement) {
                mv.visitMethodInsn(INVOKEVIRTUAL, "cubrid/jdbc/jci/UStatement", "getQuery", "()Ljava/lang/String;", false);
            }

			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, METHOD, SIGNATURE,false);
		}
		mv.visitInsn(opcode);
	}

}
