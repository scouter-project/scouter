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
import org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.agent.trace.TraceSQL;

public class PsClearParametersMV extends LocalVariablesSorter implements Opcodes {
	private final static String TRACESQL = TraceSQL.class.getName().replace('.', '/');
	private final static String METHOD = "clear";
	private static final String SIGNATURE = "(Ljava/lang/Object;Lscouter/agent/trace/SqlParameter;)V";

	// /////////////////////////////////////////////////////////////////
	public PsClearParametersMV(int access, String desc, MethodVisitor mv, String owner) {
		super(ASM9,access, desc, mv);
		this.owner = owner;
	}

	private String owner;
	
	@Override
	public void visitCode() {
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, owner, TraceSQL.PSTMT_PARAM_FIELD, "Lscouter/agent/trace/SqlParameter;");
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, METHOD, SIGNATURE,false);
		super.visitCode();

	}

}
