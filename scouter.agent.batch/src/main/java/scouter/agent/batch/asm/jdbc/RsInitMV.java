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
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.batch.trace.TraceContextManager;
import scouter.agent.batch.trace.TraceSQL;

/**
 * BCI for a constructor of Resultset
 * @author Gun Lee (gunlee01@gmail.com)
 */
public class RsInitMV extends LocalVariablesSorter implements Opcodes {

	private final static String TRACECONTEXTMANAGER = TraceContextManager.class.getName().replace('.', '/');
	private final static String METHOD = "getCurrentTraceSQL";
	private final static String SIGNATURE = "()Lscouter/agent/batch/trace/TraceSQL;";

	public RsInitMV(int access, String owner, String desc, MethodVisitor mv) {
		super(ASM5, access, desc, mv);
		this.owner = owner;
	}
	private String owner; 
	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= IRETURN && opcode <= RETURN)) {
	      mv.visitVarInsn(ALOAD, 0);
          mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACECONTEXTMANAGER, METHOD, SIGNATURE, false);
  	      mv.visitFieldInsn(PUTFIELD, owner, TraceSQL.CURRENT_TRACESQL_FIELD, "Lscouter/agent/batch/trace/TraceSQL;");   
		}
		mv.visitInsn(opcode);
	}
}