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

package scouter.agent.batch.asm.jdbc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.batch.Logger;
import scouter.agent.batch.asm.util.AsmUtil;
import scouter.agent.batch.trace.TraceContextManager;
import scouter.agent.batch.trace.TraceSQL;

/**
 * BCI for a constructor of PreparedStatement
 * @author @author Paul S.J. Kim(sjkim@whatap.io)
 * @author Gun Lee (gunlee01@gmail.com)
 */
public class PsInitMV extends LocalVariablesSorter implements Opcodes {
	private final static String TRACE = TraceContextManager.class.getName().replace('.', '/');
	private final static String METHOD = "getTraceSQL";
	private final static String SIGNATURE = "(Ljava/lang/String;)Lscouter/agent/batch/trace/TraceSQL;";

	private String owner;
	private int sqlIdx = -1;
    private boolean isUstatement = false;

	public PsInitMV(int access, String desc, MethodVisitor mv, String owner) {
		super(ASM5, access, desc, mv);
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
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, sqlIdx);
            if(isUstatement) {
    			mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKEVIRTUAL, "cubrid/jdbc/jci/UStatement", "getQuery", "()Ljava/lang/String;", false);
            }
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE, METHOD, SIGNATURE, false);
			mv.visitFieldInsn(PUTFIELD, owner, TraceSQL.CURRENT_TRACESQL_FIELD, "Lscouter/agent/batch/trace/TraceSQL;");
		}
		mv.visitInsn(opcode);
	}
}